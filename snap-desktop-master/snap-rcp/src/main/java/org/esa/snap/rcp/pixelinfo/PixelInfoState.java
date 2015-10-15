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

package org.esa.snap.rcp.pixelinfo;

import org.esa.snap.ui.product.ProductSceneView;

class PixelInfoState {
    static final PixelInfoState INVALID = new PixelInfoState(null, -1, -1, -1, false);

    final ProductSceneView view;
    final int pixelX;
    final int pixelY;
    final int level;
    final boolean pixelPosValid;

    PixelInfoState(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        this.view = view;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.level = level;
        this.pixelPosValid = pixelPosValid;
    }

    boolean equals(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        return this.view == view
                && this.pixelX == pixelX
                && this.pixelY == pixelY
                && this.level == level
                && this.pixelPosValid == pixelPosValid;
    }
}
