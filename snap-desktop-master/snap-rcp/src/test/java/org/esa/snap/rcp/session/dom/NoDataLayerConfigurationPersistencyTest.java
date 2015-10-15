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

package org.esa.snap.rcp.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.layer.NoDataLayerType;
import org.junit.After;
import org.junit.Before;

import java.awt.Color;

public class NoDataLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    private Product product;
    private RasterDataNode raster;

    public NoDataLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(NoDataLayerType.class));
    }

    @Before
    public void setup() {
        product = createTestProduct("Test", "Test");
        raster = addVirtualBand(product, "virtualBand", ProductData.TYPE_INT32, "17");

        getProductManager().addProduct(product);
    }

    @After
    public void tearDown() {
        getProductManager().removeProduct(product);
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue("raster", raster);
        configuration.setValue("color", new Color(17, 11, 67));
        return layerType.createLayer(null, configuration);
    }
}
