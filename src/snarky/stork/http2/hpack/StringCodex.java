package snarky.stork.http2.hpack;

import snarky.stork.bitman.BitCon;
import snarky.stork.bitman.StringBit;
import snarky.stork.http2.btree.BTree;
import snarky.stork.http2.btree.BTreeNode;

import java.util.HashMap;

public class StringCodex {

    private static final BTree<Integer> _bTree = new BTree<>();
    private static final HashMap<Integer, String> _encoderData = new HashMap<>();
    
    static {
        add();
    }

    public static String encodeString(String s) {
        return doEncodeString(s);
    }

    public static HT2String decodeString(byte[] b) {

        final StringBuilder sbs = new StringBuilder();
        for (byte b0 : b) {
            byte[] ebs = BitCon.byteToBits(b0);
            sbs.append(StringBit.getAsString(ebs));
        }
        return decodeString(sbs.toString());
    }

    public static HT2String decodeString(String sBits) {
        return doDecodeString(sBits);
    }

    private static void add() {
        add(0, "1111111111000");
        add(1, "11111111111111111011000");
        add(2, "1111111111111111111111100010");
        add(3, "1111111111111111111111100011");
        add(4, "1111111111111111111111100100");
        add(5, "1111111111111111111111100101");
        add(6, "1111111111111111111111100110");
        add(7, "1111111111111111111111100111");
        add(8, "1111111111111111111111101000");
        add(9, "111111111111111111101010");
        add(10, "111111111111111111111111111100");
        add(11, "1111111111111111111111101001");
        add(12, "1111111111111111111111101010");
        add(13, "111111111111111111111111111101");
        add(14, "1111111111111111111111101011");
        add(15, "1111111111111111111111101100");
        add(16, "1111111111111111111111101101");
        add(17, "1111111111111111111111101110");
        add(18, "1111111111111111111111101111");
        add(19, "1111111111111111111111110000");
        add(20, "1111111111111111111111110001");
        add(21, "1111111111111111111111110010");
        add(22, "111111111111111111111111111110");
        add(23, "1111111111111111111111110011");
        add(24, "1111111111111111111111110100");
        add(25, "1111111111111111111111110101");
        add(26, "1111111111111111111111110110");
        add(27, "1111111111111111111111110111");
        add(28, "1111111111111111111111111000");
        add(29, "1111111111111111111111111001");
        add(30, "1111111111111111111111111010");
        add(31, "1111111111111111111111111011");
        add(32, "010100");
        add(33, "1111111000");
        add(34, "1111111001");
        add(35, "111111111010");
        add(36, "1111111111001");
        add(37, "010101");
        add(38, "11111000");
        add(39, "11111111010");
        add(40, "1111111010");
        add(41, "1111111011");
        add(42, "11111001");
        add(43, "11111111011");
        add(44, "11111010");
        add(45, "010110");
        add(46, "010111");
        add(47, "011000");
        add(48, "00000");
        add(49, "00001");
        add(50, "00010");
        add(51, "011001");
        add(52, "011010");
        add(53, "011011");
        add(54, "011100");
        add(55, "011101");
        add(56, "011110");
        add(57, "011111");
        add(58, "1011100");
        add(59, "11111011");
        add(60, "111111111111100");
        add(61, "100000");
        add(62, "111111111011");
        add(63, "1111111100");
        add(64, "1111111111010");
        add(65, "100001");
        add(66, "1011101");
        add(67, "1011110");
        add(68, "1011111");
        add(69, "1100000");
        add(70, "1100001");
        add(71, "1100010");
        add(72, "1100011");
        add(73, "1100100");
        add(74, "1100101");
        add(75, "1100110");
        add(76, "1100111");
        add(77, "1101000");
        add(78, "1101001");
        add(79, "1101010");
        add(80, "1101011");
        add(81, "1101100");
        add(82, "1101101");
        add(83, "1101110");
        add(84, "1101111");
        add(85, "1110000");
        add(86, "1110001");
        add(87, "1110010");
        add(88, "11111100");
        add(89, "1110011");
        add(90, "11111101");
        add(91, "1111111111011");
        add(92, "1111111111111110000");
        add(93, "1111111111100");
        add(94, "11111111111100");
        add(95, "100010");
        add(96, "111111111111101");
        add(97, "00011");
        add(98, "100011");
        add(99, "00100");
        add(100, "100100");
        add(101, "00101");
        add(102, "100101");
        add(103, "100110");
        add(104, "100111");
        add(105, "00110");
        add(106, "1110100");
        add(107, "1110101");
        add(108, "101000");
        add(109, "101001");
        add(110, "101010");
        add(111, "00111");
        add(112, "101011");
        add(113, "1110110");
        add(114, "101100");
        add(115, "01000");
        add(116, "01001");
        add(117, "101101");
        add(118, "1110111");
        add(119, "1111000");
        add(120, "1111001");
        add(121, "1111010");
        add(122, "1111011");
        add(123, "111111111111110");
        add(124, "11111111100");
        add(125, "11111111111101");
        add(126, "1111111111101");
        add(127, "1111111111111111111111111100");
        add(128, "11111111111111100110");
        add(129, "1111111111111111010010");
        add(130, "11111111111111100111");
        add(131, "11111111111111101000");
        add(132, "1111111111111111010011");
        add(133, "1111111111111111010100");
        add(134, "1111111111111111010101");
        add(135, "11111111111111111011001");
        add(136, "1111111111111111010110");
        add(137, "11111111111111111011010");
        add(138, "11111111111111111011011");
        add(139, "11111111111111111011100");
        add(140, "11111111111111111011101");
        add(141, "11111111111111111011110");
        add(142, "111111111111111111101011");
        add(143, "11111111111111111011111");
        add(144, "111111111111111111101100");
        add(145, "111111111111111111101101");
        add(146, "1111111111111111010111");
        add(147, "11111111111111111100000");
        add(148, "111111111111111111101110");
        add(149, "11111111111111111100001");
        add(150, "11111111111111111100010");
        add(151, "11111111111111111100011");
        add(152, "11111111111111111100100");
        add(153, "111111111111111011100");
        add(154, "1111111111111111011000");
        add(155, "11111111111111111100101");
        add(156, "1111111111111111011001");
        add(157, "11111111111111111100110");
        add(158, "11111111111111111100111");
        add(159, "111111111111111111101111");
        add(160, "1111111111111111011010");
        add(161, "111111111111111011101");
        add(162, "11111111111111101001");
        add(163, "1111111111111111011011");
        add(164, "1111111111111111011100");
        add(165, "11111111111111111101000");
        add(166, "11111111111111111101001");
        add(167, "111111111111111011110");
        add(168, "11111111111111111101010");
        add(169, "1111111111111111011101");
        add(170, "1111111111111111011110");
        add(171, "111111111111111111110000");
        add(172, "111111111111111011111");
        add(173, "1111111111111111011111");
        add(174, "11111111111111111101011");
        add(175, "11111111111111111101100");
        add(176, "111111111111111100000");
        add(177, "111111111111111100001");
        add(178, "1111111111111111100000");
        add(179, "111111111111111100010");
        add(180, "11111111111111111101101");
        add(181, "1111111111111111100001");
        add(182, "11111111111111111101110");
        add(183, "11111111111111111101111");
        add(184, "11111111111111101010");
        add(185, "1111111111111111100010");
        add(186, "1111111111111111100011");
        add(187, "1111111111111111100100");
        add(188, "11111111111111111110000");
        add(189, "1111111111111111100101");
        add(190, "1111111111111111100110");
        add(191, "11111111111111111110001");
        add(192, "11111111111111111111100000");
        add(193, "11111111111111111111100001");
        add(194, "11111111111111101011");
        add(195, "1111111111111110001");
        add(196, "1111111111111111100111");
        add(197, "11111111111111111110010");
        add(198, "1111111111111111101000");
        add(199, "1111111111111111111101100");
        add(200, "11111111111111111111100010");
        add(201, "11111111111111111111100011");
        add(202, "11111111111111111111100100");
        add(203, "111111111111111111111011110");
        add(204, "111111111111111111111011111");
        add(205, "11111111111111111111100101");
        add(206, "111111111111111111110001");
        add(207, "1111111111111111111101101");
        add(208, "1111111111111110010");
        add(209, "111111111111111100011");
        add(210, "11111111111111111111100110");
        add(211, "111111111111111111111100000");
        add(212, "111111111111111111111100001");
        add(213, "11111111111111111111100111");
        add(214, "111111111111111111111100010");
        add(215, "111111111111111111110010");
        add(216, "111111111111111100100");
        add(217, "111111111111111100101");
        add(218, "11111111111111111111101000");
        add(219, "11111111111111111111101001");
        add(220, "1111111111111111111111111101");
        add(221, "111111111111111111111100011");
        add(222, "111111111111111111111100100");
        add(223, "111111111111111111111100101");
        add(224, "11111111111111101100");
        add(225, "111111111111111111110011");
        add(226, "11111111111111101101");
        add(227, "111111111111111100110");
        add(228, "1111111111111111101001");
        add(229, "111111111111111100111");
        add(230, "111111111111111101000");
        add(231, "11111111111111111110011");
        add(232, "1111111111111111101010");
        add(233, "1111111111111111101011");
        add(234, "1111111111111111111101110");
        add(235, "1111111111111111111101111");
        add(236, "111111111111111111110100");
        add(237, "111111111111111111110101");
        add(238, "11111111111111111111101010");
        add(239, "11111111111111111110100");
        add(240, "11111111111111111111101011");
        add(241, "111111111111111111111100110");
        add(242, "11111111111111111111101100");
        add(243, "11111111111111111111101101");
        add(244, "111111111111111111111100111");
        add(245, "111111111111111111111101000");
        add(246, "111111111111111111111101001");
        add(247, "111111111111111111111101010");
        add(248, "111111111111111111111101011");
        add(249, "1111111111111111111111111110");
        add(250, "111111111111111111111101100");
        add(251, "111111111111111111111101101");
        add(252, "111111111111111111111101110");
        add(253, "111111111111111111111101111");
        add(254, "111111111111111111111110000");
        add(255, "11111111111111111111101110");
        add(256, "111111111111111111111111111111"); // eos
    }

