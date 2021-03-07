package neilyich.bf.minimization;

import java.util.List;

public interface Minimizer {
    List<Implicant> minimize(BooleanFunction f);
}
