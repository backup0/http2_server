package snarky.stork.http2;

import snarky.stork.http2.header.HeaderDecoder;

import java.util.List;

public class HeaderValidityChecker
{
    private final List<Header> hdrs;

    public HeaderValidityChecker(HeaderDecoder hdc)
    {
        hdrs = hdc.getHeaderList().getValue();
    }

    private void fuckKYou()
    {

    }

    private void shit()
    {
        for (Header h : hdrs)
        {
            if (!isKnownPseudoHeader(h.key))
            {
                // bad
            }
        }
    }

    private void offa()
    {

        for (int i = 0; i < hdrs.size(); i++)
        {
            final var s = hdrs.get(i).key;
            if (!s.startsWith(":"))
            {
                continue;
            }
            // starts with ":"
            if (i == 0)
            {
                continue;
            }

            final var prev = (hdrs.get(i - 1)).key;
            if (!prev.startsWith(":"))
            {
                // bad -- break.
            }

        }

    }

    private void noOmit()
    {
        // no dupes
        // methid
        // scheme
        // path

        // no omit
        // method
        // scheme

        // can't be emoty -> path
    }

    private boolean isKnownPseudoHeader(String k)
    {
        if (k.equalsIgnoreCase(":method"))
        {
            return true;
        }

        if (k.equalsIgnoreCase(":path"))
        {
            return true;
        }

        if (k.equalsIgnoreCase(":authority"))
        {
            return true;
        }

        return k.equalsIgnoreCase(":scheme");
    }
}
