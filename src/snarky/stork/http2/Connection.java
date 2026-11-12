package snarky.stork.http2;

import snarky.stork.Main;
import snarky.stork.http2.header.HeaderDecoder;
import snarky.stork.http2.header.HeaderEncoder;
import snarky.stork.http2.hpack.HeaderTable;
import snarky.stork.http2.in.*;
import snarky.stork.http2.out.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static snarky.stork.http2.FrameType.CONTINUATION;
import static snarky.stork.http2.SpecialValues.MAX_FRAME_SIZE;

// todo: use buffered stream somewhee ...
// todo: SETTINGS_MAX_HEADER_LIST_SIZE (0x6):
public class Connection {

    // todo: never send MORE THAN ONE settings frame .. one @ a time.
    // todo: remove string padding from other stuff move it somewhere else.
    // todo: 0 length header, allowed or nay ...?

    public enum ConnectionStat {
        CONT,
        NO_ACTION,
        INVALIDATED
    }
    
    private enum ConnectionStates {
        UN_INIT,
        ACTIVE,
        READY_TO_CLOSE,
        CLOSING,
        CLOSED
    } 

    private enum ReadStats {
        OK,
        OUTBOUND_ERROR,
        BAD_FRAME_SIZE,
        HEADER_BROKEN,
        BAD_SETTING,
        CREDIT_OVERFLOW,
        STATE_MISMATCH,
        GO_AWAY,
        BAD_ID,
        BAD_STREAM,
        NOT_ENOUGH_CREDIT,
        BAD_HEADER
    }

    private final Socket _socket;
    private final InputStream _ins;
    private final OutputStream _os;

    private final HeaderBuffer _headerBuffer = new HeaderBuffer();

    // controllers(the parents) -
    private final FlowController _inboundFC = new FlowController();
    private final FlowController _outboundFC = new FlowController();
    //
    private final FlowController.FlowWindow _inFw = _inboundFC.getFlowWin();
    private final FlowController.FlowWindow _outFw = _outboundFC.createFlowWindow();

    private final StreamManager _streamManager = new StreamManager(true, _inboundFC, _outboundFC);

    // todo: the below should be handled by out manager.
    private final FlowController _flowCtrl = new FlowController();

    private boolean _allowPush = true;

    private final HeaderEncoder _headerEncoder = new HeaderEncoder();
    private final HeaderTable _inHeaderTable = new HeaderTable();

    private final OutputManager _outManager = new OutputManager(_outboundFC);

    private int _peerMaxFrameSize = MAX_FRAME_SIZE;

    private int _lastStreamProcessed = 0; // apparently we can start @ 0.

    private final InboundDataProcessor _reader;

    // wait limit.
    // how long we should wait before we (soft) close the connection
    private final long _idleTimeLimit = 150000;//60000 * 30;
    // how long we should wait before we send the final GoAway
    private final long _postIdleTimeLimit = 25000;//60000;
    // how long to wait before we (hard) close the connection after ending the final go away.
    private final long _finalTimeOut = 10000;

    boolean _someActionPrev = false;
    // delay to improve responsiveness when there are few connections.
    long _noActionDelay = 750;

    private long _lastActionTime = 0;
    private long _closeRequestTime = 0;

    // Status/state markers?
    private ConnectionStates _connState;
    private ReadStats _readStat = ReadStats.OK;

    private final ByteArrayOutputStream _prefix;

    public Connection(Socket skt) throws IOException {
        this(skt, skt.getInputStream(), skt.getOutputStream());
    }

    public Connection(InputStream ins, OutputStream os) throws IOException {
        this(null, ins, os);
    }

    private Connection(Socket skt, InputStream ins, OutputStream os) throws IOException {
        _socket = skt;
        _ins = ins;
        _os = os;
        // todo: this should be set to the highest .. when we get started.
        _reader = new InboundDataProcessor(_streamManager, _inFw);
        _prefix = new ByteArrayOutputStream();
        if (skt == null) {
            _connState = ConnectionStates.ACTIVE;
        }
        else {
            _connState = ConnectionStates.UN_INIT;
            sendInitSettings();
        }
    }

    public ConnectionStat execute() {

        if (_connState == ConnectionStates.CLOSED) {
            return ConnectionStat.INVALIDATED;
        }

        // make sure we end the loop @ some point, if things go very wrong.
        if (_socket != null && _socket.isClosed()) {
            return ConnectionStat.INVALIDATED;
        }

        // Ok, .. we need to close the socket somewhere ..., or ..or ..
        try {
            final ConnectionStat rtn;
            switch (_connState) {
                case UN_INIT:
                    // read prefix will return whatever.
                    readConnectionPrefix();
                    rtn = ConnectionStat.CONT;
                    break;
                case ACTIVE:
                case READY_TO_CLOSE:
                case CLOSING:
                    // reading part
                    rtn = readWrite();
                    break;
                default:
                    throw new RuntimeException("Bad option");
            }
            return rtn;

        } catch (Exception e) {
            // catch all error - then we just close the connection. Errors we can't handle.
            try {
                // make absolutely and positively sure it's closed.
                if (_socket != null) {
                    _socket.close();
                }
            }
            catch (Exception e2) {
                // do nothing? Yes.
                // todo: log error ...
            }
            _connState = ConnectionStates.CLOSED;
            return ConnectionStat.INVALIDATED;
        }
    }

