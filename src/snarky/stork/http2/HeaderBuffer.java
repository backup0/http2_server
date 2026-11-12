package snarky.stork.http2;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.in.HeadersReader;

import java.util.ArrayList;

public class HeaderBuffer {

    private FrameMeta _fm;
    private HeadersReader _hdr;
    private final ArrayList<byte[]> _headerBlocks;

    public HeaderBuffer() {
        _fm = null;
        _hdr = null;
        _headerBlocks = new ArrayList<>();
    }

    /**
     * Throws illegal argument exception if not HEADERS frame. Throws illegal state if it's in 'reading mode'.
     * @param fm
     * @param hr
     * @param data
     * @return
     */
    public void register(FrameMeta fm, HeadersReader hr, byte[] data) {

        if (fm.getType() != FrameType.HEADERS) {
            throw new IllegalArgumentException("Not HEADERS frame.");
        }

        if (_fm != null || _hdr != null) {
            throw new IllegalStateException("Reading headers ...");
        }

        _fm = fm;
        _hdr = hr;
        _headerBlocks.add(data);
    }

    /**
     * Add data block. If frame type isn't CONTINUATION IllegalArgumentException is thrown.
     * @param fm
     * @param data
     * @return true if ok, false if stream id doesn't match.
     */
    public boolean addDataBlock(FrameMeta fm, byte[] data) {

        if (fm.getType() != FrameType.CONTINUATION) {
            throw new IllegalArgumentException("Not CONTINUATION frame.");
        }

        // it has to be from the same stream
        if (fm.getStreamId() != _fm.getStreamId()) {
            return false;
        }

        _headerBlocks.add(data);
        return true;
    }

    public FrameMeta getHeader() {
        return _fm;
    }

    public HeadersReader getHeaders() {
        return _hdr;
    }

    public byte[] getHeaderBlocks() {
        final BitManImp bks = new BitManImp();
        for (byte[] b : _headerBlocks) {
            bks.append(b);
        }
        return bks.getBytes();
    }

    public void reset() {
        _fm = null;
        _hdr = null;
        _headerBlocks.clear();
    }

    public boolean readingHeader() {
        return _fm != null;
    }
}
