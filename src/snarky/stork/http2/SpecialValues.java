package snarky.stork.http2;

public class SpecialValues {

    public static final int CONN_STREAM_ID = 0;
    public static final int HEADER_OVERHEAD = 32;
    public static final int HEADER_TBL_SIZE = 4096;
    public static final long MAX_WINDOW_SIZE = 2147483647; // ((2^31)-1) this is max, can't get any higher.
    public static final long INIT_WIN_SIZE = 65535;
    public static final int MAX_FRAME_SIZE = 16384; // 2^14;
    public static final int MAX_STREAM_ID = 2147483647;
}
