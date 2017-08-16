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
import org.esa.snap.collocation.CollocateOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.ProductUtils;

/**
 * Removes num_obs and num_passes bands, depending on configuration.
 */
public class MonthlyProductCustomizer extends ProductCustomizer {

    private final boolean writeNumObs;
    private final boolean writeNumPasses;
    private final Product shallowProduct;

    private Product arcDayProduct;
    private Product arcNightProduct;
    private String arcBand;


    public MonthlyProductCustomizer(boolean writeNumObs, boolean writeNumPasses, Product shallowProduct) {
        this.writeNumObs = writeNumObs;
        this.writeNumPasses = writeNumPasses;
        this.shallowProduct = shallowProduct;
    }

    public void setArcData(Product arcDayProduct, Product arcNightProduct, String arcBand) {
        this.arcDayProduct = arcDayProduct;
        this.arcNightProduct = arcNightProduct;
        this.arcBand = arcBand;
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
        Product shallowCollocated = collocate(product, shallowProduct);


        if (arcDayProduct != null) {
            Product arcDayCollocated = collocate(product, arcDayProduct);
            Product arcNightCollocated = collocate(product, arcNightProduct);

            BandMathsOp.BandDescriptor bdShallow = new BandMathsOp.BandDescriptor();
            bdShallow.name = "shallow";
            bdShallow.expression = "$shallow.shallow";
            bdShallow.type = ProductData.TYPESTRING_FLOAT32;
            bdShallow.noDataValue = Double.NaN;

            BandMathsOp.BandDescriptor bdDay = new BandMathsOp.BandDescriptor();
            bdDay.name = "lswt_d_mean";
//            bdDay.expression = "$day." + arcBand + " > 0 and $shallow.shallow == 0 ? $day." + arcBand + " : NaN";
            bdDay.expression = "$day." + arcBand +
                    " > 0 and ($shallow.shallow == 0 || $shallow.shallow == 1) ? $day." + arcBand + " : NaN";
            bdDay.type = ProductData.TYPESTRING_FLOAT32;
            bdDay.noDataValue = Double.NaN;

            BandMathsOp.BandDescriptor dbNight = new BandMathsOp.BandDescriptor();
            dbNight.name = "lswt_n_mean";
//            dbNight.expression = "$night." + arcBand + " > 0 and $shallow.shallow == 0 ? $night." + arcBand + " : NaN";
            dbNight.expression = "$night." + arcBand +
                    " > 0 and ($shallow.shallow == 0 || $shallow.shallow == 1) ? $night." + arcBand + " : NaN";
            dbNight.type = ProductData.TYPESTRING_FLOAT32;
            dbNight.noDataValue = Double.NaN;

            BandMathsOp bandMathsOp = new BandMathsOp();
            bandMathsOp.setParameterDefaultValues();
            bandMathsOp.setSourceProduct("day", arcDayCollocated);
            bandMathsOp.setSourceProduct("night", arcNightCollocated);
            bandMathsOp.setSourceProduct("shallow", shallowCollocated);
            bandMathsOp.setTargetBandDescriptors(bdShallow, bdDay, dbNight);
            final Product bandMathProduct = bandMathsOp.getTargetProduct();

            ProductUtils.copyBand("shallow", bandMathProduct, product, true);
            ProductUtils.copyBand("lswt_d_mean", bandMathProduct, product, true);
            ProductUtils.copyBand("lswt_n_mean", bandMathProduct, product, true);

        } else {
            ProductUtils.copyBand("shallow", shallowCollocated, product, true);

            Band band = product.addBand("lswt_d_mean", "NaN");
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            band = product.addBand("lswt_n_mean", "NaN");
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
    }

    private Product collocate(Product masterProduct, Product slaveProduct) {
        CollocateOp collocateOp = new CollocateOp();
        collocateOp.setParameterDefaultValues();
        collocateOp.setRenameMasterComponents(true);
        collocateOp.setRenameSlaveComponents(false);
        collocateOp.setMasterProduct(masterProduct);
        collocateOp.setSlaveProduct(slaveProduct);
        return collocateOp.getTargetProduct();
    }


    public static class Config extends ProductCustomizerConfig {
        @Parameter(defaultValue = "true")
        private Boolean writeNumObs;
        @Parameter(defaultValue = "true")
        private Boolean writeNumPasses;

        @Parameter()
        private Product arcDayProduct;
        @Parameter()
        private Product arcNightProduct;
        @Parameter()
        private String arcBand;

        @Parameter()
        private Product shallowProduct;
    }

    public static class Descriptor implements ProductCustomizerDescriptor {

        @Override
        public ProductCustomizer createProductCustomizer(ProductCustomizerConfig pcc) {
            Config config = (Config) pcc;

            boolean writeNumObs = config.writeNumObs != null ? config.writeNumObs : true;
            boolean writeNumPasses = config.writeNumPasses != null ? config.writeNumPasses : true;

            MonthlyProductCustomizer productCustomizer = new MonthlyProductCustomizer(writeNumObs, writeNumPasses, config.shallowProduct);
            if (config.arcDayProduct != null && config.arcNightProduct != null && config.arcBand != null) {
                productCustomizer.setArcData(config.arcDayProduct, config.arcNightProduct, config.arcBand);
            }
            return productCustomizer;
        }

        @Override
        public String getName() {
            return "MonthlyProductDiversity";
        }

        @Override
        public ProductCustomizerConfig createConfig() {
            return new Config();
        }
    }
}
