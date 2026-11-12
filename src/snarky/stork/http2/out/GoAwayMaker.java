package snarky.stork.http2.out;

import snarky.stork.Main;
import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.ErrorCodes;
import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

import java.nio.charset.StandardCharsets;

import static snarky.stork.http2.SpecialValues.CONN_STREAM_ID;

public class GoAwayMaker extends OutputMaker {

    private final int _lastStreamId;
    private final ErrorCodes _errorCode;
    private final String _dbgData;
    private boolean _hasMoreFrame;

    public GoAwayMaker(int lastStreamId, ErrorCodes ec, String dbgData) {
        super(new Stream(), false, true);
        _lastStreamId = lastStreamId;
        _errorCode = ec;
        _dbgData = dbgData;
        _hasMoreFrame = true;

        Main.w("Go away reson -> ", ec);
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {

        final byte[] payload = create(_dbgData);

        final FrameMaker fm = new FrameMaker(FrameType.GO_AWAY);
        fm.setStreamId(CONN_STREAM_ID);

        // Why?
        // if debug data is too large causing the payload size to exceed what a single frame can carry
        // create a new one sans payload data.
        if (payload.length > maxFrameSize) {
            final byte[] newPayload = create("");
            fm.setPayload(newPayload);
            fm.setLength(newPayload.length);
        }
        else {
            fm.setPayload(payload);
            fm.setLength(payload.length);
        }
        _hasMoreFrame = false;
        return new OutputData(-1, fm);
    }

    private byte[] create(String dbgData) {

        final String idBits = StringBit.padBits(Integer.toBinaryString(_lastStreamId), 32);
        final String ecBits = StringBit.padBits(Long.toBinaryString(_errorCode.getId()), 32);

        final BitManImp bks = new BitManImp();
        bks.append(new byte[8]);
        bks.setBits(0, idBits + ecBits);
        bks.append(dbgData.getBytes(StandardCharsets.UTF_8));

        return bks.getBytes();
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.GO_AWAY;
    }

    public ErrorCodes getErrorCode() {
        return _errorCode;
    }
}
