package snarky.stork.http2.out;

import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

public class PriorityMaker extends OutputMaker {

    private final Stream _stream;
    private final boolean _exclusive;
    private final int _depStreamId;
    private final int _weight;
    private boolean _hasMoreFrame;

    public PriorityMaker(Stream stream, boolean exclusive, int depId, int weight) {
        super(stream,false, false);
        _stream = stream;
        _exclusive = exclusive;
        _depStreamId = depId;
        // todo: is this correct, 1 byte but weight is between 1 and 256
        _weight = weight - 1;
        _hasMoreFrame = true;
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {

        final String exc = (_exclusive ? "1" : "0");
        final String s = StringBit.padBits(Integer.toBinaryString(_depStreamId), 31);
        final String w = StringBit.padBits(Integer.toBinaryString(_weight), 8);

        final BitManImp bks = new BitManImp();
        bks.append(new byte[5]);
        bks.setBits(0, exc + s + w);

        final FrameMaker fm = new FrameMaker(FrameType.PRIORITY);
        fm.setStreamId(_stream.getId());
        fm.setLength(bks.size());
        fm.setPayload(bks.getBytes());

        _hasMoreFrame = false;
        return new OutputData(-1, fm);
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.PRIORITY;
    }
}
