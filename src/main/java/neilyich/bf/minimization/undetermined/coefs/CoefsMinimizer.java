package neilyich.bf.minimization.undetermined.coefs;

import neilyich.bf.minimization.BooleanFunction;
import neilyich.bf.minimization.Implicant;
import neilyich.bf.minimization.Minimizer;

import java.util.*;

// реализация метода неопределенных коэффициентов
public class CoefsMinimizer implements Minimizer {
    private int variablesCount;
    private Map<Implicant, Coef> allCoefs;
    private Map<Integer, Coef> numbersMapping;

    // получение минимизированной БФ
    public BooleanFunction minimize(BooleanFunction f) {
        variablesCount = f.getVariablesCount();
        int maxN = (int) Math.round(Math.pow(2, variablesCount));
        if(f.getOnes().size() == maxN) {
            return BooleanFunction.of(List.of(new Implicant(variablesCount)));
        }
        allCoefs = new HashMap<>();
        Set<Integer> ones = new HashSet<>(f.getOnes());
        List<List<Coef>> system = new ArrayList<>(f.getOnes().size());
        for(int i = 0; i < maxN; i++) {
            var cfs = createLine(i, ones.contains(i));
            if(cfs != null) {
                system.add(cfs);
            }
        }
        System.out.println("System of equalities:");
        printSystem(system);
        for(var row: system) {
            row.removeIf((c) -> c.getValue() != null && !c.getValue());
        }
        System.out.println("\nSystem of equalities after simplification:");
        printSystem(system);
        return BooleanFunction.of(resolveCoefs(system));
    }

    // получение строки системы уравнений для заданного номера набора
    private List<Coef> createLine(int n, boolean f) {
        boolean[] bits = new boolean[variablesCount];
        List<Integer> vars = new ArrayList<>();
        for (int i = 0; i < bits.length; i++) {
            if (n % 2 != 0) {
                bits[i] = true;
                vars.add(i);
            }
            else {
                bits[i] = false;
            }
            n /= 2;
        }

        Set<Implicant> implicants = new HashSet<>();
        for(int v = 0; v < variablesCount; v++) {
            var newImpl = new Implicant(variablesCount);
            newImpl.set(v, bits[v]);
            Set<Implicant> newImpls = new HashSet<>();
            for(var impl: implicants) {
                newImpls.add(Implicant.mult(impl, newImpl));
            }
            implicants.addAll(newImpls);
            implicants.add(newImpl);
        }
        if(f) {
            List<Coef> coefs = new ArrayList<>(implicants.size());
            for(var impl: implicants) {
                if(allCoefs.containsKey(impl)) {
                    var coef = allCoefs.get(impl);
                    if(coef.getValue() == null) {
                        coefs.add(coef);
                        coef.incContainedTimes();
                    }
                }
                else {
                    var coef = new Coef(impl);
                    coef.setContainedTimes(1);
                    allCoefs.put(impl, coef);
                    coefs.add(coef);
                }
            }
            return coefs;
        }
        for(var impl: implicants) {
            if(allCoefs.containsKey(impl)) {
                var coef = allCoefs.get(impl);
                if(coef.getValue() == null) {
                    coef.setValue(false);
                    coef.setContainedTimes(0);
                }
            }
            else {
                var coef = new Coef(impl);
                coef.setValue(false);
                coef.setContainedTimes(0);
                allCoefs.put(impl, coef);
            }
        }
        return null;
    }

    // нахождение минимального решения системы уравнений
    private List<Implicant> resolveCoefs(List<List<Coef>> system) {
        system.sort(Comparator.comparingInt(List::size));
        for (int i = 0; i < system.size(); i++) {
            if(system.get(i).isEmpty()) {
                continue;
            }
            var minOpt = system.get(i).stream()
                    .filter(c -> c.getValue() == null)
                    .min(Comparator.comparingInt((Coef c) -> c.getImplicant().literalsCount()).thenComparingInt((Coef c) -> -c.getContainedTimes()));
            if(minOpt.isEmpty()) {
                continue;
            }
            var coef = minOpt.get();
            coef.setValue(true);
            for(var row: system) {
                if(row.contains(coef)) {
                    for(var c: row) {
                        c.decContainedTimes();
                    }
                    row.clear();
                    row.add(coef);
                }
            }
            System.out.println("\nStage " + (i + 1) + ", K(" + coef.getImplicant().toString() + ") = "
                    + (coef.getValue() ? "1" : "0") + ":");
            printSystem(system);
        }
        List<Implicant> implicants = new ArrayList<>();
        for(var e: allCoefs.entrySet()) {
            if(e.getValue().getValue() != null && e.getValue().getValue()) {
                implicants.add(e.getKey());
            }
        }
        return implicants;
    }

    // вывод системы уравнений на экран
    private void printSystem(List<List<Coef>> system) {
        system.sort(Comparator.comparingInt(List::size));
        int k = 1;
        for(var line: system) {
            System.out.print((k++) + ") ");
            if(line.isEmpty()) {
                System.out.println("--------");
                continue;
            }
            for (int i = 0; i < line.size() - 1; i++) {
                var coef = line.get(i);
                String c = coef.getValue() == null ? "K" : (coef.getValue() ? "" : "0");
                System.out.print(c);
                System.out.print(coef.getImplicant().toString());
                System.out.print(" v ");
            }
            var coef = line.get(line.size() - 1);
            String c = coef.getValue() == null ? "K" : (coef.getValue() ? "" : "0");
            System.out.print(c);
            System.out.print(coef.getImplicant().toString());
            System.out.print(" = 1\n");
        }
    }
}
