package snarky.stork.http2;

public enum FrameType {

    UNKNOWN((byte) -1),
    DATA ((byte) 0),
    HEADERS ((byte) 1),
    PRIORITY ((byte) 2),
    RST_STREAM ((byte) 3),
    SETTINGS ((byte) 4),
    PUSH_PROMISE ((byte) 5),
    PING ((byte) 6),
    GO_AWAY((byte) 7),
    WINDOW_UPDATE ((byte) 8),
    CONTINUATION((byte) 9);

    private final byte _val;

    FrameType(byte b) {
        _val = b;
    }

    public byte getValue() {
        return _val;
    }

}
