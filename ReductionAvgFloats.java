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

public class ReductionAvgFloats {

    public static void reductionAvgFloats(float[] input, @Reduce float[] result) {
        result[0] = 0.0f;
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] += input[i];
        }
    }

    public void run(int size) {
        float[] input = new float[size];
        float[] result = new float[1];

        Random r = new Random();
        IntStream.range(0, size).parallel().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", ReductionAvgFloats::reductionAvgFloats, input, result)
            .streamOut(result);
        //@formatter:on

        ArrayList<Long> timers = new ArrayList<>();
        for (int i = 0; i < 50; i++) {

            long start = System.nanoTime();
            //task.execute();
            reductionAvgFloats(input, result);
            result[0] /= (float) input.length;
            long end = System.nanoTime();

            timers.add((end - start));
            System.out.println("Result: "+result[0]);
        }

        Collections.sort(timers);
        System.out.println("Median TotalTime: " + timers.get(timers.size()/2));
    }

    public static void main(String[] args) {
        int inputSize = 8192;
        if (args.length > 0) {
            inputSize = Integer.parseInt(args[0]);
        }
        System.out.println("Size = " + inputSize);
        new ReductionAvgFloats().run(inputSize);
    }
}
