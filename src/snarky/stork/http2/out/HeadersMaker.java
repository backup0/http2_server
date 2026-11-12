package snarky.stork.http2.out;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.*;
import snarky.stork.http2.header.HeaderEncoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static snarky.stork.http2.StreamStates.RESET_BY_PEER;

public class HeadersMaker extends OutputMaker {

    // todo: handle priority ...
    // ok, padded -> we should handle this
    // end stream,
    private final HeaderEncoder _hdrEnc;
    private final Stream _stream;
    private final boolean _exclusive;
    private final int _streamDep;
    private final int _weight;
    private final boolean _endStream;
    private final ByteArrayInputStream _ins;
    private final int _padLen;

    private boolean _hasMoreFrame;
    private boolean _first;

    public HeadersMaker(HeaderEncoder hdrEnc, Stream stream,
                        boolean endStream, List<Header> hdrs, int padLen) {
        this(hdrEnc, stream, false, 0, 0, endStream, hdrs, padLen);
    }

    public HeadersMaker(HeaderEncoder hdrEnc, Stream stream, boolean exclusive, int streamDep, int weight,
                        boolean endStream,
                        List<Header> hdrs, int padLen) {
        super(stream,false, true);

        _hdrEnc = hdrEnc;
        _stream = stream;
        _exclusive = exclusive;
        _streamDep = streamDep;
        _weight = weight;
        _endStream = endStream;
        _ins = new ByteArrayInputStream(getHeaderBlock(_stream.getState() == RESET_BY_PEER, hdrs));
        _padLen = (padLen < 0 || padLen > 255) ? 0 : padLen;

        _first = true;
        _hasMoreFrame = true;
    }

    private byte[] getHeaderBlock(boolean mod, List<Header> hdrs) {
        if (!mod) {
            return _hdrEnc.createHeader(hdrs);
        }

        // TODO: is this even necessary?
        final ArrayList<Header> newHeaders = new ArrayList<>();
        final ArrayList<Header.Rest> tmp = new ArrayList<>();

        // now need to modify.
        for (Header h : hdrs) {

            final Header.Rest[] rst = h.rest2;
            tmp.addAll(Arrays.asList(rst));

            boolean ok = false;
            for (Header.Rest hr : tmp) {
                // if it's no index or strict, it doesn't alter the header table,
                // so ignore ..
                if (hr == Header.Rest.NO_INDEX || hr == Header.Rest.STRICT) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                final Header nHeader;
                nHeader = new Header(h.key, h.value, tmp.toArray(new Header.Rest[0]));
                newHeaders.add(nHeader);
            }
            else {
                newHeaders.add(h);
            }
            tmp.clear();
        }
        return (new HeaderEncoder()).createHeader(newHeaders);
    }
    // todo: IO-E?
    private FrameMaker getFrame(int maxFrameSize) throws IOException {


        // continuation doesn't use pad, too lazy so, just use that .. just 255 bytes max anyway.
        maxFrameSize = maxFrameSize - (_padLen + 1);

        // data we want to write out.
        final DetReturn<Boolean, byte[]> unprepData;
        unprepData = ResponseDataReader.getData(_ins, maxFrameSize);

        final byte[] framePayload;
        // create frame meta. - from header block. Okz?
        final FrameMaker fm;
        if (_first) {
            fm = new FrameMaker(FrameType.HEADERS);
            if (_endStream) {
                fm.setFlag0();
            }
            fm.setFlag3(); // always pad.
            // priority part included.
            if (_streamDep > 0) {
                fm.setFlag5();
            }
            framePayload = createHeadersPayload(unprepData.getValue());
        }
        else {
            // todo: test remove
            fm = new FrameMaker(FrameType.CONTINUATION);
            // no padding or anything like that.
            framePayload = unprepData.getValue();
        }
        fm.setStreamId(_stream.getId());
        fm.setPayload(framePayload);
        fm.setLength(framePayload.length);

        // flag4 = end header
        // can be set on either headers frame or continuation frame.
        // both use the same flag.
        if (_ins.available() <= 0) {
        //if (!unprepData.getDeterminant()) {
            fm.setFlag2();
            _hasMoreFrame = false;
        }
        // first header block is sent in Headers frame, the rest in Continuation.
        _first = false;

        return fm;
    }

    // pad is always applied??
    private byte[] createHeadersPayload(byte[] headerBlock) {

        /*
         +---------------+
         |Pad Length? (8)|
         +-+-------------+-----------------------------------------------+
         |E|                 Stream Dependency? (31)                     |
         +-+-------------+-----------------------------------------------+
         |  Weight? (8)  |
         +-+-------------+-----------------------------------------------+
         |                   Header Block Fragment (*)                 ...
         +---------------------------------------------------------------+
         |                           Padding (*)                       ...
         +---------------------------------------------------------------+
         */

        // Flags:
        //   0 - end stream
        //   4 - end headers.
        //
        //
        final BitManImp bks = new BitManImp();
        if (_streamDep > 0) {
            // --- Ok create the stream dep part ---
            final String a = Integer.toBinaryString(_streamDep);
            final String b = StringBit.padBits(a,31);
            final String c = _exclusive ? "1" + b : "0" + b;
            // ----------
            bks.append(new byte[4]); // allocate 4 bytes
            bks.setBits(0, c);
            // set weight
            bks.append(BitCon.intToByte(_weight));
        }

        final byte[] pad = new byte[_padLen];
        Arrays.fill(pad, (byte) 0);

        bks.insert(0, BitCon.intToByte(pad.length)); // pad length,
        bks.append(headerBlock); // the data
        bks.append(pad); // the pad

        return bks.getBytes();
    }

    /**
     *
     * @param maxFrameSize
     * @return OutFrame with the following return code -1: ok, 0: IOException, 1: http2 exception...
     */
    @Override
    public OutputData getDataOut(int maxFrameSize) {
        // conn credit ins't used because - this frame type isn't subject to control flow
        if (!_hasMoreFrame) { // return empty
            return new OutputData(-1);
        }

        // todo: deal with exception
        try {
            final FrameMaker fm = getFrame(maxFrameSize);
            return new OutputData(-1, fm);
        }
        catch (IOException e) {
            return new OutputData(0);
        }
    }

    public boolean endStream() {
        return _endStream;
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    /**
     *
     * @return True, as this ..
     */
    @Override
    public boolean continuously() {
        return true;
    }

    @Override
    public boolean subjectToFlowControl() {
        return false;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.HEADERS;
    }
}

