/*
 * Copyright (c) 2013-2020, APT Group, Department of Computer Science,
 * The University of Manchester.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.annotations.Reduce;;

public class Reduction2D {

    public static final int WARMING_UP_ITERATIONS = 10;

    public static void reductionAddFloats(float[] input, @Reduce float[] result1, @Reduce float[] result2, float neutral) {
        result1[0] = neutral;
        result2[0] = neutral;
        //float[] curr;
        //for (@Parallel int k = 0; k < 2; k++) {
        //    curr = k==0 ? result1 : result2;
            for (@Parallel int i = 0; i < input.length; i++) {
                result1[0] += input[i];
            }
            /*for (@Parallel int i = 0; i < input.length; i++) {
                result2[0] += input[i];
            }*/
        //}
    }

    public void run(int size) {
        float[] input = new float[size];
        float[] result1 = new float[1];
        float[] result2 = new float[1];

        Random r = new Random();
        IntStream.range(0, size).sequential().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", Reduction2D::reductionAddFloats, input, result1, result2, 0.0f)
            .streamOut(result1, result2);
        //@formatter:on

        for (int i = 0; i < WARMING_UP_ITERATIONS; i++) {
            task.execute();
            reductionAddFloats(input, result1, result2, 0.0f);
        }

        ArrayList<Long> timers = new ArrayList<>();
        ArrayList<Long> timers_seq = new ArrayList<>();
        for (int i = 0; i < 50; i++) {

            long start_seq = System.nanoTime();
            reductionAddFloats(input, result1, result2, 0.0f);
            long end_seq = System.nanoTime();
            System.out.println("Result sequential: "+result1[0]+" "+result2[0]);
            timers_seq.add((end_seq - start_seq));

            long start = System.nanoTime();
            task.execute();
            long end = System.nanoTime();
            System.out.println("Result tornado: "+result1[0]+" "+result2[0]);
            timers.add((end - start));
        }

        Collections.sort(timers);
        Collections.sort(timers_seq);
        System.out.println("Median TotalTime Parallel: " + timers.get(timers.size()/2));
        System.out.println("Median TotalTime Sequential: " + timers_seq.get(timers_seq.size()/2));
    }

    public static void main(String[] args) {
        int inputSize = 10000;
        if (args.length > 0) {
            inputSize = Integer.parseInt(args[0]);
        }
        System.out.println("Size = " + inputSize);
        new Reduction2D().run(inputSize);
    }
}
