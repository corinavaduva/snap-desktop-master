/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.productlibrary.rcp.toolviews;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.engine_utilities.db.GeoPosList;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.NestWorldMapPane;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.NestWorldMapPaneDataModel;
import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseQueryListener;

import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**

 */
public class WorldMapUI {

    private final NestWorldMapPaneDataModel worldMapDataModel;
    private final NestWorldMapPane worlMapPane;

    private final List<DatabaseQueryListener> listenerList = new ArrayList<>(1);

    public WorldMapUI() {

        worldMapDataModel = new NestWorldMapPaneDataModel();
        worlMapPane = new NestWorldMapPane(worldMapDataModel);
        worlMapPane.getLayerCanvas().addMouseListener(new MouseHandler());
    }

    /**
     * Adds a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be added.
     */
    public void addListener(final DatabaseQueryListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public GeoPos[] getSelectionBox() {
        return worldMapDataModel.getSelectionBox();
    }

    public void setSelectionStart(final float lat, final float lon) {
        worldMapDataModel.setSelectionBoxStart(lat, lon);
    }

    public void setSelectionEnd(final float lat, final float lon) {
        worldMapDataModel.setSelectionBoxEnd(lat, lon);
    }

    /**
     * Removes a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be removed.
     */
    public void removeListener(final DatabaseQueryListener listener) {
        listenerList.remove(listener);
    }

    private void notifyQuery() {
        for (final DatabaseQueryListener listener : listenerList) {
            listener.notifyNewMapSelectionAvailable();
        }
    }

    public NestWorldMapPane getWorlMapPane() {
        return worlMapPane;
    }

    public void setAOIList(final GeoPosList[] aoiList) {
        final GeoPos[][] geoBoundaries = new GeoPos[aoiList.length][4];
        int i = 0;
        for (GeoPosList aoi : aoiList) {
            geoBoundaries[i++] = aoi.getPoints();
        }

        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    public void setSelectedAOIList(final GeoPosList[] selectedAOIList) {
        final GeoPos[][] geoBoundaries = new GeoPos[selectedAOIList.length][4];
        int i = 0;
        for (GeoPosList aoi : selectedAOIList) {
            geoBoundaries[i++] = aoi.getPoints();
        }

        worldMapDataModel.setSelectedGeoBoundaries(geoBoundaries);
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        if (productEntryList == null) return;
        final GeoPos[][] geoBoundaries = new GeoPos[productEntryList.length][4];
        int i = 0;
        for (ProductEntry entry : productEntryList) {
            geoBoundaries[i++] = entry.getGeoBoundary();
        }

        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    public void setSelectedProductEntryList(final ProductEntry[] selectedProductEntryList) {

        if (selectedProductEntryList == null) {
            worldMapDataModel.setSelectedGeoBoundaries(null);
            return;
        }
        final GeoPos[][] geoBoundaries = new GeoPos[selectedProductEntryList.length][4];
        int i = 0;
        for (ProductEntry entry : selectedProductEntryList) {
            geoBoundaries[i++] = entry.getGeoBoundary();
        }

        worldMapDataModel.setSelectedGeoBoundaries(geoBoundaries);
    }

    public NestWorldMapPaneDataModel getModel() {
        return worldMapDataModel;
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                notifyQuery();
            }
        }
    }
}
