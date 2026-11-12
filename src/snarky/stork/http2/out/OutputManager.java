package snarky.stork.http2.out;

import snarky.stork.Main;
import snarky.stork.http2.FlowController;
import snarky.stork.http2.FrameType;
import snarky.stork.http2.Stream;
import snarky.stork.http2.StreamStates;

import java.util.ArrayList;

public class OutputManager {

    /*
    DATA - var
    HEADERS - var
    PRIORITY - 5 oct
    RST_STREAM - 4 octet
    SETTINGS - var, but mutipl of 6
    PUSH_PROMISE (??)
    PING 8
    GOAWAY - var, controllable
    WINDOW_UPDATE - 4 max
    CONTINUATION - varied.
     */

    // todo - when there's error we should flush .. Ok, if there's an error

    private final ArrayList<OutputMaker> _itemsToWriteOut;
    // todo: edit flow window
    private final FlowController _flowController;
    private final FlowController.FlowWindow _flowWindow;
    private final OutputBuffer _outBuffer;

    private boolean _closed;

    private int _frameSize = 1024; //todo: set max frame size here ...
    private final int ZERO_INDEX;

    public OutputManager(FlowController fc) {
        _itemsToWriteOut = new ArrayList<>();
        _flowController = fc;
        _flowWindow = fc.getFlowWin();
        _outBuffer = new OutputBuffer();
        _closed = false;
        ZERO_INDEX = 0;
    }

    /**
     * The frame size will be this or smaller depending on the amount of data available..
     * @param i
     */
    public void setFrameSize(int i) {
        _frameSize = i;
    }

    public FlowController getPeerFlowCtrl() {
        return _flowController;
    }

    public FlowController.FlowWindow getPeerFlowWin() {
        return _flowWindow;
    }

    public void put(OutputMaker os) {
        if (_closed) {
            return;
        }

        switch (os.getFrameType()) {
            //todo: should it be @ the front? Or should be either? Depending.
            //case GO_AWAY:
            case WINDOW_UPDATE:
            case SETTINGS:
            case PING:
                putInFront(os);
                break;
            default:
                _itemsToWriteOut.add(os);
                break;
        }
    }

    private void putInFront(OutputMaker om) {
        // put this shit in front - simple enough .. How
        // check current one - see if its' continuous
        // yes .. put after the shite
        // no .. pu @ current - and process it.
        if (_itemsToWriteOut.isEmpty()) {
            _itemsToWriteOut.add(om);
        }
        else {
            final OutputMaker om2 = _itemsToWriteOut.get(ZERO_INDEX);
            final int loc = om2.continuously() ? 1 : ZERO_INDEX;
            _itemsToWriteOut.add(loc, om);
        }
    }

    /**
     *
     * @return
     */
    public byte[] getBytes() {

        // todo: check this ...
        // buffer still has data in it.
        final byte[] b = _outBuffer.get();
        if (b.length > 0) {
            return b;
        }

        if (_itemsToWriteOut.isEmpty()) {
            return new byte[0];
        }

        // buffer is empty, so we need to get more data ...
        // if we have no more items to write out, this won't be run.
        // todo: do this ..how many?
        // get from buffer
        // if buffer is empty - why not use byte buffer?
        // if buffer is empty get some data from our queue.
        final OutputMaker oms = _itemsToWriteOut.get(ZERO_INDEX);
        final Stream stream = oms.getStream();

        Main.w("F-Type - ", oms.getFrameType(), " // ", oms.getStream().getId());

        if (stream.getState() == StreamStates.RESET_BY_PEER  &&
                oms.getFrameType() != FrameType.HEADERS) {
            // stream has been reset by the peer
            // this is not a header; so remove it from queue, we're not sending the frame.
            // only headers frame will be sent otherwise we may mess up the dynamic header table
            // because we may have sent a few frames previously.
            _itemsToWriteOut.remove(ZERO_INDEX);
            return new byte[0];
        }

        // no more frame.
        if (!oms.hasMoreFrame()) {
            _itemsToWriteOut.remove(ZERO_INDEX);
            return new byte[0];
        }

        // we have more frames to write out.
        final byte[] header;
        final byte[] payload;
        if (oms.subjectToFlowControl()) {
            // todo: correct or incorrect?
            final FlowController.FlowWindow streamWin = oms.getStream().getOutFlowWin();
            // use the lower value .. of course.
            final long credit = Long.min(_flowWindow.getCredit(), streamWin.getCredit());
            final int frameSize = Integer.min((int) credit, _frameSize);

            // has to be >=, because we don't know before hand how much da???
            if (frameSize >= 0) {
                final OutputData outData = oms.getDataOut(frameSize);
                header = outData.getHeader();
                payload = outData.getPayload();

                _flowWindow.reduceCredit(payload.length);
                streamWin.reduceCredit(payload.length);
            }
            // this if credit is 0.
            else {
                header = new byte[0];
                payload = new byte[0];
            }
        }
        else {
            // not subject to flow control, don't care about credit.
            final OutputData outData = oms.getDataOut(_frameSize);
            header = outData.getHeader();
            payload = outData.getPayload();
        }

        _outBuffer.put(header);
        _outBuffer.put(payload);

        if (oms.hasMoreFrame()) {
            // if it's continuously, we continue there on the next round.
            // if it isn't, we put that @ the end of the queue so we can handle other stuff first.
            if (!oms.continuously()) {
                final OutputMaker currentOmkr = _itemsToWriteOut.remove(ZERO_INDEX);
                _itemsToWriteOut.add(currentOmkr);
            }
        }
        else {
            _itemsToWriteOut.remove(ZERO_INDEX);
        }

        // todo: remove...
        return _outBuffer.get();
    }

    /**
     * This sets the number of bytes to be returned.
     * @param i
     */
    public void setOutputSize(int i) {
        _outBuffer.setOutputSize(i);
    }

    public boolean hasMore() {
        return _outBuffer.size() > 0 || !_itemsToWriteOut.isEmpty();
    }

    /**
     * Clear out, send go away.
     * @param goAway
     */
    public void purge(OutputMaker goAway) {

        if (!_itemsToWriteOut.isEmpty()) {
            final OutputMaker oms = _itemsToWriteOut.get(ZERO_INDEX);
            _itemsToWriteOut.clear();
            if (oms.continuously()) {
                _itemsToWriteOut.add(oms);
            }
        }
        _itemsToWriteOut.add(goAway);
        _closed = true;
    }

    /**
     * Clear out.
     */
    public void purge() {
        _itemsToWriteOut.clear();
        _closed = true;
    }

    /**
     * Just close. No new output maker will be accepted.
     */
    public void close() {
        _closed = true;
    }
}
