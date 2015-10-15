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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Array;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractLayerConfigurationPersistencyTest {

    private static ProductManager productManager;
    private final LayerType layerType;

    protected AbstractLayerConfigurationPersistencyTest(LayerType layerType) {
        assertNotNull(layerType);
        this.layerType = layerType;
    }

    @BeforeClass
    public static void setupClass() {
        final Product product = createTestProduct("P", "T");
        addVirtualBand(product, "V", ProductData.TYPE_INT32, "42");
        productManager = new ProductManager();
        productManager.addProduct(product);
    }

    protected static ProductManager getProductManager() {
        return productManager;
    }

    protected static Product createTestProduct(String name, String type) {
        Product product = new Product(name, type, 10, 10);
        product.setFileLocation(new File(String.format("out/%s.dim", name)));

        return product;
    }

    protected static RasterDataNode addVirtualBand(Product product, String bandName, int dataType, String expression) {
        final Band band = new VirtualBand(bandName, dataType, 10, 10, expression);
        product.addBand(band);

        return band;
    }

    @Test
    public void testLayerConfigurationPersistency() throws Exception {
        final Layer layer = createLayer(layerType);

        final SessionDomConverter domConverter = new SessionDomConverter(getProductManager());
        final DomElement originalDomElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(layer.getConfiguration(), originalDomElement);
        //System.out.println(originalDomElement.toXml());
        
        PropertySet template = layer.getLayerType().createLayerConfig(null);
        final PropertyContainer restoredConfiguration = (PropertyContainer) domConverter.convertDomToValue(originalDomElement, template);
        compareConfigurations(layer.getConfiguration(), restoredConfiguration);

        final DomElement restoredDomElement = new DefaultDomElement("configuration");
        domConverter.convertValueToDom(restoredConfiguration, restoredDomElement);

//        assertEquals(originalDomElement.toXml(), restoredDomElement.toXml());
    }

    protected abstract Layer createLayer(LayerType layerType) throws Exception;

    private static void compareConfigurations(PropertySet originalConfiguration,
                                              PropertySet restoredConfiguration) {
        for (final Property originalModel : originalConfiguration.getProperties()) {
            final PropertyDescriptor originalDescriptor = originalModel.getDescriptor();
            final Property restoredModel = restoredConfiguration.getProperty(originalDescriptor.getName());
            final PropertyDescriptor restoredDescriptor = restoredModel.getDescriptor();

            assertNotNull(restoredModel);
            assertSame(originalDescriptor.getName(), restoredDescriptor.getName());
            assertSame(originalDescriptor.getType(), restoredDescriptor.getType());

            if (originalDescriptor.isTransient()) {
                assertEquals(originalDescriptor.isTransient(), restoredDescriptor.isTransient());
            } else {
                final Object originalValue = originalModel.getValue();
                final Object restoredValue = restoredModel.getValue();
                assertSame(originalValue.getClass(), restoredValue.getClass());

                if (originalValue.getClass().isArray()) {
                    final int originalLength = Array.getLength(originalValue);
                    final int restoredLength = Array.getLength(restoredValue);

                    assertEquals(originalLength, restoredLength);
                    for (int i = 0; i < restoredLength; i++) {
                        assertEquals(Array.get(originalValue, i), Array.get(restoredValue, i));
                    }
                }
            }
        }
    }

}
