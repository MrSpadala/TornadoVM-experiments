import java.util.Random;
import java.util.Arrays;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.annotations.Reduce;

public class KMeans {

    public static final int WARMING_UP_ITERATIONS = 1;

    public static void kmeans_calc_dists(final float[] X, final float[] C, final int[] S, final float[] S_dists,
                final int n, final int k, final int d) {
        for (@Parallel int i = 0; i < n; i++) {
            S_dists[i] = Float.MAX_VALUE;
            float[] dists = new float[k];
            for (int j = 0; j < k; j++) {
                float dist = 0.0f;
                float tmp;
                for (int z = 0; z < d; z++) {
                    tmp = X[i*d + z] - C[j*d + z];
                    dist += tmp * tmp;
                }
                dists[j] = dist;
            }

            float S_min_dist = dists[0];
            float prev_dist = S_min_dist;
            S[i] = 0;
            for (int j = 1; j < k; j++) {
                S_min_dist = Math.min(S_min_dist, dists[j]);
                S[i] = (S_min_dist == prev_dist) ? S[i] : j;
                prev_dist = S_min_dist;
            }
        }
    }

    public static void zero_C_C_num(final float[] C, final long[] C_num, final int k, final int d) {
        // Zeros center
        for (@Parallel int i = 0; i < k*d; i++) {
            C[i] = 0.0f;
        }
        for (@Parallel int i = 0; i < k; i++) {
            C_num[i] = 0;
        }
    }

    public static void kmeans_assign_centers(final float[] X, float[] C, final int[] S, long[] C_num,
                final int n, final int k, final int d) {
        // Calculate sum of points
        for (int i = 0; i < n; i++) {
            int center = S[i];
            C_num[center] += 1;
            for (int j = 0; j < d; j++) {
                C[center*d + j] += X[i*d + j];
            }
        }
    }

    public static void kmeans_average_centers(final float[] X, float[] C, final int[] S, long[] C_num,
                final int n, final int k, final int d) {
        // Average
        for (@Parallel int i = 0; i < k; i++) {
            double num = C_num[i];
            for (@Parallel int j = 0; j < d; j++) {
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
        double kmeans_dist;
        long start, stop;
        long tot_time_tornado = 0;
        long tot_time_seq = 0;

        System.out.println("Starting");

        final int n = 1700000;
        final int d = 100;
        final int k = 9;
        final int n_iters = 10;  //number of kmeans iterations
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
            for (int j = 0; j < d; j++) {
                C[idx*d + j] = r.nextFloat();
            }
        });

        TaskSchedule t = new TaskSchedule("s0")
                .streamIn(C)
                .task("t0", KMeans::kmeans_calc_dists, X, C, S, S_dists, n, k, d)
                .streamOut(S)
                .task("t1", KMeans::zero_C_C_num, C, C_num, k, d)
                .streamOut(C, C_num);

        TaskSchedule t1 = new TaskSchedule("s1")
                .streamIn(C, C_num)
                .task("t0", KMeans::kmeans_average_centers, X, C, S, C_num, n, k, d)
                .streamOut(C);

        // Run Tornado
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            t.execute();
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
            t1.execute();
        }

        System.out.println("TornadoVM");
        for (int i = 0; i < n_iters; i++) {
            start = System.currentTimeMillis();
            t.execute();
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
            t1.execute();
            stop = System.currentTimeMillis();
            tot_time_tornado += stop-start;
            
            kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
            System.out.println("iter "+i+", kmeans cost: "+kmeans_dist);
        }

        // Run sequential
        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            kmeans_calc_dists(X, C, S, S_dists, n, k, d);
            zero_C_C_num(C, C_num, k, d);
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
            kmeans_average_centers(X, C, S, C_num, n, k, d);
        }

        System.out.println("Sequential");
        for (int i = 0; i < n_iters; i++) {
            start = System.currentTimeMillis();
            kmeans_calc_dists(X, C, S, S_dists, n, k, d);
            zero_C_C_num(C, C_num, k, d);
            kmeans_assign_centers(X, C, S, C_num, n, k, d);
            kmeans_average_centers(X, C, S, C_num, n, k, d);
            stop = System.currentTimeMillis();
            tot_time_seq += stop-start;
            
            kmeans_dist = kmeans_debug_tot_dists(X, C, S, n, k, d);
            System.out.println("iter "+i+", kmeans cost: "+kmeans_dist);
        }

        System.out.println("\tTornadoVM total time = " + tot_time_tornado + " ms");
        System.out.println("\tTornadoVM avg time per iter = " + (double) tot_time_tornado / n_iters + " ms");
        System.out.println("\tSequential total Time = " + tot_time_seq + " ms");
        System.out.println("\tSequential avg time per iter = " + (double) tot_time_seq / n_iters + " ms");
        System.out.printf("\tSpeedup: %.2fx\n", ((double)tot_time_seq / (double)tot_time_tornado));
    }

}
