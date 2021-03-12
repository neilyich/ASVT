package neilyich.bf.minimization;

import lombok.Getter;

import java.util.*;

// реализация логики работы импликанты
public class Implicant implements Comparable<Implicant>{

    private final Boolean[] literals;
    @Getter
    private int weight;
    @Getter
    private final int variablesCount;

    public Implicant(int variablesCount) {
        this.variablesCount = variablesCount;
        this.literals = new Boolean[variablesCount];
    }

    // создание импликанты по числу переменных и ее порядковому номеру
    public Implicant(int variablesCount, int number) {
        this(variablesCount);
        int pos = 0;
        int w = 0;
        while(number > 0) {
            if(number % 2 == 1) {
                literals[pos] = true;
                w++;
            }
            else {
                literals[pos] = false;
            }
            number /= 2;
            pos++;
        }
        while(pos < literals.length) {
            literals[pos++] = false;
        }
        weight = w;
    }

    public Implicant(Implicant implicant) {
        weight = implicant.weight;
        variablesCount = implicant.variablesCount;
        literals = Arrays.copyOf(implicant.literals, implicant.literals.length);
    }

    // установка степени заданного литерала (val == null - исключение литерала из данной импликанты)
    public void set(int pos, Boolean val) {
        if(literals[pos] != null) {
            if(val != null) {
                if(literals[pos] && !val) {
                    weight--;
                }
                else if(!literals[pos] && val) {
                    weight++;
                }
            }
        }
        else if(val != null) {
            if(val) {
                weight++;
            }
        }
        literals[pos] = val;
    }

    // если 2 импликанты можно склеить - возвращает полученную импликанту
    public static Optional<Implicant> tryIntersect(Implicant a, Implicant b) {
        if(a.literals.length != b.literals.length) {
            throw new RuntimeException("implicants of different lengths: " + a.literals.length + ", " + b.literals.length);
        }
        Implicant result = new Implicant(a.literals.length);
        boolean dif = false;
        Boolean value;
        for (int i = 0; i < a.literals.length; i++) {
            value = null;
            if(a.literals[i] != null) {
                if(b.literals[i] != null) {
                    if(a.literals[i] == b.literals[i]) {
                        value = a.literals[i];
                    }
                    else {
                        if(dif) {
                            return Optional.empty();
                        }
                        dif = true;
                    }
                }
                else {
                    if(dif) {
                        return Optional.empty();
                    }
                    dif = true;
                }
            }
            else if(b.literals[i] != null) {
                if(dif) {
                    return Optional.empty();
                }
                dif = true;
            }
            result.set(i, value);
        }
        return Optional.of(result);
    }

