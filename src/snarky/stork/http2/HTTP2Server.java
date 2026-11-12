package snarky.stork.http2;

import snarky.stork.Main;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

import static snarky.stork.Main.w;

public class HTTP2Server {

    private final ArrayList<Connection> _connQueue;
    int _lastIndex;

    // todo: check this -> send connection prefix and everything - but then, don't send anything else
    //todo: ?? run a loop test getMin/Max value ...

    public HTTP2Server() {
        _connQueue = new ArrayList<>();
        _lastIndex = -1;

        final Thread thread = new Thread(() -> {
            try {
                handleConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // todo: wrong order? Better order?
        thread.start();
        startServer();
    }

    /*
    check if it is possible to send mutiple HEADERS - think it is,
    say 100 then anothe r one - OK?  That's fyckgin great ... test it with ff'
*/
    private void startServer() {

        //System.setProperty("javax.net.ssl.keyStore", "D:\\Disposable\\may_keystore.p12");
        System.setProperty("javax.net.ssl.keyStore", "D:\\libz\\selfsigned.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        final ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();

        try (ServerSocket socket = ssf.createServerSocket())
        {
            if (socket instanceof SSLServerSocket) //<-- we probadly don;t need this, becaise ' oretty sire creaseerver socker wilill rturn that,'
            {
                final SSLServerSocket ss = (SSLServerSocket) socket;
                //ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 443));
                ss.bind(new InetSocketAddress(443));

                int i = Integer.MAX_VALUE;
                while (i-- > 0) {
                    try {

                        final SSLSocket skt = (SSLSocket) ss.accept();

                        final SSLParameters sslParams = skt.getSSLParameters();

                        sslParams.setApplicationProtocols(new String[]{"h2", "http/1.1"});
                        skt.setSSLParameters(sslParams);
                        // can't read the protocol until handshake is finished.
                        // have to force handshake, duh!
                        skt.startHandshake();
                        final String appProtocol =  skt.getApplicationProtocol();

                        System.out.println("KinKong -> " + appProtocol);
                        // here we can decide which server to use. h2 or not h2.
                        if ("h2".equals(appProtocol)) {
                            System.out.println("FFFF");
                        }
                        else {
                            System.out.println("KF");
                        }

                        final Connection htc = new Connection(skt);

                        synchronized (_connQueue) {
                            Main.w("New connection -- ", htc, " size => ", _connQueue.size());
                            _connQueue.add(htc);
                            // what if it's not sleeping and it's notified? Test that ...,
                            _connQueue.notify();
                        }
                        Main.w("Fakz yous...");
                    }
                    catch (Exception e) {}
                }
                Main.w("Slip - away");
            }
            else
            {
                //Not good
                w("Hey this is NOT GOOD...");
            }
        }
        catch (Exception ex)
        {
            // handle exception
            ex.printStackTrace();
        }
        w("Close this .. Okayz...");
    }

    private void handleConnection() throws Exception {
        // flag to ?? if we've waited - no action
        int index = 0;
        boolean noAction = true;

        long x = Long.MAX_VALUE;
        while (x-- > 0) {

            final Connection conn;

            synchronized (_connQueue) {
                // no connection, so it's idle, sleep for a long time ...
                // wait for the other thread to wake this one up.
                if (_connQueue.isEmpty()) {
                    Main.w("Long ass wait ...");
                    // todo: maybe we want to wake up every few minutes or so we can shut thr server if ..
                    // todo: depends on how we plan to shut teh server ...
                    _connQueue.wait(Long.MAX_VALUE);
                    continue;
                }
                else {
                    final int size = _connQueue.size();
                    index = (index >= size) ? 0 : index;
                    conn = _connQueue.get(index);
                    if (index == 0) {
                        noAction = true; // todo: restart right?
                    }
                }
            }

            // do something, get some stuff.
            switch (conn.execute()) {
                case INVALIDATED:
                    _connQueue.remove(index);
                    // todo: action or no action? 9/8/20
                    break;
                case NO_ACTION:
                    ++index;
                    break;
                case CONT:
                    ++index;
                    noAction = false;
                    break;
                default:
                    throw new RuntimeException("Not implemented yet.");
            }

            synchronized (_connQueue) {
                final int size = _connQueue.size();
                if (index >= size && noAction) {
                    Main.w("Short break ...");
                    _connQueue.wait(1500);
                }

                /*
                if (index >= size) {
                    if (noAction) {
                        Main.w("Short break ...");
                        _connQueue.wait(1500);
                    }
                    ??? .. this is very strange.
                }

                 */
            }
        }
    }
}
