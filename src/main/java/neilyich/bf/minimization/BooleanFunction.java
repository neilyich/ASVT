package neilyich.bf.minimization;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

// реализация логики работы булевой функции
public class BooleanFunction {
    @Getter
    private int variablesCount;
    @Getter
    private String vector;
    @Setter
    private List<Implicant> sdnfForm;
    @Getter
    private final Set<Integer> ones;

    // конструктор принимает на вход строку, состоящую из 0 и 1
    public BooleanFunction(String s) {
        if(!isPowOf2(s.length())) {
            throw new RuntimeException("length of BF vector is not 2^n: " + s.length());
        }
        variablesCount = (int) Math.round(Math.log(s.length()) / Math.log(2));
        vector = s;
        ones = new HashSet<>();
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '1') {
                ones.add(i);
            }
            else if(s.charAt(i) != '0') {
                throw new RuntimeException("unexpected symbol: " + s.charAt(i));
            }
        }
        sdnfForm = new ArrayList<>(ones.size());
        for(var num: ones) {
            sdnfForm.add(new Implicant(variablesCount, num));
        }
    }

    private BooleanFunction() {
        ones = new HashSet<>();
        variablesCount = 0;
        vector = "";
    }

    public List<Implicant> sdnf() {
        return sdnfForm;
    }

    private boolean isPowOf2(int n) {
        return (n & (n - 1)) == 0;
    }

    // получение строкового представления БФ в виде ДНФ
    @Override
    public String toString() {
        if(sdnfForm.isEmpty()) {
            return "0";
        }
        if(sdnfForm.size() == 1 && sdnfForm.get(0).toString().length() == 0) {
            return "1";
        }
        sortSdnf();
        var builder = new StringBuilder();
        for (int i = 0; i < sdnfForm.size() - 1; i++) {
            Implicant impl = sdnfForm.get(i);
            builder.append(impl.toString()).append(" v ");
        }
        return builder.append(sdnfForm.get(sdnfForm.size() - 1)).toString();
    }

    // конструктор, позволяющий получить БФ из ее ДНФ (элемент списка - одно слагаемое)
    public static BooleanFunction of(List<Implicant> implicants) {
        if(implicants.isEmpty()) {
            return new BooleanFunction("0");
        }
        if(implicants.size() == 1 && implicants.get(0).toString().length() == 0) {
            return new BooleanFunction("1");
        }
        var f = new BooleanFunction();
        f.variablesCount = implicants.get(0).getVariablesCount();
        f.sdnfForm = implicants;
        f.sortSdnf();
        for(var impl: implicants) {
            if(impl.getVariablesCount() != f.variablesCount) {
                throw new RuntimeException("Different vars counts");
            }
            f.ones.addAll(impl.generateOnes());
        }
        var builder = new StringBuilder("0".repeat((int) Math.round(Math.pow(2, f.variablesCount))));
        for(var n: f.ones) {
            builder.setCharAt(n, '1');
        }
        f.vector = builder.toString();
        return f;
    }

    // конструктор, позволяющий получить БФ из строки ее ДНФ (пр.: x0!x1 v !x0x1)
    public static BooleanFunction fromString(int variablesCount, String sdnfStr) {
        String[] impls = sdnfStr.split("\\s+v\\s+");
        List<Implicant> implicants = new ArrayList<>(impls.length);
        for(var s: impls) {
            implicants.add(Implicant.fromString(variablesCount, s));
        }
        return BooleanFunction.of(implicants);
    }

    // подсчет числа литералов в представлении функции в виде СДНФ
    public int weight() {
        int w = 0;
        for(var s: sdnfForm) {
            w += s.literalsCount();
        }
        return w;
    }

    // true - если обе БФ реализуют одну и ту же логическую функцию (но могут иметь разные ДНФ)
    public boolean isSame(BooleanFunction f) {
        return ones.equals(f.ones);
    }

    // сравнение минимальности двух функций между собой
    public int compareSame(BooleanFunction f) {
        if(!isSame(f)) {
            throw new RuntimeException("Cant compare not same functions");
        }
        int res = Integer.compare(sdnfForm.size(), f.sdnfForm.size());
        if(res == 0) {
            int w1 = weight();
            int w2 = f.weight();
            res = Integer.compare(w1, w2);
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanFunction that = (BooleanFunction) o;
        return variablesCount == that.variablesCount &&
                Objects.equals(vector, that.vector) &&
                Objects.equals(sdnfForm, that.sdnfForm) &&
                Objects.equals(ones, that.ones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variablesCount, vector, sdnfForm, ones);
    }

    private void sortSdnf() {
        sdnfForm.sort(Implicant::compareTo);
    }

    // возвращает только те импликанты, которых нет в БФ f
    public Set<Implicant> diff(BooleanFunction f) {
        Set<Implicant> s1 = new HashSet<>(sdnfForm);
        s1.removeAll(new HashSet<>(f.sdnfForm));
        return s1;
    }
}
