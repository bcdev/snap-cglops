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

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.text.ParseException;
import java.util.Arrays;

/**
 * An aggregator for getting the observation period:
 * <p>
 * Include offset from the start of the n-day period as a number from 0
 * to (n-1) for both first_obs and last_obs bands (give a value for each pixel)
 * <p>
 * Only supported for CompositingType.MOSAICKING.
 */
public class AggregatorObservationPeriod extends AbstractAggregator {

    private final int startDay;

    AggregatorObservationPeriod(VariableContext varCtx, String startDate, String firstObsName, String lastObsName) {
        super(Descriptor.NAME,
              new String[]{"day"},
              new String[]{firstObsName, lastObsName},
              new String[]{firstObsName, lastObsName});
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        try {
            startDay = ProductData.UTC.parse(startDate, "yyyy-MM-dd").getDaysFraction();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initSpatial(BinContext binContext, WritableVector writableVector) {
        writableVector.set(0, Float.NaN);
    }

    @Override
    public void aggregateSpatial(BinContext binContext, Observation observation, WritableVector writableVector) {
        double mjd = observation.getMJD();
        int day = (int) (mjd - startDay);
        writableVector.set(0, day);
    }

    @Override
    public void completeSpatial(BinContext binContext, int numSpatialObs, WritableVector writableVector) {
        if (numSpatialObs > 1) {
            throw new IllegalArgumentException("This aggregator only supports MOSAICKING");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initTemporal(BinContext binContext, WritableVector writableVector) {
        writableVector.set(0, Float.POSITIVE_INFINITY);
        writableVector.set(1, Float.NEGATIVE_INFINITY);
    }

    @Override
    public void aggregateTemporal(BinContext binContext, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        float day = spatialVector.get(0);
        temporalVector.set(0, Math.min(temporalVector.get(0), day));
        temporalVector.set(1, Math.max(temporalVector.get(1), day));
    }

    @Override
    public void completeTemporal(BinContext binContext, int numTemporalObs, WritableVector temporalVector) {
        if (numTemporalObs == 0) {
            temporalVector.set(0, Float.NaN);
            temporalVector.set(1, Float.NaN);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
        outputVector.set(1, temporalVector.get(1));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "ObservationPeriod{" +
                "startDay=" + startDay +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static class Config extends AggregatorConfig {

        @Parameter(notEmpty = true, notNull = true, description = "First day in format 'YYYY-MM-DD'")
        String startDate;
        @Parameter(defaultValue = "first_obs")
        String firstObsName;
        @Parameter(defaultValue = "last_obs")
        String lastObsName;

        public Config() {
            super(Descriptor.NAME);
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "ObservationPeriod";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;

            String firstObsName = StringUtils.isNotNullAndNotEmpty(config.firstObsName) ? config.firstObsName : "first_obs";
            String lastObsName = StringUtils.isNotNullAndNotEmpty(config.lastObsName) ? config.lastObsName : "last_obs";

            return new AggregatorObservationPeriod(varCtx, config.startDate, firstObsName, lastObsName);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            return new String[0];
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new String[]{config.firstObsName, config.lastObsName};
        }
    }
}
