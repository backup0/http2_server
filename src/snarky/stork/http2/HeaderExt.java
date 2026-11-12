package snarky.stork.http2;

import snarky.stork.http2.header.HeaderDeterminant;

public class HeaderExt {

    private HeaderDeterminant.Det _det;
    private boolean _ne;
    private boolean _ve;
    private int _index;

    public HeaderExt() {
        this(HeaderDeterminant.Det.INDEX, false, false, 0);
    }

    public HeaderExt(HeaderDeterminant.Det det, boolean nameHuff, boolean valHuff, int index) {
        _det = det;
        _ne = nameHuff;
        _ve = valHuff;
        _index = index;
    }

    public HeaderDeterminant.Det getDeterminant() {
        return _det;
    }

    public int getIndex() {
        return _index;
    }

    /**
     * if 0, it means name is set - else, name is not set.
     * @return
     */
    public int index() {
        return _index;
    }

    /**
     * Whether the name is huffman encoded
     * @return
     */
    public boolean nameHuffmanEncoded() {
        return _ne;
    }

    public boolean valueHuffmanEncoded() {
        return _ve;
    }

    public String toString() {
        return _det + "/n:" + _ne + "/v:" + _ve + "@" + _index;
    }

}
