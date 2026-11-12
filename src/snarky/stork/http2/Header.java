package snarky.stork.http2;

public class Header {

    // todo: can't we move this out?
    public enum Rest {
        NO_INDEX,
        STRICT,
        NEW_NAME,
        NO_HUFFMAN
    }

    public final Rest[] rest2;
    private final HeaderExt _http2Ext;

    public final String key;
    public final String value;

    /*
    Indexed -> been indexed already INDEXED
    -------------------------------------
    Indexed Name / index LIT
    New Name / index LIT_NEW_NAME
    --------------------------------------
    Indexed name / no-index LIT_NO_INDEX
    new Name / no-index LIT_NEW_NAME_NO_INDEX
    --------------------------------------
    Indexed name / never-index LIT_NEVER
    new name / never-never index LIT_NEW_NAME_NEVER
    ---------------------------------------
    NO_FUSS
     */

    // todo: mod this.

    public Header(String key) {
        this(key,"", new HeaderExt(), new Rest[0]);
    }

    public Header(String key, String value) {
        this(key,value, new HeaderExt(), new Rest[0]);
    }

    public Header(String key, HeaderExt h2Ext) {
        this(key,"", h2Ext, new Rest[0]);
    }

    public Header(String key, String value, HeaderExt h2Ext) {
        this(key,value, h2Ext, new Rest[0]);
    }

    public Header(String key, Rest... restriction) {
        this(key, "", new HeaderExt(), restriction);
    }

    public Header(String key, String value, Rest... restriction) {
        this(key, value, new HeaderExt(), restriction);
    }

    private Header(String key, String value, HeaderExt h2Ext, Rest[] restriction) {
        this.key = key;
        this.value = value;
        _http2Ext = h2Ext;
        rest2 = restriction;
    }

    public HeaderExt getHTTP2Extension() {
        return _http2Ext;
    }

    public String toString() {
        return key + " = " + value;
    }
}
