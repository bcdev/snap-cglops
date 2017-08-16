/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.snap.cglops.l3;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;


/**
 * Created by marcoz on 18.02.15.
 */
public class OrderOfFloatsTest {

    @Test
    public void testSimple() {
        float[] values = new float[]{
                274.62552f, 291.9534f, 284.96017f,
                280.3997f, 273.1571f, 273.36612f,
                277.20193f, 283.06665f, 292.3408f,
                298.0398f, 297.06342f, 298.30417f
        };

        int[] order1 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] order2 = new int[]{1, 3, 7, 4, 2, 5, 6, 0, 8, 9, 10, 11};

        float sum1 = 0;
        float sumsq1 = 0;
        for (int i = 0; i < values.length; i++) {
            float value = values[order1[i]];
            sum1 = sum1 + value;
            sumsq1 = sumsq1 + (value * value);
        }
        final double mean1 = sum1 / values.length;
        final double sigmaSqr1 = sumsq1 / values.length - mean1 * mean1;
        final double sigma1 = sigmaSqr1 > 0.0 ? Math.sqrt(sigmaSqr1) : 0.0;

        System.out.println("order1 = " + Arrays.toString(order1));
        System.out.println("sum1 = " + sum1);
        System.out.println("sumsq1 = " + sumsq1);
        System.out.println("mean1 = " + (float)mean1);
        System.out.println("sigma1 = " + (float)sigma1);

        float sum2 = 0;
        float sumsq2 = 0;
        for (int i = 0; i < values.length; i++) {
            float value = values[order2[i]];
            sum2 = sum2 + value;
            sumsq2 = sumsq2 + (value * value);
        }
        final double mean2 = sum2 / values.length;
        final double sigmaSqr2 = sumsq2 / values.length - mean2 * mean2;
        final double sigma2 = sigmaSqr2 > 0.0 ? Math.sqrt(sigmaSqr2) : 0.0;

        System.out.println();
        System.out.println("order2 = " + Arrays.toString(order2));
        System.out.println("sum2 = " + sum2);
        System.out.println("sumsq2 = " + sumsq2);
        System.out.println("mean2 = " + (float)mean2);
        System.out.println("sigma2 = " + (float)sigma2);

    }

    @Test
    public void testRalf() {
        float[] values = new float[]{
                274.62552f, 291.9534f, 284.96017f,
                280.3997f, 273.1571f, 273.36612f,
                277.20193f, 283.06665f, 292.3408f,
                298.0398f, 297.06342f, 298.30417f
        };

        int[] order1 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] order2 = new int[]{1, 3, 7, 4, 2, 5, 6, 0, 8, 9, 10, 11};

        float mean1 = 0;
        float m1 = 0;
        for (int i = 0; i < values.length; i++) {
            float value = values[order1[i]];

            float delta = value - mean1;
            mean1 = mean1 + delta / (i+1);
            m1 = m1 + delta * (value - mean1);
        }
        float variance1 = m1 / (values.length);
        System.out.println("order1 = " + Arrays.toString(order1));
        System.out.println("m1 = " + m1);
        System.out.println("variance1 = " + variance1);
        System.out.println("mean1 = " + mean1);
        System.out.println("sigma1 = " + (float)Math.sqrt(variance1));



        float mean2 = 0;
        float m2 = 0;
        for (int i = 0; i < values.length; i++) {
            float value = values[order2[i]];
            float delta = value - mean2;
            mean2 = mean2 + delta / (i + 1);
            m2 = m2 + delta * (value - mean2);
        }
        float variance2 = m2 / (values.length);
        System.out.println();
        System.out.println("order2 = " + Arrays.toString(order2));
        System.out.println("m2 = " + m2);
        System.out.println("variance2 = " + variance2);
        System.out.println("mean2 = " + mean2);
        System.out.println("sigma2 = " + (float)Math.sqrt(variance2));

        assertEquals(mean1, mean2, 1e-5f);
    }


    @Test
    public void testComplex() throws Exception {

        float[] values = new float[]{
                274.62552f, 291.9534f, 284.96017f,
                280.3997f, 273.1571f, 273.36612f,
                277.20193f, 283.06665f, 292.3408f,
                298.0398f, 297.06342f, 298.30417f
        };


        int[] order1 = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] order2 = new int[]{1, 3, 7, 4, 2, 5, 6, 0, 8, 9, 10, 11};

//        float sumRef = 0;
//        for (int i = 0; i < values.length; i++) {
//            sumRef = sumRef + values[i];
//        }
//        System.out.println("sumRef = " + sumRef);

        float sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum = sum + values[order1[i]];
        }
        System.out.println("order1 = " + Arrays.toString(order1));
        System.out.println("sum1 = " + sum);

        sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum = sum + values[order2[i]];
        }
        System.out.println("order2 = " + Arrays.toString(order2));
        System.out.println("sum2 = " + sum);

//        doHeapPermute(values, sumRef);

    }

    private static void sum(float[] values, float sumRef, int[] order) {
        float sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum = sum + values[order[i]];
        }
        if (sum != sumRef) {
            System.out.println("order = " + Arrays.toString(order));
            System.out.println("sum = " + sum);
            System.out.println("sumRef = " + sumRef);
            System.exit(0);
        }
    }

    public static void permute(int[] v, int n, float[] values, float refSum) {
        if (n == 1) {
//            System.out.println(Arrays.toString(v));
            sum(values, refSum, v);
        } else {
            for (int i = 0; i < n; i++) {
                permute(v, n - 1, values, refSum);
                if (n % 2 == 1) {
                    swap(v, 0, n - 1);
                } else {
                    swap(v, i, n - 1);
                }
            }
        }
    }

    private static void swap(int[] v, int i, int j) {
        int t = v[i];
        v[i] = v[j];
        v[j] = t;
    }

    public static void doHeapPermute(float[] values, float refSum) {
        int[] ns = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        permute(ns, ns.length, values, refSum);
    }

}
