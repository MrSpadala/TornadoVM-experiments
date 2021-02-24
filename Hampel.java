import java.util.Random;
import java.util.Arrays;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;

public class Hampel {

    public static final int WARMING_UP_ITERATIONS = 3;

    public static void matrixMultiplication(final float[] inputValues,
                                            final float[] outputMin,
                                            final float[] outputMax,
                                            final float[] outputAvg,
                                            final float[] outputOutliers,
                                            final int size,
                                            final int window_size) {

        final int n_sigmas = 2;
        final float k = 1.4826f; // scale factor for Gaussian distribution

        for (@Parallel int i = 0; i < size - window_size; i++) {
            final float[] window = new float[window_size-1];

            // first pass: find average
            float sum = 0;
            for (int j = i + 1; j < i + window_size; j++) {
                sum += inputValues[j];
            }

            float x0 = sum / window_size;

            // second pass: compute diff from average
            for (int j = i + 1; j < i + window_size; j++) {
                //inputValues[j] = Math.abs(inputValues[j] - x0);
                window[j-i-1] = Math.abs(inputValues[j] - x0);
            }

            // third pass: re-find average
            sum = 0;
            for (int j = i + 1; j < i + window_size; j++) {
                //sum += inputValues[j];
                sum += window[j-i-1];
            }

            float x1 = sum / window_size;
            float S0 = k * x1;

            // final pass: identify outliers + compute min, max, average
            float minValue = Float.MAX_VALUE;
            float maxValue = Float.MIN_VALUE;
            float sumValue = 0;
            for (int j = i + 1; j < i + window_size; j++) {
                if (Math.abs(window[j-i-1] - x0) > n_sigmas * S0) {
                    //outputOutliers[j] = 1;
                    outputOutliers[i] = 1;
                } else {
                    //outputOutliers[j] = 0;
                    outputOutliers[i] = 0;

                    // check min
                    if (inputValues[j] < minValue) {
                        minValue = inputValues[j];
                    }

                    // check max
                    if (inputValues[j] > maxValue) {
                        maxValue = inputValues[j];
                    }

                    // compute average
                    sumValue += inputValues[j];
                }
            }

            outputMin[i] = minValue;
            outputMax[i] = maxValue;
            outputAvg[i] = sumValue / window_size;
        }
    }


    public static void printOutliers(final float[] outliers, final int size) {
        for (int i = 0; i < size; i++) {
            if (outliers[i] == 1) {
                System.out.print(i+", ");
            }
        }
        System.out.println("");
    }

    public static void main(String[] args) {

        int size = 8192;
        if (args.length >= 1) {
            try {
                size = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                size = -1;
            }
        }

        int window_size = 60;
        if (args.length >= 2) {
            try {
                window_size = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                window_size = 60;
            }
        }

        System.out.println("Computing outliers, min, max, average for sensor stream of " + size + " elements with window size " + window_size);

        float[] rawValues = new float[size];
        float[] windowAverage = new float[size];
        float[] windowMin = new float[size];
        float[] windowMax = new float[size];
        float[] outliers = new float[size];


        Random r = new Random();
        IntStream.range(0, size).parallel().forEach(idx -> {
            rawValues[idx] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule t = new TaskSchedule("s0")
                .task("t0", Hampel::matrixMultiplication, rawValues, windowMin, windowMax, windowAverage, outliers, size, window_size)
                .streamOut(outliers);
        //@formatter:on

        // 1. Warm up Tornado
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            t.execute();
        }

        // 2. Run parallel on the GPU with Tornado
        long start = System.nanoTime();
        t.execute();
        long end = System.nanoTime();
        System.out.println("Tornado execution outliers indices");
        printOutliers(outliers, size);

        // Run sequential
        // 1. Warm up sequential
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            matrixMultiplication(rawValues, windowMin, windowMax, windowAverage, outliers, size, window_size);
        }

        // 2. Run the sequential code
        long startSequential = System.nanoTime();
        matrixMultiplication(rawValues, windowMin, windowMax, windowAverage, outliers, size, window_size);
        long endSequential = System.nanoTime();
        System.out.println("Sequential execution outliers indices");
        printOutliers(outliers, size);

        // Compute Gigaflops and performance
        long msecGPUElapsedTime = (end - start);
        long msecCPUElaptedTime = (endSequential - startSequential);
        double flops = 2 * Math.pow(size, 3);
        double gpuGigaFlops = (1.0E-9 * flops) / (msecGPUElapsedTime / 1000.0f);
        double cpuGigaFlops = (1.0E-9 * flops) / (msecCPUElaptedTime / 1000.0f);

        String formatGPUFGlops = String.format("%.2f", gpuGigaFlops);
        String formatCPUFGlops = String.format("%.2f", cpuGigaFlops);

        System.out.println("\tCPU Execution: " + formatCPUFGlops + " GFlops, Total time = " + (endSequential - startSequential) + " ns");
        System.out.println("\tGPU Execution: " + formatGPUFGlops + " GFlops, Total Time = " + (end - start) + " ns");
        System.out.println("\tSpeedup: " + ((endSequential - startSequential) / (end - start)) + "x");
    }

}
