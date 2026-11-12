package snarky.stork.http2;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;

public class FrameMeta {

    private final int _length; // max: 16,777,215
    private final FrameType _type;
    private final byte _flag;
    private final int _streamId; // max: 2,147,483,647

    /**
     * This create UNKNOWN frame, meant to be used as a placeholder or something similar.
     */
    public FrameMeta() {
        _length = 0;
        _type = FrameType.UNKNOWN;
        _flag = 0;
        _streamId = 0;
    }

    public FrameMeta(byte[] b) {
        if (b.length != 9) {
            throw new RuntimeException("Wrong length, expect 9.");
        }

        final BitManImp bks = new BitManImp();
        bks.append(b);

        // figure out frame type.
        final byte t = bks.getByte(3);
        FrameType ft_temp = null;
        for (FrameType ft : FrameType.values() ) {
            if (ft.getValue() == t) {
                ft_temp = ft;
                break;
            }
        }

        // bad type = unknown. Absolutely no need to throw exception.
        _type = (ft_temp == null) ? FrameType.UNKNOWN : ft_temp;
        _flag = bks.getByte(4);
        final byte[] len = StringBit.toBits(bks.getBits(0, 24));
        _length = (int) BitCon.base2toDecimal(len);
        final String s = bks.getBits(41, 31);
        _streamId = (int) BitCon.base2toDecimal(StringBit.toBits(s));
    }

    public int getStreamId() {
        return _streamId;
    }

    public int getLength() {
        return _length;
    }

    public FrameType getType() {
        return _type;
    }

    public byte getFlag() {
        return _flag;
    }

    public boolean getFlag0() {
        return (_flag & 1) != 0;
    }

    public boolean getFlag1() {
        return (_flag & 2) != 0;
        // 2
    }

    public boolean getFlag2() {
        return (_flag & 4) != 0;
        // 4
    }

    public boolean getFlag3() {
        return (_flag & 8) != 0;
        // 8
    }

    public boolean getFlag4() {
        return (_flag & 16) != 0;
        //16
    }

    public boolean getFlag5() {
        return (_flag & 32) != 0;
        //32
    }

    public boolean getFlag6() {
        return (_flag & 64) != 0;
        // 64 <-- yeashs that's 64
    }

    public boolean getFlag7() {
        return (_flag & -128) != 0;
        // -128 <-- 1000 0000, 2's complement biatch
    }
}
