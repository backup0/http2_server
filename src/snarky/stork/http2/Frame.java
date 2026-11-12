package snarky.stork.http2;

public class Frame extends FrameMeta {

    private final byte[] _data;

    public Frame() {
        super();
        _data = new byte[0];
    }

    public Frame(byte[] header, byte[] data) {
        super(header);
        _data = data;
    }

    public byte[] getData() {
        return _data;
    }
}