    private ConnectionStat readWrite() throws Exception {
        // the first time this is called
        // what if the peer sends nothing, how do we close?
        // so set the value, then business as usual.
        // if the peer sends something, the last action time will be overwritten
        if (_lastActionTime == 0) {
            _lastActionTime = System.currentTimeMillis();
        }

        // pre check - check if connection is waiting to be closed etc.
        final ConnectionStat cStat = checkConnection();
        if (cStat != ConnectionStat.CONT) {
            return cStat;
        }

        // we start with read stat set to ok.
        final boolean[] krw = keepReadingWriting();
        final boolean keepReading = krw[0];
        final boolean keepWriting = krw[1];

        boolean didRead = false;
        if (keepReading) {
            // read from reader, then process the frame read, or deal with errors.
            // here's where read status is set, if we don't read anymore, read status will not change.
            didRead = readInboundBytes();
            if (didRead) {
                // add inbound credit if there is a need.
                // tell peer how many more credit we can read. For stream, we add where we read data.
                addConnectionInboundCredit();
            }
            // once read fails the state will never change, so keep reading will always be false.
            // reading is done @ that point, we can still write.
        }

        /*
        ?? 22.11.2020 -> so we can just dump headers in write list right? do i manage state - I don't think I need to manahe
    '       state of out stream, in stream, i care about that, nbot out stream.
        // write
        */
        boolean didWrite;
        if (keepWriting) {
            if (_outManager.hasMore()) {
                byte[] b = _outManager.getBytes();
                if (b.length > 0) {
                    _os.write(b);
                    _os.flush();
                }
            }
            didWrite = _outManager.hasMore();
        }
        else {
            // can't write, no point in reading,
            // we can't tell client what we've processed. So, end the ??
            _connState = ConnectionStates.CLOSING;
            _closeRequestTime = System.currentTimeMillis();
            _socket.shutdownInput();
            return ConnectionStat.NO_ACTION;
        }

        return delaySleep(didRead || didWrite);
    }

    private ConnectionStat delaySleep(boolean someAction) throws IOException {

        final ConnectionStat rtnVal;
        if (someAction) {
            _lastActionTime = System.currentTimeMillis();
            rtnVal = ConnectionStat.CONT;
            _someActionPrev = true;
        }
        else {
            // nothing has happened.
            final boolean delay;
            if (_someActionPrev) {
                // there was an action previously ..;
                // delay or no delay
                delay = System.currentTimeMillis() < (_lastActionTime + _noActionDelay);
            }
            else {
                // previously this ..; no action I mean.
                delay = false;
            }

            if (delay) {
                rtnVal = ConnectionStat.CONT;
            }
            else {
                noAction();
                _someActionPrev = false;
                rtnVal = ConnectionStat.NO_ACTION;
            }
        }
        return rtnVal;
    }

    private void noAction() throws IOException {
        final int maxId = 2147483647;
        // no action, sleep for a while.
        // decide if we need to shut down the connection
        // if _state = 2, we'll never get here.
        if (_connState == ConnectionStates.ACTIVE) {
            // we have not sent go away frame to indicate that we wish to shut the connection.
            // send go away if the connection has been idle for ?
            final long t = System.currentTimeMillis() - _lastActionTime;
            if (t > _idleTimeLimit) {
                Main.w("Idle limit raeched ...");
                // some time has lapsed since the last action ..
                // out manager is empty here ..., otherwise we still have action.
                _outManager.close();
                writeOutGoAway(maxId);
                _connState = ConnectionStates.READY_TO_CLOSE;
                _closeRequestTime = System.currentTimeMillis();
                // this block will only be executed once because we set the _state.
            }
        }
        else if (_connState == ConnectionStates.READY_TO_CLOSE) {
            // wait until ter;s no more action then ...
            // _state #1.
            // we have sent a go away frame ?? our wish to shut the connection nicely.
            // grace period between first go away
            // if some time has lapsed - we send another go away
            final long t = System.currentTimeMillis() - _closeRequestTime;
            if (t > _postIdleTimeLimit) {
                Main.w("Post idle ... ...");
                // write out everything first ...
                // then add go away - after that we don't want to write anything else.
                // todo: we may want to limit how long we should wait.
                //sendGoAway(HTTP2ErrorCodes.NO_ERROR, "", _lastStreamProcessed); ?? definitely do not want.
                // ending .. definitely.
                //?? wjhat rthe hell is this shit?
                writeOutGoAway(_lastStreamProcessed);
                _connState = ConnectionStates.CLOSING;
                _closeRequestTime = System.currentTimeMillis();
            }
        }
    }

    private void addConnectionInboundCredit() {
        final FlowController.FlowWindow flowWin = _inboundFC.getFlowWin();
        final long creditToAdd = SpecialValues.MAX_WINDOW_SIZE - flowWin.getCredit();
        
        if (creditToAdd > 0) {
            // 0 is illegal value. Don't send 0.
            _outManager.put(new WindowUpdateMaker(creditToAdd));
            _inboundFC.getFlowWin().addCredit(creditToAdd);
        }
    }

