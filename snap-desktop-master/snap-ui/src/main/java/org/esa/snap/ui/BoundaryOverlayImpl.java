package org.esa.snap.ui;

import com.bc.ceres.grender.Rendering;
import org.esa.snap.core.datamodel.Product;

/**
 * This class extends a {@link BoundaryOverlay} by the ability to draw a selected product.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 */
public class BoundaryOverlayImpl extends BoundaryOverlay {

    protected BoundaryOverlayImpl(WorldMapPaneDataModel dataModel) {
        super(dataModel);
    }

    @Override
    protected void handleSelectedProduct(Rendering rendering, Product selectedProduct) {
        if (selectedProduct != null) {
            drawProduct(rendering.getGraphics(), selectedProduct, true);
        }
    }

}
