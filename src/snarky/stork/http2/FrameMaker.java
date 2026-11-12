package snarky.stork.http2;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;

import java.util.Arrays;

public class FrameMaker {

    private final FrameType _type;
    private int _streamId;
    private byte[] _flags;
    private int _length;
    private boolean _rFlag;
    private byte[] _data;

    /**
     * Use this to construct HTTP 2 frame.
     * @param type
     */
    public FrameMaker(FrameType type) {
         _type = type;
         _streamId = -1;
         _flags = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
         _length = -1;
         _rFlag = false;
         _data = new byte[0];

    }

    public void setStreamId(int id) {
        _streamId = id;
    }

    public void setLength(int len) {
        _length = len;
    }

    public void setFlag0() { _flags[7] = 1;}

    public void setFlag1() {
        _flags[6] = 1;
    }

    public void setFlag2() {
        _flags[5] = 1;
    }

    public void setFlag3() {
        _flags[4] = 1;
    }

    public void setFlag4() {
        _flags[3] = 1;
    }

    public void setFlag5() {
        _flags[2] = 1;
    }

    public void setFlag6() {
        _flags[1] = 1;
    }

    public void setFlag7() {
        _flags[0] = 1;
    }

    public byte[] getHeader() {

        if (_streamId < 0 || _length < 0) {
            throw new RuntimeException("Invalid length or stream id.");
        }

        String s_len = Integer.toBinaryString(_length);
        String s_id = Integer.toBinaryString(_streamId);
        String s_type = StringBit.getAsString(BitCon.byteToBits(_type.getValue()));
        String s_flag = StringBit.getAsString(_flags);

        s_len = padBits(s_len, 24);
        s_id = (_rFlag ? "1" : "0") + padBits(s_id, 31);

        final String s = s_len + s_type + s_flag + s_id;

        final byte[] b = new byte[9];
        Arrays.fill(b, (byte) 0);

        final BitManImp bks = new BitManImp();
        bks.append(b);
        bks.setBits(0, s);

        return bks.getBytes();
    }
    
    private String padBits(String s, int len) {
        final StringBuilder sb = new StringBuilder(s);
        
        while (sb.length() < len) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

    public void setPayload(byte[] data) {
        _data = data;
    }

    public byte[] getPayload() {
        return _data == null ? new byte[0] : _data;
    }
}
