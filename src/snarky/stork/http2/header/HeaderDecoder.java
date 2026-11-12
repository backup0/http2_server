package snarky.stork.http2.header;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.*;
import snarky.stork.http2.hpack.HT2String;
import snarky.stork.http2.hpack.HeaderTable;
import snarky.stork.http2.hpack.IntDecoder;
import snarky.stork.http2.hpack.StringCodex;

import java.util.ArrayList;
import java.util.List;

public class HeaderDecoder {

    private final BitManImp _bks;
    private final IntDecoder _idc;
    private final HeaderTable _hTable;
    private final List<Header> _headerList;

    private int _index;

    public HeaderDecoder(byte[] b) {
        this(b, new HeaderTable());
    }

    public HeaderDecoder(byte[] b, HeaderTable hTable) {

        _idc = new IntDecoder();
        _bks = new BitManImp();
        _hTable = hTable;
        _headerList = new ArrayList<>();
        _index = 0;

        _bks.append(b);
    }

    /**
     * OK, COMPRESSION_ERROR, PROTOCOL_ERROR
     * @return
     */
    public DetReturn<ErrorResponse, List<Header>> getHeaderList() {

        int returnCode = -1;
        while (_index < _bks.size()) {
            // this is where we retrieve each header.
            returnCode = getHeader();
            if (returnCode != -1) break;
        }

        final ErrorResponse erc;
        switch (returnCode) {
            case 0:
                erc = new ErrorResponse(ErrorCodes.PROTOCOL_ERROR, "Index = 0 is not acceptable");
                break;
            case 1:
                erc = new ErrorResponse(ErrorCodes.COMPRESSION_ERROR, "Index out of bound");
                break;
            default:
                erc = new ErrorResponse(ErrorCodes.NO_ERROR);
                break;
        }
        return new DetReturn<>(erc, _headerList);
    }

    private int getHeader() {

        final int rtnCode;
        // the first byte tells us ??
        final HeaderDeterminant.Det det = HeaderDeterminant.getDet(_bks.getByte(_index));

        switch (det)
        {
            case IN_DYN_TABLE:
                // the header will be retrieved from the header table.
                // only index is sent.
                // index doesn't exist in table : COMPRESSION_ERROR
                // index = 0 : protocol_error
                rtnCode = getFromTable(det);
                break;
            case INDEX: // index this
                rtnCode = decodeHeaderIndex(det);
                break;
            case NO_INDEX: // don't index this
            case STRICT: // treat strictly - if this is to relayed, it has to be sent exactly
                rtnCode = decodeHeaderNOIndex(det);
                break;
            case SIZE_UPDATE:
                rtnCode = sizeUpdate();
                break;
            default:
                throw new RuntimeException("Bad determinant ..");
        }
        return rtnCode;
    }

    private int sizeUpdate() {

        final long val = decodeInt(5).intValue();
        if (val < 0) {
            return 0;
        }

        if (!_headerList.isEmpty()) {
            // todo: what kind of error?
            // has to be he 1st item.
            return 1;
        }

        return -1;
    }

    private int getFromTable(HeaderDeterminant.Det det) {

        // the header will be retrieved from the header table.
        // only index is sent.
        // index doesn't exist in table : COMPRESSION_ERROR
        // index = 0 : protocol_error

        final long index = decodeInt(7);
        if (index < 0) {
            return 1;
        }

        final int rtn;
        if (index == 0) {
            // bad index, set xs to empty, handle below
            // this is an error...
            // 0 = protocol error.
            rtn = 0;
        }
        else {
            final String[] ts = _hTable.getAt((int) index);
            if (ts.length == 2) {
                final HeaderExt hExt;
                hExt = new HeaderExt(det,false, false, (int) index);
                // add the stuff to our header list
                final Header hdr = new Header(ts[0], ts[1], hExt);
                // add into the header list, ok?
                _headerList.add(hdr);
                rtn = -1;
            }
            else {
                // if it's empty, it's outside the ...
                rtn = 1;
            }
        }
        return rtn;
    }