    private static void add(int val, String xe) {
        _bTree.add(val, xe);
        _encoderData.put(val, xe);

        /*
        int index = _mt.ROOT_INDEX; // 0 is the root
        final char[] chars = xe.toCharArray();
        // we start by adding stuff @ the root.
        for (char c : chars) {
            final BTreeNode currentNode = _enno.getFromDynamicTable(index);
            final int nxtNodeIndex = _enno.size();

            if (c == '0') // want to add to 0 branch
            {
                if (currentNode.v0 == null) {
                    final BTreeNode nNode = new BTreeNode();
                    _enno.add(nxtNodeIndex, nNode);
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
                    _enno.add(nxtNodeIndex, nNode);
                    currentNode.v1 = nxtNodeIndex;
                    index = nxtNodeIndex;
                } else {
                    // move to 1 branch
                    index = currentNode.v1;
                }
            }
        }

        final BTreeNode lastNode = _enno.getFromDynamicTable(index);
        if (lastNode.v0 == null && lastNode.v1 == null) {
            LeafNode ln = new LeafNode(val);
            _enno.set(index, ln);
            _enkoderData.put(val, xe);
        } else {
            w("EKRORS ...");
        }

         */
    }

    private static String doEncodeString(String s) {
        final StringBuilder sbs = new StringBuilder();
        for (char c : s.toCharArray()) {
            final String es = _encoderData.get((int) c);
            if (es == null) {
                sbs.setLength(0);
                break;
            }
            sbs.append(es);
        }

        while ((sbs.length() % 8) != 0) {
            sbs.append("1");
        }

        return sbs.toString();
    }
    
