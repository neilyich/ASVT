package neilyich.bf.minimization.indetermined.coefs;

import neilyich.bf.minimization.BooleanFunction;
import neilyich.bf.minimization.Implicant;
import neilyich.bf.minimization.Minimizer;

import java.util.*;
import java.util.stream.Collectors;

public class CoefsMinimizer implements Minimizer {
    private int variablesCount;
    private Map<Implicant, Coef> allCoefs;
    private Map<Integer, Coef> numbersMapping;

    public List<Implicant> minimize(BooleanFunction f) {
        variablesCount = f.getVariablesCount();
        int maxN = (int) Math.round(Math.pow(2, variablesCount));
        if(f.getOnes().size() == maxN) {
            return List.of(new Implicant(variablesCount));
        }
        allCoefs = new HashMap<>();
        Set<Integer> ones = new HashSet<>(f.getOnes());
        List<Set<Coef>> system = new ArrayList<>(f.getOnes().size());
        for(int i = 0; i < maxN; i++) {
            var cfs = coefs(i, ones.contains(i));
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
        if(variablesCount < 5) {
            System.out.println("\nUsing exact algorithm:");
            var res = findBestSolution(system);
            printSystem(system);
            System.out.println("\nBest solution:");
            for(var row: system) {
                row.removeIf((c) -> c.getValue() == null);
            }
            printSystem(system);
            return res;
        }
        System.out.println("\nUsing approximate algorithm:");
        return resolveCoefs(system);
    }

    private List<Implicant> resolveSystem(List<Set<Coef>> system) {
        Set<Coef> coefsSet = new HashSet<>();
        system.forEach(coefsSet::addAll);
        List<Coef> coefs = new ArrayList<>(coefsSet);
        coefs.sort(Comparator.comparingInt(o -> o.getImplicant().literalsCount()));
        Set<Integer> ones = new HashSet<>();
        for (int i = 0; i < coefs.size(); i++) {
            coefs.forEach(c -> System.out.print(c.getImplicant().toString() + " "));
            System.out.println();
            var c = coefs.get(i);
            List<Coef> uncovered = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                uncovered.add(coefs.get(j));
            }
            var newOnes = c.getImplicant().generateOnes();
            if(!ones.containsAll(newOnes)) {
                uncovered.add(c);
                ones.addAll(newOnes);
            }
            else {
                System.out.println("skip");
            }
            for (int k = i + 1; k < coefs.size(); k++) {
                if(!c.covers(coefs.get(k))) {
                    uncovered.add(coefs.get(k));
                }
            }
            coefs = uncovered;
        }
        coefs.forEach(c -> System.out.print(c.getImplicant().toString() + " "));

        return coefs.stream().map(Coef::getImplicant).collect(Collectors.toList());
    }

    private Set<Coef> coefs(int n, boolean f) {
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
            Set<Coef> coefs = new HashSet<>(implicants.size());
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

    private List<Implicant> resolveCoefs(List<Set<Coef>> system) {
        system.sort(Comparator.comparingInt(Set::size));
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
                else {
                    row.add(coef);
                    row.remove(coef);
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

    private List<Implicant> findBestSolution(List<Set<Coef>> system) {
        var implicants = remapImplicants(system);
        var mult = Implicant.multAll(implicants);
        if(mult.isEmpty()) {
            return new ArrayList<>();
        }
        var it = mult.iterator();
        Implicant bestImplicant = it.next();
        while(it.hasNext()) {
            bestImplicant = betterMappedImplicant(bestImplicant, it.next());
        }
        getCoefs(bestImplicant).forEach(c -> c.setValue(true));
        return getImplicants(bestImplicant);
    }

    private List<Set<Implicant>> remapImplicants(List<Set<Coef>> system) {
        List<Set<Implicant>> res = new ArrayList<>(system.size());
        Map<Coef, Integer> coefsMapping = new HashMap<>();
        numbersMapping = new HashMap<>();
        int varsCount = (int) allCoefs.values().stream().filter(c -> c.getValue() == null).count();
        int num = 0;
        for(var line: system) {
            Set<Implicant> implicants = new HashSet<>(line.size());
            for(var c: line) {
                Implicant impl;
                if(coefsMapping.containsKey(c)) {
                    int n = coefsMapping.get(c);
                    impl = new Implicant(varsCount);
                    impl.set(n, true);
                }
                else {
                    impl = new Implicant(varsCount);
                    impl.set(num, true);
                    coefsMapping.put(c, num);
                    numbersMapping.put(num, c);
                    num++;
                }
                implicants.add(impl);
            }
            res.add(implicants);
        }
        return res;
    }

    private Implicant betterMappedImplicant(Implicant r, Implicant l) {
        var coefsR = getCoefs(r);
        int wR = 0;
        for(var c: coefsR) {
            wR += c.getImplicant().literalsCount();
        }
        var coefsL = getCoefs(l);
        int wL = 0;
        for(var c: coefsL) {
            wL += c.getImplicant().literalsCount();
        }
        if(wR < wL) {
            return r;
        }
        return l;
    }

    private Set<Coef> getCoefs(Implicant impl) {
        Set<Coef> res = new HashSet<>();
        for(int i = 0; i < impl.getVariablesCount(); i++) {
            var b = impl.get(i);
            if(b != null && b) {
                var coef = numbersMapping.get(i);
                res.add(coef);
            }
        }
        return res;
    }

    private List<Implicant> getImplicants(Implicant remappedImplicant) {
        List<Implicant> res = new ArrayList<>();
        for(int i = 0; i < remappedImplicant.getVariablesCount(); i++) {
            var b = remappedImplicant.get(i);
            if(b != null && b) {
                var impl = numbersMapping.get(i).getImplicant();
                res.add(impl);
            }
        }
        return res;
    }

    private void printSystem(List<Set<Coef>> systemSets) {
        List<List<Coef>> system = new ArrayList<>(systemSets.size());
        systemSets.forEach(line -> system.add(new ArrayList<>(line)));
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
