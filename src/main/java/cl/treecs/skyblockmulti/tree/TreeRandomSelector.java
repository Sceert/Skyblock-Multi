package cl.treecs.skyblockmulti.tree;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.function.IntUnaryOperator;

/** Selección uniforme: cada definición disponible participa exactamente una vez. */
public final class TreeRandomSelector {
    private TreeRandomSelector() {
    }

    public static <T> T choose(List<T> available, RandomGenerator random) {
        return choose(available, (IntUnaryOperator) random::nextInt);
    }

    public static <T> T choose(List<T> available, IntUnaryOperator nextInt) {
        if (available.isEmpty()) {
            throw new IllegalArgumentException("No hay árboles disponibles para seleccionar");
        }
        return available.get(nextInt.applyAsInt(available.size()));
    }
}
