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

import java.util.Arrays;
import java.util.Calendar;

/**
 * An aggregator for getting one value per month.
 */
public class FirstN extends AbstractAggregator {

    private final int varIndex;
    private final int n;

    public FirstN(VariableContext varCtx, String varName, int n) {
        super(Descriptor.NAME,
              new String[]{varName, "month"},
              getOutputFeatureNames(varName, n),
              getOutputFeatureNames(varName, n));
        this.n = n;

        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        this.varIndex = varCtx.getVariableIndex(varName);

    }

    @Override
    public void initSpatial(BinContext binContext, WritableVector writableVector) {
        writableVector.set(0, Float.NaN);
    }

    @Override
    public void aggregateSpatial(BinContext binContext, Observation observation, WritableVector writableVector) {
        writableVector.set(0, observation.get(varIndex));
        double mjd = observation.getMJD();
        ProductData.UTC utc = new ProductData.UTC(mjd);
        writableVector.set(1, utc.getAsCalendar().get(Calendar.MONTH));
    }

    @Override
    public void completeSpatial(BinContext binContext, int numSpatialObs, WritableVector writableVector) {
    }

    @Override
    public void initTemporal(BinContext binContext, WritableVector writableVector) {
        for (int i = 0; i < n; i++) {
            writableVector.set(i, Float.NaN);
        }
    }

    @Override
    public void aggregateTemporal(BinContext binContext, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        float value = spatialVector.get(0);
        float month = spatialVector.get(1);
        temporalVector.set((int)month, value);
    }

    @Override
    public void completeTemporal(BinContext binContext, int i, WritableVector writableVector) {

    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        for (int i = 0; i < n; i++) {
            outputVector.set(i, temporalVector.get(i));
        }
    }

    static String[] getOutputFeatureNames(String varName, int n) {
        String[] features = new String[n];
        for (int i = 0; i < features.length; i++) {
            features[i] = String.format("%s_%d", varName, i + 1);
        }
        return features;
    }

    @Override
    public String toString() {
        return "FirstN{" +
                "varIndex=" + varIndex +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static class Config extends AggregatorConfig {

        @Parameter
        String varName;
        @Parameter
        int n;

        public Config() {
            super(Descriptor.NAME);
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "FirstN";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new FirstN(varCtx, config.varName, config.n);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return getOutputFeatureNames(config.varName, config.n);
        }
    }
}
