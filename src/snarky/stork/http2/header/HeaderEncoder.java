package snarky.stork.http2.header;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.Header;
import snarky.stork.http2.hpack.HeaderTable;

import java.util.List;

public class HeaderEncoder {

    private final HeaderTable _headerTable;
    private boolean _reduceSize = false;

    public HeaderEncoder() {
        _headerTable = new HeaderTable(32);
        //_headerTable = new HeaderTable();
    }

    public byte[] createHeader(List<Header> headers) {

        // todo: check size .. maximum list size.
        final BitManImp bks = new BitManImp();

        if (_reduceSize) {
            byte[] bbs = RawHeaderCreator.tableUpdateSize((int) _headerTable.getMaxSizes()[1]);
            bks.append(bbs);
            _reduceSize = false;
        }

        for (Header h : headers) {
            bks.append(procHeader(h));
        }

        return bks.getBytes();
    }

    public void reduceSize(int size) {
        _reduceSize = true;
        _headerTable.setMaxSize(size);}

    public HeaderTable getHeaderTable() {
        return _headerTable;
    }

    private byte[] procHeader(Header hdr) {

        // header name has to be in lower case.
        final String key = hdr.key.toLowerCase();
        final String value = hdr.value;

        final int[] indc;
        if (useNewName(hdr)) { // force this to use new name, regardless of whether name has been indexed
            indc = new int[0];
        }
        else { // else retrieve from table, if there's none indc will be empty
            indc = _headerTable.getIndice(key, value);
        }

        final boolean useHuffman = useHuffmanEncoding(hdr);

        // if useNewName() returns true, we always use the name literally, that's because
        // indc is empty.
        final byte[] headerBlock;
        if (isStrict(hdr)) {
            if (indc.length == 0) {
                headerBlock = RawHeaderCreator.strictNew(key, value, useHuffman);
            }
            else {
                headerBlock = RawHeaderCreator.strictNewValue(indc[0], value, useHuffman);
            }
        }
        else if (noIndex(hdr)) {
            if (indc.length == 0) {
                headerBlock = RawHeaderCreator.noIndexNew(key, value, useHuffman);
            }
            else {
                headerBlock = RawHeaderCreator.noIndexNewValue(indc[0], value, useHuffman);
            }
        }
        else { // standard stuff
            switch (indc.length) {
                case 0: // none - ne value
                    _headerTable.indexThis(new String[]{key, value});
                    headerBlock = RawHeaderCreator.newEntry(key, value, useHuffman);
                    break;
                case 1: // found name ...
                    _headerTable.indexThis(new String[]{key, value});
                    headerBlock = RawHeaderCreator.newValue(indc[0], value, useHuffman);
                    break;
                case 2: // found matching pair, no need to index.
                    headerBlock = RawHeaderCreator.fromIndex(indc[0]);
                    break;
                default:
                    throw new RuntimeException("Code fuckup.");
            }
        }

        return headerBlock;
    }

    private boolean isStrict(Header hdr) {

        for (Header.Rest r : hdr.rest2) {
            if (r == Header.Rest.STRICT) {
                return true;
            }
        }
        return false;
    }

    private boolean noIndex(Header hdr) {

        for (Header.Rest r : hdr.rest2) {
            if (r == Header.Rest.NO_INDEX) {
                return true;
            }
        }
        return false;
    }

    private boolean useNewName(Header hdr) {

        for (Header.Rest r : hdr.rest2) {
            if (r == Header.Rest.NEW_NAME) {
                return true;
            }
        }
        return false;
    }

    // todo: back to not.
    private boolean useHuffmanEncoding(Header hdr) {

        for (Header.Rest r : hdr.rest2) {
            if (r == Header.Rest.NO_HUFFMAN) {
                return false;
            }
        }
        return true;
    }
}
