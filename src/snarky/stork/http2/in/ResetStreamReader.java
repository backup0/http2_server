package snarky.stork.http2.in;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.ErrorCodes;
import snarky.stork.http2.Utils;

public class ResetStreamReader {

    private ErrorCodes _errorCode;

    public ResetStreamReader(byte[] b) {
        if (b.length != 4) {
            throw new RuntimeException("Data length has to be 4.");
        }
        read(b);
    }

    private void read(byte[] b) {

        final BitManImp bks = new BitManImp();
        bks.append(b);

        final long i = Long.parseUnsignedLong(bks.getBits(0, 32), 2);
        _errorCode = Utils.getErrorCode(i);
    }

    public ErrorCodes getErrorCode() {
        return _errorCode;
    }

    public String toString() {
        return "" + _errorCode;
    }
}
