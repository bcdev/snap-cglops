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


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.MergeOp;
import org.esa.snap.core.util.ProductUtils;

/**
 * Extends the basic Merging Operator with the option
 * to get a PixelGeocoing from a dedicated source product
 *
 * @author marcoz
 */
@OperatorMetadata(alias = "LakeMerge",
        description = "Allows copying raster data from any number of source products to a specified 'master'" +
                " product. Enhanced for lakes.",
        authors = "Marco Zuehlke",
        version = "1.0",
        copyright = "(c) 2015 by Brockmann Consult",
        internal = false)
public class LakeMergeOp extends Operator {

    @SourceProduct(description = "The master product, which receives nodes from subsequently provided products.")
    private Product masterProduct;

    @SourceProduct(description = "The geocoding product. This product provides the 'corrected_latitude' and 'corrected_longitude' for the PixelGeoCoding.",
                   optional = true)
    private Product geoProduct;

    @SourceProducts(description = "The products to be merged into the master product.")
    private Product[] sourceProducts;

    @TargetProduct
    private Product targetProduct;

    @Parameter(itemAlias = "include",
            description = "Defines nodes to be included in the master product. If no includes are provided, all" +
                    " nodes are copied.")
    private MergeOp.NodeDescriptor[] includes;

    @Override
    public void initialize() throws OperatorException {
        MergeOp mergeOp = new MergeOp();
        mergeOp.setParameterDefaultValues();
        mergeOp.setSourceProduct("masterProduct", masterProduct);
        for (int i = 0; i < sourceProducts.length; i++) {
            String productName = "sourceProducts" + i;
            mergeOp.setSourceProduct(productName, getSourceProduct(productName));
        }

        mergeOp.setParameter("includes", includes);
        mergeOp.setParameter("geographicError", 0.01f);
        Product targetProduct = mergeOp.getTargetProduct();

        if (geoProduct != null) {
            Band lat = ProductUtils.copyBand("corrected_latitude", geoProduct, targetProduct, true);
            Band lon = ProductUtils.copyBand("corrected_longitude", geoProduct, targetProduct, true);
            GeoCoding pixelGeocoding = GeoCodingFactory.createPixelGeoCoding(lat, lon, "NOT l1_flags.INVALID", 6);
            targetProduct.setSceneGeoCoding(pixelGeocoding);
        }


        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LakeMergeOp.class);
        }
    }
}