    private void addStreamInboundCredit(Stream stream) {
        final FlowController.FlowWindow flowWin = stream.getInFlowWin();
        final long creditToAdd = SpecialValues.MAX_WINDOW_SIZE - flowWin.getCredit();

        if (creditToAdd > 0) {
            // 0 is illegal value. Don't send 0.
            _outManager.put(new WindowUpdateMaker(stream, creditToAdd));
            flowWin.addCredit(creditToAdd);
        }
    }

    private ConnectionStat checkConnection() throws IOException {
        // if state = closed, this one shouldn't be called.
        // no reading, no writing ..
        if (_connState == ConnectionStates.CLOSING) {

            final long t = System.currentTimeMillis() - _closeRequestTime;
            if (t > _finalTimeOut) {
                Main.w("Clozing connection ......");
                _socket.close();
                _connState = ConnectionStates.CLOSED;
                return ConnectionStat.INVALIDATED;
            }
            else {
                return ConnectionStat.NO_ACTION;
            }
        }
        else {
            return ConnectionStat.CONT;
        }
    }

    // Write out Go Away frame immediately.
    private void writeOutGoAway(int lastId) throws IOException {
        // have to make sure that the output manager thing is closed, ...
        // and empty. .. We don't want to accidentally send data in between stuff that have to
        // be send continuously.
        final GoAwayMaker gam = new GoAwayMaker(lastId, ErrorCodes.NO_ERROR, "");
        final OutputData ods = gam.getDataOut(_peerMaxFrameSize);
        _os.write(ods.getHeader());
        _os.write(ods.getPayload());
        _os.flush();
    }

    private boolean[] keepReadingWriting() {

        final boolean keepReading;
        final boolean keepWriting;

        switch (_readStat) {
            // -V- Input errors. We can write out what we have processed.
            case NOT_ENOUGH_CREDIT:
                // not enough credit. Keep writing, writing uses peer credit. We're good.
            case BAD_FRAME_SIZE:
                // wrong frame size by peer. Continue write, stop read.
            case HEADER_BROKEN:
                // header/continuation don't follow one another. Continue write, end read.
            case BAD_SETTING:
                // peers sends invalid value(s) in setting frame. Continue write, end read.
            case CREDIT_OVERFLOW:
                // init size causes illegal credit value.
            case STATE_MISMATCH:
                // wrong frame + wrong state. Write, stop reading.
            case BAD_ID:
                // invalid id is used - stream frame has 0 id, conn frame has non-0 id.
            case BAD_HEADER:
                // header decompression failed.
            case BAD_STREAM:
            case GO_AWAY:
                // peer sends go away, no error - remember not to process the request.
                keepReading = false;
                keepWriting = true;
                break;
            // -V- No more out, no point in reading because we can't process input. We're not allowed
            // to write ..
            case OUTBOUND_ERROR:
                // bad output by us. End write, end read too. There's no point to continue
                // reading we can't send output. So, we can't process shit.
                keepReading = false;
                keepWriting = false;
                break;
            case OK:
            default:
                // we're ok.
                keepReading = true;
                keepWriting = true;
                break;
        }
        return new boolean[]{keepReading, keepWriting};
    }

    private void readConnectionPrefix() throws IOException {

        while (_prefix.size() < 24) {
            try {
                _socket.setSoTimeout(15);
                final int i = _ins.read();
                if (i >= 0) {
                    _prefix.write(i);
                } 
                else {
                    // end of stream. error, ..
                    // change to close.
                    _connState = ConnectionStates.CLOSED;
                    return;
                }
            } catch (java.net.SocketTimeoutException ex) {
                // timeout.
                // we're not done yet.
                // maintain current state which is UN_INIT.
                return;
            }
        }

        // if we're here, then we've read the prefix.
        final int prefixHash = -55355989; // hash from hash function, ...
        final int hash = Arrays.hashCode(_prefix.toByteArray());
        if (hash == prefixHash) {
            _connState = ConnectionStates.ACTIVE;
            //todo: set here once.
            //todo: 22/11/2020 - set timeout onec here?
            //todo: 3/1/2021 - I mean, set the connection timout here; once don't call it over &
            //  over - we wsitch to timeout = 1 now, ok?
            _socket.setSoTimeout(1);
        }
        else {
            rejectConnection();
            // bad prefix - soes, closed, if ?? is called, it'll return invalidated.
            _connState = ConnectionStates.CLOSED;
        }
    }

    private void rejectConnection() throws IOException {

        final GoAwayMaker gam;
        gam = new GoAwayMaker(0, ErrorCodes.PROTOCOL_ERROR, "");
        _outManager.put(gam);

        // todo: send init settings...
        // 19.12.21 ^^^ ?? wtf? Copy + paste? From send init?
        byte[] b;
        int safetyCounter = 0;
        do {
            b = _outManager.getBytes();
            _os.write(b);

            if (safetyCounter++ > 1000) {
                throw new IOException();
            }
        } while (b.length > 0);
        _os.flush();
    }

