package snarky.stork.http2.hpack;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManException;
import snarky.stork.bitman.StringBit;

import java.util.Arrays;

public class IntDecoder {

    private int _prefSize = 0;

    public Long restart(int prefixSize, byte firstByte) {

        m0 = 0;
        i0 = 0;
        _prefSize = 0;


        //?? Needs fixing .. morun.
        int val;
        switch (prefixSize) {
            case 8:
                val = firstByte;
                break;
            case 7:
                val = firstByte & 127;
                break;
            case 6:
                val = firstByte & 63;
                break;
            case 5:
                val = firstByte & 31;
                break;
            case 4:
                val = firstByte & 15;
                break;
            case 3:
                val = firstByte & 7;
                break;
            case 2:
                val = firstByte & 3;
                break;
            case 1:
                val = firstByte & 1;
                break;
            default:
                throw new RuntimeException("Blah");
        }

        final long max = BitCon.power(2, prefixSize) - 1;

        if (val < max) {
            return (long) val;
        }
        else {
            _prefSize = prefixSize;
            return null;
        }
    }

    private int m0 = 0;
    private long i0 = 0;

    public Long update(byte b) {

        if (_prefSize == 0) {
            throw new RuntimeException("Blahsz ...");
        }

        final byte[] bits = BitCon.byteToBits(b);
        final boolean last = bits[0] == 0; // the first bit is 1 for the last byte

        bits[0] = 0; // so we don't have to use subarray to convert, duh ..

        final long val = BitCon.base2toDecimal(bits);
        i0 = i0 + val * BitCon.power(2, m0);
        m0 = m0 + 7;

        if (last) {
            return (i0 + (BitCon.power(2, _prefSize)) - 1);
        }
        else {
            return null;
        }
    }

    public static long decode(int prefixSize, String sb) throws BitManException
    {
        final byte[] valInBits = StringBit.toBits(sb);
        if (valInBits[0] == 1) // pref is filled - meaning it's all 1
        {
            final byte[] valSansPrefix = Arrays.copyOfRange(valInBits, prefixSize, valInBits.length);

            int m = 0;
            long i = 0;

            final int byteCount = valSansPrefix.length / 8;
            int index = 0;
            while (index < byteCount)
            {
                final int nextIndx = index++ * 8;
                final byte[] bits = Arrays.copyOfRange(valSansPrefix, nextIndx, nextIndx + 8);
                bits[0] = 0; // the first bit is a flag, the last octet has 1, don't want to include
                // this when converting to decimal.
                final long val = BitCon.base2toDecimal(bits);
                i = i + val * BitCon.power(2, m);
                m = m + 7;
            }
            return (i + (BitCon.power(2, prefixSize)) - 1);
        }
        else
        {
            final byte[] bits = Arrays.copyOfRange(valInBits, 0, prefixSize);
            return BitCon.base2toDecimal(bits);
        }
    }
}
