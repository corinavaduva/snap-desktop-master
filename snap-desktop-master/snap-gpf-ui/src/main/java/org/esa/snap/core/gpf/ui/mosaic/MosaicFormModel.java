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

package org.esa.snap.core.gpf.ui.mosaic;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.common.MosaicOp;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.BoundsInputPanel;
import org.esa.snap.ui.WorldMapPaneDataModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 * @author Ralf Quast
 * @since BEAM 4.7
 */
class MosaicFormModel {

    public static final String PROPERTY_UPDATE_PRODUCT = "updateProduct";
    public static final String PROPERTY_UPDATE_MODE = "updateMode";
    public static final String PROPERTY_SHOW_SOURCE_PRODUCTS = "showSourceProducts";
    public static final String PROPERTY_ELEVATION_MODEL_NAME = "elevationModelName";
    public static final String PROPERTY_ORTHORECTIFY = "orthorectify";
    public static final String PROPERTY_WEST_BOUND = "westBound";
    public static final String PROPERTY_NORTH_BOUND = "northBound";
    public static final String PROPERTY_EAST_BOUND = "eastBound";
    public static final String PROPERTY_SOUTH_BOUND = "southBound";
    public static final String PROPERTY_CRS = "crs";
    public static final String PROPERTY_PIXEL_SIZE_X = "pixelSizeX";
    public static final String PROPERTY_PIXEL_SIZE_Y = "pixelSizeY";

    private final PropertySet container;
    private final Map<String, Object> parameterMap = new HashMap<>();
    private final Map<File, Product> sourceProductMap = Collections.synchronizedMap(new HashMap<File, Product>());
    private final WorldMapPaneDataModel worldMapModel = new WorldMapPaneDataModel();