    private boolean readInboundBytes() throws IOException {

        /*
        if (_socket != null) {
            // todo: what's the proper time?
            // todo: I may need to create a class that can read stuff from input stream, using ->
            // * use multiple threads
            // * use time out too, but use multiple thread to return immediately.. leave somme threads running.
            //_socket.setSoTimeout(1);
            ?? may not be necessary to set @ each call.
            Main.w("????? ", _socket.getSoTimeout());
        }
        */

        // todo: is this krayzee?
        // todo: set buffer == tcp buffer, like .. do we have soecket here?
        final byte[] buffer = new byte[1024 * 30];
        byte[] b;
        int byteRead;
        try {
            byteRead = _ins.read(buffer);
            if (byteRead < 0) {
                b = new byte[0];
            }
            else {
                b = Arrays.copyOfRange(buffer, 0, byteRead);
            }
        }
        catch (SocketTimeoutException ste) {
            byteRead = 0;
            b = new byte[0];
        }

        final DetReturn<InboundDataProcessor.Stat, Frame> rslt = _reader.put(b);
        final InboundDataProcessor.Stat det = rslt.getDeterminant();

        switch (det) {
            case OK:
                processFrame(rslt.getValue());
                break;
            case NOT_YET:
                // not yet do nothing .. because we don't have enough data.
                break;
            case BAD_STREAM:
                queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
                _readStat = ReadStats.BAD_STREAM;
                break;
            case CONN_OVERFLOW:                
            case STREAM_OVERFLOW:
                queueGoAway(ErrorCodes.FLOW_CONTROL_ERROR, "", _lastStreamProcessed);
                _readStat = ReadStats.NOT_ENOUGH_CREDIT;
                break;
            case BAD_ID:
                queueGoAway(ErrorCodes.PROTOCOL_ERROR, "Bad id", _lastStreamProcessed);
                _readStat = ReadStats.BAD_ID;
                break;
            case FRAME_TOO_LARGE:
                queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
                _readStat = ReadStats.BAD_FRAME_SIZE;
                break;
            default:
                throw new RuntimeException("Bad stat.");
        }
        // todo: we need has more, why? So we don't stall?
        return byteRead > 0 || _reader.hasMore();
    }

    private void sendInitSettings() throws IOException {

        final SettingsMaker sm;
        sm = new SettingsMaker(Collections.emptyList(), false);
        _outManager.put(sm);
        // todo: send init settings...
        byte[] b;
        int safetyCounter = 0;
        do {
            b = _outManager.getBytes();
            System.out.println(Arrays.toString(b));

            if (b.length == 0)
            {
                break;
            }

            _os.write(b);

            if (safetyCounter++ > 1000) {
                throw new IOException();
            }
        } while (true);
        _os.flush();
    }

    private void processFrame(Frame fm) throws IOException {

        Main.w("Process frame -> ", fm.getType());
        // @ this point the frame is ok, valid ... content may not be valid, but the
        // frame itself is a'ok.
        switch (fm.getType()) {
            case SETTINGS:
                procFrame_Settings(fm);
                break;
            case PING:
                procFrame_Ping(fm);
                break;
            case GO_AWAY:
                procFrame_GoAway(fm);
                break;
            case WINDOW_UPDATE:
                if (fm.getStreamId() == 0) {
                    procFrame_ConWinUpdate(fm);
                }
                else {
                    processStreamFrame(fm);
                }
                break;
            // stream frames
            case DATA:
            case HEADERS:
            case PRIORITY:
            case RST_STREAM:
            case PUSH_PROMISE:
            case CONTINUATION:
                processStreamFrame(fm);
                break;
            default:
                //todo: is this how we process this? Ignore?
                if (_headerBuffer.readingHeader()) {
                    queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
                    _readStat = ReadStats.HEADER_BROKEN;
                }
                break;
        }
    }

    private void processStreamFrame(Frame fm) throws IOException {

        final DetReturn<Integer, Stream> sgt = _streamManager.getStream(fm.getStreamId());
        if (sgt.getDeterminant() != -1) {
            // todo: handle error ...
            _readStat = ReadStats.BAD_ID;
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
            return;
        }

        final Stream stream = sgt.getValue();

        final ErrorCodes erc = checkIfReadAllowed(stream, fm.getType());
        // todo: here or somewhere else, lazy or eager?
        if (erc == ErrorCodes.NO_ERROR) {
            processStreamFrameStage2(stream, fm);
        }
        else {
            _readStat = ReadStats.STATE_MISMATCH;
            // we send protocol error.
            queueGoAway(erc, "", _lastStreamProcessed);
        }

        if (_readStat == ReadStats.OK) {
            _lastStreamProcessed = stream.getId();
        }
    }

    private ErrorCodes checkIfReadAllowed(Stream stream, FrameType ft) {

        final ErrorCodes er;

        switch (stream.getState()) {
            case IDLE:
                er = ReadPermissionChecker.idle(ft);
                break;
            case RESERVED_LOCAL:
                er = ReadPermissionChecker.reservedLocal(ft);
                break;
            case RESERVED_REMOTE:
                er = ReadPermissionChecker.reservedRemote(ft);
                break;
            case OPEN:
                er = ReadPermissionChecker.open(ft);
                break;
            case HALF_CLOSED_LOCAL:
                er = ReadPermissionChecker.halfCloseLocal(ft);
                break;
            case HALF_CLOSED_REMOTE:
                er = ReadPermissionChecker.halfClosedRemote(ft);
                break;
            case CLOSED_BY_US:
                final long closingTime = stream.getClosingTime();
                // if it's less than some time - then we treat this as half-closed remote.
                // we can only get into this state if the stream state is half-closed remote.
                // else treat as closed.
                if (System.currentTimeMillis() - closingTime < 16000) {
                    er = ReadPermissionChecker.halfClosedRemote(ft);
                }
                else {
                    er = ReadPermissionChecker.closed(ft);
                }
                break;
            case RESET_BY_PEER:
                // peer sent reset stream.
            case CLOSED_BY_PEER:
                // peer sends 'end stream' on half-closed stream, should know that this will
                // cause the stream to transition into closed state, no?
                er = ReadPermissionChecker.closed(ft);
                break;
            default:
                throw new RuntimeException("Wrong state.");
        }
        return er;
    }

