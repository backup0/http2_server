package snarky.stork.http2.in;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.ErrorCodes;
import snarky.stork.http2.Utils;

import java.util.Arrays;

public class GoAwayReader {

    // todo: test this...
    /*
    +-+-------------------------------------------------------------+
    |R|                  Last-Stream-ID (31)                        |
    +-+-------------------------------------------------------------+
    |                      Error Code (32)                          |
    +---------------------------------------------------------------+
    |                  Additional Debug Data (*)                    |
    +---------------------------------------------------------------+
    */

    private int _lastStreamId;
    private byte[] _data;
    private ErrorCodes _errCode;

    // todo: check go away use again ...
    public GoAwayReader(byte[] b) {
        if (b.length < 8) {
            throw new RuntimeException("Data length has to be at least 8 bytes.");
        }

        _lastStreamId = 0;
        _data = new byte[0];
        read(b);
    }

    /**
     * stat = 0 bad length, -1 ok.
     * @param b
     * @return
     */
    private void read(byte[] b) {

        // todo: can you read debug data properly???
        // todo: fix this ...
        final BitManImp bks = new BitManImp();
        bks.append(b[0]);
        bks.append(b[1]);
        bks.append(b[2]);
        bks.append(b[3]);
        bks.setBits(0, "0");

        _lastStreamId = Integer.parseInt(bks.getBits(0), 2);
        bks.clear();

        bks.append(b[4]);
        bks.append(b[5]);
        bks.append(b[6]);
        bks.append(b[7]);
        final long errorCode;
        errorCode = Long.parseUnsignedLong(bks.getBits(0), 2);

        _data = Arrays.copyOfRange(b, 8, b.length);
        _errCode = Utils.getErrorCode(errorCode);
    }

    public byte[] getData() {
        return _data;
    }

    public int getLastStreamId() {
        return _lastStreamId;
    }

    public ErrorCodes getErrorCode() {
        return _errCode;
    }

    public String toString() {
        return  _lastStreamId + "/" + Arrays.hashCode(_data) + "/" + _errCode;
    }
}
