import com.gurobi.gurobi.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final String INPUT_FILE_FORMAT_ERROR = "Input file format error";
    public static final int BIGM = 10;
    public static final double TOLLERANCE = 1e-6;
    public static final double INF_LIMIT = 1e50;
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
    static GRBVar z;

    public static void main(String[] args) {
        try {
            String filename = "instance-11";
            parseInputFile("input/" + filename + ".txt");

            GRBEnv env = new GRBEnv("logs/" + filename + ".log");
            impostaParametri(env);

            GRBModel intero = new GRBModel(env);
            intero.set(GRB.StringAttr.ModelName, "Intero");

            x = aggiungiVariabiliIntere(intero);
            y = aggiungiVariabiliBinarie(intero);

            aggiungiFunzioneObiettivo(intero);

            aggiungiVincolo1(intero);
            aggiungiVincolo2(intero);
            aggiungiVincolo3(intero);
            aggiungiVincolo4(intero);
            aggiungiVincolo5(intero);

            intero.optimize();

            GRBModel rilassato = new GRBModel(intero.relax());
            rilassato.set(GRB.StringAttr.ModelName, "Rilassato");
            rilassato.optimize();

            GRBModel interoStar = new GRBModel(intero);
            interoStar.set(GRB.StringAttr.ModelName, "Nuovo Intero");

            aggiungiVincolo6(interoStar);
            aggiungiVincolo7(interoStar, b);
            aggiungiVincolo7(interoStar, c);
            aggiungiVincolo8(interoStar);

            interoStar.optimize();

            System.out.println("GRUPPO 3");
            System.out.println("Componenti: Bararu Valperta");
            System.out.println("QUESITO I:");
            stampaValoreOttimo(intero);
            stampaVariabili(intero);
            stampaValoreOttimo(rilassato);
            stampaVincoliAttivi(rilassato);
            stampaDegenere(rilassato);
            stampaSoluzioneMultipla(rilassato);
            stampaOttimoCoincidente(rilassato, intero);

            System.out.println("QUESITO II:");
            stampaValoreOttimo(interoStar);

            System.out.println("QUESITO III:");
            stampaIntervalloDeltaPi2(rilassato);

            GRBModel minimizeTMAX = new GRBModel(interoStar);
            z = minimizeTMAX.addVar(0, tmax, 0, GRB.INTEGER, "z");
            editObjective(minimizeTMAX);
            editVincolo2(minimizeTMAX);
            minimizeTMAX.optimize();

            stampaValoreMinimoTMAX(minimizeTMAX);

            intero.dispose();
            rilassato.dispose();
            interoStar.dispose();
            minimizeTMAX.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("An exception occurred: " + e.getMessage());
        }
    }

    /**
     * Stampa il valore minimo che tmax può assumere affinchè il problema sia risolvibile.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaValoreMinimoTMAX(GRBModel model) throws GRBException {
        double tmax_min = tmax - model.get(GRB.DoubleAttr.ObjVal);
        System.out.printf("valore minimo t max = %s\n", tmax_min);
    }

    /**
     * Stampa la minima e massima variazione che potrebbe assumere tau_2
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaIntervalloDeltaPi2(GRBModel model) throws GRBException {
        System.out.print("intervallo DELTA tau_2 = ");
        double minTau = getMinTauVariation(model, 2);
        if (minTau < -INF_LIMIT)
            System.out.print("(-INF, ");
        else
            System.out.printf("[%s", minTau);
        double maxTau = getMaxTauVariation(model, 2);
        if (maxTau > INF_LIMIT)
            System.out.print("+INF)\n");
        else
            System.out.printf("%s]\n", maxTau);
    }

    /**
     * Modifica l'obiettivo del modello utilizzato per cercare la variazione massima di tmax
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void editObjective(GRBModel model) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        expr.addTerm(1, z);
        model.setObjective(expr, GRB.MAXIMIZE);
    }

    /**
     * Modifica il vincolo 2 del modello per risolvere la variazione massima di tmax
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void editVincolo2(GRBModel model) throws GRBException {
        String constrName;
        for (int j = 0; j < d; j++) {
            constrName = String.format("ore_massime_studiate_nel_giorno_%d", j);

            model.remove(model.getConstrByName(constrName));

            GRBLinExpr lhs = new GRBLinExpr();
            for (int i = 0; i < n; i++)
                lhs.addTerm(1, x[i][j]);
            GRBLinExpr rhs = new GRBLinExpr();
            rhs.addConstant(tmax);
            rhs.addTerm(-1, z);
            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("ore_massime_studiate_nel_giorno_%d", j));

        }
    }

    /**
     * Ottiene la variazione massima di tau per una data materia.
     *
     * @param model         il modello Gurobi
     * @param indiceMateria l'indice della materia
     * @return la variazione massima di tau
     * @throws GRBException se c'è un errore Gurobi
     */
    public static double getMaxTauVariation(GRBModel model, int indiceMateria) throws GRBException {
        String constrName = "ore_minime_studio_materia_%d_nel_giorno_%d_se_studiata";
        GRBConstr constr = model.getConstrByName(String.format(constrName, indiceMateria, 0));
        double minValue = constr.get(GRB.DoubleAttr.SARHSUp);

        for (int j = 1; j < d; j++) {
            constr = model.getConstrByName(String.format(constrName, indiceMateria, j));

            // return the most restrictive constraint
            minValue = Math.min(minValue, constr.get(GRB.DoubleAttr.SARHSUp));
        }

        return (minValue + BIGM) - tau[indiceMateria];
    }

    /**
     * Ottiene la variazione minima di tau per una data materia.
     *
     * @param model         il modello Gurobi
     * @param indiceMateria l'indice della materia
     * @return la variazione minima di tau
     * @throws GRBException se c'è un errore Gurobi
     */
    public static double getMinTauVariation(GRBModel model, int indiceMateria) throws GRBException {
        String constrName = "ore_minime_studio_materia_%d_nel_giorno_%d_se_studiata";
        GRBConstr constr = model.getConstrByName(String.format(constrName, indiceMateria, 0));
        double maxValue = constr.get(GRB.DoubleAttr.SARHSLow);

        for (int j = 1; j < d; j++) {
            constr = model.getConstrByName(String.format(constrName, indiceMateria, j));

            // return the most restrictive constraint
            maxValue = Math.max(maxValue, constr.get(GRB.DoubleAttr.SARHSLow));
        }

        return -(tau[indiceMateria] - (maxValue + BIGM));
    }


    /**
     * Stampa se gli ottimi coincidono tra due modelli.
     *
     * @param model1 il primo modello Gurobi
     * @param model2 il secondo modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaOttimoCoincidente(GRBModel model1, GRBModel model2) throws GRBException {
        System.out.printf("%s coincide con %s: %s\n",
                model1.get(GRB.StringAttr.ModelName),
                model2.get(GRB.StringAttr.ModelName),
                isOttimoCoincidente(model1, model2) ? "sì" : "no");
    }

    /**
     * Ritorna se gli ottimi coincidono tra due modelli.
     *
     * @return se l'ottimo tra i due modelli è coincidente
     * @param model1 il primo modello Gurobi
     * @param model2 il secondo modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
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

    /**
     * Stampa se c'è soluzione multipla.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaSoluzioneMultipla(GRBModel model) throws GRBException {
        System.out.printf("Multipla: %s\n",
                isSoluzioneOttimaUnica(model) ? "no" : "sì");
    }

    /**
     * Ritorna se la soluzione del modello è unica
     *
     * @param model il modello Gurobi
     * @return se la soluzione è ottima
     * @throws GRBException se c'è un errore Gurobi
     */
    public static boolean isSoluzioneOttimaUnica(GRBModel model) throws GRBException {
        for (GRBVar v : model.getVars())
            if (v.get(GRB.IntAttr.VBasis) != GRB.BASIC && Math.abs(v.get(GRB.DoubleAttr.RC)) < TOLLERANCE)
                return false;
        for (GRBConstr c : model.getConstrs())
            if (c.get(GRB.IntAttr.CBasis) != GRB.BASIC && Math.abs(c.get(GRB.DoubleAttr.Pi)) < TOLLERANCE)
                return false;

        return true;
    }

    /**
     * Stampa se il la soluzione è degenere.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaDegenere(GRBModel model) throws GRBException {

        System.out.printf("Degenere: %s\n", isDegenere(model) ? "sì" : "no");
    }

    /**
     * Ritorna se la soluzione è degenere.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static boolean isDegenere(GRBModel model) throws GRBException {
        for (GRBVar v : model.getVars())
            if (v.get(GRB.IntAttr.VBasis) == GRB.BASIC && Math.abs(v.get(GRB.DoubleAttr.X)) < TOLLERANCE)
                return true;

        for (GRBConstr c : model.getConstrs())
            if (c.get(GRB.IntAttr.CBasis) == GRB.BASIC && Math.abs(c.get(GRB.DoubleAttr.Slack)) < TOLLERANCE)
                return true;

        return false;
    }

    /**
     * Stampa tutte le variabili del modello.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaVariabili(GRBModel model) throws GRBException {
        System.out.println("Variabili:");
        for (GRBVar v : model.getVars())
            System.out.printf("\t%s = %s\n", v.get(GRB.StringAttr.VarName), v.get(GRB.DoubleAttr.X));

        System.out.println("Slack/Surplus:");
        for (GRBConstr c : model.getConstrs())
            System.out.printf("\t%s = %s\n", c.get(GRB.StringAttr.ConstrName), c.get(GRB.DoubleAttr.Slack));

    }

    /**
     * Stampa il valore ottimo del modello ottimizzato.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaValoreOttimo(GRBModel model) throws GRBException {
        if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL)
            System.out.printf("Obj %s = %s\n", model.get(GRB.StringAttr.ModelName), model.get(GRB.DoubleAttr.ObjVal));
        else
            System.out.println("Il modello non è stato risolto");
    }

    /**
     * Stampa la lista di vincoli attivi alla soluzione ottima del modello.
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void stampaVincoliAttivi(GRBModel model) throws GRBException {
        System.out.println("Elenco nomi vincoli attivi:");
        for (GRBConstr c : model.getConstrs())
            if (Math.abs(c.get(GRB.DoubleAttr.Slack)) < TOLLERANCE)
                System.out.printf("\t%s\n", c.get(GRB.StringAttr.ConstrName));
    }

    /**
     * Aggiunge la funzione obiettivo richiesta nel modello
     *
     * @param model il modello Gurobi
     * @throws GRBException se c'è un errore Gurobi
     */
    public static void aggiungiFunzioneObiettivo(GRBModel model) throws GRBException {
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
                lhs.addConstant(BIGM);
                lhs.addTerm(-BIGM, y[i][j]);
                model.addConstr(lhs, GRB.GREATER_EQUAL, tau[i], String.format("ore_minime_studio_materia_%d_nel_giorno_%d_se_studiata", i, j));
            }
        }
    }

    public static void aggiungiVincolo3(GRBModel model) throws GRBException {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                GRBLinExpr lhs = new GRBLinExpr();
                lhs.addTerm(1, x[i][j]);
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(BIGM, y[i][j]);
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("BIG_M_se_materia_%d_studiata_nel_giorno_%d", i, j));
            }
        }
    }

    public static void aggiungiVincolo4(GRBModel model) throws GRBException {
        for (int j = 0; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int i = 0; i < n; i++)
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
                for (int m = j; m > j - 3; m--) {
                    lhs.addTerm(1, y[i][m]);
                }
                model.addConstr(lhs, GRB.LESS_EQUAL, 2, String.format("materia_%d_non_studiata_3_giorni_di_fila_dal_giorno_%d", i, j - 2));
            }

        }
    }

    public static void aggiungiVincolo7(GRBModel model, int i) throws GRBException {
        for (int j = 1; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            GRBLinExpr rhs = new GRBLinExpr();

            lhs.addTerm(1, y[a][i]);
            rhs.addConstant(1);
            rhs.addTerm(-1, y[i][j - 1]);

            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("materia_%d_studiata_giorno_%d_se_giorno_%d_non_studiata_%d", a, j, j - 1, i));
        }
    }

    public static void aggiungiVincolo8(GRBModel model) throws GRBException {
        for (int j = 1; j < d; j++) {
            GRBLinExpr lhs = new GRBLinExpr();
            GRBLinExpr rhs = new GRBLinExpr();

            lhs.addTerm(1, y[a][j]);
            rhs.addConstant(1);
            rhs.addTerm(-1, y[b][j - 1]);
            rhs.addTerm(-1, y[c][j - 1]);
            model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("dobbia_implicazione_%d_%d_%d", b, c, j - 1));
        }
    }

    public static void impostaParametri(GRBEnv env) throws GRBException {
        env.set(GRB.IntParam.Method, 0);
        env.set(GRB.IntParam.Presolve, 0);
        env.set(GRB.DoubleParam.Heuristics, 0);
        env.set(GRB.IntParam.LogToConsole, 0);
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
        if (!fileScanner.hasNext() || !fileScanner.next().equals(expectedVariable))
            throw new IllegalArgumentException(INPUT_FILE_FORMAT_ERROR);
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