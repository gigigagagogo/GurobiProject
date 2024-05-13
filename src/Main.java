import com.gurobi.gurobi.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final String INPUT_FILE_FORMAT_ERROR = "Input file format error";
    static Scanner fileScanner;
    static Scanner lineScanner;
    static int d; // inumero giorni
    static int n; // numero di materie
    static int[] t; // ore per materia nell'arco dei giorni
    static int[] tau; // ore minime per materia, se studiata, al giorno
    static int l; // numero di materie differenti massime al giorno
    static int tmax; // ore massime di studio totali al giorno
    static int k; // indice relativo alla materia piu importante
    static int a;
    static int b;
    static int c;

    public static void main(String[] args) {
        try {
            String filename = "input/instance-11.txt";
            parseInputFile(filename);

            GRBEnv env = new GRBEnv("/dev/null");

        } catch (GRBException e) {
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }

    static int getVariableFromScanner(String expectedVariable) {
        if (!fileScanner.hasNext() || !fileScanner.next().equals(expectedVariable)) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
        try {
            String value = fileScanner.nextLine();
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
        }
    }

    static int[] getArrayFromScanner(String expectedArrayName) {
        try {
            String line = fileScanner.nextLine();
            lineScanner = new Scanner(line);
        } catch (Exception e) {
            throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
        }

        if (!lineScanner.next().equals(expectedArrayName)) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
        ArrayList<Integer> list = new ArrayList<>();
        while (lineScanner.hasNext()) {
            try {
                String value = lineScanner.next();
                list.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            }
        }
        lineScanner.close();
        return list.stream().mapToInt(i -> i).toArray();
    }

    static void parseInputFile(String filename) {
        try {
            File file = new File(filename);
            fileScanner = new Scanner(file);

            //read d
            d = getVariableFromScanner("d");

            // read t array
            t = getArrayFromScanner("t");
            n = t.length;

            // read l
            l = getVariableFromScanner("l");

            // read tau
            tau = getArrayFromScanner("tau");
            if (tau.length != n) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);

            // read tmax
            tmax = getVariableFromScanner("tmax");

            // read k
            k = getVariableFromScanner("k");

            // read a
            a = getVariableFromScanner("a");

            // read b
            b = getVariableFromScanner("b");

            // read c
            c = getVariableFromScanner("c");

            fileScanner.close();

        } catch (Exception e) {
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }
}
