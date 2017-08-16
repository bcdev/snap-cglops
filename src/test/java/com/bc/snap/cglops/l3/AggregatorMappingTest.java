/*
 * Copyright (C) 2017 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.junit.Before;
import org.junit.Test;

import static com.bc.snap.cglops.l3.AggregatorTestUtils.aggregate;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.assertVectorEquals;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.obsNT;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.vec;
import static java.lang.Float.NaN;
import static org.junit.Assert.assertArrayEquals;

public class AggregatorMappingTest {

    private VariableContext varCtx;

    @Before
    public void setUp() throws Exception {
        varCtx = new MyVariableContext("r1");
    }

    @Test
    public void testMetadata() {
        varCtx = new MyVariableContext();
        float[] bounds = {1f, 3f};
        float[] codes = {42f};
        Aggregator agg = new AggregatorMapping(varCtx, "r1", "map", bounds, codes, -1f);

        assertArrayEquals(new String[]{"r1_sum", "r1_count"}, agg.getSpatialFeatureNames());
        assertArrayEquals(new String[]{"r1_sum", "r1_count"}, agg.getTemporalFeatureNames());
        assertArrayEquals(new String[]{"map"}, agg.getOutputFeatureNames());
    }

    @Test
    public void testAggregate_e2e() throws Exception {
        float[] bounds = {1, 3, 5};
        float[] codes = {42, 50};
        Aggregator agg = new AggregatorMapping(varCtx, "r1", "map", bounds, codes, -3);
        BinManager bm = new BinManager(varCtx, agg);
        Observation[][] multipleProductObs;

        // 0 obs
        multipleProductObs = new Observation[][]{{}};
        assertVectorEquals(vec(-3), aggregate(bm, multipleProductObs));

        // 1 obs - in range
        multipleProductObs = new Observation[][]{
                {obsNT(1.5f)}
        };
        assertVectorEquals(vec(42), aggregate(bm, multipleProductObs));

        // 1 obs - NaN
        multipleProductObs = new Observation[][]{
                {obsNT(NaN)}
        };
        assertVectorEquals(vec(-3), aggregate(bm, multipleProductObs));

        // 1 obs - out of range
        multipleProductObs = new Observation[][]{
                {obsNT(9.5f)}
        };
        assertVectorEquals(vec(-3), aggregate(bm, multipleProductObs));

        // 1 pass 2 obs
        multipleProductObs = new Observation[][]{
                {obsNT(2)},
                {obsNT(6)}
        };
        assertVectorEquals(vec(50), aggregate(bm, multipleProductObs));

        // 2 passes 4 obs
        multipleProductObs = new Observation[][]{
                {obsNT(2), obsNT(4)},
                {obsNT(6), obsNT(7)}
        };
        assertVectorEquals(vec(50), aggregate(bm, multipleProductObs));
    }
}