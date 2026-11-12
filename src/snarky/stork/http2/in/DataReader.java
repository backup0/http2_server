package snarky.stork.http2.in;

import snarky.stork.bitman.BitCon;
import snarky.stork.http2.FrameMeta;

import java.util.Arrays;

public class DataReader {

    private int _rtnCode;
    private byte[] _data;

    public DataReader(FrameMeta fm, byte[] data) {
        _rtnCode = -1;
        _data = new byte[0];
        read(fm, data);
    }

    private void read(FrameMeta fm, byte[] data) {

        // padded flag
        if (fm.getFlag3()) {
            final int padLen = BitCon.byteToInt(data[0]);

            // one byte is for setting the pad length, so the total (pad length + data + pad) has to be
            // bigger than pad length.
            if (data.length > padLen) {
                // 1 byte for pad length, then the padding.
                final int dataLen = data.length - padLen - 1;
                _data = Arrays.copyOfRange(data, 1, 1 + dataLen);
            }
            else {
                _rtnCode = 0;
            }
        } else {
            _data = data;
        }
    }

    /**
     *
     * @return -1: ok, valid instance. 0: wrong length.
     */
    public int getReturnCode() {
        return _rtnCode;
    }

    public byte[] getData() {
        return _data;
    }

    public String toString() {
        return _rtnCode + "/" + Arrays.hashCode(_data);
    }
}
