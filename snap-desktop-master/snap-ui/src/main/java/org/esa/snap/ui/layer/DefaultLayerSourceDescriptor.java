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

package org.esa.snap.ui.layer;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;

/**
 * The {@code DefaultLayerSourceDescriptor} provides metadata and
 * a factory method for a {@link LayerSource}.
 * <p>
 * Instances of this class are created by reading the extension configuration of
 * the extension point {@code "layerSources"} in the {@code module.xml}.
 * </p>
 * Example 1:
 * <pre>
 *    &lt;extension point="beam-visat-rcp:layerSources"&gt;
 *      &lt;layerSource&gt;
 *          &lt;id&gt;shapefile-layer-source&lt;/id&gt;
 *          &lt;name&gt;ESRI Shapefile&lt;/name&gt;
 *          &lt;description&gt;Displays shapes from an ESRI Shapefile&lt;/description&gt;
 *          &lt;class&gt;org.esa.snap.visat.toolviews.layermanager.layersrc.shapefile.ShapefileLayerSource&lt;/class&gt;
 *      &lt;/layerSource&gt;
 *    &lt;/extension&gt;
 * </pre>
 * Example 2:
 * <pre>
 *    &lt;extension point="beam-visat-rcp:layerSources"&gt;
 *      &lt;layerSource&gt;
 *          &lt;id&gt;bluemarble-layer-source&lt;/id&gt;
 *          &lt;name&gt;NASA Blue Marble;/name&gt;
 *          &lt;description&gt;Adds NASA Blue Marble image layer to the background.&lt;/description&gt;
 *          &lt;layerType&gt;org.esa.snap.worldmap.BlueMarbleLayerType&lt;/class&gt;
 *      &lt;/layerSource&gt;
 *    &lt;/extension&gt;
 * </pre>
 * <p>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerSourceDescriptor implements LayerSourceDescriptor {

    private final String id;
    private final String name;
    private final String description;
    private final Class<? extends LayerSource> layerSourceClass;
    private final String layerTypeClassName;
    private LayerType layerType;

    public DefaultLayerSourceDescriptor(String id, String name, String description,
                                        Class<? extends LayerSource> layerSourceClass) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.layerSourceClass = layerSourceClass;
        this.layerTypeClassName = null;
    }

    public DefaultLayerSourceDescriptor(String id, String name, String description,
                                        String layerTypeClassName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.layerTypeClassName = layerTypeClassName;
        this.layerSourceClass = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public LayerSource createLayerSource() {
        if (layerSourceClass == null) {
            return new SimpleLayerSource(getLayerType());
        }
        try {
            return layerSourceClass.newInstance();
        } catch (Exception e) {
            String message = String.format("Could not create instance of class [%s]", layerSourceClass.getName());
            throw new IllegalStateException(message, e);
        }
    }

    @Override
    public synchronized LayerType getLayerType() {
        if (layerTypeClassName == null) {
            return null;
        }
        if (layerType == null) {
            try {
                return LayerTypeRegistry.getLayerType(layerTypeClassName);
            } catch (Exception e) {
                String message = String.format("Could not create instance of class [%s]", layerTypeClassName);
                throw new IllegalStateException(message, e);
            }
        }
        return layerType;
    }


}