    private static HT2String doDecodeString(String s) {

        final StringBuilder tempSbs = new StringBuilder();
        final StringBuilder sbs = new StringBuilder();

        final char[] chars = s.toCharArray();
        final int charsLen = chars.length;

        Integer loc = _bTree.ROOT_INDEX; // root.
        int i;
        for (i = 0; i < charsLen; i++) {
            final char c = chars[i];
            // todo: correct location?
            tempSbs.append(c);
            final BTreeNode n = _bTree.getNode(loc);
            // pick branch...
            if (c == '0') {
                loc = n.v0;
            } else {
                loc = n.v1;
            }

            // this means the value doesn't exist.
            // we're breaking out early ...
            if (loc == null) {
                break;
            }

            final BTreeNode ns = _bTree.getNode(loc);
            if (ns.val != null) {
                final int i2 = _bTree.getData(ns);

                if (i2 == 256) {
                    // this is illegal, trick pad validity checker to return false.
                    // 0 is not a legal pad.
                    tempSbs.append("0");
                    break;
                }

                sbs.append((char) i2);
                tempSbs.setLength(0); // reset this.
                loc = _bTree.ROOT_INDEX;
            }
        }

        if (i < charsLen) {
            // this means he loop was ended before every bit checked,
            // this is not correct. Again, trick that into returning false.
            tempSbs.append("0");
        }

        if (padIsValid(tempSbs.toString())) {
            // good huffman encoded string.
            return HT2String.getHuffmanEncodedString(sbs.toString());
        }
        else {
            // failed.
            return HT2String.getBadlyEncodedString();
        }
    }

    private static boolean padIsValid(String s) {

        final boolean isValid;
        switch (s) {
            // padding can only be max 7 bits, all 1.
            case "":
            case "1":
            case "11":
            case "111":
            case "1111":
            case "11111":
            case "111111":
            case "1111111":
                isValid = true;
                break;
            default:
                isValid = false;
                break;
        }
        return isValid;
    }
}
