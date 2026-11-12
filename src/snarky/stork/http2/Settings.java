package snarky.stork.http2;

public enum Settings {

    // code, min, max, initial/default value ...
    HEADER_TABLE_SIZE(0x1, 0, Long.MAX_VALUE, 4096),
    ENABLE_PUSH (0x2, 0, 1, 1),
    MAX_CONCURRENT_STREAMS (0x3, 0, Long.MAX_VALUE, Long.MAX_VALUE),
    INITIAL_WINDOW_SIZE (0x4, 0, 2147483647, 65535),
    MAX_FRAME_SIZE (0x5, 0, 16777215, 16384),
    MAX_HEADER_LIST_SIZE (0x6, 0, Long.MAX_VALUE, Long.MAX_VALUE),
    X_UNKNOWN(-1, 0, 0, 0);

    private final int _code;
    private final long _minValue;
    private final long _maxValue;
    private final long _initVal;

    Settings(int code, long min, long max, long intVal) {
        _code = code;
        _minValue = min;
        _maxValue = max;
        _initVal = intVal;
    }

    public int getCode() {
        return _code;
    }

    public long getMinValue() {
        return _minValue;
    }

    public long getMaxValue() {
        return _maxValue;
    }

    public long getInitialValue() {
        return _initVal;
    }

    public String toString() {
        return "(" + _code  + ") min: "
                + _minValue + ", max: " + _maxValue
                + ", init : " + _initVal;
    }
}
