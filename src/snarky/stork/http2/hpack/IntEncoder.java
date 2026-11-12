package snarky.stork.http2.hpack;

import snarky.stork.bitman.*;

import java.util.Arrays;

public class IntEncoder
{

    /**
     * bits preceding the prefix are all 0.
     * @param prefixSize
     * @param value
     * @return
     * @throws BitManException
     */
    public static byte[] encode122(int prefixSize, long value) throws BitManException
    {
        final StringBuilder sbs;
        sbs = new StringBuilder(encode(prefixSize, value));

        while ((sbs.length() % 8 != 0)) {
            sbs.insert(0, "0");
        }

        final BitManImp bks = new BitManImp();
        final byte[] b = new byte[sbs.length() / 8];
        Arrays.fill(b,(byte) 0);

        bks.append(b);
        bks.setBits(0, sbs.toString());

        return bks.getBytes();
    }

    /**
     *
     * @param prefixSize Size of prefix 8 max
     * @param value The integer to encode
     * @return Encoded integer (just the right number of bits)
     * @throws BitManException
     */
    public static String encode(int prefixSize, long value) throws BitManException
    {
        // largest value that the prefix can hold
        final long prefMaxVal = BitCon.power(2, prefixSize) - 1;

        if (value < prefMaxVal) // if the value is less than what prefix can hold we store in that.
        {
            final byte[] valInBit = BitCon.decimalToBase2(value);
            final byte[] prefBits = new byte[prefixSize];
            return StringBit.getAsString(BitOps.or(valInBit, prefBits));
        }
        else
        {
            final BitManImp bitMan = new BitManImp();

            long val = value - prefMaxVal;
            int bitCount = 0;//pfx;

            // need to decrement a bit @ a time
            while (val >= 128) {
                final long v = (val % 128) + 128;
                final byte[] bits = BitCon.decimalToBase2(v);
                bitMan.append(StringBit.getAsString(bits));
                val = val / 128;
                bitCount = bitCount + 8;
            }

            // now encode the remaining value
            byte[] remainder = BitCon.decimalToBase2(val);
            if (remainder.length <= 7) {
                    bitMan.append("?" + StringBit.getAsString(remainder));
            }
            else {
                throw new BitManException("length > 7");
            }

            final String[] filler = new String[]{
                "",
                "1",
                "11",
                "111",
                "1111",
                "11111",
                "111111",
                "1111111"
            };

            return filler[prefixSize] + bitMan.getBits(0,bitMan.size() * 8);
        }
    }

    public static long decode(int prefixSize, String sb) throws BitManException
    {
        throw new BitManException("Wrong stuff!");
        /*
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
        */
    }
}

//////////////
//////////////////////////////////////////////
//   THE ALGORITHM
////////////////////////////////
//////////////////////
/*

    if I < 2^N - 1, encode I on N bits
    else
        encode (2^N - 1) on N bits
        I = I - (2^N - 1)
        while I >= 128
             encode (I % 128 + 128) on 8 bits
             I = I / 128
        encode I on 8 bits

    Pseudocode to decode an integer I is as follows:

    decode I from the next N bits
    if I < 2^N - 1, return I
    else
        M = 0
        repeat
            B = next octet
            I = I + (B & 127) * 2^M
            M = M + 7
        while B & 128 == 128
        return I
*/
