package snarky.stork.http2.in;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.BitManImp;

public class PriorityReader {

    /*
     +-+-------------------------------------------------------------+
     |E|                  Stream Dependency (31)                     |
     +-+-------------+-----------------------------------------------+
     |   Weight (8)  | 1 - 256 <-- int, of course,
     +-+-------------+
     */

    private boolean _exc;
    private int _streamDepId;
    private int _weight;
    
    public PriorityReader(byte[] b) {
        if (b.length != 5) {
            throw new RuntimeException("Data length has to be 5.");
        }

        _exc = false;
        _streamDepId = 0;
        _weight = 0;
        read(b);
    }

    public boolean getExclusiveFlag() {
        return _exc;
    }

    public int getStreamDepId() {
        return _streamDepId;
    }

    public int getWeight() {
        return _weight;
    }
    
    private void read(byte[] b) {

        final BitManImp bks = new BitManImp();
        bks.append(b);

        _exc = (bks.getBit(0) != 0);
        _streamDepId = Integer.parseInt(bks.getBits(1, 31), 2);
        _weight = 1 + BitCon.byteToInt(b[4]);
    }

    public String toString() {
        return _exc + "/" + _streamDepId + "/" + _weight;
    }
}
