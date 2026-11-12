package snarky.stork.http2.out;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class OutputBuffer {

    private final ArrayList<byte[]> _byteList = new ArrayList<>();
    private int _outSize = 1024 * 30 ;
    private int _marker = 0;

    public void put(byte[] b) {
        _byteList.add(b);
    }

    public byte[] get() {

        final int retrieveLocation = 0;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int remaining = _outSize;

        while (remaining > 0) {
            // otherwise we'll get some exception
            if (_byteList.isEmpty()) {
                break;
            }

            // pick a byte array from the list
            final byte[] b = _byteList.get(retrieveLocation);

            // get length of 'unconsumed' data
            final int length = b.length - _marker;
            // data to write can fit the buffer, entirely.
            if (length <= remaining) {
                // we can store the whole thing.
                bos.write(b, _marker, length);
                remaining = remaining - length;
                // used up - so remove
                _byteList.remove(retrieveLocation);
                _marker = 0;
            }
            // we still have some remaining...
            else {
                // remaining is less than length
                bos.write(b, _marker, remaining);
                _marker = _marker + remaining;
                remaining = 0;
            }
        }
        return bos.toByteArray();
    }

    public void purge() {
        _byteList.clear();
        _marker = 0;
    }

    public void setOutputSize(int i) {
        _outSize = i;
    }

    public int size() {
         return _byteList.size();
    }
}