    private void processStreamFrameStage2(Stream stream, Frame fm) throws IOException {

        // We're reading header block.
        if (_headerBuffer.readingHeader()) {
            // if we're currently reading header block, we can't read anything else.
            if (fm.getType() != CONTINUATION) {
                queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
                _readStat = ReadStats.HEADER_BROKEN;
                return;
            }
            procFrame_Continuation(stream, fm);
        }
        // not reading headers .. anything goes
        // this block will only run when we're not reading header ..
        else {
            // todo: push promise?? Push promise is like HEADERS - can be followed ..
            switch (fm.getType()) {
                case DATA: // must be stream, we channel this to stream io, out .. or something.
                    procFrame_Data(stream, fm);
                    break;
                case HEADERS: // stream, header stuff
                    procFrame_Headers(stream, fm);
                    break;
                case PRIORITY: // stream - re prioritize stream
                    procFrame_Priority(stream, fm);
                    break;
                case RST_STREAM: // stream, no idle..., kill stream ...
                    procFrame_ResetStream(stream, fm);
                    break;
                case WINDOW_UPDATE: //BOTH ...
                    procFrame_StreamWinUpdate(stream, fm);
                    break;
                case CONTINUATION:
                    // Can't receive CONT when we're not reading HEADERS
                    // CONT has to be preceded by HEADERS or CONT frame.
                    _readStat = ReadStats.HEADER_BROKEN;
                    queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
                    break;
                case PUSH_PROMISE:
                    throw new RuntimeException("Nawts implemented yets...");
                default:
                    // todo: error or ignore?
                    // CONNECTION ERROR
                    // protocol error
                    // is the user sends some shit that we can't handle.
                    break;
            }

            //??
        }
    }

    private void procFrame_Headers(Stream stream, Frame fm) throws IOException {

        // read header...
        final byte[] cont = fm.getData();
        // header data has to be processed first ... because it contains other stuff

        final HeadersReader hdr = new HeadersReader(fm, cont);
        if (hdr.getReturnCode() != -1) {
            // bad length ..
            // applies if padding is used.
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }
        //
        _headerBuffer.register(fm, hdr, hdr.getHeaderBlock());

        // if it's full, it's time to handle the headers.
        // else just run it.
        if (fm.getFlag2()) {
            processRequest(stream);
        }

        /*
        // todo: we can call isEndStream from header reader.
        // end stream, ...
        if (fm.getFlag0()) {
            //<-- end stream, so no data body to read ... what if this is the other headers?
            //?? This has to be somethign to do with strea,.
            endStreamByRead(stream);
            processServerResponse(stream);
        }
         */
    }

    private void procFrame_Continuation(Stream stream, Frame fm) throws IOException {

        final boolean ok = _headerBuffer.addDataBlock(fm, fm.getData());

        if (!ok) {
            // from different stream ..
            // todo: from different stream error ...
            _readStat = ReadStats.HEADER_BROKEN;
            return;
        }

        if (fm.getFlag2()) {
            // end headers is set - so process it
            processRequest(stream);
        }
        // end header is set here
        // if end header
        // process the stuff
        // else
        // push into ..
    }

