package snarky.stork.http2.out;

import snarky.stork.bitman.BitManImp;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.*;

import java.util.List;

public class SettingsMaker extends OutputMaker {

    private final List<Setting> _settings;
    private final boolean _ack;
    private boolean _hasMoreFrame;

    /**
     *
     * @param settings
     * @param ack
     */
    public SettingsMaker(List<Setting> settings, boolean ack) {
        // if setting has to broken down - let's send them one after another.
        super(new Stream(), false, true);
        _settings = settings;
        _ack = ack;
        _hasMoreFrame = true;
    }

    @Override
    public OutputData getDataOut(int maxFrameSize) {

        if (!_hasMoreFrame) {
            return new OutputData(-1);
        }

        // conn credit is irrelevant here ... maxFrameSize is, well - just make sure we don't go over.
        final FrameMaker fm = new FrameMaker(FrameType.SETTINGS);
        fm.setStreamId(SpecialValues.CONN_STREAM_ID);

        final BitManImp bks = new BitManImp();

        if (_ack) {
            // if acknowledgement, there's no payload.
            fm.setFlag0();
            // so we can set _lastFrame properly. This is supposed to be empty anyway.
            _settings.clear();
        }
        else {

            final int codeLen = 16; // identifier code
            final int valLen = 32; // the value

            // maxCount is the number of settings we can cram into a single frame.
            // if it's 0, it means the frame size is too small, can't even put one. That's an error.
            final int maxCount = (maxFrameSize * 8) / (codeLen + valLen);
            if (maxCount <= 0) {
                // todo: better way to handle this. Just catch then kill the connection ....
                throw new RuntimeException("Frame size is too small.");
            }

            int count = 0;
            while (!_settings.isEmpty()) {

                if (count >= maxCount) {
                    break;
                }

                final Setting hs = _settings.remove(0);
                // convert the values into - bits
                final String iCode = Integer.toBinaryString(hs.getSetting().getCode());
                final String iVal = Long.toBinaryString(hs.getValue());
                // pad with 0 - because, we need that
                // todo: possible to optimize this - push straight into Kamporos, then set bits
                final String code = StringBit.padBits(iCode, codeLen);
                final String val = StringBit.padBits(iVal, valLen);
                // store the payload.
                bks.append(new byte[6]);
                bks.setBits( count * 8, code + val);
                ++count;
            }
        }
        fm.setLength(bks.size());
        fm.setPayload(bks.getBytes());
        _hasMoreFrame = !_settings.isEmpty();

        return new OutputData(-1, fm);
    }

    @Override
    public boolean hasMoreFrame() {
        return _hasMoreFrame;
    }

    @Override
    public FrameType getFrameType() {
        return FrameType.SETTINGS;
    }
}
