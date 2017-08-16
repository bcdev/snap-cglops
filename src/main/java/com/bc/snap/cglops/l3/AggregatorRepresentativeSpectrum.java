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
import org.esa.snap.binning.support.GrowableVector;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.acos;
import static java.lang.Math.sqrt;

/**
 * An aggregator for getting the "most representative spectrum":
 * <p>
 * Collect the median spectrum from the per-pixel observations over 10
 * days, then calculate the spectral angle and spectral difference from
 * each observation (spectrum) to the median spectrum. The spectrum that
 * has the best metric (angle close to unity, smallest difference) is the
 * 'representative spectrum'.
 * <p>
 * Only supported for CompositingType.MOSAICKING.
 */
public class AggregatorRepresentativeSpectrum extends AbstractAggregator {

    private final int startDay;

    private final Method method;
    private final int[] varIndices;
    private final String[] varNames;
    private final String[] searchVarNames;

    AggregatorRepresentativeSpectrum(VariableContext varCtx, String startDate, String bestObsDateName, Method method, String targetSuffix, String[] varNames, String[] searchVarNames) {
        super(Descriptor.NAME,
              createNames(bestObsDateName != null ? "day" : null, targetSuffix, varNames),
              createNames(bestObsDateName, targetSuffix, varNames),
              createNames(bestObsDateName, targetSuffix, varNames));
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (method == null) {
            throw new NullPointerException("method");
        }
        if (bestObsDateName != null){
            try {
                this.startDay = ProductData.UTC.parse(startDate, "yyyy-MM-dd").getDaysFraction();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            this.startDay = -1;
        }
        this.method = method;
        varIndices = new int[varNames.length];
        for (int i = 0; i < varNames.length; i++) {
            int varIndex = varCtx.getVariableIndex(varNames[i]);
            if (varIndex < 0) {
                throw new IllegalArgumentException("varNames[" + i + "] '" + varNames[i] + "' does not exist");
            }
            varIndices[i] = varIndex;
        }
        this.varNames = varNames;
        this.searchVarNames = searchVarNames;
    }

    private static String[] createNames(String dateName, String suffix, String... varNames) {
        ArrayList<String> featureNames = new ArrayList<>(varNames.length+1);
        for (final String varName : varNames) {
            if (suffix != null && suffix.length() > 0) {
                featureNames.add(varName + "_" + suffix);
            } else {
                featureNames.add(varName);
            }
        }
        if (dateName != null) {
            featureNames.add(dateName);
        }
        return featureNames.toArray(new String[featureNames.size()]);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initSpatial(BinContext binContext, WritableVector writableVector) {
        for (int i = 0; i < varIndices.length; i++) {
            writableVector.set(i, Float.NaN);
        }
        if (startDay != -1) {
            writableVector.set(varIndices.length, Float.NaN);
        }
    }

    @Override
    public void aggregateSpatial(BinContext binContext, Observation observation, WritableVector writableVector) {
        for (int i = 0; i < varIndices.length; i++) {
            float value = observation.get(varIndices[i]);
            if (Float.isNaN(value)) {
                // if any value isNaN, throw away the complete spectra
                for (int j = 0; j < writableVector.size(); j++) {
                    writableVector.set(j, Float.NaN);
                }
                return;
            } else {
                writableVector.set(i, value);
            }
        }
        if (startDay != -1) {
            double mjd = observation.getMJD();
            int day = (int) (mjd - startDay);
            writableVector.set(varIndices.length, day);            
        }
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
        for (int i = 0; i < varNames.length; i++) {
            writableVector.set(i, Float.NaN);
            binContext.put(varNames[i], new GrowableVector(10));
        }
        if (startDay != -1) {
            writableVector.set(varNames.length, Float.NaN);
            binContext.put("day", new GrowableVector(10));
        }
    }

    @Override
    public void aggregateTemporal(BinContext binContext, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        float firstValue = spatialVector.get(0);
        if (!Float.isNaN(firstValue)) {
            for (int i = 0; i < varNames.length; i++) {
                GrowableVector measurementsVec = binContext.get(varNames[i]);
                measurementsVec.add(spatialVector.get(i));
            }
            if (startDay != -1) {
                GrowableVector measurementsVec = binContext.get("day");
                measurementsVec.add(spatialVector.get(varNames.length)); 
            }
        }
    }

    @Override
    public void completeTemporal(BinContext binContext, int numTemporalObs, WritableVector temporalVector) {
        // handle special cases: 0 or 1 observation
        GrowableVector firstVector = binContext.get(varNames[0]);
        int numSpectra = firstVector.size();
        if (numSpectra == 0) {
            return;
        } else if (numSpectra == 1) {
            for (int i = 0; i < varNames.length; i++) {
                GrowableVector measurementsVec = binContext.get(varNames[i]);
                temporalVector.set(i, measurementsVec.get(0));
            }
            if (startDay != -1) {
                GrowableVector measurementsVec = binContext.get("day");
                temporalVector.set(varNames.length, measurementsVec.get(0));
            }
            return;
        }
        float[][] data = new float[searchVarNames.length][0];
        for (int i = 0; i < searchVarNames.length; i++) {
            GrowableVector vector = binContext.get(searchVarNames[i]);
            data[i] = vector.getElements();
        }
        // Calculate the median spectrum (as an intermediary step) as the spectrum of per-band median values.
        // For a set of 1 or 2 observations the median is the mean.
        // For 3 or more observations the median is the central value,
        // i.e. the middle one when values are ordered from low to high
        double[][] allSpectra = new double[numSpectra][searchVarNames.length];
        double[] medianSpectrum = computeMedianSpectrum(data, allSpectra);
        int bestSpectraIndex = findBestSpectra(allSpectra, medianSpectrum);
        if (bestSpectraIndex > -1) {
            for (int i = 0; i < varNames.length; i++) {
                GrowableVector measurementsVec = binContext.get(varNames[i]);
                temporalVector.set(i, measurementsVec.get(bestSpectraIndex));
            }
            if (startDay != -1) {
                GrowableVector measurementsVec = binContext.get("day");
                temporalVector.set(varNames.length, measurementsVec.get(bestSpectraIndex));
            }
        }
    }

    private int findBestSpectra(double[][] allSpectra, double[] medianSpectrum) {
        int bestIndex = -1;
        double bestValue = Double.POSITIVE_INFINITY;
        for (int i = 0; i < allSpectra.length; i++) {
            double value = method.compute(allSpectra[i], medianSpectrum);
            if (value < bestValue) {
                bestValue = value;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        for (int i = 0; i < temporalVector.size(); i++) {
            outputVector.set(i, temporalVector.get(i));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "RepresentativeSpectrum{" +
                "method=" + method +
                "varNames=" + Arrays.toString(varNames) +
                "varIndices=" + Arrays.toString(varIndices) +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static class Config extends AggregatorConfig {

        @Parameter(notEmpty = true, notNull = true, description = "The variables making up the spectra.")
        String[] varNames;
        @Parameter(description = "The variables used to find the best representative spectra. " +
                "If given it must be a subset of 'varNames', if not all variables from 'varNames' will be used.")
        String[] searchVarNames;
        @Parameter(label = "Target band name suffix (optional)",
                description = "The name suffix for the resulting bands. If empty, the source band names are used.")
        String targetSuffix;
        @Parameter(notEmpty = true, notNull = true,
                description = "The method used for finding the best representative spectra",
                defaultValue = "SpectralAngle")
        Method method;
        @Parameter(notEmpty = true, notNull = true, description = "First day in format 'YYYY-MM-DD'")
        String startDate;
        @Parameter(defaultValue = "best_obs")
        String bestObsDateName;

        public Config() {
            super(Descriptor.NAME);
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        private static final String NAME = "RepresentativeSpectrum";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            if (config.varNames == null || config.varNames.length == 0) {
                throw new IllegalArgumentException("'varNames is a required parameter'");
            }
            Method method = config.method != null ? config.method : Method.SpectralAngle;
            String targetSuffix = StringUtils.isNotNullAndNotEmpty(config.targetSuffix) ? config.targetSuffix : "";
            String[] searchVarNames= config.varNames;
            boolean searchVarNamesGiven = config.searchVarNames != null && config.searchVarNames.length > 0;
            if (searchVarNamesGiven) {
                if (!Arrays.asList(config.varNames).containsAll(Arrays.asList(config.searchVarNames))) {
                    throw new IllegalArgumentException("'searchVarNames' must be a subset of 'varNames'.");
                }
                searchVarNames = config.searchVarNames;
            }

            String bestObsDateName = StringUtils.isNotNullAndNotEmpty(config.bestObsDateName) ? config.bestObsDateName : null;
            
            return new AggregatorRepresentativeSpectrum(varCtx, config.startDate, bestObsDateName, method, targetSuffix, config.varNames, searchVarNames);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return config.varNames;
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            String targetSuffix = StringUtils.isNotNullAndNotEmpty(config.targetSuffix) ? config.targetSuffix : "";
            String dateName = StringUtils.isNotNullAndNotEmpty(config.bestObsDateName) ? config.bestObsDateName : null;
            
            return createNames(dateName, targetSuffix, config.varNames);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static double[] computeMedianSpectrum(float[][] data, double[][] allSpectra) {
        double[] medianSpectrum = new double[allSpectra[0].length];
        for (int i = 0; i < medianSpectrum.length; i++) {
            float[] measurements = data[i];
            for (int spectraIndex = 0; spectraIndex < measurements.length; spectraIndex++) {
                allSpectra[spectraIndex][i] = measurements[spectraIndex];
            }
            Arrays.sort(measurements);
            medianSpectrum[i] = computeMedian(measurements);
        }
        return medianSpectrum;
    }

    static float computeMedian(float... values) {
        if (values.length % 2 == 0) {
            return (values[values.length / 2] + values[values.length / 2 - 1]) / 2;
        } else {
            return values[values.length / 2];
        }
    }

    private static double mean(double sum, int n) {
        if (sum == 0) {
            return 0;
        } else {
            return sum / n;
        }

    }

    enum Method {
        SpectralAngle {
            @Override
            public double compute(double[] spectrum, double[] medianSpectrum) {
                double sumXY = 0;
                double sumXX = 0;
                double sumYY = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    sumXX += x * x;
                    sumYY += y * y;
                    sumXY += x * y;
                }
                return acos(sumXY / (sqrt(sumXX) * sqrt(sumYY)));
            }
        },
        AbsoluteDifference {
            @Override
            public double compute(double[] spectrum, double[] medianSpectrum) {
                double sum = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    sum += Math.abs((x - y) / y);
                }
                if (sum > 0) {
                    return sum / spectrum.length;
                }
                return mean(sum, spectrum.length);
            }
        },
        RMSDifference {
            @Override
            public double compute(double[] spectrum, double[] medianSpectrum) {
                double sum = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    double difference = x - y;
                    sum += difference * difference;
                }
                if (sum > 0) {
                    return Math.sqrt(sum / spectrum.length);
                }
                return sum;
            }
        },
        Bias {
            @Override
            public double compute(double[] spectrum, double[] medianSpectrum) {
                double sum = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    sum += (x - y) / y;
                }
                return Math.abs(mean(sum, spectrum.length));
            }
        },
        CoeffOfDetermination {
            @Override
            public double compute(double[] spectrum, double[] medianSpectrum) {
                double sumX = 0;
                double sumY = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    sumX += x;
                    sumY += y;
                }
                final double meanX = mean(sumX, spectrum.length);
                final double meanY = mean(sumY, spectrum.length);
                double sumXXYY = 0;
                double sumXX2 = 0;
                double sumYY2 = 0;
                for (int i = 0; i < spectrum.length; i++) {
                    double x = spectrum[i];
                    double y = medianSpectrum[i];
                    final double xx = x - meanX;
                    final double yy = y - meanY;
                    sumXXYY += xx * yy;
                    sumXX2 += xx * xx;
                    sumYY2 += yy * yy;
                }
                return 1 - ((sumXXYY * sumXXYY) / (sumXX2 * sumYY2));
            }
        };

        public abstract double compute(double[] spectrum, double[] medianSpectrum);
    }
}
