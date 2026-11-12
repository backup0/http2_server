package snarky.stork.http2.out;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.DetReturn;
import snarky.stork.http2.FrameMaker;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

import java.io.InputStream;
import java.util.Arrays;

public class DataMaker extends OutputMaker {

    private final Stream _stream;
    private final InputStream _ins;
    private boolean _hasMoreFrame;
    private final int _padLen;
    private final boolean _endStream;

    public DataMaker(Stream stream, boolean endStream, int padLen, InputStream in) {
        super(stream,true, false);
        _stream = stream;
        _endStream = endStream;
        _ins = in;
        _hasMoreFrame = true;
        _padLen = (padLen < 0 || padLen > 255) ? 0 : padLen;
    }

    // todo: if exception is thrown maybe send reset stream or go away - error @ reading. LATER.
    private FrameMaker getFrame(int frameSize) throws Exception {
        // frame size is 0 or more, ok?
        // max frame size > 0;
        // 1 byte for pad size
        // the rest for the padding.
        final int padSize = 1 + _padLen;

        final FrameMaker fm  = new FrameMaker(FrameType.DATA);
        fm.setStreamId(_stream.getId());

        final DetReturn<Boolean, byte[]> content;
        if (frameSize > padSize) {
            // frame can fit pad + @ least 1 byte of data
            content = ResponseDataReader.getData(_ins, (frameSize - padSize));
            final byte[] payload = createPaddedPayload(_padLen, content.getValue());
            fm.setLength(payload.length);
            fm.setPayload(payload);
            // use pad flag.
            fm.setFlag3();
        }
        else {
            // don't bother padding.
            content = ResponseDataReader.getData(_ins, frameSize);
            final byte[] payload = content.getValue();
            fm.setPayload(payload);
            fm.setLength(payload.length);
        }

        final boolean hasMore = content.getDeterminant();
        if (!hasMore) {
            if (_endStream) {
                //fm.setLength(0); ??
                fm.setFlag0();
                _hasMoreFrame = false;
            }
        }
        return fm;
    }

    private byte[] createPaddedPayload(int padLen, byte[] data) {

        // Note: A frame can be increased in size by one octet by including a Pad Length field with a value of zero.
        // if you set pad value to 0, there's no padding - but the size is +1 because pad len field is included.
        final byte[] pad = new byte[padLen];
        Arrays.fill(pad, (byte) 0);

        final BitManImp bks = new BitManImp();
        bks.append(BitCon.intToByte(pad.length)); // pad length,
        bks.append(data); // the data
        bks.append(pad); // the pad

        return bks.getBytes();
    }

    public boolean endStream() {
        return _endStream;
    }
    /**
     *
     * @param frameSize
     * @return OutFrame with return code -1 if ok, or 0 there's an exception.
     */
    @Override
    public OutputData getDataOut(int frameSize) {

        // return empty data, caller will do nothing
        if (!_hasMoreFrame) {
            return new OutputData(-1);
        }

        try {
            // data out depends on frame size.
            final FrameMaker fm = getFrame(frameSize);
            return new OutputData(-1, fm);
        }
        catch (Exception e) {
            return new OutputData(0);
        }
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    /**
     *
     * @return Always false.
     */
    @Override
    public boolean continuously() {
        return false;
    }

    @Override
    public boolean subjectToFlowControl() {
        return true;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.DATA;
    }
}
