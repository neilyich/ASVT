package neilyich.bf.minimization.indetermined.coefs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import neilyich.bf.minimization.Implicant;

import java.util.*;

@Getter
@RequiredArgsConstructor
public class Coef {
    @Setter
    private Boolean value;
    private final Implicant implicant;
    @Setter
    private int containedTimes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coef coef = (Coef) o;
        return Objects.equals(value, coef.value) &&
                Objects.equals(implicant, coef.implicant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(implicant);
    }

    public void incContainedTimes() {
        containedTimes++;
    }

    public void decContainedTimes() {
        containedTimes--;
    }

    public static List<Set<Coef>> mult(Set<Coef> dnf1, Set<Coef> dnf2) {
        List<Set<Coef>> res = new LinkedList<>();
        for(var r: dnf1) {
            Set<Coef> coefs = new HashSet<>();
            for(var l: dnf2) {
                if(l.implicant.equals(r.implicant)) {
                    coefs.add(r);
                }
                else {
                    coefs.add(r);
                    coefs.add(l);
                }
            }
            res.add(coefs);
        }
        return res;
    }

    public boolean covers(Coef c) {
        return implicant.covers(c.implicant);
    }
}
