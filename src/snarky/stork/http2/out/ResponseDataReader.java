package snarky.stork.http2.out;

import snarky.stork.http2.DetReturn;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ResponseDataReader {

    /**
     *
     * @param ins
     * @param limit
     * @return Determinant: true if there's more data, false if there isn't. Value: the data.
     * @throws IOException
     */
    //?? this is used by headers + data, should be modified.
    public static DetReturn<Boolean, byte[]> getData(InputStream ins, int limit) throws IOException {

        // todo: don't have to wait until we have 0 frame - ok?
        final byte[] buf = new byte[limit];
        final int read = ins.read(buf);

        if (read >= 0) {
            return new DetReturn<>(true, getBytesRead(read, buf));
        }
        else {
            return new DetReturn<>(false, new byte[0]);
        }

        //todo: clean up ...
        /*
        final int available = ins.available();
        
        final byte[] b;
        if (available > limit) {
            // no problem
            b = new byte[limit];
            final int byteRead = ins.read(b);
            return new XReturn<>(true, getBytesRead(byteRead, b));
        }
        else { // we can't fill the frame, maybe we're @ the end.
            b = new byte[available];
            final int byteRead = ins.read(b);

            final boolean hasMore = ins.read(new byte[0]) != -1;

            return new XReturn<>(hasMore, getBytesRead(byteRead, b));
        }
         */
    }

    private static byte[] getBytesRead(int i, byte[] b) {
        return Arrays.copyOfRange(b, 0, i);
    }
}
