package com.ontariotechu.sofe3980U;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;

public class App {
    private static final double EPS = 1e-15;

    /** Evaluates a binary classification CSV: true (0/1), predicted probability. */
    private static void evaluateBinaryModel(String fileName) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        double bceSum = 0.0;
        int n = 0;

        // First pass: collect probabilities and true labels for ROC
        double[] probs = new double[0];
        int[] trues = new int[0];

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // Skip header if present
                if (n == 0) {
                    try { Integer.parseInt(line[0]); } catch (NumberFormatException e) { continue; }
                }
                if (line.length < 2) continue;

                int yTrue = Integer.parseInt(line[0]);
                double yProb = Double.parseDouble(line[1]);
                // clip probability to avoid log(0)
                if (yProb < EPS) yProb = EPS;
                if (yProb > 1.0 - EPS) yProb = 1.0 - EPS;

                // BCE contribution
                if (yTrue == 1) {
                    bceSum += Math.log(yProb);
                } else {
                    bceSum += Math.log(1.0 - yProb);
                }

                // Confusion matrix with threshold 0.5
                int pred = (yProb >= 0.5) ? 1 : 0;
                if (yTrue == 1 && pred == 1) tp++;
                else if (yTrue == 0 && pred == 1) fp++;
                else if (yTrue == 0 && pred == 0) tn++;
                else if (yTrue == 1 && pred == 0) fn++;

                // Store for ROC
                probs = java.util.Arrays.copyOf(probs, probs.length + 1);
                trues = java.util.Arrays.copyOf(trues, trues.length + 1);
                probs[n] = yProb;
                trues[n] = yTrue;
                n++;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        if (n == 0) {
            System.err.println("No data rows found in " + fileName);
            return;
        }

        double bce = -bceSum / n;

