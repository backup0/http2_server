package snarky.stork.http2;

public class ErrorResponse {

    private final ErrorCodes _ec;
    private final String _inf;

    public ErrorResponse(ErrorCodes errorCode) {
        this(errorCode, "");
    }

    public ErrorResponse(ErrorCodes errorCode, String info) {
        _ec = errorCode;
        _inf = info;
    }

    public ErrorCodes getErrorCode() {
        return _ec;
    }

    public String getString() {
        return _inf;
    }
}
