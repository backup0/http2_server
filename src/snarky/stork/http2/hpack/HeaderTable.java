package snarky.stork.http2.hpack;

import snarky.stork.http2.SpecialValues;

import java.util.ArrayList;

public class HeaderTable {

    private final ArrayList<String[]> _entries;
    private final int STATIC_TBL_LEN;

    private long _size;

    private long _maxSizeSetByPeer;
    private long _maxSizeSetByUs;

    public HeaderTable() {
        this(SpecialValues.HEADER_TBL_SIZE);
    }

    public HeaderTable(long maxSize) {
        _entries = new ArrayList<>();
        STATIC_TBL_LEN = StaticHeaderTable.entries.length;

        _maxSizeSetByPeer = SpecialValues.HEADER_TBL_SIZE;
        _maxSizeSetByUs = maxSize;

        _size = 0;
    }

    public int[] getIndice(String name, String value) {

        final int dynTblSize = _entries.size();
        final int totSize = STATIC_TBL_LEN + dynTblSize;

        int[] rtnVal = new int[]{};

        for (int i = 1; i < totSize; i++) {

            final String[] entry;
            // retrieve from static table
            if (i < STATIC_TBL_LEN) {
                entry = StaticHeaderTable.entries[i];
            }
            // retrieve from dynamic table
            else {
                entry = _entries.get(i - STATIC_TBL_LEN);
            }

            // we've found an entry with a matching name
            if (entry[0].compareTo(name) == 0) {
                // assign the value, if we've found a match before just ignore it, the lower
                // value has higher priority.
                if (rtnVal.length == 0) rtnVal = new int[]{i};

                // now check if value matches, if it does - we overwrite then we break,
                // this is the entry we want to use.
                if (entry.length >= 2 && entry[1].compareTo(value) == 0) {
                    rtnVal = new int[]{i, i};
                    break;
                }
            }
        }
        return rtnVal;
    }

    /**
     * Get item @ index.
     * @param index The index.
     * @return Entry @ index. Empty => Decoding error (index outside range).
     */
    public String[] getAt(int index) {
        final int totLen = STATIC_TBL_LEN + _entries.size();
        if (index < 0 || index >= totLen) {
            return new String[]{};
        }

        final String[] s;
        if (index < STATIC_TBL_LEN) {
            s = StaticHeaderTable.entries[index];
        }
        else {
            int nIndex = index - STATIC_TBL_LEN;
            if (nIndex >= _entries.size()) {
                s = new String[]{};
            }
            else {
                s = _entries.get(nIndex);
            }
        }
        // todo: (WHAT IF NULL?) return correct value plz.
        return s;
    }

    /**
     * Index this entry.
     */
    public void indexThis(String[] ss) {
        // todo: does it matter if you remove first or index first?
        // you need to pick stuff from the list first.
        _entries.add(0, ss);
        _size = _size + getEntrySize(ss);
        evictIfNecessary();
    }

    public void setMaxSize(long maxSize) {
        _maxSizeSetByPeer = maxSize;
        evictIfNecessary();
    }

    /*
    public void sizar00() {
        evictIfNecessary();
    }
     */

    /**
     * Evict some stuff so size is under maximus.
     */
    private void evictIfNecessary() {

        final long maxSize = Long.min(_maxSizeSetByPeer, _maxSizeSetByUs);

        while (_size > maxSize) {
            final String[] entry = _entries.remove(_entries.size() - 1);
            if (entry.length == 0) continue;
            _size = _size - getEntrySize(entry);
        }
    }

    private int getEntrySize(String[] entry) {

        int entrySize = entry[0].getBytes().length;
        if (entry.length >= 2) {
            entrySize = entrySize + entry[1].getBytes().length;
        }
        return entrySize + SpecialValues.HEADER_OVERHEAD;
    }

    /**
     * index 0 - max size set by peer
     * index 1 - max size set by us.
     * @return
     */
    public long[] getMaxSizes() {
        return new long[]{_maxSizeSetByPeer, _maxSizeSetByUs};
    }
}
