package snarky.stork.http2;

import snarky.stork.bitman.BitManImp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Process raw data coming in from peer.
 * Put bytes in, this will return frame when there's enough data.
 */
public class InboundDataProcessor {

    private final StreamManager _streamManager;
    private final FlowController.FlowWindow _flowWin;
    private final BitManImp _bks;
    private final ByteArrayOutputStream _bos;
    private final ByteArrayOutputStream _tempBuffer;
    // todo: we shall allow this to be changed ...
    private int _maxFrameSize = SpecialValues.MAX_FRAME_SIZE;

    public enum Stat {
        OK,
        NOT_YET,
        BAD_STREAM,
        CONN_OVERFLOW,
        STREAM_OVERFLOW,
        BAD_ID,
        FRAME_TOO_LARGE
    }

    private final Frame _dummyFrame; 
    private FrameMeta _fm;

    public InboundDataProcessor(StreamManager streamMgr, FlowController.FlowWindow inFW) {

        _streamManager = streamMgr;
        _flowWin = inFW;
        _bks = new BitManImp();
        _bos = new ByteArrayOutputStream();
        _tempBuffer = new ByteArrayOutputStream();
        _dummyFrame = new Frame();
        _fm = null;
    }

    /**
     * Determinant:
     * -1: ok, frame is read
     * 0: no full frame yet
     * 1: bad stream
     * 2: connection overflow
     * 3: stream overflow
     * 4: bad id (0 for stream frame, !0 for connection frame)
     * 5: frame too large
     * @param bytes
     * @return
     * @throws IOException
     */
    public DetReturn<Stat, Frame> put(byte[] bytes) throws IOException {

        // combine previous data with current data.
        _tempBuffer.write(bytes);
        final byte[] bytes2Process = _tempBuffer.toByteArray();
        if (bytes2Process.length == 0) {
            return new DetReturn<>(Stat.NOT_YET, _dummyFrame);
        }
        _tempBuffer.reset();
        //

        int toWrite = (_fm == null) ? 0 : _fm.getLength();
        int stat = -1;

        for (byte b : bytes2Process) {
            
            // read headers first
            if (_bks.size() < 9) {
                _bks.append(b); 
                if (_bks.size() == 9) {
                    _fm = new FrameMeta(_bks.getBytes());
                    if (_fm.getLength() > _maxFrameSize) {
                        stat = 4;
                        break;
                    }

                    if (!idValidForFrame()) {
                        stat = 3;
                        break;
                    }                    
                    // see if we can process this frame.
                    // this checks credits, etc. ..
                    stat = creditCheck();
                    if (stat != -1) {
                        break;
                    }
                    toWrite = _fm.getLength();
                }
            }
            else {
                if (_bos.size() < toWrite) {
                    _bos.write(b);
                }
                else {
                    _tempBuffer.write(b);
                }
            }
        }

        final DetReturn<Stat, Frame> rtn;

        switch (stat) {
            case 0:
                // bad stream
                rtn = new DetReturn<>(Stat.BAD_STREAM, _dummyFrame);
                break;
            case 1:
                // conn overflow
                rtn = new DetReturn<>(Stat.CONN_OVERFLOW, _dummyFrame);
                break;
            case 2:
                // stream overflow
                rtn = new DetReturn<>(Stat.STREAM_OVERFLOW, _dummyFrame);
                break;
            case 3:
                // bad id, ..
                rtn = new DetReturn<>(Stat.BAD_ID, _dummyFrame);
                break;
            case 4:
                // frame's too large
                rtn = new DetReturn<>(Stat.FRAME_TOO_LARGE, _dummyFrame);
                break;
            default:
                if (_bks.size() == 9 && toWrite == _bos.size()) {
                    final Frame fm = new Frame(_bks.getBytes(), _bos.toByteArray());
                    rtn = new DetReturn<>(Stat.OK, fm);
                }
                else {
                    rtn = new DetReturn<>(Stat.NOT_YET,_dummyFrame);
                }
                break;
        }

        // det = -1 means we have a full frame, and it's available now
        if (rtn.getDeterminant() == Stat.OK) {
            _bks.clear();
            _bos.reset();
            _fm = null;
        }
        return rtn;
    }

    // -1 no error
    // 0 no stream; .. maybe it doesn't exist
    // 1 connection error
    // 2 stream error
    private int creditCheck() {
        // this is connection frame.
        // no need to check credit ...
        if (_fm.getStreamId() == 0) {
            return -1;
        }

        final DetReturn<Integer, Stream> sr =  _streamManager.getStream(_fm.getStreamId());
        if (sr.getDeterminant() != -1) {
            // todo: right ..., no stream ...
            return 0;
        }

        // non data frame isn't subject to flow control
        if (_fm.getType() != FrameType.DATA) {
            return -1;
        }

        // deal with credit stuff.
        final Stream stream = sr.getValue();
        final int length = _fm.getLength();

        final FlowController.FlowWindow streamFlowWin = stream.getInFlowWin();

        // do we have enough connection credit?
        //_peerFlowWin.initWindowSize + _peerFlowWin.credit;
        if (_flowWin.getCredit() < length) {
            // todo: flow control error
            return 1;
        }

        // do we have enough stream credit?
        if (streamFlowWin.getCredit() < length) {
            // todo: flow control error
            return 2;
        }

        streamFlowWin.reduceCredit(_fm.getLength());
        _flowWin.reduceCredit(_fm.getLength());

        return -1;
    }

    private boolean idValidForFrame() {
        // this involves the entire connection
        // the stream id = 0;
        final int streamId = _fm.getStreamId(); 
        final boolean ok;
        switch (_fm.getType()) {
            case DATA:
            case HEADERS:
            case PRIORITY:
            case RST_STREAM:
            case PUSH_PROMISE:
            case CONTINUATION:
                ok = streamId > 0;
                break;
            case SETTINGS:
            case PING:
            case GO_AWAY:
                ok = streamId == 0;
                break;
            case WINDOW_UPDATE:
            default:
                // unknown frame to be ignored ...
                ok = true;
                break;
        }
        return ok;
    }

    public void setMaxFrameSize(int size) {
        _maxFrameSize = size;
    }

    /**
     * Check if there's more data.
     * @return
     */
    public boolean hasMore() {
        return _tempBuffer.size() > 0;
    }
}
