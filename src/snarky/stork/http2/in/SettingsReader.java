package snarky.stork.http2.in;

import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.FrameMeta;
import snarky.stork.http2.Setting;
import snarky.stork.http2.Settings;
import snarky.stork.http2.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettingsReader {

    // ACK 01
    private boolean _ack;
    private List<Setting> _settings;

    public SettingsReader(FrameMeta fm, byte[] b) {
        if (b.length % 6 != 0) {
            throw new RuntimeException("Data length has to be a multiple of 6.");
        }

        _ack = false;
        _settings = Collections.emptyList();
        read(fm, b);
    }

    public boolean isAck() {
        return _ack;
    }

    public List<Setting> getSettings() {
        return _settings;
    }

    /**
     * stat -1: no error
     * stat 0: frame size error, not mutiple of 6
     * stat 1: frame size error, acknowledgement with data.
     * @param fm
     * @param data
     * @return
     */
    private void read(FrameMeta fm, byte[] data) {

        // flag0 = acknowledgement
        _ack = fm.getFlag0();

        final ArrayList<Setting> settings = new ArrayList<>();
        final BitManImp bks = new BitManImp();

        for (int i = 0; i < data.length; i = i + 6) {
            // code 2 bytes
            final byte[] id = new byte[]{data[i], data[i+1]};
            // data 4 bytes
            final byte[] val = new byte[]{data[i+2], data[i+3], data[i+4], data[i+5]};

            bks.append(id);
            final int id0 = Integer.parseUnsignedInt(bks.getBits(0), 2);
            bks.clear();

            bks.append(val);
            long val0 = Long.parseUnsignedLong(bks.getBits(0), 2);
            bks.clear();

            final Settings setting = Utils.getSetting(id0);
            // wrong code can be ignored, not an error
            if (setting != null) {
                settings.add(new Setting(setting, val0));
            }
        }
        _settings = settings;
    }

    public String toString() {
        return _ack + "/" + Arrays.toString(_settings.toArray(new Setting[0]));
    }
}
