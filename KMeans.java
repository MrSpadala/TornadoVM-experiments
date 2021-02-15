import java.util.Random;
import java.util.Arrays;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.annotations.Reduce;

public class KMeans {

    public static final int WARMING_UP_ITERATIONS = 1;

    public static void mmul_mm(final float[] A, final float[] B, final float[] C, final int size) {
        for (@Parallel int i = 0; i < size; i++) {
            for (@Parallel int j = 0; j < size; j++) {
                float sum = 0.0f;
                for (int k = 0; k < size; k++) {
                    sum += A[(i * size) + k] * B[(k * size) + j];
                }
                C[(i * size) + j] = sum;
            }
        }
    }

    public static void mmul_mmt(final float[] A, final float[] B, final float[] C,
                final int A_rows, final int A_cols, final int B_rows) {
        final int B_cols = A_cols;
        for (@Parallel int i = 0; i < A_rows; i++) {
            for (@Parallel int j = 0; j < B_rows; j++) {
                float sum = 0.0f;
                for (int k = 0; k < A_cols; k++) {
                    sum += A[(i * A_cols) + k] * B[(j * B_cols) + k];
                }
                C[(i * A_cols) + j] = sum;
            }
        }
    }

    public static void mmul_mtm(final float[] A, final float[] B, final float[] C,
                final int A_rows, final int A_cols, final int B_cols) {
        final int B_rows = A_rows;
        for (@Parallel int i = 0; i < A_cols; i++) {
            for (@Parallel int j = 0; j < B_cols; j++) {
                float sum = 0.0f;
                for (int k = 0; k < A_rows; k++) {
                    sum += A[(k * A_cols) + i] * B[(k * B_cols) + j];
                }
                C[(i * A_cols) + j] = sum;
            }
        }
    }

    public static void kmeans_calc_dists(final float[] X, final float[] C, final int[] S, final float[] S_dists,
                final int n, final int k, final int d) {
        for (@Parallel int i = 0; i < n; i++) {
            S_dists[i] = Float.MAX_VALUE;
            for (int j = 0; j < k; j++) {
                float dist = 0.0f;
                float tmp;
                for (int z = 0; z < d; z++) {
                    tmp = X[i*d + z] - C[j*d + z];
                    dist += tmp * tmp;
                }

                if (dist < S_dists[i]) {
                    //System.out.println("assigning to point "+i+" cluster "+j+" with dist "+dist);
                    S_dists[i] = dist;
                    S[i] = j;
                }
            }
        }
    }

    public static void kmeans_assign_centers(final float[] X, float[] C, final int[] S, long[] C_num,
                final int n, final int k, final int d) {
        // Zeros center
        for (@Parallel int i = 0; i < k*d; i++) {
            C[i] = 0.0f;
        }
        for (@Parallel int i = 0; i < k; i++) {
            C_num[i] = 0;
        }

        // Calculate sum of points
        for (int i = 0; i < n; i++) {
            int center = S[i];
            C_num[center] += 1;
            for (int j = 0; j < d; j++) {
                C[center*d + j] += X[i*d + j];
            }
        }

        // Average
        for (@Parallel int i = 0; i < k; i++) {
            double num = C_num[i];
            for (int j = 0; j < d; j++) {
                C[i*d + j] /= num;
            }
        }
    }

    public static double kmeans_debug_tot_dists(final float[] X, final float[] C, final int[] S,
                final int n, final int k, final int d) {
        double tot = 0.0f;
        for (int i = 0; i < n; i++) {
            int center = S[i];
            for (int z = 0; z < d; z++) {
                tot += (X[i*d + z] - C[center*d + z]) * (X[i*d + z] - C[center*d + z]);
            }
        }
        return tot;
    }

    public static void kmeans_debug_print_centers(final float[] C, final int k, final int d) {
        for (int i = 0; i < k; i++) {
            System.out.print("center "+i+": [");
            for (int z = 0; z < d; z++) {
                System.out.print(C[i*d+z]+", ");
            }
            System.out.println("]");
        }
    }