    private int decodeHeaderIndex(HeaderDeterminant.Det det) {
        final long index = decodeInt(6);
        if (index < 0) {
            return 1;
        }

        final int rtn;
        final HT2String[] xs = decodeHeader(index);
        if (xs.length == 2) {
            final HeaderExt hExt;
            hExt = new HeaderExt(det, xs[0].huffmanEncoded, xs[1].huffmanEncoded, (int) index);
            // add the stuff to our header list
            final Header hdr = new Header(xs[0].string, xs[1].string, hExt);
            _headerList.add(hdr);
            _hTable.indexThis(new String[]{hdr.key, hdr.value});
            rtn = -1;
        }
        else {
            return 1;
        }
        return rtn;
    }

    private int decodeHeaderNOIndex(HeaderDeterminant.Det det) {
        final long index = decodeInt(4);
        if (index < 0) {
            return 1;
        }

        final int rtn;
        final HT2String[] xs = decodeHeader(index);
        if (xs.length == 2) {
            final HeaderExt hExt;
            hExt = new HeaderExt(det, xs[0].huffmanEncoded, xs[1].huffmanEncoded, (int) index);
            // add the stuff to our header list
            final Header hdr = new Header(xs[0].string, xs[1].string, hExt);
            _headerList.add(hdr);
            rtn = -1;
        }
        else {
            return 1;
        }
        return rtn;
    }

    private HT2String[] decodeHeader(long index) {

        final HT2String[] rtn;
        // new - less is not possible, but what the hell if it's 0.
        // 0 is literal.
        if (index <= 0) {
            final HT2String key = decodeString();
            if (key == null) {
                rtn = new HT2String[0];
            }
            else {
                final HT2String val = decodeString();
                if (val == null) {
                    rtn = new HT2String[0];
                }
                else {
                    rtn = new HT2String[]{key, val};
                }
            }
        }
        else { // name already indexed

            final String[] hft = _hTable.getAt((int) index);
            if (hft.length == 0) { // out of range ...
                rtn = new HT2String[0];
            }
            else {
                final HT2String key = HT2String.getPlainString(hft[0]);
                final HT2String val =  decodeString();
                if (val == null) {
                    rtn = new HT2String[0];
                }
                else {
                    rtn = new HT2String[]{key, val};
                }
            }
        }
        return rtn;
    }

    /**
     *
     * @param prefSize size of prefix
     */
    private Long decodeInt(int prefSize) {
        // grab the byte @ current location.
        // decode the integer starting with that byte.
        // set location to the next data (string or another integer, depending).

        // todo: correctness also depends on correctness of integer decoder.
        Long val = _idc.restart(prefSize, _bks.getByte(_index));
        while (val == null) {
            // next location.
            final int nextIndx = ++_index;
            // we need to retrieve bytes - so need to check.
            if (nextIndx >= _bks.size()) {
                // todo: is this correct?
                val = -1L;
                break;
            }
            val = _idc.update(_bks.getByte(nextIndx));
        }
        ++_index; // advance by one - so the other ?? doesn't text have to do this.
        return val;
    }

    private HT2String decodeString() {
        // decode integer - this move the location one 'step' up.
        // so location can still be >= size
        // todo: what is there's something off??? OFF???
        if (_index >= _bks.size()) {
            return null;
        }

        final byte byte1 = _bks.getByte(_index);
        // this will move the index 1 step up.
        final Long length = decodeInt(7);
        if (_index >= _bks.size()) {
            return null;
        }

        // todo: wrong stuff ...
        final byte[] stringData = _bks.getBytes(_index, length.intValue());
        // this updates the index as well.
        _index = _index + length.intValue();

        final HT2String rtnData;
        // todo: correctness depends on string decoder. May want to test that.
        if ((byte1 & -128) != 0) {
            // this means it's huffman encoded
            // decode, if there's error ...
            rtnData = StringCodex.decodeString(stringData);
        }
        else {
            //s = new String(stringData, StandardCharsets.UTF_8);
            rtnData = HT2String.getPlainString(new String(stringData));
        }
        return rtnData.ok ? rtnData : null;
    }
}