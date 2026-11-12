package snarky.stork.http2.header;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.hpack.IntEncoder;
import snarky.stork.http2.hpack.StringCodex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringEncoder {

    private static int _prefixSize = 7;

    /**
     * Encode string as string block thingy...
     * @param s
     * @param huffman if true, use huffman encoding, if possible - if not possible, it's encoded literally.
     * @return
     */
    public static byte[] encode(String s, boolean huffman) {
        if (huffman) {
            return encode(s);
        }
        else {
            return encodeLiterally(s);
        }
    }

    private static byte[] encode(String s) {
        final String encoded = StringCodex.encodeString(s);

        if (encoded.length() == 0) { // failure - so, it has to be literal.
            return encodeLiterally(s);
        }
        else {
            return encodeHuffmanly(encoded);
        }
    }

    private static byte[] encodeLiterally(String s) {
        final byte[] ebs = s.getBytes(StandardCharsets.UTF_8);
        final String hpLen = "0" + IntEncoder.encode(_prefixSize, ebs.length);
        if (hpLen.length() % 8 != 0) {
            throw new RuntimeException("Fail to encode length.");
        }

        final BitManImp bks = new BitManImp();
        final byte[] b = new byte[hpLen.length() / 8];
        Arrays.fill(b, (byte) 0);
        bks.append(b);
        bks.setBits(0, hpLen);
        bks.append(ebs);
        return bks.getBytes();
    }

    private static byte[] encodeHuffmanly(String s) {
        final int len = s.length() / 8; // how many bytes we need.
        final String hpLen = "1" + IntEncoder.encode(_prefixSize, len); // first few bytes
        if (hpLen.length() % 8 != 0) {
            throw new RuntimeException("Fail to encode length.");
        }

        final BitManImp bks = new BitManImp();
        final byte[] b1 = new byte[hpLen.length() / 8];
        Arrays.fill(b1, (byte) 0);
        bks.append(b1);
        bks.setBits(0, hpLen);

        final BitManImp bks1 = new BitManImp();
        final byte[] b0 = new byte[len];
        Arrays.fill(b0, (byte) 0);
        bks1.append(b0);
        bks1.setBits(0, s);

        final BitManImp bks2 = new BitManImp();
        bks2.append(bks.getBytes());
        bks2.append(bks1.getBytes());
        
        return bks2.getBytes();
    }
}