    public static void main(String[] args) {

        /*int size = 512;
        if (args.length >= 1) {
            try {
                size = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                size = 512;
            }
        }

        System.out.println("Computing MxM of " + size + "x" + size);*/
        System.out.println("Starting");

        final int n = 1700000;
        final int d = 100;
        final int k = 9;
        assert(k < n);

        float[] X = new float[n * d];
        float[] C = new float[k * d];
        int[] S = new int[n];
        float[] S_dists = new float[n];  //aux, distance from the i-th point to its cluster center
        long[] C_num = new long[k];  //aux, how many points belong to each center

        // Initialize matrices
        Random r = new Random();
        IntStream.range(0, n * d).parallel().forEach(idx -> {
            X[idx] = r.nextFloat();
        });
        IntStream.range(0, n).parallel().forEach(idx -> {
            S[idx] = 0;
            S_dists[idx] = Float.MAX_VALUE;
        });
        IntStream.range(0, k).parallel().forEach(idx -> {
            C_num[idx] = 0;
            //int center = r.nextInt(n+1);
            for (int j = 0; j < d; j++) {
                //C[idx*d + j] = X[center*d + j];
                C[idx*d + j] = r.nextFloat();
            }
        });

        //@formatter:off
        TaskSchedule t = new TaskSchedule("s0")
                .streamIn(C)
                .task("t0", KMeans::kmeans_calc_dists, X, C, S, S_dists, n, k, d)
                .streamOut(S);
        //@formatter:on

        // 1. Warm up Tornado
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            t.execute();
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
        }

        double kmeans_dist;

        // 2. Run parallel on the GPU with Tornado
        long start = System.currentTimeMillis();
        t.execute();
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        t.execute();
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        t.execute();
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        long end = System.currentTimeMillis();
        System.out.println("parallel done");
        //System.out.println(Arrays.toString(S));

        // Run sequential
        // 1. Warm up sequential
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            kmeans_calc_dists(X, C, S, S_dists, n, k, d);
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
        }

        // 2. Run the sequential code
        long startSequential = System.currentTimeMillis();
        
//        kmeans_debug_print_centers(C, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);

        kmeans_calc_dists(X, C, S, S_dists, n, k, d);
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        //kmeans_debug_print_centers(C, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        //System.out.println(Arrays.toString(S));
        //System.out.println(Arrays.toString(S_dists));
        
        kmeans_calc_dists(X, C, S, S_dists, n, k, d);
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        //kmeans_debug_print_centers(C, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        //System.out.println(Arrays.toString(S));
        //System.out.println(Arrays.toString(S_dists));
        
        kmeans_calc_dists(X, C, S, S_dists, n, k, d);
        kmeans_assign_centers(X, C, S, C_num, n, k, d);
        //kmeans_debug_print_centers(C, k, d);
        kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
        System.out.println("kmeans cost: "+kmeans_dist);
        //System.out.println(Arrays.toString(S));
        //System.out.println(Arrays.toString(S_dists));
        long endSequential = System.currentTimeMillis();
        System.out.println("sequential done");

        // Compute Gigaflops and performance
        /*long msecGPUElapsedTime = (end - start);
        long msecCPUElaptedTime = (endSequential - startSequential);
        double flops = 2 * ((long)n)*((long)k)*((long)d);
        double gpuGigaFlops = (1.0E-9 * flops) / (1.0E-9 + msecGPUElapsedTime / 1000.0f);
        double cpuGigaFlops = (1.0E-9 * flops) / (1.0E-9 + msecCPUElaptedTime / 1000.0f);

        String formatGPUFGlops = String.format("%.2f", gpuGigaFlops);
        String formatCPUFGlops = String.format("%.2f", cpuGigaFlops);

        System.out.println("\tCPU Execution: " + formatCPUFGlops + " GFlops, Total time = " + (endSequential - startSequential) + " ms");
        System.out.println("\tGPU Execution: " + formatGPUFGlops + " GFlops, Total Time = " + (end - start) + " ms");
        System.out.println("\tSpeedup: " + ((endSequential - startSequential) / (end - start)) + "x");*/
    
        System.out.println("\tTotal time = " + (endSequential - startSequential) + " ms");
        System.out.println("\tTotal Time = " + (end - start) + " ms");
        System.out.println("\tSpeedup: " + ((endSequential - startSequential) / (end - start)) + "x");

    }

}
