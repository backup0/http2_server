package snarky.stork.http2.hpack;

public class HT2String {

    public static HT2String getBadlyEncodedString() {
        return new HT2String(false, true, "");
    }

    public static HT2String getHuffmanEncodedString(String s) {
        return new HT2String(true, true, s);
    }

    public static HT2String getPlainString(String s) {
        return new HT2String(true, false, s);
    }

    public final boolean huffmanEncoded;
    public final boolean ok;
    public final String string;

    public HT2String(boolean v, boolean hec, String s) {
        huffmanEncoded = hec;
        ok = v;
        string = s;
    }
}
