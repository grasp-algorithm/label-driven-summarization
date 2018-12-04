package label.driven.summarization.util;

import groovy.lang.Tuple2;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/9/18.
 */
public class Util {

    private Util() {
        throw new UnsupportedOperationException("This class cannot be instatiated");
    }

    public static Tuple2<String, String> getOrderedLabels(String label1, String label2) {
        if (label1.compareTo(label2) > 0) {
            return new Tuple2<>(label1, label2);
        } else {
            return new Tuple2<>(label2, label1);
        }
    }
}
