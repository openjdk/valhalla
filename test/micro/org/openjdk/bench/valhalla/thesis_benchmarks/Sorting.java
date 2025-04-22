package org.openjdk.bench.valhalla.thesis_benchmarks;

import java.util.Random;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.bench.valhalla.types.Q32int;


import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class Sorting {


    static final int N = 1_000;
    static Q32int[] array = (Q32int[]) ValueClass.newNullRestrictedArray(Q32int.class, N);

    public static void initialize() {
        // initialize randomly
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = new Q32int(rand.nextInt());
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static int bubblesort(Q32int[] array) {
        Q32int temp;
//        for (int i = 1; i < array.length; i++) {
//            for (int j = 0; j < array.length - i; j++) {
        return array[1].intValue();
//                if (array[j].intValue() > array[j + 1].intValue()) {
//                    temp = array[j];
//                    array[j] = array[j + 1];
//                    array[j + 1] = temp;
//                }
//            }
//        }
        //return array;
    }

    public static Q32int[] fill() {
        int N = 1_000_000;
        Q32int[] array = (Q32int[]) ValueClass.newNullRestrictedArray(Q32int.class, N);
        // initialize randomly
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            array[i] = new Q32int(rand.nextInt());
        }
        return array;
    }

    public static Q32int getMax() {
        int N = 1_000_000;
        Q32int max = new Q32int(Integer.MIN_VALUE);
        Q32int[] array = (Q32int[]) ValueClass.newNullRestrictedArray(Q32int.class, N);
        // initialize randomly
        for (int i = 0; i < array.length; i++) {
            if (max.intValue() < array[i].intValue()) {
                max = array[i];
            }
        }
        return max;
    }

    public static Q32int[] selectionSort(Q32int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[j].intValue() < array[minIndex].intValue()) {
                    minIndex = j;
                }
            }
            Q32int temp = array[i];
            array[i] = array[minIndex];
            array[minIndex] = temp;
        }
        return array;
    }

    public static Q32int[] insertionSort(Q32int[] array) {
        for (int i = 1; i < array.length; i++) {
            Q32int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j].intValue() > key.intValue()) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
        return array;
    }

//    @Benchmark
//    @OperationsPerInvocation(1_000_000)
//    public void benchmark_max() {
//        getMax();
//        //initialize();
//        //bubblesort(array);
//        //initialize();
//        //selectionSort(array);
//        //initialize();
//        //insertionSort(array);
//    }
//
//    @Benchmark
//    @OperationsPerInvocation(1_000_000)
//    public void benchmark_fill() {
//        fill();
//        //initialize();
//        //bubblesort(array);
//        //initialize();
//        //selectionSort(array);
//        //initialize();
//        //insertionSort(array);
//    }

    @Benchmark
    @OperationsPerInvocation(1_000)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void benchmark_sorting() {
        //fill();
        //initialize();
        bubblesort(array);
        //initialize();
        //selectionSort(array);
        //initialize();
        //insertionSort(array);
    }
}