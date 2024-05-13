import com.gurobi.gurobi.*;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final String INPUT_FILE_FORMAT_ERROR = "Input file format error";
    static int d; // numero giorni
    static int n; // numero di materie
    static int t[]; // ore per materia nell'arco dei giorni
    static int tau[]; // ore minime per materia, se studiata, al giorno
    static int l; // numero di materie differenti massime al giorno
    static int tmax; // ore massime di studio totali al giorno
    static int k; // indice relativo alla materia piu importante
    static int a;
    static int b;
    static int c;

    public static void main(String[] args) {
        try {
            readFile();

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    static void readFile() {
        try {
            String filename = "input/instance-11.txt";
            File file = new File(filename);
            Scanner scanner = new Scanner(file);

            //read d
            if (!scanner.next().equals("d")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            d = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read t array
            String line = scanner.nextLine();
            Scanner lineScanner = new Scanner(line);
            if (!lineScanner.next().equals("t")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            ArrayList<Integer> arrayList = new ArrayList<>();
            while (lineScanner.hasNext()) {
                arrayList.add(lineScanner.nextInt());
            }
            t = new int[arrayList.size()];
            int n = 0;
            for (Integer num : arrayList) {
                t[n++] = num;
            }
            lineScanner.nextLine(); // remove trailing newline char
            lineScanner.close();

            // read l
            if (!scanner.next().equals("l")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            l = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read tau
            line = scanner.nextLine();
            lineScanner = new Scanner(line);
            if (!lineScanner.next().equals("tau")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            arrayList = new ArrayList<>();
            while (lineScanner.hasNext()) {
                arrayList.add(lineScanner.nextInt());
            }
            if (arrayList.size() != n) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            tau = new int[n];
            for (int i = 0; i < n; i++) {
                tau[i] = arrayList.get(i);

            }
            lineScanner.nextLine(); // remove trailing newline char
            lineScanner.close();

            // read tmax
            if (!scanner.next().equals("tmax")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            tmax = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read k
            if (!scanner.next().equals("k")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            k = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read a
            if (!scanner.next().equals("a")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            a = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read b
            if (!scanner.next().equals("b")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            b = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            // read c
            if (!scanner.next().equals("c")) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
            c = scanner.nextInt();
            scanner.nextLine(); // read trailing newline

            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        };
    }
}
