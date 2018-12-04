package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/12/18.
 */
public class LoadGraphDataException extends RuntimeException {

    public LoadGraphDataException() {
    }

    public LoadGraphDataException(String message) {
        super(message);
    }

    public LoadGraphDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadGraphDataException(Throwable cause) {
        super(cause);
    }

    public LoadGraphDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