    // true - если данная импликанта (this) покрывает вторую импликанту (implicant)
    public boolean covers(Implicant implicant) {
        if(literals.length != implicant.literals.length) {
            throw new RuntimeException("implicants of different lengths: " + literals.length + ", " + implicant.literals.length);
        }
        for(int i = 0; i < literals.length; i++) {
            Boolean r = literals[i];
            Boolean l = implicant.literals[i];
            if(r != null) {
                if(l != null) {
                    if(r != l) {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
        }
        return true;
    }

    // получение строкового представления импликанты (с литералами)
    @Override
    public String toString() {
        var builder = new StringBuilder();
        for (int i = 0; i < literals.length; i++) {
            Boolean l = literals[i];
            if (l != null) {
                if (l) {
                    builder.append('x').append(i);
                } else {
                    builder.append("!x").append(i);
                }
            }
        }
        return builder.toString();
    }

    // получение строкового представления импликанты (в бинарном виде)
    public String toBinaryString() {
        var builder = new StringBuilder();
        for (Boolean l : literals) {
            if (l != null) {
                if (l) {
                    builder.append('1');
                } else {
                    builder.append('0');
                }
            } else {
                builder.append('*');
            }
        }
        return builder.toString();
    }

    // сравнение двух импликант по числу литералов
    @Override
    public int compareTo(Implicant impl) {
        if(this.literals.length != impl.literals.length) {
            throw new RuntimeException("implicants of different lengths: " + this.literals.length + ", " + impl.literals.length);
        }
        int res = Integer.compare(this.literalsCount(), impl.literalsCount());
        if(res != 0) {
            return res;
        }
        for (int i = 0; i < literals.length; i++) {
            var n1 = literals[i] == null;
            var n2 = impl.literals[i] == null;
            if(n1 && !n2) {
                return 1;
            }
            else if(!n1 && n2) {
                return -1;
            }
            else if(!n1 && !n2) {
                int prior = Boolean.compare(impl.literals[i], literals[i]);
                if(prior != 0) {
                    return prior;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Implicant implicant = (Implicant) o;
        return weight == implicant.weight &&
                Arrays.equals(literals, implicant.literals);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(weight);
        result = 31 * result + Arrays.hashCode(literals);
        return result;
    }

    // перемножение двух импликант
    public static Implicant mult(Implicant l, Implicant r) {
        if(l.literals.length != r.literals.length) {
            throw new RuntimeException("implicants of different lengths: " + l.literals.length + ", " + r.literals.length);
        }
        var res = new Implicant(l.literals.length);
        for(int i = 0; i < l.literals.length; i++) {
            if(l.literals[i] != null) {
                if(r.literals[i] != null) {
                    if(l.literals[i] == r.literals[i]) {
                        res.set(i, l.literals[i]);
                    }
                    else {
                        throw new RuntimeException("Cant mult!");
                    }
                }
                else {
                    res.set(i, l.literals[i]);
                }
            }
            else if(r.literals[i] != null) {
                res.set(i, r.literals[i]);
            }
        }
        return res;
    }

    // перемножение двух ДНФ между собой
    public static Set<Implicant> mult(Set<Implicant> dnf1, Set<Implicant> dnf2) {
        Set<Implicant> res = new HashSet<>();
        for(var i: dnf1) {
            for(var j: dnf2) {
                res.add(mult(i, j));
            }
        }
        return res;
    }

    // перемножение всех ДНФ между собой
    public static Set<Implicant> multAll(List<Set<Implicant>> implicants) {
        if(implicants.size() == 0) {
            return new HashSet<>();
        }
        if(implicants.size() == 1) {
            return implicants.get(0);
        }
        var res = implicants.get(0);
        for (int i = 1; i < implicants.size(); i++) {
            res = mult(res, implicants.get(i));
        }
        return res;
    }

    public Boolean get(int i) {
        return literals[i];
    }

    // генерация номеров наборов, на которых данная импликанта равна 1
    public Set<Integer> generateOnes() {
        var impl = new Implicant(variablesCount);
        for (int i = 0; i < literals.length; i++) {
            if(literals[i] == null) {
                impl.literals[i] = false;
            }
            else {
                impl.literals[i] = literals[i];
            }
        }
        Set<Implicant> implicants = new HashSet<>();
        implicants.add(impl);
        for (int i = 0; i < literals.length; i++) {
            if(literals[i] == null) {
                Set<Implicant> newImpls = new HashSet<>(implicants.size());
                for(var oldImpl: implicants) {
                    impl = new Implicant(oldImpl);
                    impl.set(i, true);
                    newImpls.add(impl);
                }
                implicants.addAll(newImpls);
            }
        }
        Set<Integer> res = new HashSet<>(implicants.size());
        for(var im: implicants) {
            res.add(im.generateNumber());
        }
        return res;
    }

    // получение номера импликанты
    private int generateNumber() {
        int n = 0, p = 1;
        for (var literal : literals) {
            if (literal == null) {
                throw new RuntimeException("Cant convert to number: " + toString());
            } else if (literal) {
                n += p;
            }
            p = p << 1;
        }
        return n;
    }

    // подсчет числа литералов в импликанте
    public int literalsCount() {
        int n = 0;
        for(var l: literals) {
            if(l != null) {
                n++;
            }
        }
        return n;
    }

    // получение импликанты из строкового представления (пр.: x0x1!x3x5)
    public static Implicant fromString(int variablesCount, String implStr) {
        implStr = implStr.trim();
        if(!implStr.matches("(!?x\\d+)+")) {
            throw new RuntimeException("Cant parse implicant: " + implStr);
        }
        var impl = new Implicant(variablesCount);
        for(int i = 0; i < implStr.length(); i++) {
            boolean pos = true;
            if(implStr.charAt(i) == '!') {
                pos = false;
                i++;
            }
            if(implStr.charAt(i) == 'x') {
                i++;
            }
            else {
                throw new RuntimeException("Cant parse implicant, pos: " + i + implStr);
            }
            int endNum = i;
            while (endNum < implStr.length() && implStr.charAt(endNum) >= '0' && implStr.charAt(endNum) <= '9') {
                endNum++;
            }
            int num = Integer.parseInt(implStr.substring(i, endNum));
            impl.set(num, pos);
            i = endNum - 1;
        }
        return impl;
    }
}
