package neilyich.bf.minimization.undetermined.coefs;

import neilyich.bf.minimization.BooleanFunction;
import neilyich.bf.minimization.Implicant;
import neilyich.bf.minimization.Minimizer;

import java.util.*;
import java.util.stream.Collectors;

// реализация метода неопределенных коэффициентов
public class CoefsMinimizer implements Minimizer {

    // получение минимизированной БФ
    public BooleanFunction minimize(BooleanFunction f) {
        int variablesCount = f.getVariablesCount();
        int maxN = (int) Math.round(Math.pow(2, variablesCount));
        if(f.getOnes().size() == maxN) {
            return BooleanFunction.of(List.of(new Implicant(variablesCount)));
        }
        Map<Implicant, Coef> allCoefs = new HashMap<>();
        Set<Integer> ones = new HashSet<>(f.getOnes());
        List<List<Coef>> system = new ArrayList<>(f.getOnes().size());
        for(int i = 0; i < maxN; i++) {
            var cfs = createLine(variablesCount, i, ones.contains(i), allCoefs);
            if(cfs != null) {
                system.add(cfs);
            }
        }
        System.out.println("System of equalities:");
        printSystem(system);
        simplifySystem(system);
        System.out.println("\nSystem of equalities after removing from each line all coefs which are covered by others:");
        printSystem(system);
        return BooleanFunction.of(resolveCoefs(system));
    }

    // упрощение системы путем удаления из строк коэффициентов, покрываемых другими
    private void simplifySystem(List<List<Coef>> system) {
        for(var row: system) {
            row.removeIf((c) -> c.getValue() != null && !c.getValue());
        }
        for (var line : system) {
            Set<Coef> minimizedLine = new HashSet<>(line);
            for (var coef : line) {
                minimizedLine.removeIf(c -> c != coef && coef.getImplicant().covers(c.getImplicant()));
            }
            line.clear();
            line.addAll(minimizedLine);
        }
    }

    // получение строки системы уравнений для заданного номера набора
    private List<Coef> createLine(int variablesCount, int n, boolean f, Map<Implicant, Coef> allCoefs) {
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
                    }
                }
                else {
                    var coef = new Coef(impl);
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
                }
            }
            else {
                var coef = new Coef(impl);
                coef.setValue(false);
                allCoefs.put(impl, coef);
            }
        }
        return null;
    }

    // упрощение системы после приравнивания одного из коэффициентов к 1
    private void simplifySystem(Coef newOneCoef, List<List<Coef>> system) {
        newOneCoef.setValue(true);
        for(var row: system) {
            if(row.contains(newOneCoef)) {
                row.clear();
                row.add(newOneCoef);
            }
        }
    }

    // расчет веса СДНФ, соответствующей данной импликанте (каждый литерал импликанты соответствует отдельному коэффициенту)
    private int calcRate(Implicant mappedImplicant, Map<Integer, Coef> intMapping) {
        int rate = 0;
        for (int i = 0; i < mappedImplicant.getVariablesCount(); i++) {
            if(mappedImplicant.get(i) != null && mappedImplicant.get(i)) {
                var c = intMapping.get(i);
                rate += c.getImplicant().getVariablesCount();
            }
        }
        return rate;
    }

    // получение СДНФ, соответствующего данной импликанте (каждый литерал импликанты соответствует отдельному коэффициенту)
    private List<Coef> remapImplicant(Implicant mappedImplicant, Map<Integer, Coef> intMapping) {
        List<Coef> implicants = new ArrayList<>();
        for (int i = 0; i < mappedImplicant.getVariablesCount(); i++) {
            if(mappedImplicant.get(i) != null && mappedImplicant.get(i)) {
                var c = intMapping.get(i);
                implicants.add(c);
            }
        }
        return implicants;
    }

    // нахождение минимального решения системы уравнений
    private List<Implicant> resolveCoefs(List<List<Coef>> system) {
        system.sort(Comparator.comparingInt(List::size));
        int lineNum = 0;
        Set<Implicant> result = new HashSet<>();
        for (; lineNum < system.size() && system.get(lineNum).size() == 1; lineNum++) {
            result.add(system.get(lineNum).get(0).getImplicant());
            simplifySystem(system.get(lineNum).get(0), system);
        }
        System.out.println("\nSystem of equalities after assigning 1 to coefs in lines of length 1:");
        printSystem(system);
        Map<Coef, Implicant> coefMapping = new HashMap<>();
        Map<Integer, Coef> intMapping = new HashMap<>();

        int num = 0;
        Set<Coef> distinct = new HashSet<>();
        for (var line : system) {
            distinct.addAll(line);
        }
        var varsCount = distinct.size();
        List<Set<Implicant>> implicants = new ArrayList<>(system.size() - lineNum);
        while(lineNum < system.size()) {
            var line = system.get(lineNum);
            Set<Implicant> term = new HashSet<>(line.size());
            for (var c: line) {
                if(coefMapping.containsKey(c)) {
                    term.add(coefMapping.get(c));
                }
                else {
                    var impl = new Implicant(varsCount);
                    impl.set(num, true);
                    coefMapping.put(c, impl);
                    intMapping.put(num, c);
                    term.add(impl);
                    num++;
                }
            }
            implicants.add(term);
            lineNum++;
        }
        var mult = Implicant.multAll(implicants);
        var best = mult.stream().min(Comparator.comparingInt(impl -> calcRate(impl, intMapping)));

        var coefs = remapImplicant(best.orElseThrow(), intMapping);
        System.out.println("\nFound minimal solution:");
        int r = 1;
        for (var c : coefs) {
            System.out.println(r++ + ") " + c.toString() + " = 1");
        }
        result.addAll(coefs.stream().map(Coef::getImplicant).collect(Collectors.toList()));
        return new ArrayList<>(result);
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
