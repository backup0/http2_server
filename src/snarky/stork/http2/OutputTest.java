package snarky.stork.http2;

import snarky.stork.http2.header.HeaderEncoder;
import snarky.stork.http2.out.DataMaker;
import snarky.stork.http2.out.HeadersMaker;
import snarky.stork.http2.out.OutputMaker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OutputTest {

    private final HeaderEncoder _htbl;
    private final Stream _stream;

    public OutputTest(HeaderEncoder ht, Stream stream) {
        _htbl = ht;
        _stream = stream;
    }

    public List<OutputMaker> fffx2() {

        final ArrayList<Header> hdrs = new ArrayList<>();
        hdrs.add(new Header(":status", "200"));
        hdrs.add(new Header("content-type", "img/ico"));
        hdrs.add(new Header("Access-Control-Allow-Origin", "*"));

        HeadersMaker hms = new HeadersMaker(_htbl, _stream, false, hdrs, 0);
        ByteArrayInputStream b1s;

        try {
            byte[] b = Files.readAllBytes(Paths.get("D:\\php\\docs\\oldern\\new\\favicon.ico"));
            b1s = new ByteArrayInputStream(b);
        } catch (IOException e) {
            e.printStackTrace();
            b1s = new ByteArrayInputStream(new byte[0]);
        }

        DataMaker dms = new DataMaker(_stream, true, 0, b1s);

        ArrayList<OutputMaker> oms = new ArrayList<>();
        oms.add(hms);
        oms.add(dms);
        return oms;
    }

    public List<OutputMaker> fffx() {

        final byte[] out = xyz();

        final ArrayList<Header> hdrs = new ArrayList<>();
        hdrs.add(new Header(":status", "200"));
        hdrs.add(new Header("content-type", "text/plain; charset=utf-8"));
        hdrs.add(new Header("Access-Control-Allow-Origin", "*"));
        //hdrs.add(new Header("etag", "001244"));
        hdrs.add(new Header("server", "Serval 0.0.1 (Alpha)"));
        hdrs.add(new Header("x-nyet", "Its russian"));
        hdrs.add(new Header("x-Monyet", "Its russian"));
        hdrs.add(new Header("x-money", "RM200"));
        hdrs.add(new Header("x-location", "https://localhost/"));
        hdrs.add(new Header("x-server", "Serval 0.0.1 (Alpha)"));
        hdrs.add(new Header("x-nyet", "Its russian, but it's not really"));
        hdrs.add(new Header("x-Monyet", "Its russian, 00000, mail order this"));
        hdrs.add(new Header("x-Vetag", "001244"));
        hdrs.add(new Header("x-xlocation", "https://localhost/"));
        hdrs.add(new Header("x-cserver", "Serval 0.0.1 (Alpha)"));
        hdrs.add(new Header("x-Muynyet", "Its russian"));
        hdrs.add(new Header("x-27_hours", "Its russian"));

        HeadersMaker hms = new HeadersMaker(_htbl, _stream, false, hdrs, 0);

        String s = "This is the b0dy stream. The b0dy electric. Hey, file this under, UNDERWEAR. Fuck.";
        DataMaker dms = new DataMaker(_stream, true, 0, new ByteArrayInputStream(out));

        ArrayList<OutputMaker> oms = new ArrayList<>();
        oms.add(hms);
        oms.add(dms);
        return oms;
    }

    private byte[] xyz() {
        final StringBuilder sbs = new StringBuilder();

        for (Header h : _stream.getHeaders()) {
            sbs.append(h.key).append(": ").append(h.value).append("\n");
        }

        sbs.append("\n\n");
        sbs.append(System.currentTimeMillis()).append("\n");
        sbs.append("Some server response - this is the server response.\n");
        sbs.append("¯\\_(ツ)_/¯");
        sbs.append("\n");
        sbs.append("Oh this is fucking stupid");
        sbs.append("(c) ServalHT2.");

        return sbs.toString().getBytes(StandardCharsets.UTF_8);
    }
}
