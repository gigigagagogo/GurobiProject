import com.gurobi.gurobi.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final String INPUT_FILE_FORMAT_ERROR = "Input file format error";
    public static final int BIG_M = 24;
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
    static GRBVar[][] x;
    static GRBVar[][] y;

    public static void main(String[] args) {
        try {
            String filename = "input/instance-11.txt";
            parseInputFile(filename);

            GRBEnv env = new GRBEnv("logs/instance-11.log");
            impostaParametri(env);

            GRBModel intero = new GRBModel(env);

            x = aggiungiVariabiliIntere(intero);
            y = aggiungiVariabiliBinarie(intero);

            aggiungiFunzioneObiettivo(intero);

            aggiungiVincolo1(intero);
            aggiungiVincolo2(intero);
            aggiungiVincolo3(intero);
            aggiungiVincolo4(intero);

            intero.write("logs/write.lp");


        } catch (GRBException e) {
            e.printStackTrace();
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }

    private static void impostaParametri(GRBEnv env) throws GRBException {
        env.set(GRB.IntParam.Method, 0);
        env.set(GRB.IntParam.Presolve, 0);
        env.set(GRB.DoubleParam.Heuristics, 0);
    }

    private static GRBVar[][] aggiungiVariabiliIntere(GRBModel model) throws GRBException {
        GRBVar[][] x = new GRBVar[n][d];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < d; j++)
                x[i][j] = model.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "x_" + i + "_" + j);

        return x;
    }

    private static GRBVar[][] aggiungiVariabiliBinarie(GRBModel model) throws GRBException {
        GRBVar[][] y = new GRBVar[n][d];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < d; j++)
                y[i][j] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + i + "_" + j);

        return y;
    }

    private static void aggiungiFunzioneObiettivo(GRBModel model) throws GRBException
    {
        GRBLinExpr expr = new GRBLinExpr();
        for (int j = 0; j < d; j++)
            expr.addTerm(1, x[k][j]);

        model.setObjective(expr, GRB.MAXIMIZE);
    }

    public static void aggiungiVincolo1(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int j = 0; j < d; j++) {
                lhs.addTerm(1, x[i][j]);
            }
            model.addConstr(lhs, GRB.GREATER_EQUAL, tau[i], "ore_minime_materia_totali");
        }
    }

    public static void aggiungiVincolo2(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                GRBLinExpr lhs = new GRBLinExpr();
                GRBLinExpr rhs = new GRBLinExpr();
                GRBLinExpr rhs2 = new GRBLinExpr();
                lhs.addTerm(1, x[i][j]);
                rhs.addTerm(tau[i], y[i][j]);
                rhs2.addTerm(BIG_M, y[i][j]);
                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "min_ore_giornaliere_materia");
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs2, "materia_se_studiata");
            }
        }
    }


    public static void aggiungiVincolo3(GRBModel model) throws GRBException{
        for(int j = 0; j < d; j++){
            GRBLinExpr lhs = new GRBLinExpr();
            for(int i = 0; i < n; i++){
                lhs.addTerm(1, y[i][j]);
            }
            model.addConstr(lhs, GRB.LESS_EQUAL, l, "materie_max_giorno");
        }
    }

    public static void aggiungiVincolo4(GRBModel model) throws GRBException{
        for (int j = 0; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int i = 0; i < n; i++) {
                lhs.addTerm(1, x[i][j]);
            }
            model.addConstr(lhs, GRB.LESS_EQUAL, tmax, "ore_max_giorno");
        }
    }


    public static int getVariableFromScanner(String expectedVariable) {
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

            d = getVariableFromScanner("d");

            t = getArrayFromScanner("t");
            n = t.length;

            l = getVariableFromScanner("l");

            tau = getArrayFromScanner("tau");
            if (tau.length != n) throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);

            tmax = getVariableFromScanner("tmax");
            k = getVariableFromScanner("k");
            a = getVariableFromScanner("a");
            b = getVariableFromScanner("b");
            c = getVariableFromScanner("c");

            fileScanner.close();

        } catch (Exception e) {
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }
}
