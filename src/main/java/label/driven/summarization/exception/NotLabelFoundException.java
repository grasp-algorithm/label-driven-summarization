package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/14/18.
 */
public class NotLabelFoundException extends RuntimeException {
    public NotLabelFoundException() {
    }

    public NotLabelFoundException(String message) {
        super(message);
    }

    public NotLabelFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotLabelFoundException(Throwable cause) {
        super(cause);
    }

    public NotLabelFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
