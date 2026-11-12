package snarky.stork.http2.in;

import snarky.stork.bitman.BitManImp;

public class WindowUpdateReader {

    private boolean _flag;
    private int _val;

    public WindowUpdateReader(byte[] b) {
        if (b.length != 4) {
            throw new RuntimeException("Data length has to be 4.");
        }

        _flag = false;
        _val = 0;
        read(b);
    }

    private void read(byte[] b) {

        final BitManImp bks = new BitManImp();
        bks.append(b);

        _flag = bks.getBit(1) != 0;
        _val = Integer.parseInt(bks.getBits(1), 2);
    }

    public boolean getFlag() {
        return _flag;
    }

    public int getValue() {
        return _val;
    }

    public String toString() {
        return _flag + "/" + _val;
    }
}
