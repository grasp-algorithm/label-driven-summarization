package label.driven.summarization.exception;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/12/18.
 */
public class ConversionAdjacencyMatrixException extends RuntimeException {

    public ConversionAdjacencyMatrixException() {
    }

    public ConversionAdjacencyMatrixException(String message) {
        super(message);
    }

    public ConversionAdjacencyMatrixException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionAdjacencyMatrixException(Throwable cause) {
        super(cause);
    }

    public ConversionAdjacencyMatrixException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
