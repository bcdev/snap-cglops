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

import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.support.VectorImpl;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static com.bc.snap.cglops.l3.AggregatorTestUtils.aggregate;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.assertVectorEquals;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.obs;
import static com.bc.snap.cglops.l3.AggregatorTestUtils.vec;
import static java.lang.Float.NaN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AggregatorObservationPeriodTest {

    private static double mjd(String date) throws ParseException {
        return ProductData.UTC.parse(date, "yyyy-MM-dd HH:mm:ss").getMJD();
    }

    private VariableContext varCtx;

    @Before
    public void setUp() throws Exception {
        varCtx = new MyVariableContext();
    }

    @Test
    public void testMetadata() {
        varCtx = new MyVariableContext();
        AggregatorObservationPeriod agg = new AggregatorObservationPeriod(varCtx, "2011-03-04", "first_obs", "last_obs");

        assertArrayEquals(new String[]{"day"}, agg.getSpatialFeatureNames());
        assertArrayEquals(new String[]{"first_obs", "last_obs"}, agg.getTemporalFeatureNames());
        assertArrayEquals(new String[]{"first_obs", "last_obs"}, agg.getOutputFeatureNames());
    }

    @Test
    public void testAggregate_detailed() throws Exception {
        BinContext ctx = AggregatorTestUtils.createCtx();
        AggregatorObservationPeriod agg = new AggregatorObservationPeriod(varCtx, "2011-03-04", "first_obs", "last_obs");

        VectorImpl svec = vec(42);
        VectorImpl tvec = vec(NaN, NaN);
        VectorImpl ovec = vec(NaN, NaN);
        /////////////////////////////////////////////
        agg.initSpatial(ctx, svec);
        assertTrue(Float.isNaN(svec.get(0)));

        agg.aggregateSpatial(ctx, obs(mjd("2011-03-05 11:22:33")), svec);
        assertEquals(1, svec.get(0), 0.0f);

        agg.completeSpatial(ctx, 1, svec);
        assertEquals(1, svec.get(0), 0.0f);
        /////////////////////////////////////////////
        agg.initTemporal(ctx, tvec);
        assertEquals(Float.POSITIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, tvec.get(1), 0.0f);

        agg.aggregateTemporal(ctx, svec, 1, tvec);
        assertEquals(1, tvec.get(0), 0.0f);
        assertEquals(1, tvec.get(1), 0.0f);

        agg.completeTemporal(ctx, 1, tvec);
        assertEquals(1, tvec.get(0), 0.0f);
        assertEquals(1, tvec.get(1), 0.0f);
        /////////////////////////////////////////////
        agg.computeOutput(tvec, ovec);
        assertEquals(1, ovec.get(0), 0.0f);
        assertEquals(1, ovec.get(1), 0.0f);
    }

    @Test
    public void testAggregate_e2e() throws Exception {
        AggregatorObservationPeriod agg = new AggregatorObservationPeriod(varCtx, "2011-03-04", "first_obs", "last_obs");
        BinManager bm = new BinManager(varCtx, agg);

        // no obs
        Observation[][] multipleProductObs = new Observation[][]{{}};
        assertVectorEquals(vec(NaN, NaN), aggregate(bm, multipleProductObs));

        // one obs
        multipleProductObs = new Observation[][]{
                {obs(mjd("2011-03-06 11:22:33"))}
        };
        assertVectorEquals(vec(2.0f, 2.0f), aggregate(bm, multipleProductObs));

        // multiple obs
        multipleProductObs = new Observation[][]{
                {obs(mjd("2011-03-05 11:22:33"))},
                {obs(mjd("2011-03-06 11:22:33"))},
                {obs(mjd("2011-03-09 11:22:33"))}
        };
        assertVectorEquals(vec(1.0f, 5.0f), aggregate(bm, multipleProductObs));

        // multiple obs fro single product
        multipleProductObs = new Observation[][]{
                {obs(mjd("2011-03-05 11:22:33")),
                        obs(mjd("2011-03-06 11:22:33"))},
                {obs(mjd("2011-03-09 11:22:33"))}
        };
        try {
            aggregate(bm, multipleProductObs);
            fail();
        } catch (IllegalArgumentException iae) {
            assertEquals("This aggregator only supports MOSAICKING", iae.getMessage());
        }
    }
}
