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
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

/**
 * An aggregator that maps values from value ranges to given constant values.
 */
public class AggregatorMapping extends AbstractAggregator {

    private final int varIndex;
    private final float[] bounds;
    private final float[] codes;
    private final float fillValue;

    public AggregatorMapping(VariableContext varCtx, String varName, String targetName, float[] bounds, float[] codes, float fillValue) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "count"),
              createFeatureNames(varName, "sum", "count"),
              new String[]{targetName});
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
        this.bounds = bounds;
        this.codes = codes;
        this.fillValue = fillValue;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, spatialVector.get(0) + value);
            spatialVector.set(1, spatialVector.get(1) + 1f);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {

    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        float sum = spatialVector.get(0);
        if (!Float.isNaN(sum)) {
            temporalVector.set(0, temporalVector.get(0) + sum);
            temporalVector.set(1, temporalVector.get(1) + spatialVector.get(1));
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {

    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final double sum = temporalVector.get(0);
        final double count = temporalVector.get(1);
        float mappedValue = fillValue;
        if (count > 0 && sum != 0.0) {
            final double mean = sum / count;
            mappedValue = mapToIndex(mean);
        }
        outputVector.set(0, mappedValue);
    }

    private float mapToIndex(double value) {
        for (int i = 0; i < codes.length; i++) {
            final float code = codes[i];
            final double lowerBound = bounds[i];
            final double upperBound = bounds[i + 1];
            if (value >= lowerBound && value < upperBound) {
                return code;
            }
        }
        return fillValue;
    }

    public static class Config extends AggregatorConfig {

        @Parameter(label = "Source band name",
                notEmpty = true,
                notNull = true,
                description = "The source band used for aggregation.")
        String varName;
        @Parameter(label = "Target band name (optional)",
                description = "The name for the resulting band. If empty, the source band name is used.")
        String targetName;
        @Parameter(description = "An array with the bounds of the  value ranges.",
                notEmpty = true,
                notNull = true)
        float[] bounds;
        @Parameter(description = "An array with the codes associated with every range.")
        float[] codes;
        @Parameter(description = "The value used if the input is not in any range.",
                defaultValue = "NaN")
        Float fillValue;
    }


    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "Mapping";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            AggregatorMapping.Config config = (AggregatorMapping.Config) aggregatorConfig;

            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName) ? config.targetName : config.varName;

            float[] codes;
            if (config.codes != null) {
                codes = config.codes;
            } else {
                codes = new float[config.bounds.length];
                for (int i = 0; i < codes.length; i++) {
                    codes[i] = i;
                }
            }
            float fillValue = config.fillValue != null ? config.fillValue : Float.NaN;

            return new AggregatorMapping(varCtx, config.varName, targetName, config.bounds, codes, fillValue);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new AggregatorMapping.Config();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            AggregatorMapping.Config config = (AggregatorMapping.Config) aggregatorConfig;
            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            AggregatorMapping.Config config = (AggregatorMapping.Config) aggregatorConfig;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName) ? config.targetName : config.varName;
            return new String[]{targetName};
        }
    }
}
