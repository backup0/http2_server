package snarky.stork.http2;

import java.util.HashMap;
import java.util.Map;

public class StreamManager {

    private final FlowController _inboundFC;
    private final FlowController _outboundFC;

    private final boolean _serverStream;
    private final int MAXIMUS = 2147483647; // 31 bits
    private final HashMap<Integer, Stream> _streams = new HashMap<>();
    private int _lastStreamId; // 1 cannot be used .. so we need to start @ minimum 3.

    /*
    Streams initiated by a client MUST use odd-numbered stream identifiers;
    those initiated by the server MUST use even-numbered stream identifiers.
     */

    public StreamManager(boolean server, FlowController inFC, FlowController outFC) {
        _serverStream = server;
        _lastStreamId = server ? 1 : 0;

        _inboundFC = inFC;
        _outboundFC = outFC;
    }

    /**
     * Get a stream from stream queue. -1 if ok. 0 if the stream is closed or has never been created.
     * 1 bad stream id.
     * @param id The id of the stream.
     * @return Status + stream (if any)
     */
    public DetReturn<Integer, Stream> getStream(int id) {
        // max value for int is the max .. don't have to worry about that,

        final Stream stream = _streams.get(id);
        if (stream != null) {
            return new DetReturn<>(-1, stream);
        }

        // else - well, this is a new one, idle ..
        if (id < _lastStreamId) {
            // new one has to be >= expected
            // if the stream has id that's less than the last one that we've created and it is not
            // inside the list it means
            // - it has been closed
            // - it has never been created
            // either case is an error.
            return new DetReturn<>(0, null);
        }
        else {
            // 'idle' stream.
            if (id % 2 == 0) {
                // can't use even number, client initiated.
                return new DetReturn<>(1, null);
            }
            else {
                _lastStreamId = id;
                final Stream newStream;
                newStream = new Stream(_lastStreamId, _inboundFC.createFlowWindow(), _outboundFC.createFlowWindow());
                _streams.put(_lastStreamId, newStream);
                return new DetReturn<>(-1, newStream);
            }
        }
    }

    public boolean outOverflow(long i) {
        return overflow(false, i);
    }

    public boolean inOverflow(long i) {
        return overflow(true, i);
    }

    private boolean overflow(boolean in, long i)
    {
        for (Map.Entry<Integer, Stream> s : _streams.entrySet()) {
            final Stream stream = s.getValue();
            final FlowController.FlowWindow fw;
            fw = in ? stream.getInFlowWin() : stream.getOutFlowWin();
            if (!fw.canSetInitSize(i)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "StreamManager";
    }
}
