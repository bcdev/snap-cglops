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

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.util.Arrays;
import java.util.Random;

/**
 * An aggregator for index classifications
 */
public class AggregatorMajorityClass extends AbstractAggregator {

    private final int varIndex;
    private final int[] classes;
    private final Random random;

    public AggregatorMajorityClass(VariableContext varCtx, String varName, int[] classes) {
        super(Descriptor.NAME,
              getIntermediateFeatureNames(varName, classes),
              getIntermediateFeatureNames(varName, classes),
              getOutputFeatureNames(varName, classes));
        this.classes = classes;

        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
        random = new Random();
    }

    @Override
    public void initSpatial(BinContext binContext, WritableVector writableVector) {
        for (int i = 0; i < classes.length + 1; i++) {
            writableVector.set(i, 0F);
        }
    }

    @Override
    public void aggregateSpatial(BinContext binContext, Observation observation, WritableVector writableVector) {
        float value = observation.get(varIndex);
        if (Float.isNaN(value)) {
            return; // don't aggregate NaN at all
        }
        for (int i = 0; i < classes.length; i++) {
            if (value == classes[i]) {
                writableVector.set(i, writableVector.get(i) + 1);
                return;
            }
        }
        // value is not NaN, but another class
        writableVector.set(classes.length, writableVector.get(classes.length) + 1);
    }

    @Override
    public void completeSpatial(BinContext binContext, int numSpatialObs, WritableVector writableVector) {
    }

    @Override
    public void initTemporal(BinContext binContext, WritableVector writableVector) {
        for (int i = 0; i < classes.length + 1; i++) {
            writableVector.set(i, 0F);
        }
    }

    @Override
    public void aggregateTemporal(BinContext binContext, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        for (int i = 0; i < classes.length + 1; i++) {
            temporalVector.set(i, temporalVector.get(i) + spatialVector.get(i));
        }
    }

    @Override
    public void completeTemporal(BinContext binContext, int i, WritableVector writableVector) {

    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        // test for valid observations
        boolean hasValidObservation = false;
        for (int i = 0; i < classes.length + 1; i++) {
            float counts = temporalVector.get(i);
            if (counts > 0) {
                hasValidObservation = true;
                break;
            }
        }
        if (hasValidObservation) {
            int majorityClassIndex = -1;
            float majorityClassCounts = -1;
            float sum_all = 0f;
            float sum_analyzed = 0f;
            for (int i = 0; i < classes.length; i++) {
                float counts = temporalVector.get(i);
                outputVector.set(i, counts);
                sum_analyzed += counts;
                sum_all += counts;
                if (counts > majorityClassCounts) {
                    majorityClassCounts = counts;
                    majorityClassIndex = i;
                } else if (counts == majorityClassCounts) {
                    // TODO decide (cb,do) if this is the right thing (TM)
                    // TODO random decision are not predictable science
                    if (random.nextBoolean()) {
                        majorityClassCounts = counts;
                        majorityClassIndex = i;
                    }
                }
            }
            sum_all += temporalVector.get(classes.length);
            outputVector.set(classes.length, sum_all);
            outputVector.set(classes.length + 1, sum_analyzed);
            outputVector.set(classes.length + 2, classes[majorityClassIndex]);
        } else {
            for (int i = 0; i < outputVector.size(); i++) {
                outputVector.set(i, Float.NaN);
            }
        }
    }

    static String[] getIntermediateFeatureNames(String varName, int[] classes) {
        String[] features = new String[classes.length + 1];
        for (int i = 0; i < classes.length; i++) {
            features[i] = String.format("%s_class_%d_counts", varName, classes[i]);
        }
        features[classes.length] = varName + "_class_other_counts";
        return features;
    }

    static String[] getOutputFeatureNames(String varName, int[] classes) {
        String[] features = new String[classes.length + 3];
        for (int i = 0; i < classes.length; i++) {
            features[i] = String.format("%s_class_%d_counts", varName, classes[i]);
        }
        features[classes.length] = varName + "_sum_all";
        features[classes.length + 1] = varName + "_sum_analyzed";
        features[classes.length + 2] = varName + "_majority_class";
        return features;
    }


    @Override
    public String toString() {
        return "AggregatorMajorityClass{" +
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
        int[] classes;

        public Config() {
            super(Descriptor.NAME);
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "MAJORITY_CLASS";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new AggregatorMajorityClass(varCtx, config.varName, config.classes);
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
            return getOutputFeatureNames(config.varName, config.classes);
        }
    }
}
