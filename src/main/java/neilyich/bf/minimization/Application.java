package neilyich.bf.minimization;

import neilyich.bf.minimization.undetermined.coefs.CoefsMinimizer;
import neilyich.bf.minimization.quine.mccluskey.QuineMinimizer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Application {
    public static void main(String[] args) throws IOException {
        String vector = getBFVector(args);
        test(vector);
    }

    // получение вектора БФ в зависимости от аргументов
    private static String getBFVector(String[] args) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        for(var arg : args) {
            var pair = arg.split("=");
            if(pair.length == 2) {
                parameters.put(pair[0].trim(), pair[1].trim());
            }
            else {
                System.out.println("Unknown parameter: " + arg);
            }
        }

        String vector;
        if(parameters.containsKey("vector")) {
            vector = parameters.get("vector");
        }
        else if(parameters.containsKey("input")) {
            vector = Files.readString(Path.of(parameters.get("input")));
        }
        else {
            System.out.println("Enter vector of BF:");
            vector = new Scanner(System.in).nextLine();
        }

        if(parameters.containsKey("output")) {
            System.setOut(new PrintStream(new FileOutputStream(parameters.get("output"))));
        }
        return vector;
    }

    // проведение минимизации функции заданным алгоритмом
    private static BooleanFunction minimize(BooleanFunction f, Minimizer minimizer) {
        var min = minimizer.minimize(f);
        System.out.println("\n\nMinimized function:");
        System.out.println(min);
        return min;
    }

    // минимизация функции двумя алгоритмами и сравнение результатов
    private static boolean test(String vector) {
        System.out.println("\n\nminimizing f = " + vector + ":");
        System.out.println("Sdnf: " + new BooleanFunction(vector).toString());
        var quineMinimizer = new QuineMinimizer();
        var coefsMinimizer = new CoefsMinimizer();
        System.out.println("\nUNDETERMINED COEFS ALGORITHM:\n");
        var f2 = minimize(new BooleanFunction(vector), coefsMinimizer);
        System.out.println("\n\nQUINE MCCLUSSKEY ALGORITHM:\n");
        var f1 = minimize(new BooleanFunction(vector), quineMinimizer);
        System.out.println("\n\n\n----------------\n\n");
        System.out.println("Quine McCluskey algorithm    : " + f1.toString() + " ; weight = " + f1.weight() + "; vector = " + f1.getVector());
        System.out.println("Undetermined coefs algorithm : " + f2.toString() + " ; weight = " + f2.weight() + "; vector = " + f2.getVector());
        System.out.println();
        var dif1 = f1.diff(f2);
        var dif2 = f2.diff(f1);
        System.out.println("Implicants only in Quine McCluskey algorithm result    : " + dif1.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()).toString());
        System.out.println("Implicants only in Undetermined coefs algorithm result : " + dif2.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()).toString());
        if(f1.isSame(f2)) {
            System.out.println("Same functions");
            int cmp = f1.compareSame(f2);
            if(cmp == 0) {
                System.out.println("Sdnfs are equal");
                return true;
            }
            else if(cmp == -1) {
                System.out.println("Quine McCluskey result is better:");
                System.out.println(f1.weight() + " vs " + f2.weight());
            }
            else {
                System.out.println("Undetermined coefs result is better");
                System.out.println(f2.weight() + " vs " + f1.weight());
            }
        }
        else {
            System.out.println("Functions are different");
            //throw new RuntimeException(vector);
        }
        return false;
    }
}
