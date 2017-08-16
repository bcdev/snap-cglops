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

import org.esa.snap.binning.ProductCustomizer;
import org.esa.snap.binning.ProductCustomizerConfig;
import org.esa.snap.binning.ProductCustomizerDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * Removes num_obs and num_passes bands, depending on configuration.
 */
public class ObsPassesCustomizer extends ProductCustomizer {

    private final boolean writeNumObs;
    private final boolean writeNumPasses;

    public ObsPassesCustomizer(boolean writeNumObs, boolean writeNumPasses) {
        this.writeNumObs = writeNumObs;
        this.writeNumPasses = writeNumPasses;
    }

    @Override
    public void customizeProduct(Product product) {
        if (!writeNumObs) {
            Band numObsBand = product.getBand("num_obs");
            if (numObsBand != null) {
                product.removeBand(numObsBand);
            }
        }
        if (!writeNumPasses) {
            Band numPassesBand = product.getBand("num_passes");
            if (numPassesBand != null) {
                product.removeBand(numPassesBand);
            }
        }
    }


    public static class Config extends ProductCustomizerConfig {
        @Parameter(defaultValue = "true")
        private Boolean writeNumObs;
        @Parameter(defaultValue = "true")
        private Boolean writeNumPasses;
    }

    public static class Descriptor implements ProductCustomizerDescriptor {

        @Override
        public ProductCustomizer createProductCustomizer(ProductCustomizerConfig config) {
            Config numWriterConfig = (Config) config;
            boolean writeNumObs = numWriterConfig.writeNumObs != null ? numWriterConfig.writeNumObs : true;
            boolean writeNumPasses = numWriterConfig.writeNumPasses != null ? numWriterConfig.writeNumPasses : true;
            return new ObsPassesCustomizer(writeNumObs, writeNumPasses);
        }

        @Override
        public String getName() {
            return "ObsPasses";
        }

        @Override
        public ProductCustomizerConfig createConfig() {
            return new Config();
        }
    }
}
