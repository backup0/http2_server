package snarky.stork.http2;

/**
 * Some methods I need to use in multiple places that don't fit anywhere else.
 */
public class Utils {

    /**
     * Get the error code enum. If none matches INTERNAL_ERROR is returned, this is never null
     * @param code
     * @return
     */
    public static ErrorCodes getErrorCode(long code) {

        ErrorCodes errCode = null;
        for (ErrorCodes er : ErrorCodes.values()) {

            if (code == er.getId()) {
                errCode = er;
            }
        }

        if (errCode == null) {
            errCode = ErrorCodes.INTERNAL_ERROR;
        }
        return errCode;
    }

    /**
     * Get settings. if code isn't recognized, X_UNKNOWN is return. Should ignore that. Not an error.
     * @param code
     * @return
     */
    public static Settings getSetting(int code) {

        Settings setting = null;
        for (Settings s : Settings.values()) {
            if (s.getCode() == code) {
                setting = s;
            }
        }
        return setting == null ? Settings.X_UNKNOWN : setting;
    }
}
