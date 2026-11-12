package snarky.stork;

import snarky.stork.bitman.BitConv;
import snarky.stork.bitman.BitManException;
import snarky.stork.bitman.BitManImp;
import snarky.stork.http2.HTTP2Server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;

public class Main {

   public void doStuff() throws BitManException {


        //StringCodex hr = new StringCodex();
        //hr.add("");
        //w(hr.encodeString("o"));
        //hr.se("1111001101011");

        //snarky.stork
        //EnTree et = new EnTree();
        //w(et.read(et.encodeString("Give me SOMETHING to kill the pain ...")));
        try {

            HTTP2Server v = new HTTP2Server();

            String[] ss = new String[]{
            };

            //hexTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // positive valuez .. (0 to 127)
        String[] poz = new String[] {"000", "001", "002", "003", "010", "011", "012", "013", "020", "021",
                "022", "023", "030", "031", "032", "033", "040", "041", "042", "043", "050", "051", "052",
                "053", "060", "061", "062", "063", "070", "071", "072", "073", "100", "101", "102", "103",
                "110", "111", "112", "113", "120", "121", "122", "123", "130", "131", "132", "133", "140",
                "141", "142", "143", "150", "151", "152", "153", "160", "161", "162", "163", "170", "171",
                "172", "173", "200", "201", "202", "203", "210", "211", "212", "213", "220", "221", "222",
                "223", "230", "231", "232", "233", "240", "241", "242", "243", "250", "251", "252", "253",
                "260", "261", "262", "263", "270", "271", "272", "273", "300", "301", "302", "303", "310",
                "311", "312", "313", "320", "321", "322", "323", "330", "331", "332", "333", "340", "341",
                "342", "343", "350", "351", "352", "353", "360", "361", "362", "363", "370", "371", "372",
                "373"};

        // negative valuez... (0 / -1 to -128)
        String[] nez = new String[] {"000", "773", "772", "771", "770", "763", "762", "761", "760", "753",
                "752", "751", "750", "743", "742", "741", "740", "733", "732", "731", "730", "723", "722",
                "721", "720", "713", "712", "711", "710", "703", "702", "701", "700", "673", "672", "671",
                "670", "663", "662", "661", "660", "653", "652", "651", "650", "643", "642", "641", "640",
                "633", "632", "631", "630", "623", "622", "621", "620", "613", "612", "611", "610", "603",
                "602", "601", "600", "573", "572", "571", "570", "563", "562", "561", "560", "553", "552",
                "551", "550", "543", "542", "541", "540", "533", "532", "531", "530", "523", "522", "521",
                "520", "513", "512", "511", "510", "503", "502", "501", "500", "473", "472", "471", "470",
                "463", "462", "461", "460", "453", "452", "451", "450", "443", "442", "441", "440", "433",
                "432", "431", "430", "423", "422", "421", "420", "413", "412", "411", "410", "403", "402",
                "401", "400"};

    }

    private byte[][] lol(byte[] b) {
        BitManImp bks = new BitManImp();
        bks.append(b);

        byte[] b1 = bks.getBytes(0, 9);
        byte[] b2 = bks.getBytes(9);
        return new byte[][]{b1, b2};
    }

    private byte[] getAllBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i <= 255; i++) {
            bos.write(i);
        }
        return bos.toByteArray();
    }

    private ByteArrayOutputStream getFilledBOS() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i <= 255; i++) {
            bos.write(i);
        }
        return bos;
    }

    private int[] getIntFromBOS(ByteArrayOutputStream bos) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        int[] x = new int[256];

        int count = 0;
        int d;

        while ((d = bis.read()) != -1) {
            x[count++] = d;
        }
        return x;
    }

    private void hexTest() throws Exception {
        ByteArrayOutputStream bos = getFilledBOS();
        // from 0 - 255;
        byte[] bbs = bos.toByteArray();

        String a = BitConv.getHexString(bbs); // uses map
        String b = BitConv.getHexStringBuilt(bbs); // don't use map

        if (!a.equalsIgnoreCase(b)) {
            throw new Exception("Not ...");
        }
    }

    private void testIntToByte() throws Exception {
        ByteArrayOutputStream bos = getFilledBOS();
        // bos contains bytes 0 - 255;
        // So .. Let's convert that to byte array ..
        byte[] bbs = bos.toByteArray();
        // bbs contains bos but as byte
        // now let's getFromDynamicTable that as byte array
        int[] iis = getIntFromBOS(bos);

        if (bbs.length != iis.length && bbs.length == 256) {
            throw new Exception("Wrong length");
        }

        // now lets' ccomapre
        for (int i = 0; i <= 255; i++) {
            // we want to chcek out converted .. byte -> int they have to mathc
            int intVal = iis[i];
            // byte we want to convert
            byte byteToConvert = bbs[i];
            // he convereted
            int ours = BitConv.byteToInt(byteToConvert);
            int built = Byte.toUnsignedInt(byteToConvert);

            final boolean ok = (intVal == ours) && (intVal == built);
            if (!ok) {
                throw new Exception("Bad stuff ... not equalz");
            }
        }
    }

    public static void w(Object... t) {

        if (t.length > 0) return;

        StringBuilder sb = new StringBuilder();
        for (Object s : t) {
            sb.append(s);
        }
        System.out.println(sb.toString());
    }

    public static void main(String[] args) {


        String opx;
        Scanner scanner = new Scanner(System.in);
        do {
            try {
                Main m = new Main();
                m.doStuff();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            System.out.println("? ");
            opx = scanner.nextLine();

        } while (!"x".equalsIgnoreCase(opx));
    }
}
