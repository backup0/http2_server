package snarky.stork.http2;

public class DetReturn<D, V> {

    private final D _det;
    private final V _val;

    public DetReturn(D det, V val) {
        _det = det;
        _val = val;
    }

    public D getDeterminant() {
        return _det;
    }

    public V getValue() {
        return _val;
    }
}
