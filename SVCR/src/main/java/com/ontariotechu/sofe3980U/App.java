package com.ontariotechu.sofe3980U;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;

public class App {
    private static final double EPSILON = 1e-8;

    /** Reads a CSV with two columns: true, predicted and prints MSE, MAE, MARE. */
    private static void evaluateRegressionModel(String fileName) {
        double sumSE = 0.0, sumAE = 0.0, sumARE = 0.0;
        int count = 0;

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // Skip header if it is not numeric
                if (count == 0) {
                    try {
                        Double.parseDouble(line[0]);
                    } catch (NumberFormatException e) {
                        continue; // assume header
                    }
                }
                if (line.length < 2) continue;

                double yTrue = Double.parseDouble(line[0]);
                double yPred = Double.parseDouble(line[1]);

                double error = yTrue - yPred;
                sumSE += error * error;
                sumAE += Math.abs(error);
                sumARE += Math.abs(error) / (Math.abs(yTrue) + EPSILON);
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

        double mse = sumSE / count;
        double mae = sumAE / count;
        double mare = sumARE / count;

        System.out.println("for " + fileName);
        System.out.printf("        MSE =%.5f%n", mse);
        System.out.printf("        MAE =%.5f%n", mae);
        System.out.printf("        MARE =%.5f%n", mare);
    }

    public static void main(String[] args) {
        // Evaluate the three models
        evaluateRegressionModel("model_1.csv");
        evaluateRegressionModel("model_2.csv");
        evaluateRegressionModel("model_3.csv");

        // Determine best model per metric (reuse the same logic or store results)
        // For simplicity we recompute or we could store during the first pass.
        // Here we reuse a helper that returns the metrics.
        double[] m1 = getRegressionMetrics("model_1.csv");
        double[] m2 = getRegressionMetrics("model_2.csv");
        double[] m3 = getRegressionMetrics("model_3.csv");

        String bestMSE = "model_1.csv";
        String bestMAE = "model_1.csv";
        String bestMARE = "model_1.csv";
        double minMSE = m1[0], minMAE = m1[1], minMARE = m1[2];

        if (m2[0] < minMSE) { minMSE = m2[0]; bestMSE = "model_2.csv"; }
        if (m3[0] < minMSE) { minMSE = m3[0]; bestMSE = "model_3.csv"; }

        if (m2[1] < minMAE) { minMAE = m2[1]; bestMAE = "model_2.csv"; }
        if (m3[1] < minMAE) { minMAE = m3[1]; bestMAE = "model_3.csv"; }

        if (m2[2] < minMARE) { minMARE = m2[2]; bestMARE = "model_2.csv"; }
        if (m3[2] < minMARE) { minMARE = m3[2]; bestMARE = "model_3.csv"; }

        System.out.println("According to MSE, The best model is " + bestMSE);
        System.out.println("According to MAE, The best model is " + bestMAE);
        System.out.println("According to MARE, The best model is " + bestMARE);
    }

    /** Helper that returns [MSE, MAE, MARE] for a given file. */
    private static double[] getRegressionMetrics(String fileName) {
        double sumSE = 0.0, sumAE = 0.0, sumARE = 0.0;
        int count = 0;
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (count == 0) {
                    try { Double.parseDouble(line[0]); } catch (NumberFormatException e) { continue; }
                }
                if (line.length < 2) continue;
                double yTrue = Double.parseDouble(line[0]);
                double yPred = Double.parseDouble(line[1]);
                double err = yTrue - yPred;
                sumSE += err * err;
                sumAE += Math.abs(err);
                sumARE += Math.abs(err) / (Math.abs(yTrue) + EPSILON);
                count++;
            }
        } catch (IOException e) {
            return new double[]{0,0,0};
        }
        if (count == 0) return new double[]{0,0,0};
        return new double[]{sumSE/count, sumAE/count, sumARE/count};
    }
}
