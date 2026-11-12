package snarky.stork.http2.out;

import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.ErrorCodes;
import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

public class ResetStreamMaker extends OutputMaker {

    private final int _streamId;
    private final ErrorCodes _errorCode;
    private boolean _hasMoreFrame;

    public ResetStreamMaker(int id, ErrorCodes erc) {
        super(new Stream(),false, false);
        _streamId = id;
        _errorCode = erc;
        _hasMoreFrame = true;
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {

        final String bits;
        bits = StringBit.padBits(Long.toBinaryString(_errorCode.getId()), 32);

        final BitManImp bks = new BitManImp();
        bks.append(new byte[4]);
        bks.setBits(0, bits);

        final FrameMaker fm = new FrameMaker(FrameType.RST_STREAM);
        fm.setStreamId(_streamId);
        fm.setLength(4);
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
        return FrameType.RST_STREAM;
    }
}
