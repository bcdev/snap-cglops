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

package com.bc.snap.cglops.l2;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.CellProcessorConfig;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.ProductCustomizerConfig;
import org.esa.snap.binning.operator.AggregatorConfigDomConverter;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.operator.CellProcessorConfigDomConverter;
import org.esa.snap.binning.operator.ProductCustomizerConfigDomConverter;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.converters.JtsGeometryConverter;


import java.io.File;

/**
 * Simplifies the basic Binning Operator for lake processing.
 *
 * @author marcoz
 */
@OperatorMetadata(alias = "LakeAggregate",
        version = "1.0",
        authors = "Marco Zühlke",
        copyright = "(c) 2015 by Brockmann Consult GmbH",
        description = "Performs spatial and temporal aggregation of pixel values into cells ('bins') of a planetary grid over a lake",
        autoWriteDisabled = true)
public class LakeAggregateOp extends Operator {

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).")
    private String[] sourceProductPaths;

    // TODO nf/mz 2013-11-05: this could be a common Operator parameter, it accelerates opening of products
    @Parameter(description = "The common product format of all source products. This parameter is optional and may be used in conjunction " +
            "with parameter 'sourceProductPaths' and only to speed up source product opening." +
            "Try \"NetCDF-CF\", \"GeoTIFF\", \"BEAM-DIMAP\", or \"ENVISAT\", etc.",
            defaultValue = "")
    private String sourceProductFormat;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The considered geographical region as a geometry in well-known text format (WKT).\n" +
                    "If not given, the geographical region will be computed according to the extents of the " +
                    "input products.")
    private Geometry region;

    @Parameter(description = "Number of rows in the (global) planetary grid. Must be even.", defaultValue = "2160")
    private int numRows;

    @Parameter(description = "The square of the number of pixels used for super-sampling an input pixel into multiple sub-pixels", defaultValue = "1")
    private Integer superSampling;

    @Parameter(description = "The band maths expression used to filter input pixels")
    private String maskExpr;

    @Parameter(alias = "variables", itemAlias = "variable",
            description = "List of variables. A variable will generate a virtual band " +
                    "in each source data product, so that it can be used as input for the binning.")
    private VariableConfig[] variableConfigs;

    @Parameter(alias = "aggregators", domConverter = AggregatorConfigDomConverter.class,
            description = "List of aggregators. Aggregators generate the bands in the binned output products")
    private AggregatorConfig[] aggregatorConfigs;

    @Parameter(alias = "postProcessor", domConverter = CellProcessorConfigDomConverter.class)
    private CellProcessorConfig postProcessorConfig;

    @Parameter
    private String outputFile;

    @Parameter(defaultValue = "BEAM-DIMAP")
    private String outputFormat;

    @Parameter(alias = "outputBands", itemAlias = "band", description = "Configures the target bands. Not needed " +
            "if output type 'Product' is chosen.")
    private BinningOp.BandConfiguration[] bandConfigurations;

    @Parameter(alias = "productCustomizer", domConverter = ProductCustomizerConfigDomConverter.class)
    private ProductCustomizerConfig productCustomizerConfig;

    @Parameter(description = "The name of the file containing metadata key-value pairs (google \"Java Properties file format\").",
            defaultValue = "./metadata.properties")
    private File metadataPropertiesFile;

    @Parameter(description = "The name of the directory containing metadata templates (google \"Apache Velocity VTL format\").",
            defaultValue = ".")
    private File metadataTemplateDir;

    @Parameter(description = "The type of metadata aggregation to be used. Possible values are:\n" +
            "'NAME': aggregate the name of each input product\n" +
            "'FIRST_HISTORY': aggregates all input product names and the processing history of the first product\n" +
            "'ALL_HISTORIES': aggregates all input product names and processing histories",
            defaultValue = "NAME")
    private String metadataAggregatorName;

    @Override
    public void initialize() throws OperatorException {
        BinningOp binningOp = new BinningOp();
        binningOp.setParameterDefaultValues();
        binningOp.setParameter("sourceProductPaths", sourceProductPaths);
        binningOp.setParameter("sourceProductFormat", sourceProductFormat);
        binningOp.setParameter("region", region);
        binningOp.setParameter("startDateTime", null);
        binningOp.setParameter("periodDuration", null);
        binningOp.setParameter("timeFilterMethod", null);
        binningOp.setParameter("minDataHour", null);
        binningOp.setParameter("timeFilterMethod", null);
        binningOp.setParameter("numRows", numRows);
        binningOp.setParameter("superSampling", 1);
        binningOp.setParameter("maskExpr", maskExpr);
        binningOp.setParameter("numRows", numRows);
        binningOp.setParameter("variableConfigs", variableConfigs);
        binningOp.setParameter("aggregatorConfigs", aggregatorConfigs);
        binningOp.setParameter("postProcessorConfig", postProcessorConfig);
        binningOp.setParameter("outputType", "Product");
        binningOp.setParameter("outputFile", outputFile);
        binningOp.setParameter("outputFormat", outputFormat);
        binningOp.setParameter("bandConfigurations", null);
        binningOp.setParameter("productCustomizerConfig", productCustomizerConfig);
        binningOp.setParameter("outputBinnedData", false);
        binningOp.setParameter("outputTargetProduct", true);
        binningOp.setParameter("metadataPropertiesFile", metadataPropertiesFile);
        binningOp.setParameter("metadataTemplateDir", metadataTemplateDir);
        binningOp.setParameter("metadataAggregatorName", metadataAggregatorName);

        binningOp.setPlanetaryGridClass("org.esa.beam.binning.support.PlateCarreeGrid");
        binningOp.setCompositingType(CompositingType.MOSAICKING);

        setTargetProduct(binningOp.getTargetProduct());
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(LakeAggregateOp.class);
        }
    }
}
