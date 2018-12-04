package label.driven.summarization.merge.classes;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/2/18.
 */
public class Functions {

    private Functions() {
        throw new UnsupportedOperationException("Not instance allowed");
    }

    public static Float avg(Integer first, Integer second) {
        return (first + second) / 2F;
    }

    public static Float avg(Float first, Float second) {
        return (first + second) / 2F;
    }
}
