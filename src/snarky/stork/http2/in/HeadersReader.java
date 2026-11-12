package snarky.stork.http2.in;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.FrameMeta;

import java.util.Arrays;

public class HeadersReader {
/*
+---------------+
|Pad Length? (8)|
+-+-------------+-----------------------------------------------+
|E|                 Stream Dependency? (31)                     |
+-+-------------+-----------------------------------------------+
|  Weight? (8)  |
+-+-------------+-----------------------------------------------+
|                   Header Block Fragment (*)                 ...
+---------------------------------------------------------------+
|                           Padding (*)                       ...
+---------------------------------------------------------------+

The HEADERS frame payload has the following fields:

Pad Length:
    An 8-bit field containing the length of the frame padding in units of octets.
    This field is only present if the PADDED flag is set.
E:
    A single-bit flag indicating that the stream dependency is exclusive (see Section 5.3).
    This field is only present if the PRIORITY flag is set.
Stream Dependency:
    A 31-bit stream identifier for the stream that this stream depends on (see Section 5.3).
    This field is only present if the PRIORITY flag is set.
Weight:
    An unsigned 8-bit integer representing a priority weight for the stream (see Section 5.3).
    Add one to the value to obtain a weight between 1 and 256. This field is only present if the PRIORITY flag is set.
Header Block Fragment:
    A header block fragment (Section 4.3).
Padding:
    Padding octets.

The HEADERS frame defines the following flags:

END_STREAM (0x1):
    When set, bit 0 indicates that the header block (Section 4.3) is the last that the endpoint
    will send for the identified stream.
    A HEADERS frame carries the END_STREAM flag that signals the end of a stream.
    However, a HEADERS frame with the END_STREAM flag set can be followed by
    CONTINUATION frames on the same stream. Logically, the CONTINUATION frames are part of the HEADERS frame.
END_HEADERS (0x4):
    When set, bit 2 indicates that this frame contains an entire header block (Section 4.3)
    and is not followed by any CONTINUATION frames.
    A HEADERS frame without the END_HEADERS flag set MUST be followed by a
    CONTINUATION frame for the same stream. A receiver MUST treat the receipt of any other type
    of frame or a frame on a different stream as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
PADDED (0x8):
    When set, bit 3 indicates that the Pad Length field and any padding that it describes are present.
PRIORITY (0x20):
    When set, bit 5 indicates that the Exclusive Flag (E), Stream Dependency,
    and Weight fields are present; see Section 5.3.
*/

    // todo: logical check ...
    private int _rtnCode;
    private byte[] _hBlock;
    private boolean _exc;
    private int _streamDep;
    private int _weight;

    public HeadersReader(FrameMeta fm, byte[] b) {
        _rtnCode = -1;
        _hBlock = new byte[0];
        _exc = false;
        _streamDep = 0;
        _weight = 0;
        read(fm, b);
    }

    private void read(FrameMeta fm, byte[] b) {
        // flags:
        // padded - 08 #3
        // priority - 020 #5
        final int frameSize = b.length;
        final boolean prioritySet = fm.getFlag5();

        // it's padded
        if (fm.getFlag3()) {
            // pad length
            final int padLen = Byte.toUnsignedInt(b[0]);
            final boolean lengthOk;
            // do sanity check
            if (prioritySet) {
                // 1 byte for pad lengths
                // 4 bytes for stream id & exclusive flag (1 bit)
                // 1 byte for weight.
                lengthOk = frameSize >= (padLen + 6);
            }
            else {
                // payload has to be @ least the size of padding + one byte for padding size
                // (data length = 0)
                lengthOk = frameSize > padLen;
            }

            if (lengthOk) {
                b = Arrays.copyOfRange(b,1, b.length - padLen);
            }
            else {
                _rtnCode = 0;
                return;
            }
        }

        if (prioritySet) {
            final BitManImp bks = new BitManImp();
            bks.append(b);

            _exc = bks.getBit(0) != 0; // first bit is flag.
            // we start @ bit #9, first byte(0 - 7) is pad length, 8 = excFlag
            _streamDep = Integer.parseInt(bks.getBits(1, 31), 2);
            // weight is @ the fifth octet.
            _weight = bks.getByte(4);
            _hBlock = bks.getBytes(5); // we've removed the pad already, so from byte 5, data.
        }
        else {
            _hBlock = b;
        }
    }

    public int getReturnCode() {
        return _rtnCode;
    }

    public byte[] getHeaderBlock() {
        return _hBlock;
    }

    public int getStreamDep() {
        return _streamDep;
    }

    public boolean getExclusiveFlag() {
        return _exc;
    }

    public int getWeight() {
        return _weight;
    }

    public String toString() {
        return _rtnCode + "/" + Arrays.hashCode(_hBlock) + "/" +
                _exc + "/" + _streamDep + "/" +
                _weight;
    }

}
