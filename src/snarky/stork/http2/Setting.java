package snarky.stork.http2;

public class Setting {

    private final Settings _setting;
    private final long _value;

    public Setting(Settings setting, long val) {
        _setting = setting;
        _value = val;
    }

    public Settings getSetting() {
        return _setting;
    }

    public long getValue() {
        return _value;
    }

    public String toString() {
        return _setting.name() + "/" + _value;
    }
}
