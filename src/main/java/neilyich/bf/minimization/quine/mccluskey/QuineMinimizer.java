package neilyich.bf.minimization.quine.mccluskey;

import neilyich.bf.minimization.BooleanFunction;
import neilyich.bf.minimization.Implicant;
import neilyich.bf.minimization.Minimizer;

import java.util.*;
import java.util.function.Supplier;

// реализация алгоритма Квайна МакКласски
public class QuineMinimizer implements Minimizer {
    private int variablesCount;
    private List<List<Implicant>> stages;

    // получение минимизированной БФ
    public BooleanFunction minimize(BooleanFunction f) {
        variablesCount = f.getVariablesCount();
        int maxN = (int) Math.round(Math.pow(2, variablesCount));
        if(f.getOnes().size() == maxN) {
            return BooleanFunction.of(List.of(new Implicant(variablesCount)));
        }
        stages = new ArrayList<>();
        log("Minimizing function:\n", f.toString(), "\n");
        var sdnf = f.sdnf();
        var intersected = intersect(new HashSet<>(sdnf), 1);
        System.out.println("Finished intersecting implicants:");
        printStages();
        var table = new CoverageTable(new ArrayList<>(intersected), sdnf);
        return BooleanFunction.of(table.calcMinCoverage());
    }

    // склеивание всех импликант между собой
    private Set<Implicant> intersect(Set<Implicant> implicants, int stage) {
        stages.add(new ArrayList<>(implicants));
        Set<Implicant>[] groups = new Set[variablesCount + 1];
        for(int i = 0; i < groups.length; i++) {
            groups[i] = new HashSet<>();
        }
        for(var impl: implicants) {
            groups[impl.getWeight()].add(impl);
        }
        Set<Implicant> newImplicants = new HashSet<>();
        Set<Implicant> used = new HashSet<>();
        for(int i = 0; i < groups.length - 1; i++) {
            for(var impl1: groups[i]) {
                for(var impl2: groups[i + 1]) {
                    var sum = Implicant.tryIntersect(impl1, impl2);
                    if(sum.isPresent()) {
                        used.add(impl1);
                        used.add(impl2);
                        newImplicants.add(sum.get());
                    }
                }
            }
        }
        if(newImplicants.size() > 1) {
            implicants.removeAll(used);
            newImplicants.addAll(implicants);
            return intersect(newImplicants, stage + 1);
        }
        if(newImplicants.size() == 1) {
            implicants.removeAll(used);
            newImplicants.addAll(implicants);
            stages.add(new ArrayList<>(newImplicants));
        }
        else {
            implicants.removeAll(used);
            newImplicants.addAll(implicants);
        }
        return newImplicants;
    }

    private static void log(Object... args) {
        for(var o: args) {
            System.out.print(o);
        }
        System.out.println();
    }

    private static final String verticalSeparator = " | ";
    private static final String horizontalSeparator = "-";
    private static final String cornerSeparator = "+";

    // вывод стадий склейки импликант в виде таблицы
    private void printStages() {
        if(stages.get(0).isEmpty()) {
            System.out.println("--------");
            return;
        }
        int columnWidth = stages.get(0).get(0).toBinaryString().length();
        if(columnWidth == 0) {
            System.out.println("--------");
            return;
        }
        int height = stages.stream().max(Comparator.comparingInt(List::size)).get().size();
        int columnsCount = stages.size();
        var builder = new StringBuilder();
        Supplier<StringBuilder> printRow = () -> builder.append(' ').append(cornerSeparator)
                .append(horizontalSeparator.repeat(columnsCount * (columnWidth + verticalSeparator.length()) - 1))
                .append(cornerSeparator).append('\n');
        printRow.get();
        builder.append(verticalSeparator);
        for (int i = 0; i < stages.size(); i++) {
            builder.append(i + 1).append(" ".repeat(columnWidth - ((int) Math.log10(i + 1) + 1))).append(verticalSeparator);
        }
        builder.append('\n');
        printRow.get();
        for(int row = 0; row < height; row++) {
            builder.append(verticalSeparator);
            for (var stage : stages) {
                if (stage.size() <= row) {
                    builder.append(" ".repeat(columnWidth));
                } else {
                    builder.append(stage.get(row).toBinaryString());
                }
                builder.append(verticalSeparator);
            }
            builder.append('\n');
        }
        printRow.get();
        System.out.println(builder.toString());
    }
}
