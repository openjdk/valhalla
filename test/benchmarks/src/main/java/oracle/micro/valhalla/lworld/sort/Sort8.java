package oracle.micro.valhalla.lworld.sort;

import oracle.micro.valhalla.SortBase;
import oracle.micro.valhalla.lworld.types.Value8;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.ThreadLocalRandom;

public class Sort8 extends SortBase {

    public Value8[] makeRandomArray() {
        Value8[] arr = new Value8[size];
        for (int i = 0; i < size; i++) {
            arr[i] = Value8.of(
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt(),
                    ThreadLocalRandom.current().nextInt());
        }
        return arr;
    }

    @Benchmark
    public Value8[] quicksort() {
        Value8[] toSort = makeRandomArray();
        sort1(toSort, 0, toSort.length);
        return toSort;
    }

    @Benchmark
    public Value8[] mergeSort() {
        Value8[] toSort = makeRandomArray();
        Value8[] aux = new Value8[toSort.length];
        System.arraycopy(toSort, 0, aux, 0 , toSort.length);
        mergeSort(aux, toSort, 0, toSort.length, 0);
        return toSort;
    }


    private static int compare(Value8 a, Value8 b) {
        int c0 = Integer.compare(a.f0, b.f0);
        if (c0 != 0) {
            return c0;
        } else {
            int c1 = Integer.compare(a.f1, b.f1);
            if (c1 != 0) {
                return c1;
            } else {
                int c2 = Integer.compare(a.f2, b.f2);
                if (c2 != 0) {
                    return c2;
                } else {
                    int c3 = Integer.compare(a.f3, b.f3);
                    if (c3 != 0) {
                        return c3;
                    } else {
                        int c4 = Integer.compare(a.f4, b.f4);
                        if (c4 != 0) {
                            return c4;
                        } else {
                            int c5 = Integer.compare(a.f5, b.f5);
                            if (c5 != 0) {
                                return c5;
                            } else {
                                int c6 = Integer.compare(a.f6, b.f6);
                                if (c6 != 0) {
                                    return c6;
                                } else {
                                    return Integer.compare(a.f7, b.f7);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //##########3 dual pivot quick sort##############################

    private static void sort1(Value8 x[], int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && compare(x[j - 1], x[j]) > 0; j--)
                    swap(x, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        Value8 v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && compare(x[b], v) <= 0) {
                if (compare(x[b], v) == 0)
                    swap(x, a++, b);
                b++;
            }
            while (c >= b && compare(x[c], v) >= 0) {
                if (compare(x[c], v) == 0)
                    swap(x, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, off, s);
        if ((s = d - c) > 1)
            sort1(x, n - s, s);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(Value8 x[], int a, int b) {
        Value8 t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(Value8 x[], int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++)
            swap(x, a, b);
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(Value8 x[], int a, int b, int c) {
        return (compare(x[a], x[b]) < 0 ?
                (compare(x[b], x[c]) < 0 ? b : compare(x[a], x[c]) < 0 ? c : a) :
                (compare(x[b], x[c]) > 0 ? b : compare(x[a], x[c]) > 0 ? c : a));
    }


    //########## merge sort##############################


    private static void mergeSort(Value8[] src,
                                  Value8[] dest,
                                  int low, int high, int off) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < 7) {
            for (int i = low; i < high; i++)
                for (int j = i; j > low && compare(dest[j - 1], dest[j]) > 0; j--)
                    swap(dest, j, j - 1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow = low;
        int destHigh = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }
}
