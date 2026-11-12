package snarky.stork.http2.out;

import snarky.stork.http2.FrameMaker;

public class OutputData {

    private final byte[] _header;
    private final byte[] _payload;
    private final int _rtnCode;

    /**
     * Blank output data ...
     * @param rtnCode
     */
    public OutputData(int rtnCode) {
        _rtnCode = rtnCode;
        _header = new byte[0];
        _payload = new byte[0];
    }

    public OutputData(int rtnCode, FrameMaker fm) {
        _rtnCode = rtnCode;
        _header = fm.getHeader();
        _payload = fm.getPayload();
    }

    public byte[] getHeader() {
        return _header;
    }

    public byte[] getPayload() {
        return _payload;
    }

    public int returnCode() {
        return _rtnCode;
    }
}

