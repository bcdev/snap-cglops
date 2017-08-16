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



import org.esa.snap.binning.ProductCustomizer;
import org.esa.snap.binning.ProductCustomizerConfig;
import org.esa.snap.binning.ProductCustomizerDescriptor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.io.IOException;


/**
 * Removes num_obs and num_passes bands, depending on configuration.
 */
public class LakeMonthlyProductCustomizer extends ProductCustomizer {

    @Override
    public void customizeProduct(Product product) {
        throw new IllegalStateException("Not implemented.");
    }

    public static class Config extends ProductCustomizerConfig {
        @Parameter(defaultValue = "true")
        private Boolean writeNumObs;
        @Parameter(defaultValue = "true")
        private Boolean writeNumPasses;

        @Parameter()
        private String arcDayProductPath;
        @Parameter()
        private String arcNightProductPath;
        @Parameter()
        private String arcBand;

        @Parameter()
        private String shallowProductPath;
    }

    public static class Descriptor implements ProductCustomizerDescriptor {

        @Override
        public ProductCustomizer createProductCustomizer(ProductCustomizerConfig pcc) {
            Config config = (Config) pcc;

            boolean writeNumObs = config.writeNumObs != null ? config.writeNumObs : true;
            boolean writeNumPasses = config.writeNumPasses != null ? config.writeNumPasses : true;

            Product shallowProduct;
            try {
                shallowProduct = ProductIO.readProduct(config.shallowProductPath);
            } catch (IOException e) {
                throw new OperatorException(e);
            }
            MonthlyProductCustomizer productCustomizer = new MonthlyProductCustomizer(writeNumObs, writeNumPasses, shallowProduct);
            if (config.arcDayProductPath != null && config.arcNightProductPath != null && config.arcBand != null) {
                try {
                    Product arcDayProduct = ProductIO.readProduct(config.arcDayProductPath);
                    Product arcNightProduct = ProductIO.readProduct(config.arcNightProductPath);
                    productCustomizer.setArcData(arcDayProduct, arcNightProduct, config.arcBand);
                } catch (IOException e) {
                    throw new OperatorException(e);
                }
            }
            return productCustomizer;
        }

        @Override
        public String getName() {
            return "LakeMonthlyProduct";
        }

        @Override
        public ProductCustomizerConfig createConfig() {
            return new Config();
        }
    }
}
