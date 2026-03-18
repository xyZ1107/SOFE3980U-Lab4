package com.ontariotechu.sofe3980U;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;

public class App {
    private static final double EPS = 1e-15;
    private static final int NUM_CLASSES = 5; // classes 1..5

    /** Evaluates a multiclass CSV: first column true class (1‑5),
     *  next NUM_CLASSES columns are predicted probabilities for each class. */
    private static void evaluateMulticlassModel(String fileName) {
        double ceSum = 0.0;
        int[][] cm = new int[NUM_CLASSES][NUM_CLASSES]; // [pred][true]
        int count = 0;

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // Skip header if first column is not an integer
                if (count == 0) {
                    try { Integer.parseInt(line[0]); } catch (NumberFormatException e) { continue; }
                }
                if (line.length < NUM_CLASSES + 1) continue;

                int trueCls = Integer.parseInt(line[0]); // 1..5
                if (trueCls < 1 || trueCls > NUM_CLASSES) continue;

                double[] probs = new double[NUM_CLASSES];
                for (int j = 0; j < NUM_CLASSES; j++) {
                    probs[j] = Double.parseDouble(line[j + 1]);
                    if (probs[j] < EPS) probs[j] = EPS;
                    if (probs[j] > 1.0 - EPS) probs[j] = 1.0 - EPS;
                }

                // Cross‑entropy contribution
                double pTrue = probs[trueCls - 1];
                ceSum += Math.log(pTrue);

                // Predicted class = argmax
                int predCls = 1;
                double maxP = probs[0];
                for (int j = 1; j < NUM_CLASSES; j++) {
                    if (probs[j] > maxP) {
                        maxP = probs[j];
                        predCls = j + 1;
                    }
                }
                cm[predCls - 1][trueCls - 1]++; // note: [pred][true]

                count++;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        if (count == 0) {
            System.err.println("No data rows found in " + fileName);
            return;
        }

        double ce = -ceSum / count;

        System.out.println("CE =" + String.format("%.7f", ce));
        System.out.println("Confusion matrix");
        // Header
        System.out.print("                ");
        for (int c = 1; c <= NUM_CLASSES; c++) {
            System.out.printf("y=%-3d    ", c);
        }
        System.out.println();
        // Rows
        for (int r = 0; r < NUM_CLASSES; r++) {
            System.out.printf("        y^=%-2d   ", r + 1);
            for (int c = 0; c < NUM_CLASSES; c++) {
                System.out.printf("%-6d    ", cm[r][c]);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        evaluateMulticlassModel("model.csv");
    }
}
