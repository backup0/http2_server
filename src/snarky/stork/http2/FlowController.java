package snarky.stork.http2;

public class FlowController {

    private final FlowWindow _win;
    private final long _conInitWinSize;
    private long _streamInitWinSize;

    // todo: explain how this work...
    /**
     * ?? - ??
     */
    public FlowController() {
        _streamInitWinSize =  SpecialValues.INIT_WIN_SIZE;
        _conInitWinSize = SpecialValues.INIT_WIN_SIZE;
        _win = new FlowWindow(true);
    }

    public void setInitWindowSize(long val) {
        _streamInitWinSize = val;
    }

    public FlowWindow getFlowWin() {
        return _win;
    }

    public FlowWindow createFlowWindow() {
        return new FlowWindow(false);
    }

    public class FlowWindow {

        private final boolean _con;
        private long credit;

        private FlowWindow(boolean con) {
            _con = con;
            credit = 0;
        }

        public boolean canAddCredit(long val) {
            final long i = _con ? _conInitWinSize : _streamInitWinSize;
            return canAddCredit(i, val);
        }

        public boolean canSetInitSize(long iWinSize) {
            return canAddCredit(iWinSize,0);
        }

        private boolean canAddCredit(long initWindSize, long val) {

            if (credit < 0) {
                // credit is negative so x + negative = x - negative, duh!
                final long currentCredit = initWindSize + credit;
                // so we can add more credit as long as the new amount + current <= max
                final long canAdd = SpecialValues.MAX_WINDOW_SIZE - currentCredit;
                return val <= canAdd;
            }
            else {
                final long maxCredit = SpecialValues.MAX_WINDOW_SIZE - initWindSize;
                final long canAdd = maxCredit - credit;
                return val <= canAdd;
            }
        }

        public long getCredit() {
            if (_con) {
                return _conInitWinSize + credit;
            }
            else {
                return _streamInitWinSize + credit;
            }
        }

        // todo: int or long?
        public void reduceCredit(long val) {
            credit = credit - val;
        }

        // todo: int or long?
        public void addCredit(long val) {
            credit = credit + val;
        }
    }
}
