package snarky.stork.http2;

public class ReadPermissionChecker {

    // todo: Run checks again OK
    
    public static ErrorCodes idle(FrameType ft) {

        final ErrorCodes er;
        switch (ft) {
            case HEADERS:
            case PRIORITY:
            case CONTINUATION:
                er = ErrorCodes.NO_ERROR;
                break;
            default:
                er = ErrorCodes.PROTOCOL_ERROR;
                break;
        }
        return er;
    }

    public static ErrorCodes reservedLocal(FrameType ft) {
        final ErrorCodes er;
        switch (ft) {
            case PRIORITY:
            case RST_STREAM:
            case WINDOW_UPDATE:
                er = ErrorCodes.NO_ERROR;
                break;
            default:
                er = ErrorCodes.PROTOCOL_ERROR;
                break;
        }
        return er;
    }

    public static ErrorCodes reservedRemote(FrameType ft) {

        final ErrorCodes er;
        switch (ft) {
            case HEADERS:
            case PRIORITY:
            case RST_STREAM:
            case CONTINUATION:
                er = ErrorCodes.NO_ERROR;
                break;
            default:
                er = ErrorCodes.PROTOCOL_ERROR;
                break;
        }
        return er;
    }

    public static ErrorCodes open(FrameType ft) {
        // Open state, anything goes ..
        return ErrorCodes.NO_ERROR;
    }

    public static ErrorCodes halfCloseLocal(FrameType ft) {
        return ErrorCodes.NO_ERROR;
    }

    public static ErrorCodes halfClosedRemote(FrameType ft) {
        // the peer has sent a frame with end stream set, that's an indication that
        // the peer has no intention to send more frame, so nothing else but Priority,
        // Reset Stream and Window Update can be sent (by the peer).
        
        final ErrorCodes er;
        switch (ft) {
            case PRIORITY:
            case RST_STREAM:
            case WINDOW_UPDATE:
                er = ErrorCodes.NO_ERROR;
                break;
            default:
                er = ErrorCodes.STREAM_CLOSED;
                break;
        }
        return er;
    }

    public static ErrorCodes closed(FrameType ft) {
        return (ft == FrameType.PRIORITY) ? ErrorCodes.NO_ERROR : ErrorCodes.STREAM_CLOSED;
    }
}
