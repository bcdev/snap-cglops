/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static com.bc.snap.cglops.l3.AggregatorTestUtils.obsNT;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.vec;
import static java.lang.Float.NaN;
import static org.junit.Assert.assertEquals;

public class AggregatorMajorityClassTest {
    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = AggregatorTestUtils.createCtx();
    }

    @Test
    public void testMetadata() {
        MyVariableContext varCtx = new MyVariableContext("owt");
        Aggregator agg = new AggregatorMajorityClass(varCtx, "owt", new int[]{1, 3});

        assertEquals("MAJORITY_CLASS", agg.getName());

        assertEquals(3, agg.getSpatialFeatureNames().length);
        assertEquals("owt_class_1_counts", agg.getSpatialFeatureNames()[0]);
        assertEquals("owt_class_3_counts", agg.getSpatialFeatureNames()[1]);
        assertEquals("owt_class_other_counts", agg.getSpatialFeatureNames()[2]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("owt_class_1_counts", agg.getTemporalFeatureNames()[0]);
        assertEquals("owt_class_3_counts", agg.getTemporalFeatureNames()[1]);
        assertEquals("owt_class_other_counts", agg.getTemporalFeatureNames()[2]);

        assertEquals(5, agg.getOutputFeatureNames().length);
        assertEquals("owt_class_1_counts", agg.getOutputFeatureNames()[0]);
        assertEquals("owt_class_3_counts", agg.getOutputFeatureNames()[1]);
        assertEquals("owt_sum_all", agg.getOutputFeatureNames()[2]);
        assertEquals("owt_sum_analyzed", agg.getOutputFeatureNames()[3]);
        assertEquals("owt_majority_class", agg.getOutputFeatureNames()[4]);
    }

    @Test
    public void tesAggregator() {
        MyVariableContext varCtx = new MyVariableContext("owt");
        Aggregator agg = new AggregatorMajorityClass(varCtx, "owt", new int[]{1, 3});

        VectorImpl svec = vec(NaN, NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN, NaN, NaN);

        ////////////////////////////////////////////////////////
        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(0.0f, svec.get(1), 0.0f);
        assertEquals(0.0f, svec.get(2), 0.0f);

        agg.aggregateSpatial(ctx, obsNT(1f), svec);
        assertEquals(1f, svec.get(0), 1e-5f);
        assertEquals(0f, svec.get(1), 1e-5f);
        assertEquals(0f, svec.get(2), 1e-5f);

        agg.aggregateSpatial(ctx, obsNT(2f), svec);
        assertEquals(1f, svec.get(0), 1e-5f);
        assertEquals(0f, svec.get(1), 1e-5f);
        assertEquals(1f, svec.get(2), 1e-5f);

        agg.aggregateSpatial(ctx, obsNT(3f), svec);
        assertEquals(1f, svec.get(0), 1e-5f);
        assertEquals(1f, svec.get(1), 1e-5f);
        assertEquals(1f, svec.get(2), 1e-5f);

        agg.completeSpatial(ctx, 3, svec);
        assertEquals(1f, svec.get(0), 1e-5f);
        assertEquals(1f, svec.get(1), 1e-5f);
        assertEquals(1f, svec.get(2), 1e-5f);

        ////////////////////////////////////////////////////////
        agg.initTemporal(ctx, tvec);
        assertEquals(0.0f, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);

        agg.aggregateTemporal(ctx, vec(1f, 2f, 3f), 3, tvec);
        assertEquals(1f, tvec.get(0), 1e-5f);
        assertEquals(2f, tvec.get(1), 1e-5f);
        assertEquals(3f, tvec.get(2), 1e-5f);

        agg.aggregateTemporal(ctx, vec(5f, 6f, 7f), 5, tvec);
        assertEquals(6f, tvec.get(0), 1e-5f);
        assertEquals(8f, tvec.get(1), 1e-5f);
        assertEquals(10f, tvec.get(2), 1e-5f);

        agg.completeTemporal(ctx, 3, svec);
        assertEquals(6f, tvec.get(0), 1e-5f);
        assertEquals(8f, tvec.get(1), 1e-5f);
        assertEquals(10f, tvec.get(2), 1e-5f);

        ////////////////////////////////////////////////////////
        agg.computeOutput(tvec, out);
        assertEquals(6f, out.get(0), 1e-5f);
        assertEquals(8f, out.get(1), 1e-5f);
        assertEquals(24f, out.get(2), 1e-5f);
        assertEquals(14f, out.get(3), 1e-5f);
        assertEquals(3f, out.get(4), 1e-5f);
    }
}