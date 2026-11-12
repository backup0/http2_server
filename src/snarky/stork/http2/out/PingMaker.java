package snarky.stork.http2.out;

import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

public class PingMaker extends OutputMaker {

    private final byte[] _payload;
    private boolean _hasMoreFrame;

    /**
     * This is for ping 'request'
     */
    public PingMaker() {
        this(new byte[0]);
    }

    /**
     * Ping response, payload should be the data sent by peer
     */
    public PingMaker(byte[] payload) {
        super(new Stream(), false, false);
        if (payload.length != 8) {
            throw new RuntimeException("Payload must be 8 bytes");
        }

        _payload = payload;
        _hasMoreFrame = true;
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {

        final FrameMaker fm = new FrameMaker(FrameType.PING);
        fm.setStreamId(0);
        // this is the payload we're sending.
        final byte[] b = new byte[8];
        // the size has to be 8..
        fm.setLength(b.length);

        // acknowledgement, we pass the payload back .. and set the ack flag.
        if (_payload.length > 0) {
            fm.setFlag0();
            fm.setPayload(_payload);
        }
        // request, we generate the payload, ack flag is not set.
        else {
            fm.setPayload(b);
        }

        _hasMoreFrame = false;
        return new OutputData(-1, fm);
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.PING;
    }
}