    MosaicFormModel() {
        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic", parameterMap);
        addTransientProperty(PROPERTY_UPDATE_PRODUCT, Product.class);
        addTransientProperty(PROPERTY_UPDATE_MODE, Boolean.class);
        addTransientProperty(PROPERTY_SHOW_SOURCE_PRODUCTS, Boolean.class);

        container.setDefaultValues();
        container.setValue(PROPERTY_UPDATE_MODE, false);
        container.setValue(PROPERTY_SHOW_SOURCE_PRODUCTS, false);
        container.addPropertyChangeListener(PROPERTY_SHOW_SOURCE_PRODUCTS, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    final Collection<Product> products = sourceProductMap.values();
                    worldMapModel.setProducts(products.toArray(new Product[products.size()]));
                } else {
                    worldMapModel.setProducts(null);
                }
            }
        });
    }

    private void addTransientProperty(String propertyName, Class<?> propertyType) {
        PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, propertyType);
        descriptor.setTransient(true);
        container.addProperty(new Property(descriptor,
                                           new MapEntryAccessor(parameterMap, propertyName)));
    }

    void setSourceProducts(File[] files) throws IOException {
        final List<File> fileList = Arrays.asList(files);
        final Iterator<Map.Entry<File, Product>> iterator = sourceProductMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<File, Product> entry = iterator.next();
            if (!fileList.contains(entry.getKey())) {
                final Product product = entry.getValue();
                worldMapModel.removeProduct(product);
                iterator.remove();
                product.dispose();
            }
        }
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            Product product = sourceProductMap.get(file);
            if (product == null) {
                product = ProductIO.readProduct(file);
                sourceProductMap.put(file, product);
                if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_SHOW_SOURCE_PRODUCTS))) {
                    worldMapModel.addProduct(product);
                }
            }
            final int refNo = i + 1;
            if (product.getRefNo() != refNo) {
                product.resetRefNo();
                product.setRefNo(refNo);
            }
        }
    }

    Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    Map<String, Product> getSourceProductMap() {
        final HashMap<String, Product> map = new HashMap<>(sourceProductMap.size());
        for (final Product product : sourceProductMap.values()) {
            map.put(GPF.SOURCE_PRODUCT_FIELD_NAME + product.getRefNo(), product);
        }
        if (Boolean.TRUE.equals(container.getValue(PROPERTY_UPDATE_MODE))) {
            final Product updateProduct = getUpdateProduct();
            if (updateProduct != null) {
                map.put(PROPERTY_UPDATE_PRODUCT, updateProduct);
            }
        }
        return map;
    }

    boolean isUpdateMode() {
        return Boolean.TRUE.equals(getPropertyValue(PROPERTY_UPDATE_MODE));
    }

    Product getUpdateProduct() {
        final Object value = getPropertyValue(PROPERTY_UPDATE_PRODUCT);
        if (value instanceof Product) {
            return (Product) value;
        }

        return null;
    }

    void setUpdateProduct(Product product) {
        setPropertyValue(PROPERTY_UPDATE_PRODUCT, product);
        if (product != null && product.getSceneGeoCoding() != null && product.getSceneGeoCoding().getMapCRS() != null) {
            setTargetCRS(product.getSceneGeoCoding().getMapCRS().toWKT());
        }
    }

    MosaicOp.Variable[] getVariables() {
        return (MosaicOp.Variable[]) getPropertyValue("variables");
    }

    MosaicOp.Condition[] getConditions() {
        return (MosaicOp.Condition[]) getPropertyValue("conditions");
    }

    PropertySet getPropertySet() {
        return container;
    }

    public Object getPropertyValue(String propertyName) {
        return container.getValue(propertyName);
    }

    public void setPropertyValue(String propertyName, Object value) {
        container.setValue(propertyName, value);
    }

    public Product getReferenceProduct() throws IOException {
        for (Product product : sourceProductMap.values()) {
            if (product.getRefNo() == 1) {
                return product;
            }
        }
        return null;
    }

    public Product getBoundaryProduct() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = getTargetCRS();
        if (mapCRS != null) {
            final ReferencedEnvelope envelope = getTargetEnvelope();
            final Envelope mapEnvelope = envelope.transform(mapCRS, true);

            final double pixelSizeX = (Double) getPropertyValue(PROPERTY_PIXEL_SIZE_X);
            final double pixelSizeY = (Double) getPropertyValue(PROPERTY_PIXEL_SIZE_Y);
            final int w = MathUtils.floorInt(mapEnvelope.getSpan(0) / pixelSizeX);
            final int h = MathUtils.floorInt(mapEnvelope.getSpan(1) / pixelSizeY);

            final Product product = new Product("mosaic", "MosaicBounds", w, h);
            final GeoCoding geoCoding = new CrsGeoCoding(mapCRS,
                                                         w, h,
                                                         mapEnvelope.getMinimum(0),
                                                         mapEnvelope.getMaximum(1),
                                                         pixelSizeX, pixelSizeY);
            product.setSceneGeoCoding(geoCoding);

            return product;
        }
        return null;
    }

    void setTargetCRS(String crs) {
        setPropertyValue("crs", crs);
    }

    CoordinateReferenceSystem getTargetCRS() throws FactoryException {
        final String crs = (String) getPropertyValue("crs");
        if (crs == null) {
            return null;
        }
        try {
            return CRS.parseWKT(crs);
        } catch (FactoryException ignored) {
            return CRS.decode(crs, true);
        }
    }

    ReferencedEnvelope getTargetEnvelope() {
        final double west = (Double) getPropertyValue(BoundsInputPanel.PROPERTY_WEST_BOUND);
        final double north = (Double) getPropertyValue(BoundsInputPanel.PROPERTY_NORTH_BOUND);
        final double east = (Double) getPropertyValue(BoundsInputPanel.PROPERTY_EAST_BOUND);
        final double south = (Double) getPropertyValue(BoundsInputPanel.PROPERTY_SOUTH_BOUND);

        final Rectangle2D bounds = new Rectangle2D.Double();
        bounds.setFrameFromDiagonal(west, north, east, south);

        return new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
    }

    public WorldMapPaneDataModel getWorldMapModel() {
        return worldMapModel;
    }

    public String getElevationModelName() {
        boolean orthorectify = (boolean) getPropertyValue(PROPERTY_ORTHORECTIFY);
        if (orthorectify) {
            return (String) getPropertyValue(PROPERTY_ELEVATION_MODEL_NAME);
        }
        return null;
    }
}
