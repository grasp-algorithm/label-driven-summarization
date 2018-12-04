package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
public class MergeStrategyException extends Exception {

    public MergeStrategyException() {
    }

    public MergeStrategyException(String message) {
        super(message);
    }

    public MergeStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MergeStrategyException(Throwable cause) {
        super(cause);
    }

    public MergeStrategyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
