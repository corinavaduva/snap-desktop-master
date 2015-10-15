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

package org.esa.snap.rcp.placemark.pin;

import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.rcp.placemark.PlacemarkUtils;
import org.esa.snap.ui.product.AbstractPlacemarkTableModel;

import java.awt.Color;

public class PinTableModel extends AbstractPlacemarkTableModel {

    private final int xIndex = 0;
    private final int yIndex = 1;
    private final int lonIndex = 2;
    private final int latIndex = 3;
    private final int colorIndex = 4;
    private final int labelIndex = 5;

    public PinTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        super(placemarkDescriptor, product, selectedBands, selectedGrids);
    }

    @Override
    public String[] getStandardColumnNames() {
        return new String[]{"X", "Y", "Lon", "Lat", "Color", "Label"};
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (getProduct().getSceneGeoCoding() == null && (columnIndex == lonIndex || columnIndex == latIndex)) {
            return false;
        }
        return columnIndex < getStandardColumnNames().length;
    }

    @Override
    protected Object getStandardColumnValueAt(int rowIndex, int columnIndex) {
        Assert.notNull(getProduct());
        final Placemark placemark = getPlacemarkDescriptor().getPlacemarkGroup(getProduct()).get(rowIndex);
        double x = Double.NaN;
        double y = Double.NaN;

        final PixelPos pixelPos = placemark.getPixelPos();
        if (pixelPos != null) {
            x = pixelPos.x;
            y = pixelPos.y;
        }

        double lon = Double.NaN;
        double lat = Double.NaN;

        final GeoPos geoPos = placemark.getGeoPos();
        if (geoPos != null) {
            lon = geoPos.lon;
            lat = geoPos.lat;
        }

        switch (columnIndex) {
            case xIndex:
                return x;
            case yIndex:
                return y;
            case lonIndex:
                return lon;
            case latIndex:
                return lat;
            case colorIndex:
                return PlacemarkUtils.getPlacemarkColor(placemark);
            case labelIndex:
                return placemark.getLabel();
            default:
                return "";
        }
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case xIndex:
                return Double.class;
            case yIndex:
                return Double.class;
            case lonIndex:
                return Double.class;
            case latIndex:
                return Double.class;
            case colorIndex:
                return Color.class;
            case labelIndex:
                return String.class;
        }
        return Object.class;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == colorIndex) {
            final String colorName = DefaultFigureStyle.FILL_COLOR.getName();
            final Placemark pin = getPlacemarkAt(rowIndex);
            final String styleCss = pin.getStyleCss();
            FigureStyle style = new DefaultFigureStyle();
            style.fromCssString(styleCss);
            style.setValue(colorName, value);
            pin.setStyleCss(style.toCssString());
        } else {
            super.setValueAt(value, rowIndex, columnIndex);
        }
    }
}
