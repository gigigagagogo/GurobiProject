import com.gurobi.gurobi.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final String INPUT_FILE_FORMAT_ERROR = "Input file format error";
    public static final int BIG_M_VINCOLO3 = 24;
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
            String filename = "instance-11";
            parseInputFile("input/" + filename + ".txt");

            GRBEnv env = new GRBEnv("logs/" + filename + ".log");
            impostaParametri(env);

            GRBModel intero = new GRBModel(env);
            intero.set(GRB.StringAttr.ModelName, "intero");

            x = aggiungiVariabiliIntere(intero);
            y = aggiungiVariabiliBinarie(intero);

            aggiungiFunzioneObiettivo(intero);

            aggiungiVincolo1(intero);
            aggiungiVincolo2(intero);
            aggiungiVincolo3(intero);
            aggiungiVincolo4(intero);
            aggiungiVincolo5(intero);

            intero.optimize();

            System.out.printf("------------------------------ Modello: %s ------------------------------\n", intero.get(GRB.StringAttr.ModelName));
            System.out.print("[1.a] ");
            stampaValoreOttimo(intero);
            System.out.print("[1.a] ");
            stampaVariabili(intero);

            GRBModel rilassato = new GRBModel(intero.relax());
            rilassato.set(GRB.StringAttr.ModelName, "rilassato");
            rilassato.optimize();

            System.out.printf("------------------------------ Modello: %s ------------------------------\n", rilassato.get(GRB.StringAttr.ModelName));
            System.out.print("[1.b] ");
            stampaVincoliAttivi(rilassato);
            System.out.print("[1.b] ");
            stampaDegenere(rilassato);
            System.out.print("[1.b] ");
            stampaSoluzioneUnica(rilassato);
            System.out.print("[1.b] ");
            stampaOttimoCoincidente(intero, rilassato);

            GRBModel interoStar = new GRBModel(intero);
            interoStar.set(GRB.StringAttr.ModelName, "interoStar");

            aggiungiVincolo6(interoStar);
            aggiungiVincolo7(interoStar, b);
            aggiungiVincolo7(interoStar, c);
            aggiungiVincolo8(interoStar);

            interoStar.optimize();

            System.out.printf("------------------------------ Modello: %s ------------------------------\n", intero.get(GRB.StringAttr.ModelName));
            System.out.print("[2] ");
            stampaValoreOttimo(interoStar);


            intero.write("logs/write.lp");

            intero.dispose();
            rilassato.dispose();
            interoStar.dispose();
            env.dispose();

        } catch (GRBException e) {
            e.printStackTrace();
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }

    public static void stampaOttimoCoincidente(GRBModel model1, GRBModel model2) throws GRBException {
        System.out.printf("Le soluzioni ottime di %s e %s %scoincidono\n",
                model1.get(GRB.StringAttr.ModelName),
                model2.get(GRB.StringAttr.ModelName),
                isOttimoCoincidente(model1, model2) ? "" : "non ");
    }
    public static boolean isOttimoCoincidente(GRBModel model1, GRBModel model2) throws GRBException {
        GRBVar[] variablesModel1 = model1.getVars();
        GRBConstr[] constrModel1 = model1.getConstrs();
        GRBVar[] variablesModel2 = model2.getVars();
        GRBConstr[] constrModel2 = model2.getConstrs();
        if ((variablesModel1.length != variablesModel2.length) | (constrModel1.length != constrModel2.length))
            return false;
        for (int i = 0; i < variablesModel1.length; i++) {
            if (variablesModel1[i].get(GRB.DoubleAttr.X) != variablesModel2[i].get(GRB.DoubleAttr.X))
                return false;
        }
        for (int i = 0; i < constrModel1.length; i++) {
            if (constrModel1[i].get(GRB.DoubleAttr.Slack) != constrModel2[i].get(GRB.DoubleAttr.Slack))
                return false;
        }

        return true;
    }

    public static void stampaSoluzioneUnica(GRBModel model) throws GRBException {
        System.out.printf("La soluzione ottima %sè unica\n",
                isSoluzioneOttimaUnica(model) ? "" : "non ");
    }

    public static boolean isSoluzioneOttimaUnica(GRBModel model) throws GRBException {
        for (GRBVar v: model.getVars())
            if (v.get(GRB.IntAttr.VBasis) != GRB.BASIC && Math.abs(v.get(GRB.DoubleAttr.RC)) < 1e-6)
                return false;
        for (GRBConstr c: model.getConstrs())
            if (c.get(GRB.IntAttr.CBasis) != GRB.BASIC && Math.abs(c.get(GRB.DoubleAttr.Pi)) < 1e-6)
                return false;

        return true;
    }

    public static void stampaDegenere(GRBModel model) throws GRBException {
        ArrayList<GRBVar> variabiliDegenere = getVariabiliDegenere(model);
        ArrayList<GRBConstr> constrDegenere = getConstrDegenere(model);
        if (variabiliDegenere.isEmpty() && constrDegenere.isEmpty()) {
            System.out.println("La soluzione trovata non è degenere\n");
            return;
        }

        System.out.println("La soluzione trovata è degenere\nVariabili in base equivalenti a 0:");
        for (GRBVar v: variabiliDegenere)
            System.out.printf("\t%s\n", v.get(GRB.StringAttr.VarName));
        for (GRBConstr c: constrDegenere)
            System.out.printf("\t%s\n", c.get(GRB.StringAttr.ConstrName));
        System.out.printf("\t(totale %d)\n", variabiliDegenere.size() + constrDegenere.size());

    }

    public static ArrayList<GRBVar> getVariabiliDegenere(GRBModel model) throws GRBException {
        ArrayList<GRBVar> list = new ArrayList<>();
        for (GRBVar v: model.getVars())
            if (v.get(GRB.IntAttr.VBasis) == GRB.BASIC && Math.abs(v.get(GRB.DoubleAttr.X)) < 1e-6)
                list.add(v);

        return list;
    }

    public static ArrayList<GRBConstr> getConstrDegenere(GRBModel model) throws GRBException {
        ArrayList<GRBConstr> list = new ArrayList<>();
        for (GRBConstr c: model.getConstrs())
            if (c.get(GRB.IntAttr.CBasis) == GRB.BASIC && Math.abs(c.get(GRB.DoubleAttr.Slack)) < 1e-6)
                list.add(c);

        return list;
    }

    public static void stampaVariabili(GRBModel model) throws GRBException {
        System.out.println("Variabili non di slack/surplus:");
        for (GRBVar v: model.getVars())
            System.out.printf("\t%s = %s\n", v.get(GRB.StringAttr.VarName), v.get(GRB.DoubleAttr.X));

        System.out.println("Variabili di slack/surplus:");
        for (GRBConstr c: model.getConstrs())
            System.out.printf("\t%s = %s\n", c.get(GRB.StringAttr.ConstrName), c.get(GRB.DoubleAttr.Slack));

    }

    public static void stampaValoreOttimo(GRBModel model) throws GRBException {
        System.out.printf("Valore ottimo della funzione obiettivo: %s\n", model.get(GRB.DoubleAttr.ObjVal));
    }

    public static void stampaVincoliAttivi(GRBModel model) throws GRBException {
        int count = 0;
        System.out.println("Vincoli attivi:");
        for (GRBConstr c: model.getConstrs())
            if (Math.abs(c.get(GRB.DoubleAttr.Slack)) < 1e-6) {
                System.out.printf("\t%d - %s\n", c.index(), c.get(GRB.StringAttr.ConstrName));
                count++;
            }
        System.out.printf("\t(totale %d)\n", count);
    }

    public static void aggiungiFunzioneObiettivo(GRBModel model) throws GRBException
    {
        GRBLinExpr expr = new GRBLinExpr();
        for (int j = 0; j < d; j++)
            expr.addTerm(1, x[k][j]);
        model.setObjective(expr, GRB.MAXIMIZE);
    }

    public static void aggiungiVincolo1(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int j = 0; j < d; j++)
                lhs.addTerm(1, x[i][j]);

            model.addConstr(lhs, GRB.GREATER_EQUAL, t[i], String.format("ore_minime_totali_materia_%d", i));
        }
    }

    public static void aggiungiVincolo2(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                GRBLinExpr lhs = new GRBLinExpr();
                lhs.addTerm(1, x[i][j]);
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(tau[i], y[i][j]);
                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("ore_minime_studio_materia_%d_nel_giorno_%d_se_studiata", i, j));
            }
        }
    }

    public static void aggiungiVincolo3(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                GRBLinExpr lhs = new GRBLinExpr();
                lhs.addTerm(1, x[i][j]);
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(BIG_M_VINCOLO3, y[i][j]);
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("BIG_M_se_materia_%d_studiata_nel_giorno_%d", i, j));
            }
        }
    }

    public static void aggiungiVincolo4(GRBModel model) throws GRBException {
        for (int j = 0; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for(int i = 0; i < n; i++)
                lhs.addTerm(1, y[i][j]);

            model.addConstr(lhs, GRB.LESS_EQUAL, l, String.format("materie_massime_studiate_nel_giorno_%d", j));
        }
    }

    public static void aggiungiVincolo5(GRBModel model) throws GRBException {
        for (int j = 0; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int i = 0; i < n; i++)
                lhs.addTerm(1, x[i][j]);
            model.addConstr(lhs, GRB.LESS_EQUAL, tmax, String.format("ore_massime_studiate_nel_giorno_%d", j));
        }
    }

    public static void aggiungiVincolo6(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            for (int j = 2; j < d; j++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int m = j; m > j - 3; m--){
                    lhs.addTerm(1, y[i][m]);
                }
                model.addConstr(lhs, GRB.LESS_EQUAL, 2, String.format("materia_%d_non_studiata_3_giorni_di_fila_dal_giorno_%d", i, j-2));
            }

        }
    }

    public static void aggiungiVincolo7(GRBModel model, int i) throws GRBException {
        for (int j = 1; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            GRBLinExpr rhs = new GRBLinExpr();

            lhs.addTerm(1, y[a][i]);
            rhs.addConstant(1);
            rhs.addTerm(-1, y[i][j-1]);

            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("materia_%d_studiata_giorno_%d_se_giorno_%d_non_studiata_%d", a, j, j-1,i));
        }
    }

    public static void aggiungiVincolo8(GRBModel model) throws GRBException{
        for(int j = 1; j < d; j++){
            GRBLinExpr lhs = new GRBLinExpr();
            GRBLinExpr rhs = new GRBLinExpr();

            lhs.addTerm(1, y[a][j]);
            rhs.addConstant(1);
            rhs.addTerm(-1, y[b][j-1]);
            rhs.addTerm(-1,y [c][j-1]);
            model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("dobbia_implicazione_%d_%d_%d",b,c,j-1));
        }
    }

    public static void impostaParametri(GRBEnv env) throws GRBException {
        env.set(GRB.IntParam.Method, -1);
        env.set(GRB.IntParam.Presolve, -1);
        env.set(GRB.DoubleParam.Heuristics, 0);
        env.set(GRB.IntParam.LogToConsole, 1);
    }

    public static GRBVar[][] aggiungiVariabiliIntere(GRBModel model) throws GRBException {
        GRBVar[][] x = new GRBVar[n][d];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < d; j++)
                x[i][j] = model.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "x_" + i + "_" + j);

        return x;
    }

    public static GRBVar[][] aggiungiVariabiliBinarie(GRBModel model) throws GRBException {
        GRBVar[][] y = new GRBVar[n][d];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < d; j++)
                y[i][j] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + i + "_" + j);

        return y;
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
