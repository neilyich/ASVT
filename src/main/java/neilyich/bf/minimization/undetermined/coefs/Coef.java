package neilyich.bf.minimization.undetermined.coefs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import neilyich.bf.minimization.Implicant;

import java.util.*;

// коэффициент из метода неопределенных коэффициентов, содержит значение, импликанту, к которой относится и число вхождений в уравнения системы
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
        return Objects.hash(value, implicant);
    }

    public void incContainedTimes() {
        containedTimes++;
    }

    public void decContainedTimes() {
        containedTimes--;
    }
}
