package snarky.stork.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Stream {

    // todo: when do we dump out frames?
    private final int _streamId;
    // private int _streamInCredit = 0; // MAX - 2,147,483,647
    // private int _streamOutCredit = 0; // MAX - 2,147,483,647
    private StreamStates _state = StreamStates.IDLE;

    private final FlowController.FlowWindow _inWin;
    private final FlowController.FlowWindow _outWin;

    //private long _lastActionTime;
    //private FrameType _lastActionFrame;

    private long _resetStream = -1;

    private long _closingTime = -1;

    // yes we do need this, this is the one that produces the output.
    private Object _handler;

    private ArrayList<Header> _headers = new ArrayList<>();
    // todo: definitely have to chnage this .. important.
    private ByteArrayOutputStream _body = new ByteArrayOutputStream();
    private ArrayList<Header> _trailers = new ArrayList<>();

    private enum ProcState {
        NO,
        HEADER_SET,
        WRITING_DATA,
        COMPLETE
    }

    private ProcState _prog = ProcState.NO;

    // Receive header ---> OPEN
    // receive end srtream ---> HALF_CLOSE (REMOTRE) // remote stream is galf closed, it has sent all that it wants
    //  to send, so remotely it's closed.
    // send end stream --> CLOSED
    // send reset --> CLOSED

    /*
        Ok, sending push promise
        > reserve a a stream ..
        > .. it's reserved now (local), reserved for us to use
        > send headers on that stream it becomes half-close (remote)
     */

    /**
     * This is for creating 'dummy' stream, for connection stuff.
     */
    public Stream() {
        this(0, (new FlowController()).createFlowWindow(), (new FlowController()).createFlowWindow());
    }

    public Stream(int id, FlowController.FlowWindow inFW, FlowController.FlowWindow outFW) {
        _streamId = id;
        _inWin = inFW;
        _outWin = outFW;
        //_lastActionTime = System.currentTimeMillis();
        //_lastActionFrame = FrameType.UNKNOWN;
    }

    public FlowController.FlowWindow getInFlowWin() {
        return _inWin;
    }

    public FlowController.FlowWindow getOutFlowWin() {
        return _outWin;
    }

    public void getDataOut(int connectionLimit) {
        // - rigth, we need conection limit so it know how much
        // connection limit being how much data we can send out, ..
        // say if the limit is 0, we shouldnt pull any data out.

        // if if creadit = 0; also don't ..

        // if conenction limit is not enough don;t ..
        // else ok
        // if creadit is @ the limit don't .. else ok.
        // wait until creadit is reset.
    }

    public int getId() {
        return _streamId;
    }

    public void setState(StreamStates state) {
        // we can't use 0 as id, this is just a 'placeholder(?)'
        if (_streamId == 0) {
            return;
        }

        if (state == StreamStates.CLOSED_BY_US) {
            // we need to accept some frames for a short period of time, after tit's closed.
            _closingTime = System.currentTimeMillis();
        }
        _state = state;
    }

    // handler can only be set once we've read the header.
    // handler is the one that processes the request, and sends out response.
    public void setHandler(Object handler) {
        _handler = handler;
    }

    public StreamStates getState() {
        // place holder...
        if (_streamId == 0) {
            return StreamStates.OPEN;
        }
        else {
            return _state;
        }
    }

    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream();
    }

    public int getCredit() {

        //return 0;
        throw new RuntimeException("Not implemnetd yet.");
    }

    public void invalidate() {
        if (_resetStream != -1) {
            _resetStream = System.currentTimeMillis();
        }
    }

    public boolean streamInvalidated() {
        return _resetStream >= 0;
    }

    public long getClosingTime() {
        return _closingTime;
    }

    public void write(byte[] b) {

        switch (_prog) {
            case NO:
                // todo: error ...
                throw new RuntimeException("Not implmneted yet ...");
            case WRITING_DATA:
            case HEADER_SET:
                try {
                    _body.write(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                _prog = ProcState.WRITING_DATA;
                break;
            default:
                throw new RuntimeException("Not implmneted yet ...");
        }
    }

    public void setHeaders(List<Header> hdr) {

        switch (_prog) {
            case NO:
                _headers.addAll(hdr);
                _prog = ProcState.HEADER_SET;
                break;
            case WRITING_DATA:
                _trailers.addAll(hdr);
                _prog = ProcState.COMPLETE;
                break;
            case HEADER_SET:
                // todo: trailer without content maybe wrong.
                _trailers.addAll(hdr);
                _prog = ProcState.COMPLETE;
                break;
            default:
                throw new RuntimeException("Not implemented yet ...");
        }
    }

    public List<Header> getHeaders() {
        return _headers;
    }

    public String toString() {
        final StringBuilder sbs = new StringBuilder();
        sbs.append("@Stream id: ").append(_streamId);
        sbs.append("\n/Start headers/\n");
        for (Header h : _headers) {
            sbs.append(h).append("\n");
        }
        sbs.append("/End headers/\n");
        sbs.append("--------");
        sbs.append(new String(_body.toByteArray()));
        sbs.append("--------");
        sbs.append("\n");
        for (Header h : _trailers) {
            sbs.append(h).append("\n");
        }
        sbs.append("???\n");
        return sbs.toString();
    }
}
