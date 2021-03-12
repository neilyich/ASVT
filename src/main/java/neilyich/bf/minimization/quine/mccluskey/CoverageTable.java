package neilyich.bf.minimization.quine.mccluskey;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import neilyich.bf.minimization.Implicant;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// реализация логики работы таблицы покрытия
public class CoverageTable {
    private final List<Implicant> intersected;
    private final List<Implicant> sdnf;

    private final Map<Integer, boolean[]> table;

    private static final String verticalSeparator = " | ";
    private static final String horizontalSeparator = "-";
    private static final String cornerSeparator = "+";

    private final Set<Column> columns;
    private final Set<Row> rows;

    // столбец таблицы (содержит строки, которые в данном столбце имеют метку)
    @Getter
    @RequiredArgsConstructor
    class Column implements Comparable<Column>  {
        private final int number;
        private final Set<Row> ones = new HashSet<>();
        @Override
        public int compareTo(Column o) {
            return Integer.compare(o.ones.size(), ones.size());
        }
    }

    // строка матрицы (содержит столбцы, которые в данной строке имеют метку)
    @Getter
    @RequiredArgsConstructor
    class Row implements Comparable<Row> {
        private final int number;
        private final Set<Column> containedBy = new HashSet<>();
        @Override
        public int compareTo(Row o) {
            return Integer.compare(containedBy.size(), o.containedBy.size());
        }
    }

    // построение таблицы по склеенным импликантам и наборам, на которых значение функции равно 1
    public CoverageTable(List<Implicant> intersected, List<Implicant> sdnf) {
        this.intersected = intersected;
        this.sdnf = sdnf;
        columns = new HashSet<>(sdnf.size());
        for(int i = 0; i < sdnf.size(); i++) {
            var column = new Column(i);
            columns.add(column);
        }
        rows = new HashSet<>(intersected.size());
        for (int i = 0; i < intersected.size(); i++) {
            var row = new Row(i);
            for(var col: columns) {
                if(intersected.get(i).covers(sdnf.get(col.number))) {
                    row.containedBy.add(col);
                    col.ones.add(row);
                }
            }
            rows.add(row);
        }
        table = new HashMap<>(sdnf.size());
        for (int i = 0; i < sdnf.size(); i++) {
            var implicant = sdnf.get(i);
            var column = new boolean[intersected.size()];
            table.put(i, column);
            for (int k = 0; k < intersected.size(); k++) {
                var term = intersected.get(k);
                column[k] = term.covers(implicant);
            }
        }
    }

    // вычисление минимального покрытия таблицы
    public List<Implicant> calcMinCoverage() {
        List<Set<Implicant>> terms = new ArrayList<>(columns.size());
        int varsCount = rows.size();
        for(var col: columns) {
            Set<Implicant> term = new HashSet<>(col.ones.size());
            for(var r: col.ones) {
                var impl = new Implicant(varsCount);
                impl.set(r.number, true);
                term.add(impl);
            }
            terms.add(term);
        }
        if(intersected.size() == 0) {
            return new ArrayList<>();
        }
        List<Integer> all = new ArrayList<>(intersected.size());
        for (int i = 0; i < intersected.size(); i++) {
            all.add(i);
        }
        log("Calculating minimal coverage for table:");
        printTable(all);
        var coverages = Implicant.multAll(terms);
        var it = coverages.iterator();
        var minCoverage = it.next();
        int minWeight = calcWeight(minCoverage);
        while (it.hasNext()) {
            var cov = it.next();

            int w = calcWeight(cov);
            if(w < minWeight) {
                minCoverage = cov;
                minWeight = w;
            }
        }
        List<Integer> usedRows = new ArrayList<>(minCoverage.getWeight());
        for (int i = 0; i < varsCount; i++) {
            if (minCoverage.get(i) != null && minCoverage.get(i)) {
                usedRows.add(i);
            }
        }
        log("Found minimal coverage (", usedRows.size(), " rows):");
        printTable(usedRows);
        List<Implicant> minSdnf = new ArrayList<>(usedRows.size());
        for(var r: usedRows) {
            minSdnf.add(intersected.get(r));
        }
        return minSdnf;
    }

    // вывод таблицы на экран
    private void printTable(List<Integer> usedRows) {
        if(usedRows.isEmpty()) {
            System.out.println("--------");
            return;
        }
        int maxRow = usedRows.stream().max(Integer::compareTo).get();
        int numsWidth = (int) Math.log10(maxRow + 1) + 1;
        var builder = new StringBuilder();
        var implLength = intersected.get(0).toBinaryString().length();
        if(implLength == 0) {
            System.out.println("--------");
            return;
        }
        int columnsCount = table.size();
        List<Integer> columnsNums = table.keySet().stream().sorted(Integer::compare).collect(Collectors.toList());
        Supplier<StringBuilder> printRow = () -> builder.append(" ").append(cornerSeparator)
                .append(horizontalSeparator.repeat((columnsCount + 1) * (implLength + verticalSeparator.length()) + numsWidth + verticalSeparator.length() - 1))
                .append(cornerSeparator).append('\n');
        printRow.get();
        builder.append(verticalSeparator).append(" ".repeat(numsWidth)).append(verticalSeparator).append(" ".repeat(implLength)).append(verticalSeparator);
        for(int col: columnsNums) {
            builder.append(sdnf.get(col).toBinaryString()).append(verticalSeparator);
        }
        builder.append('\n');
        printRow.get();
        for (int row : usedRows) {
            builder.append(verticalSeparator)
                    .append(" ".repeat(numsWidth - ((int) Math.log10(row + 1) + 1)))
                    .append(row + 1).append(verticalSeparator)
                    .append(intersected.get(row).toBinaryString())
                    .append(verticalSeparator);
            for (int col : columnsNums) {
                boolean f = table.get(col)[row];
                builder.append(f ? "V" : " ").append(" ".repeat(implLength - 1)).append(verticalSeparator);
            }
            builder.append('\n');
        }
        printRow.get();
        System.out.print(builder.toString());
    }

    private static <T> void log(T t) {
        System.out.println(t);
    }

    private static void log(Object... args) {
        for(var o: args) {
            System.out.print(o);
        }
        System.out.println();
    }

    // подсчет веса полученного покрытия таблицы
    private int calcWeight(Implicant tableCoverage) {
        int varsCount = rows.size();
        int w = 0;
        for (int i = 0; i < varsCount; i++) {
            if(tableCoverage.get(i) != null && tableCoverage.get(i)) {
                w += intersected.get(i).literalsCount();
            }
        }
        return w;
    }
}
