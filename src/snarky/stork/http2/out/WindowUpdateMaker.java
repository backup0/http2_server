package snarky.stork.http2.out;

import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

public class WindowUpdateMaker extends OutputMaker {

    private final long _sizeInc;
    private boolean _hasMoreFrame;

    /**
     * For connection window.
     * @param sizeInc
     */
    public WindowUpdateMaker(long sizeInc) {
        this(new Stream(), sizeInc);
    }

    public WindowUpdateMaker(Stream stream, long sizeInc) {
        super(stream, false, false);
        if (sizeInc <= 0) {
            throw new RuntimeException("Illegal size, 0 not allowed");
        }
        _sizeInc = sizeInc;
        _hasMoreFrame = true;
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {
        final byte[] payload = create(_sizeInc);

        final FrameMaker fm = new FrameMaker(FrameType.WINDOW_UPDATE);
        fm.setStreamId(getStream().getId());
        fm.setLength(payload.length);
        fm.setPayload(payload);

        _hasMoreFrame = false;
        return new OutputData(-1, fm);
    }

    private byte[] create(long size) {
        final String s = StringBit.padBits(Long.toBinaryString(size), 32);

        final BitManImp bks = new BitManImp();
        bks.append(new byte[4]);
        bks.setBits(0, s);

        return bks.getBytes();
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.WINDOW_UPDATE;
    }
}
