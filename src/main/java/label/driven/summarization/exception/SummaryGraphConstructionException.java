package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/19/18.
 */
public class SummaryGraphConstructionException extends Exception {

    public SummaryGraphConstructionException(String message) {
        super(message);
    }

    public SummaryGraphConstructionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SummaryGraphConstructionException(Throwable cause) {
        super(cause);
    }

    public SummaryGraphConstructionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