        double accuracy = (tp + tn) / (double)(tp + tn + fp + fn);
        double precision = (tp + fp) > 0 ? tp / (double)(tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? tp / (double)(tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? 2.0 * precision * recall / (precision + recall) : 0.0;

        // ----- ROC and AUC -----
        int nPos = 0, nNeg = 0;
        for (int label : trues) {
            if (label == 1) nPos++; else nNeg++;
        }
        double[] tpr = new double[101];
        double[] fpr = new double[101];
        for (int i = 0; i <= 100; i++) {
            double th = i / 100.0;
            int tpTh = 0, fpTh = 0;
            for (int k = 0; k < n; k++) {
                if (probs[k] >= th) {
                    if (trues[k] == 1) tpTh++; else fpTh++;
                }
            }
            tpr[i] = nPos > 0 ? tpTh / (double)nPos : 0.0;
            fpr[i] = nNeg > 0 ? fpTh / (double)nNeg : 0.0;
        }
        double auc = 0.0;
        for (int i = 1; i <= 100; i++) {
            auc += (tpr[i-1] + tpr[i]) * Math.abs(fpr[i-1] - fpr[i]) / 2.0;
        }

        // ----- Output -----
        System.out.println("for " + fileName);
        System.out.printf("        BCE =%.8f%n", bce);
        System.out.println("        Confusion matrix");
        System.out.println("                        y=1      y=0");
        System.out.printf("                y^=1    %d    %d%n", tp, fp);
        System.out.printf("                y^=0    %d    %d%n", fn, tn);
        System.out.printf("        Accuracy =%.4f%n", accuracy);
        System.out.printf("        Precision =%.8f%n", precision);
        System.out.printf("        Recall =%.8f%n", recall);
        System.out.printf("        f1 score =%.8f%n", f1);
        System.out.printf("        auc roc =%.8f%n", auc);
    }

    public static void main(String[] args) {
        evaluateBinaryModel("model_1.csv");
        evaluateBinaryModel("model_2.csv");
        evaluateBinaryModel("model_3.csv");

        // Determine best model per metric (smallest BCE, largest others)
        double[] m1 = getBinaryMetrics("model_1.csv");
        double[] m2 = getBinaryMetrics("model_2.csv");
        double[] m3 = getBinaryMetrics("model_3.csv");

        // BCE: smaller is better
        String bestBCE = "model_1.csv";
        double bestBCEVal = m1[0];
        if (m2[0] < bestBCEVal) { bestBCEVal = m2[0]; bestBCE = "model_2.csv"; }
        if (m3[0] < bestBCEVal) { bestBCEVal = m3[0]; bestBCE = "model_3.csv"; }

        // Accuracy, Precision, Recall, F1, AUC: larger is better
        String bestAcc = "model_1.csv"; double bestAccVal = m1[1];
        String bestPrec = "model_1.csv"; double bestPrecVal = m1[2];
        String bestRec = "model_1.csv"; double bestRecVal = m1[3];
        String bestF1 = "model_1.csv"; double bestF1Val = m1[4];
        String bestAUC = "model_1.csv"; double bestAUCVal = m1[5];

        if (m2[1] > bestAccVal) { bestAccVal = m2[1]; bestAcc = "model_2.csv"; }
        if (m3[1] > bestAccVal) { bestAccVal = m3[1]; bestAcc = "model_3.csv"; }

        if (m2[2] > bestPrecVal) { bestPrecVal = m2[2]; bestPrec = "model_2.csv"; }
        if (m3[2] > bestPrecVal) { bestPrecVal = m3[2]; bestPrec = "model_3.csv"; }

        if (m2[3] > bestRecVal) { bestRecVal = m2[3]; bestRec = "model_2.csv"; }
        if (m3[3] > bestRecVal) { bestRecVal = m3[3]; bestRec = "model_3.csv"; }

        if (m2[4] > bestF1Val) { bestF1Val = m2[4]; bestF1 = "model_2.csv"; }
        if (m3[4] > bestF1Val) { bestF1Val = m3[4]; bestF1 = "model_3.csv"; }

        if (m2[5] > bestAUCVal) { bestAUCVal = m2[5]; bestAUC = "model_2.csv"; }
        if (m3[5] > bestAUCVal) { bestAUCVal = m3[5]; bestAUC = "model_3.csv"; }

        System.out.println("According to BCE, The best model is " + bestBCE);
        System.out.println("According to Accuracy, The best model is " + bestAcc);
        System.out.println("According to Precision, The best model is " + bestPrec);
        System.out.println("According to Recall, The best model is " + bestRec);
        System.out.println("According to F1 score, The best model is " + bestF1);
        System.out.println("According to AUC ROC, The best model is " + bestAUC);
    }

    /** Helper returns [BCE, Accuracy, Precision, Recall, F1, AUC] for a file. */
    private static double[] getBinaryMetrics(String fileName) {
        double bceSum = 0.0;
        int tp = 0, fp = 0, tn = 0, fn = 0;
        int n = 0;
        double[] probs = new double[0];
        int[] trues = new int[0];   // FIXED: was double[]

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (n == 0) {
                    try { Integer.parseInt(line[0]); } catch (NumberFormatException e) { continue; }
                }
                if (line.length < 2) continue;
                int y = Integer.parseInt(line[0]);
                double p = Double.parseDouble(line[1]);
                if (p < EPS) p = EPS;
                if (p > 1.0 - EPS) p = 1.0 - EPS;

                bceSum += (y == 1) ? Math.log(p) : Math.log(1.0 - p);
                int pred = (p >= 0.5) ? 1 : 0;
                if (y == 1 && pred == 1) tp++;
                else if (y == 0 && pred == 1) fp++;
                else if (y == 0 && pred == 0) tn++;
                else if (y == 1 && pred == 0) fn++;

                probs = java.util.Arrays.copyOf(probs, probs.length + 1);
                trues = java.util.Arrays.copyOf(trues, trues.length + 1);
                probs[n] = p;
                trues[n] = y;
                n++;
            }
        } catch (IOException e) {
            return new double[]{0,0,0,0,0,0};
        }
        if (n == 0) return new double[]{0,0,0,0,0,0};

        double bce = -bceSum / n;
        double accuracy = (tp + tn) / (double)(tp + tn + fp + fn);
        double precision = (tp + fp) > 0 ? tp / (double)(tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? tp / (double)(tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? 2.0 * precision * recall / (precision + recall) : 0.0;

        // ROC/AUC
        int nPos = 0, nNeg = 0;
        for (int label : trues) {
            if (label == 1) nPos++; else nNeg++;
        }
        double[] tpr = new double[101];
        double[] fpr = new double[101];
        for (int i = 0; i <= 100; i++) {
            double th = i / 100.0;
            int tpTh = 0, fpTh = 0;
            for (int k = 0; k < n; k++) {
                if (probs[k] >= th) {
                    if (trues[k] == 1) tpTh++; else fpTh++;
                }
            }
            tpr[i] = nPos > 0 ? tpTh / (double)nPos : 0.0;
            fpr[i] = nNeg > 0 ? fpTh / (double)nNeg : 0.0;
        }
        double auc = 0.0;
        for (int i = 1; i <= 100; i++) {
            auc += (tpr[i-1] + tpr[i]) * Math.abs(fpr[i-1] - fpr[i]) / 2.0;
        }
        return new double[]{bce, accuracy, precision, recall, f1, auc};
    }
}
