package snarky.stork.http2.out;

import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;

public abstract class OutputMaker {

    private final boolean _isFlowControlled;
    private final boolean _continuously;
    private Stream _stream;

    /**
     * Last frame defaults to true. Set it appropriately.
     * @param flowCtrl
     * @param cont
     */
    public OutputMaker(Stream stream, boolean flowCtrl, boolean cont) {
        _isFlowControlled = flowCtrl;
        _continuously = cont;
        _stream = stream;
    }

    /**
     *
     * @return Integer -> return code, OutFrame -> the output.
     */
    public abstract OutputData getDataOut(int maxFrameSize);

    public abstract boolean hasMoreFrame();

    /**
     * Whether the frame has to follow one another
     * @return
     */
    public boolean continuously() {
        return _continuously;
    }
    /**
     * Whether the (??) is subject to flow control.
     * @return True if this ?? is subject to slow control, false is it is not.
     */
    public boolean subjectToFlowControl() {
        return _isFlowControlled;
    }

    public Stream getStream() {
        return _stream;
    }

    public abstract FrameType getFrameType();
}
