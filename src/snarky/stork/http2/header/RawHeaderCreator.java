package snarky.stork.http2.header;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.hpack.IntEncoder;

public class RawHeaderCreator {

    private static final int INDEX_ZERO = 0;

    // todo: check ALL of these ...

    /**
     * Entry from index.
     * @param index
     * @return
     */
    public static byte[] fromIndex(int index) {
        // 1[7p]
        final byte[] b = IntEncoder.encode122(7, index);
        final int i = b[0] | 128;
        b[0] = BitCon.intToByte(i);

        return b;
    }

    /**
     * New value, name from index.
     * @param index
     * @param value
     * @return
     */
    public static byte[] newValue(int index, String value, boolean huffman) {
        // 01[6p]
        final byte[] b = IntEncoder.encode122(6, index);
        final int i = b[0] | 64; // 0100 0000
        b[0] = BitCon.intToByte(i);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(value, huffman));

        return bks.getBytes();
    }

    /**
     * New entry. Indexable.
     * @param name
     * @param value
     * @return
     */
    public static byte[] newEntry(String name, String value, boolean huffman) {
        // 01[6p]
        final byte[] b = IntEncoder.encode122(6, INDEX_ZERO);
        final int i = b[0] | 64; // 0100 0000
        b[0] = BitCon.intToByte(i);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(name, huffman));
        bks.append(StringEncoder.encode(value, huffman));
        return bks.getBytes();

    }

    /**
     * New value, name from index. Don'text index.
     * @param index
     * @param value
     * @return
     */
    public static byte[] noIndexNewValue(int index, String value, boolean huffman) {
        // 0000[4p]
        final byte[] b = IntEncoder.encode122(4, index);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(value, huffman));
        return bks.getBytes();
    }

    /**
     * New entry. Don'text index.
     * @param name
     * @param value
     * @return
     */
    public static byte[] noIndexNew(String name, String value, boolean huffman) {
        // 0000[4p]
        final byte[] b = IntEncoder.encode122(4, INDEX_ZERO);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(name, huffman));
        bks.append(StringEncoder.encode(value, huffman));

        return bks.getBytes();
    }

    /**
     * New value, name from index. Never index.
     * @param index
     * @param value
     * @return
     */
    public static byte[] strictNewValue(int index, String value, boolean huffman) {
        // 0001[4p]
        final byte[] b = IntEncoder.encode122(4, index);
        final int i = b[0] | 16; // 0001 0000
        b[0] = BitCon.intToByte(i);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(value, huffman));

        return bks.getBytes();
    }

    /**
     * New entry. Never index.
     * @param name
     * @param value
     * @return
     */
    public static byte[] strictNew(String name, String value, boolean huffman) {
        // 0001[4p]
        final byte[] b = IntEncoder.encode122(4, INDEX_ZERO);
        final int i = b[0] | 16; // 0001 0000
        b[0] = BitCon.intToByte(i);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.append(StringEncoder.encode(name, huffman));
        bks.append(StringEncoder.encode(value, huffman));
        return bks.getBytes();
    }

    /**
     * Update table size.
     * @param size
     * @return
     */
    public static byte[] tableUpdateSize(int size) {
        // 001[5p]
        final byte[] b = IntEncoder.encode122(5, size);
        BitManImp bks = new BitManImp();
        bks.append(b);
        bks.setBits(0, "001");
        return bks.getBytes();
    }
}
