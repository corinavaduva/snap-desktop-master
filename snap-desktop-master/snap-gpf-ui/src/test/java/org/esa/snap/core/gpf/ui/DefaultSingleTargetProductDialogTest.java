/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.converters.GeneralExpressionConverter;
import org.esa.snap.ui.DefaultAppContext;

import javax.swing.UIManager;


public class DefaultSingleTargetProductDialogTest {

    private static final TestOp.Spi SPI = new TestOp.Spi();

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(SPI);

        try {
            DefaultAppContext app = new DefaultAppContext("Killer App");
            app.getApplicationWindow().setSize(200, 200);

            final DefaultSingleTargetProductDialog dialog = (DefaultSingleTargetProductDialog) DefaultSingleTargetProductDialog.createDefaultDialog(
                    TestOp.Spi.class.getName(), app);
            dialog.setTargetProductNameSuffix("_test");
            dialog.getJDialog().setTitle("TestOp GUI");
            dialog.show();
        } finally {
            GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(SPI);
        }
    }

    public static class TestOp extends Operator {
        @SourceProduct
        Product masterProduct;
        @SourceProduct
        Product slaveProduct;
        @TargetProduct
        Product target;
        @Parameter(defaultValue = "true")
        boolean copyTiePointGrids;
        @Parameter(defaultValue = "false")
        Boolean copyMetadata;
        @Parameter(interval = "[-1,+1]", defaultValue = "-0.1")
        double threshold;
        @Parameter(valueSet = {"ME-203", "ME-208", "ME-002"}, defaultValue = "ME-208")
        String method;
        @Parameter(description = "Mask expression", label = "Mask expression", converter = GeneralExpressionConverter.class)
        String validExpression;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("N", "T", 16, 16);
            product.addBand("B1", ProductData.TYPE_FLOAT32);
            product.addBand("B2", ProductData.TYPE_FLOAT32);
            product.setPreferredTileSize(4, 4);
            //System.out.println("product = " + product);
            target = product;
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(TestOp.class);
            }
        }
    }
}