    private void processRequest(Stream stream) throws IOException {
        // decode the header here ...
        // we have all the blocks.
        if (stream.getState() == StreamStates.IDLE) { // todo: $$ --?? --- naw'z'
            stream.setState(StreamStates.OPEN);
        }

        // push promise stuff
        if (stream.getState() == StreamStates.RESERVED_REMOTE) {
            stream.setState(StreamStates.HALF_CLOSED_LOCAL);
        }

        final FrameMeta fm = _headerBuffer.getHeader();
        // todo: process priority etc.
        final byte[] headerBlock = _headerBuffer.getHeaderBlocks();
        // todo: process header block here ...
        // todo: actual header table ..
        final HeaderDecoder hdc = new HeaderDecoder(headerBlock, _inHeaderTable);

        for (Header d : hdc.getHeaderList().getValue())
        {
            if (d.key.matches(".*\\p{Lu}.*"))
            {
                try
                {
                    _socket.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                _connState = ConnectionStates.CLOSED;
                return;
            }
        } -->>

        final DetReturn<ErrorResponse, List<Header>> rtn = hdc.getHeaderList();
        if (rtn.getDeterminant().getErrorCode() != ErrorCodes.NO_ERROR) {
            // todo : do something - failure
        }

        boolean expect = false;
        for (Header h : rtn.getValue()) {
            if ("Expect".equalsIgnoreCase(h.key)) {
                expect = "100-continue".equalsIgnoreCase(h.value);
                break;
            }
        }

        // todo: Check if we need to send Continue-X.
        if (expect) {
            sendExpectResponse(stream);
        }

        // reset out temporary header store, since we're done.
        _headerBuffer.reset();

        // read the header to figure out how we should process the body
        // could be the other one, Stream should handle this.
        processRequestHeader(stream, hdc);
        if (_readStat != ReadStats.OK) {
            return;
        }

        // End stream ...
        if (fm.getFlag0()) {
            //<-- end stream, so no data body to read ... what if this is the other headers?
            //?? This has to be something to do with stream
            endStreamByRead(stream);
            respond(stream);
        }
        // todo: $$
        // << no need to flag end stream meh?
        // <<_--- here?
    }

    private boolean checkValidityOfHeaders(HeaderDecoder hdc)
    {
        return true;

    }

    private void procFrame_Data(Stream stream, Frame fm) {
        //final DataRtn dr = Data.read(fm, readKontent(fm));
        final DataReader data = new DataReader(fm, fm.getData());

        if (data.getReturnCode() != -1) {
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }

        // todo: push data into stream here - ok, biatchi?
        stream.write(data.getData());

        if (fm.getFlag0()) {
            // end stream (flag 0) - state transition.
            // todo: state transition ..!
            endStreamByRead(stream);
            // btw. End stream flag can be carried by either DATA or HEADERS frame
            // H + ES = no content body
            // H, H + ES = No content body + trailing header? Is it ok?
            // H, D + ES = +content body
            // H, D, H + ES = +content body +end headers.
            respond(stream);
        }
        else {
            // Good place to do this.
            // We update credit as long as the peer has data to send.
            addStreamInboundCredit(stream);
        }

        /*
        A receiver that receives a flow-controlled frame MUST always account for its
        contribution against the connection flow-control window, unless the receiver
        treats this as a connection error (Section 5.4.1). This is necessary even if
        the frame is in error. The sender counts the frame toward the flow-control window,
        but if the receiver does not, the flow-control window at the sender and receiver can become different.
         */
    }

    private void procFrame_Priority(Stream stream, Frame fm) {

        if (fm.getLength() != 5) {
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }

        final byte[] data = fm.getData();
        final PriorityReader prt = new PriorityReader(data);
        // todo: prochess ... this.
    }

    private void procFrame_ResetStream(Stream stream, Frame fm) {

        if (fm.getLength() != 4) {
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }
        // think here we change state,...
        // todo: just kill the stream ...
        final byte[] data = fm.getData();
        final ResetStreamReader rst = new ResetStreamReader(data);

        // else we're good.
        final StreamStates ss = stream.getState();
        final boolean ignore = (ss == StreamStates.RESET_BY_US) || (ss == StreamStates.CLOSED_BY_US);
        if (!ignore) {
            // todo: not satisfied with this check again
            stream.setState(StreamStates.RESET_BY_PEER);
        }
        Main.w(rst.getErrorCode(), " / ", fm.getStreamId());
    }

    private void procFrame_Settings(Frame fm) {
        // iti window size changes the size of all I think ...
        // say 001 has 16,000 credit, .. wait, this makes no sense. the difference between? .. between actual init and
        // ?? ..
        // init 60
        // we're going to set @ 16
        // the other guy sends 60
        // we can only accept 16 max, we get 60, ... we got 44 more than what we can handle.
        // let's suppose the user sends 25 instead.
        // 60 - init
        // 25 - rec.
        // 35 - balance...
        // set to 16
        // 16
        // 25 rec
        // 9 <-- bytes off.
        if (fm.getLength() % 6 != 0) {
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            // error - it has to be a multiple of 6 bytes.
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }

        final byte[] data = fm.getData();
        final SettingsReader setting = new SettingsReader(fm, data);

        // acknowledgement - process this.
        // acknowledgement does not accept payload, so length has to be 0.
        if (setting.isAck()) {
            if (fm.getLength() > 0) {
                queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
                _readStat = ReadStats.BAD_FRAME_SIZE;
            }
            else {
                // process
                // todo: process ///
            }
            return;
        }

        for (Setting s : setting.getSettings()) {

            final long l = s.getValue();

            Main.w(s);

            switch (s.getSetting()) {
                case HEADER_TABLE_SIZE:
                    // set @ header.
                    set_HeaderTableSize(l);
                    break;
                case ENABLE_PUSH:
                    set_EnablePush(l);
                    break;
                case MAX_CONCURRENT_STREAMS:
                    // just set ... 0 is acceptable.
                    set_MaxConcurrentStreams(l);
                    break;
                case INITIAL_WINDOW_SIZE:
                    set_InitWindowSize(l);
                    break;
                case MAX_FRAME_SIZE:
                    set_MaxFrameSize(l);
                    break;
                case MAX_HEADER_LIST_SIZE:
                    // check later ..
                    // this is the size of the data sent in teh header block
                    // todo: do this...
                    set_MaxHeaderListSize(l);
                    break;
                case X_UNKNOWN:
                default:
                    // 2 are not
                    // unknown settings are supposed to be ignored.
                    break;
            }
        }

        if (_readStat == ReadStats.OK) {
            _outManager.put(new SettingsMaker(Collections.emptyList(), true));
            //
            // ???
            final long[] maxSizes = _headerEncoder.getHeaderTable().getMaxSizes();
            if (maxSizes[0] > maxSizes[1]) {
                _headerEncoder.reduceSize((int) maxSizes[1]);
            }
        }
    }

    private void set_HeaderTableSize(long l) {
        // todo: how is this different from? From the one you send with headers?
        // encoder sends that .. we encode and we decode. For out data, we hold the encoder, the other guy
        // hold the decoder. - We send header with that, what does that tell?
        // what the peer tells us that maximum, we can change the size on the ?? .. why is that? We can set to 0 to
        // flush.
        Main.w("Maz able size set to -> ", l);
        _headerEncoder.getHeaderTable().setMaxSize(l);
    }

    private void set_MaxConcurrentStreams(long l) {
        // todo: later - this tricky.
    }

    private void set_MaxHeaderListSize(long l) {
        // todo: encoder right?
        // where do we check this?
        // WHEN? Before we send, when we receive.
    }

    private void set_MaxFrameSize(long l) {
        /*
        The initial value is 2^14 (16,384) octets.
        The value advertised by an endpoint MUST
        be between this initial value and the maximum allowed frame size
        ((2^24)-1 or 16,777,215 octets), inclusive. Values outside this range MUST be
        treated as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
         */

        long min = 16384; // 2^14
        long max = 16777215; // 2^24-1
        boolean x = l >= min && l <= max;

        if (x) {
            _outManager.setFrameSize((int) l);
        }
        else {
            // todo: PROTOCOL_ERROR. Connection error.
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "Invalid frame size value.", _lastStreamProcessed);
            _readStat = ReadStats.BAD_SETTING;
        }
    }

