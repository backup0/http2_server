package snarky.stork.http2.btree;

import java.util.ArrayList;

public class BTree<T> {

    public final int ROOT_INDEX = 0;
    private final ArrayList<BTreeNode> _nodes;
    private final ArrayList<T> _data;

    public BTree() {
        _nodes = new ArrayList<>();
        _nodes.add(new BTreeNode());
        _data = new ArrayList<>();
    }

    public void add(T val, String xe) {
        int index = ROOT_INDEX; // 0 is the root
        final char[] chars = xe.toCharArray();
        // we start by adding stuff @ the root.
        for (char c : chars) {
            final BTreeNode currentNode = _nodes.get(index);
            final int nxtNodeIndex = _nodes.size();

            if (c == '0') // want to add to 0 branch
            {
                if (currentNode.v0 == null) {
                    final BTreeNode nNode = new BTreeNode();
                    _nodes.add(nxtNodeIndex, nNode);
                    currentNode.v0 = nxtNodeIndex;
                    index = nxtNodeIndex;
                } else {
                    // move to 0 branch
                    index = currentNode.v0;
                }
            } else // .. 1 branch
            {
                if (currentNode.v1 == null) {
                    final BTreeNode nNode = new BTreeNode();
                    _nodes.add(nxtNodeIndex, nNode);
                    currentNode.v1 = nxtNodeIndex;
                    index = nxtNodeIndex;
                } else {
                    // move to 1 branch
                    index = currentNode.v1;
                }
            }
        }

        final BTreeNode lastNode = _nodes.get(index);
        if (lastNode.v0 == null && lastNode.v1 == null) {
            int loc = _data.size();
            _data.add(loc,val);
            lastNode.val = loc;
            //BTreeLeafNode ln = new BTreeLeafNode(val);
            //_enno.set(index, ln);
        } else {
            //w("EKRORS ...");
            // todo: what to do on error?
        }
    }

    public BTreeNode getNode(int loc) {
        return _nodes.get(loc); //??? what if we're outside?'
    }

    public T getData(BTreeNode node) {
        if (node.val == null) {
            return null;
        }
        else {
            return _data.get(node.val);
        }
    }
}