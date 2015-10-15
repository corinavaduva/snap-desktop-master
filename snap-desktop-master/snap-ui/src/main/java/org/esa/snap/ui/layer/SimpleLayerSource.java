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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;

/**
 * This layer source uses the given layer type to construct new layer.
 * <p>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Marco Peters
 * @version $ Revision $ $ Date $
 * @since BEAM 4.6
 */
public class SimpleLayerSource implements LayerSource {

    private LayerType layerType;

    public SimpleLayerSource(LayerType layerType) {
        this.layerType = layerType;
    }

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return layerType.isValidFor(pageContext.getLayerContext());
    }

    @Override
    public boolean hasFirstPage() {
        return false;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return null;
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        LayerContext layerCtx = pageContext.getLayerContext();

        Layer layer = layerType.createLayer(layerCtx, new PropertyContainer());
        if (layer != null) {
            layerCtx.getRootLayer().getChildren().add(layer);
            return true;
        }
        return false;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}