    private void set_EnablePush(long l) {
        if (l == 0) {
            _allowPush = false;
        }
        else if (l == 1) {
            _allowPush = true;
        }
        else {
            // todo: PROTOCOL_ERROR. Connection error.
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "Invalid value", _lastStreamProcessed);
            _readStat = ReadStats.BAD_SETTING;
        }
    }

    private void set_InitWindowSize(long l) {

        // todo: check l value ...
        final FlowController.FlowWindow sub = _outManager.getPeerFlowWin();
        final boolean b0 = sub.canSetInitSize(l);
        final boolean b1 = !_streamManager.outOverflow(l);

        if (!(b0 && b1)) {
            queueGoAway(ErrorCodes.FLOW_CONTROL_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.CREDIT_OVERFLOW;
        }
        _outManager.getPeerFlowCtrl().setInitWindowSize(l);
    }

    private void procFrame_Ping(Frame fm) {

        // todo: what the fucking fuck why is it wrong ...
        if (fm.getLength() != 8) {
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return;
        }

        // todo: ...
        if (fm.getFlag0()) { // acknowledgement - we sent a ping before, and this is ack.
            // todo: ignore for now ... We're not sending ping.
        }
        else {
            // todo: korrect?
            // request - we need to reply.
            final PingMaker pm = new PingMaker(fm.getData());
            _outManager.put(pm);
        }
    }

    private void procFrame_GoAway(Frame fm) {
        //??

        try
        {
            _socket.close();
        }
        catch (Exception e)
        {

        }
        _connState = ConnectionStates.CLOSED;

        /*
        ??
        if (fm.getData().length < 8) {
            _readStat = ReadStats.BAD_FRAME_SIZE;
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            return;
        }

        final byte[] data = fm.getData();
        final GoAwayReader gAway = new GoAwayReader(data);

        if (gAway.getErrorCode() == ErrorCodes.NO_ERROR) {
            // @ least write out the header, then include go away
            queueGoAway(ErrorCodes.NO_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.GO_AWAY;
        }
        else {
            // we mess up output, so shouldn't be writing out anymore.
            _outManager.purge();
            _readStat = ReadStats.OUTBOUND_ERROR;
        }*/
    }

    // connection
    private void procFrame_ConWinUpdate(Frame fm) {

        // if it's invalid, the function will send go away, .. so just return.
        final int val = winUpdateValidityCheck(fm);
        if (val == 0) {
            return;
        }

        final FlowController.FlowWindow fw = _outManager.getPeerFlowWin();
        if (fw.canAddCredit(val)) {
            fw.addCredit(val);
        }
        else {
            queueGoAway(ErrorCodes.FLOW_CONTROL_ERROR, "", _lastStreamProcessed );
            _readStat = ReadStats.OK;
        }
    }

    // stream one
    private void procFrame_StreamWinUpdate(Stream stream, Frame fm) {

        final int val = winUpdateValidityCheck(fm);
        if (val == 0) {
            return;
        }

        final StreamStates ss  = stream.getState();

        // ignore if the stream is closed ..
        // if the peer has sent rst stream or frame with end stream that closes the stream,
        // it shouldn't send this.
        final boolean ignore;
        ignore = (ss == StreamStates.CLOSED_BY_US) || (ss == StreamStates.RESET_BY_US);

        if (ignore) {
            // todo: bad stuff
            return;
        }

        final FlowController.FlowWindow fow = stream.getOutFlowWin();
        if (fow.canAddCredit(val)) {
            fow.addCredit(val);
        }
        else {
            // todo: can't do too large
            // some confusion ..
            //queueGoAway(ErrorCodes.FLOW_CONTROL_ERROR, "", _lastStreamProcessed );
            queueResetStream(ErrorCodes.FLOW_CONTROL_ERROR, fm);
            _readStat = ReadStats.CREDIT_OVERFLOW;
        }


        /*
        BBloody helll ....
        // read the data first
        final int[] vals = WindowUpdate.read(b);
        if (vals.length == 0) {
            // todo: bad ..
        }

        final WindowUpdate w = new WindowUpdate(b);
        if (w.getReturnCode() != -1) {
            // todo: error ... bad length
        }

        final int increment = w.getValue();
        if (increment <= 0) {
            // todo: ...PROTOCOL ERROR
        }

        ?? chnage to long ... I'll explain why'
        if (stream.getInC() + increment > HTTP2Defaults.MAX_WINDOW_SIZE) {
            // todo: too lage
        }

        stream.addInCredit(increment);
        ????

        if (vals[1] == 0) {
            // stream error
            //  PROTOCOL_ERROR;
        }

        final long newCredit = stream.getInC() + (long) b[1];

        if (_peerFlowWin.initWindowSize + newCredit > FlowWin.MAX_WINDOW_SIZE) {
            // todo bad...
        }

        stream.addInCredit(b[1]);

        /*
        For example, if the client sends 60 KB immediately on connection establishment and the server
        sets the initial window size to be 16 KB, the client will recalculate the available flow-control
        window to be -44 KB on receipt of the SETTINGS frame. The client retains a negative flow-control
        window until WINDOW_UPDATE frames restore the window to being positive, after which the client
        can resume sending.

        ?? https://http2.github.io/http2-spec/#WINDOW_UPDATE
         */

        /*
        final int i = WindowUpdate.readShit(b);
        if (i < 0) {
            // eroor
        }

        if (stream == null) {

        }
        else {
            stream.setStreamCredit(i);
        }
        */
    }

    private int winUpdateValidityCheck(Frame fm) {

        if (fm.getLength() != 4) {
            queueGoAway(ErrorCodes.FRAME_SIZE_ERROR, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_FRAME_SIZE;
            return 0;
        }

        final WindowUpdateReader wu = new WindowUpdateReader(fm.getData());
        if (wu.getValue() > 0) {
            return wu.getValue();
        }
        else {
            queueGoAway(ErrorCodes.PROTOCOL_ERROR, "", _lastStreamProcessed);
            // todo: bad stuff ...
            _readStat = ReadStats.BAD_SETTING;
            return 0;
        }
    }

    private void endStreamByRead(Stream stream) {

        // todo: return something ...
        switch (stream.getState()) {
            case OPEN:
                stream.setState(StreamStates.HALF_CLOSED_REMOTE);
                break;
            case HALF_CLOSED_LOCAL:
                // we initiated the close, technically - but it's not closed, it's partly close
                // the peer sends a frame with ES, so the peer should know that ..
                stream.setState(StreamStates.CLOSED_BY_PEER);
                break;
            default:
                throw new RuntimeException("Other frames shouldn't be here.");
        }
    }

    private void processRequestHeader(Stream stream, HeaderDecoder hdc) {
        // after we've got headers
        // there may be 2, the first one then the one after the body
        // todo: put stuff in OutPut?
        final DetReturn<ErrorResponse, List<Header>> hs = hdc.getHeaderList();
        final ErrorCodes erc = hs.getDeterminant().getErrorCode();

        if (erc == ErrorCodes.NO_ERROR) {
            stream.setHeaders(hs.getValue());
            // todo: $$ <<--- iz'zit here?

        }
        else {
            //Main.w("// HELL IS ON ERROR //" , erc);
            // todo: do this shire,
            //throw new RuntimeException("Not implemented yet");
            queueGoAway(erc, "", _lastStreamProcessed);
            _readStat = ReadStats.BAD_HEADER;
        }
    }

    private void respond(Stream stream) {


        // todo: put stuff in OutPut?
        //byte[] b = stream.getData();
        Main.w("OutPut ---> ");
        Main.w(stream.toString());

        Header hed = null;
        final List<Header> hrd = stream.getHeaders();
        for (Header h : hrd) {
            if (h.key.equalsIgnoreCase(":path")) {
                hed = h;
                break;
            }
        }

        final List<OutputMaker> oms;
        switch (hed.value) {
            case "/favicon.ico":
                oms =(new OutputTest(_headerEncoder, stream)).fffx2();
                break;
            case "/":
            case "/index":
            default:
                oms =(new OutputTest(_headerEncoder, stream)).fffx();
                break;
        }

        for (OutputMaker o : oms) {
            _outManager.put(o);
        }
    }

    private void queueGoAway(ErrorCodes ec, String dbgData, int lastId) {
        final GoAwayMaker gam = new GoAwayMaker(lastId, ec, dbgData);
        _outManager.purge(gam);
    }

    private void queueResetStream(ErrorCodes ec, Frame fm) {
        final ResetStreamMaker gam = new ResetStreamMaker(fm.getStreamId(), ec);
        _outManager.purge(gam);
    }

    private void sendExpectResponse(Stream stream) throws IOException {

        final ArrayList<Header> hdrs = new ArrayList<>();
        hdrs.add(new Header(":status", "100"));
        final HeadersMaker hm = new HeadersMaker(_headerEncoder, stream, false, hdrs, 0);
        _outManager.put(hm);
        // write out everything inside - if there's more than just tht to write, write all - OK?
        byte[] b;
        int safetyCounter = 0;
        do {
            b = _outManager.getBytes();
            _os.write(b);

            if (safetyCounter++ > 1000) {
                throw new IOException();
            }
        } while (b.length > 0);
        _os.flush();
    }

    public String toString() {
        return (_socket == null) ? "Null" : _socket.toString();
    }
}
