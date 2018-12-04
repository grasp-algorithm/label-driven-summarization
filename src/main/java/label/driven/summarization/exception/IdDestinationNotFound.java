package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/4/18.
 */
public class IdDestinationNotFound extends RuntimeException {
    public IdDestinationNotFound() {
    }

    public IdDestinationNotFound(String message) {
        super(message);
    }

    public IdDestinationNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public IdDestinationNotFound(Throwable cause) {
        super(cause);
    }

    public IdDestinationNotFound(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
