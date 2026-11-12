package snarky.stork.http2.header;

import snarky.stork.bitman.BitCon;
import snarky.stork.http2.btree.BTree;
import snarky.stork.http2.btree.BTreeNode;

// todo: can'text we ...?? can'text we nto use nested enum?
public class HeaderDeterminant {

    public enum Det {
        IN_DYN_TABLE,
        INDEX,
        NO_INDEX,
        STRICT,
        SIZE_UPDATE
    }

    private static final BTree<Det> _bt = new BTree<>();
    static {
        _bt.add(Det.IN_DYN_TABLE, "1"); // indexed
        _bt.add(Det.INDEX, "01"); // inc indexing
        _bt.add(Det.NO_INDEX, "0000"); // no index
        _bt.add(Det.STRICT, "0001"); //never index
        _bt.add(Det.SIZE_UPDATE, "001"); // size
    }

    /**
     * 1 - indexed
     * 2 - index
     * 3 - don'text index
     * 4 - strict
     * 5 - size update.
     * @param b
     * @return
     */
    public static Det getDet(byte b) {
        // there's no invalid values.
        final byte[] bits = BitCon.byteToBits(b);

        BTreeNode node = _bt.getNode(0);

        for (byte bit : bits) {

            if (bit == 0) {
                if (node.v0 != null) {
                    node = _bt.getNode(node.v0);
                }
            }
            else {
                if (node.v1 != null) {
                    node = _bt.getNode(node.v1);
                }
            }
            if (node.val != null) break;
        }

        return _bt.getData(node);
    }
}
