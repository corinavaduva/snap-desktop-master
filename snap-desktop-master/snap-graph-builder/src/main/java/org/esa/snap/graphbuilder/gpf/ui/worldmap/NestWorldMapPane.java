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
package org.esa.snap.graphbuilder.gpf.ui.worldmap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glayer.swing.WakefulComponent;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.ButtonOverlayControl;
import org.esa.snap.ui.UIUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This class displays a world map specified by the {@link NestWorldMapPaneDataModel}.
 *
 * @author Marco Peters
 */
public class NestWorldMapPane extends JPanel {

    private LayerCanvas layerCanvas;
    private Layer worldMapLayer;
    private final NestWorldMapPaneDataModel dataModel;
    private boolean navControlShown;
    private WakefulComponent navControlWrapper;

    private final static Color transWhiteColor = new Color(255, 255, 255, 5);
    private final static Color borderWhiteColor = new Color(255, 255, 255, 100);
    private final static Color transRedColor = new Color(255, 0, 0, 30);
    private final static Color borderRedColor = new Color(255, 0, 0, 100);

    private final static Color selectionFillColor = new Color(255, 255, 0, 70);
    private final static Color selectionBorderColor = new Color(255, 255, 0, 255);

    public NestWorldMapPane(NestWorldMapPaneDataModel dataModel) {
        this.dataModel = dataModel;
        try {
            layerCanvas = new LayerCanvas();
            layerCanvas.getModel().getViewport().setModelYAxisDown(false);
            installLayerCanvasNavigation(layerCanvas, dataModel);
            layerCanvas.addOverlay(new BoundaryOverlay());
            final Layer rootLayer = layerCanvas.getLayer();

            final Dimension dimension = new Dimension(400, 150);
            final Viewport viewport = layerCanvas.getViewport();
            viewport.setViewBounds(new Rectangle(dimension));

            setPreferredSize(dimension);
            setSize(dimension);
            setLayout(new BorderLayout());
            add(layerCanvas, BorderLayout.CENTER);

            dataModel.addModelChangeListener(new ModelChangeListener());

            worldMapLayer = dataModel.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
            layerCanvas.getLayer().getChildren().add(worldMapLayer);
            layerCanvas.getViewport().zoom(worldMapLayer.getModelBounds());

            setNavControlVisible(true);
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Error in worldmap initialization", e);
        }

    }

    public LayerCanvas getLayerCanvas() {
        return layerCanvas;
    }

    @Override
    public void doLayout() {
        if (navControlShown && navControlWrapper != null) {
            navControlWrapper.setLocation(getWidth() - navControlWrapper.getWidth() - 4, 4);
        }
        super.doLayout();
    }

    public Product getSelectedProduct() {
        return dataModel.getSelectedProduct();
    }

    public Product[] getProducts() {
        return dataModel.getProducts();
    }

    public float getScale() {
        return (float) layerCanvas.getViewport().getZoomFactor();
    }

    public void setScale(final float scale) {
        if (getScale() != scale) {
            final float oldValue = getScale();
            layerCanvas.getViewport().setZoomFactor(scale);
            firePropertyChange("scale", oldValue, scale);
        }
    }

    public void zoomToProduct(Product product) {
        final GeoPos[][] selGeoBoundaries = dataModel.getSelectedGeoBoundaries();
        if ((product == null || product.getSceneGeoCoding() == null) && selGeoBoundaries.length == 0) {
            return;
        }

        //NESTMOD
        final GeneralPath[] generalPaths;
        if (product != null && product.getSceneGeoCoding() != null) {
            generalPaths = getGeoBoundaryPaths(product);
        } else {
            final ArrayList<GeneralPath> pathList = assemblePathList(selGeoBoundaries[0]);
            generalPaths = pathList.toArray(new GeneralPath[pathList.size()]);
        }

        Rectangle2D modelArea = new Rectangle2D.Double();
        final Viewport viewport = layerCanvas.getViewport();
        for (GeneralPath generalPath : generalPaths) {
            final Rectangle2D rectangle2D = generalPath.getBounds2D();
            if (modelArea.isEmpty()) {
                if (!viewport.isModelYAxisDown()) {
                    modelArea.setFrame(rectangle2D.getX(), rectangle2D.getMaxY(),
                            rectangle2D.getWidth(), rectangle2D.getHeight());
                }
                modelArea = rectangle2D;
            } else {
                modelArea.add(rectangle2D);
            }
        }
        Rectangle2D modelBounds = modelArea.getBounds2D();
        modelBounds.setFrame(modelBounds.getX() - 2, modelBounds.getY() - 2,
                modelBounds.getWidth() + 4, modelBounds.getHeight() + 4);

        modelBounds = cropToMaxModelBounds(modelBounds);

        viewport.zoom(modelBounds);
    }

    /**
     * None API. Don't use this method!
     *
     * @param navControlShown true, if this canvas uses a navigation control.
     */
    public void setNavControlVisible(boolean navControlShown) {
        boolean oldValue = this.navControlShown;
        if (oldValue != navControlShown) {
            if (navControlShown) {
                final ButtonOverlayControl navControl = new ButtonOverlayControl(new ZoomAllAction(),
                        new ZoomToSelectedAction());//, new ZoomToLocationAction());
                navControlWrapper = new WakefulComponent(navControl);
                navControlWrapper.setMinAlpha(0.5f);
                layerCanvas.add(navControlWrapper);
            } else {
                layerCanvas.remove(navControlWrapper);
                navControlWrapper = null;
            }
            validate();
            this.navControlShown = navControlShown;
        }
    }

    private void updateUiState(PropertyChangeEvent evt) {
        if (NestWorldMapPaneDataModel.PROPERTY_LAYER.equals(evt.getPropertyName())) {
            exchangeWorldMapLayer();
        }
        if (NestWorldMapPaneDataModel.PROPERTY_PRODUCTS.equals(evt.getPropertyName())) {
            repaint();
        }
        if (NestWorldMapPaneDataModel.PROPERTY_SELECTED_PRODUCT.equals(evt.getPropertyName()) ||
                NestWorldMapPaneDataModel.PROPERTY_AUTO_ZOOM_ENABLED.equals(evt.getPropertyName())) {
            final Product selectedProduct = dataModel.getSelectedProduct();
            if (selectedProduct != null && dataModel.isAutoZommEnabled()) {
                zoomToProduct(selectedProduct);
            } else {
                repaint();
            }
        }
        if (NestWorldMapPaneDataModel.PROPERTY_ADDITIONAL_GEO_BOUNDARIES.equals(evt.getPropertyName()) ||
                NestWorldMapPaneDataModel.PROPERTY_SELECTED_GEO_BOUNDARIES.equals(evt.getPropertyName())) {
            repaint();
        }
    }

    private void exchangeWorldMapLayer() {
        final List<Layer> children = layerCanvas.getLayer().getChildren();
        for (Layer child : children) {
            child.dispose();
        }
        children.clear();
        final Layer rootLayer = layerCanvas.getLayer();
        worldMapLayer = dataModel.getWorldMapLayer(new WorldMapLayerContext(rootLayer));
        children.add(worldMapLayer);
        layerCanvas.getViewport().zoom(worldMapLayer.getModelBounds());
    }

    private Rectangle2D cropToMaxModelBounds(Rectangle2D modelBounds) {
        final Rectangle2D maxModelBounds = worldMapLayer.getModelBounds();
        if (modelBounds.getWidth() >= maxModelBounds.getWidth() - 1 ||
                modelBounds.getHeight() >= maxModelBounds.getHeight() - 1) {
            modelBounds = maxModelBounds;
        }
        return modelBounds;
    }


    public static GeneralPath[] getGeoBoundaryPaths(Product product) {
        final int step = Math.max(16, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 250);
        return ProductUtils.createGeoBoundaryPaths(product, null, step);
    }

    private PixelPos getProductCenter(final Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        PixelPos centerPos = null;
        if (geoCoding != null) {
            final float pixelX = (float) Math.floor(0.5f * product.getSceneRasterWidth()) + 0.5f;
            final float pixelY = (float) Math.floor(0.5f * product.getSceneRasterHeight()) + 0.5f;
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(pixelX, pixelY), null);
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            final Point2D point2D = transform.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), null);
            centerPos = new PixelPos((float) point2D.getX(), (float) point2D.getY());
        }
        return centerPos;
    }

    private static void installLayerCanvasNavigation(final LayerCanvas layerCanvas, final NestWorldMapPaneDataModel dataModel) {
        MouseHandler mouseHandler = new MouseHandler(layerCanvas, dataModel);
        layerCanvas.addMouseListener(mouseHandler);
        layerCanvas.addMouseMotionListener(mouseHandler);
        layerCanvas.addMouseWheelListener(mouseHandler);
    }

    /**
     * @param bufferedImage is ignored
     * @deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    @SuppressWarnings({"UnusedDeclaration"})
    public void setWorldMapImage(java.awt.image.BufferedImage bufferedImage) {
    }

    private class ModelChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateUiState(evt);
        }
    }

    public static class MouseHandler extends MouseInputAdapter {

        private final LayerCanvas layerCanvas;
        private final NestWorldMapPaneDataModel dataModel;
        private Point p0;
        private Point.Float selectionStart = new Point.Float();
        private Point.Float selectionEnd = new Point.Float();
        private boolean leftButtonDown = false;

        private MouseHandler(final LayerCanvas layerCanvas, final NestWorldMapPaneDataModel dataModel) {
            this.layerCanvas = layerCanvas;
            this.dataModel = dataModel;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                leftButtonDown = true;
                final AffineTransform viewToModelTransform = layerCanvas.getViewport().getViewToModelTransform();
                viewToModelTransform.transform(e.getPoint(), selectionStart);
                dataModel.setSelectionBoxStart(selectionStart.y, selectionStart.x);
                dataModel.setSelectionBoxEnd(selectionStart.y, selectionStart.x);
                layerCanvas.updateUI();
            } else {
                p0 = e.getPoint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                leftButtonDown = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final Point p = e.getPoint();
            if (leftButtonDown) {
                final AffineTransform viewToModelTransform = layerCanvas.getViewport().getViewToModelTransform();
                viewToModelTransform.transform(e.getPoint(), selectionEnd);
                dataModel.setSelectionBoxEnd(selectionEnd.y, selectionEnd.x);
                layerCanvas.updateUI();
            } else if (p0 != null) {
                final double dx = p.x - p0.x;
                final double dy = p.y - p0.y;
                layerCanvas.getViewport().moveViewDelta(dx, dy);
                p0 = p;
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            final int wheelRotation = e.getWheelRotation();
            final double newZoomFactor = layerCanvas.getViewport().getZoomFactor() * FastMath.pow(1.1, wheelRotation);
            layerCanvas.getViewport().setZoomFactor(newZoomFactor);
        }
    }


    private static class WorldMapLayerContext implements LayerContext {

        private final Layer rootLayer;

        private WorldMapLayerContext(Layer rootLayer) {

            this.rootLayer = rootLayer;
        }

        @Override
        public Object getCoordinateReferenceSystem() {
            return DefaultGeographicCRS.WGS84;
        }

        @Override
        public Layer getRootLayer() {
            return rootLayer;
        }
    }

    private class BoundaryOverlay implements LayerCanvas.Overlay {

        @Override
        public void paintOverlay(LayerCanvas canvas, Rendering rendering) {

            for (final GeoPos[] extraGeoBoundary : dataModel.getAdditionalGeoBoundaries()) {
                drawGeoBoundary(rendering.getGraphics(), extraGeoBoundary, null, null,
                        transWhiteColor, borderWhiteColor);
            }

            for (final GeoPos[] selectGeoBoundary : dataModel.getSelectedGeoBoundaries()) {
                drawGeoBoundary(rendering.getGraphics(), selectGeoBoundary, null, null,
                        transRedColor, borderRedColor);
            }

            final Product selectedProduct = dataModel.getSelectedProduct();
            for (final Product product : dataModel.getProducts()) {
                if (product != null && selectedProduct != product) {
                    drawProduct(rendering.getGraphics(), product,
                            transWhiteColor, Color.WHITE);
                }
            }

            if (selectedProduct != null) {
                drawProduct(rendering.getGraphics(), selectedProduct,
                        transWhiteColor, Color.RED);
            }

            drawGeoBoundary(rendering.getGraphics(), dataModel.getSelectionBox(), null, null,
                    selectionFillColor, selectionBorderColor);
        }

        private void drawProduct(final Graphics2D g2d, final Product product,
                                 final Color fillColor, final Color borderColor) {
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            if (geoCoding == null) {
                return;
            }

            GeneralPath[] boundaryPaths = getGeoBoundaryPaths(product);
            final String text = String.valueOf(product.getRefNo());
            final PixelPos textCenter = getProductCenter(product);
            drawGeoBoundary(g2d, boundaryPaths, text, textCenter, fillColor, borderColor);
        }

        private void drawGeoBoundary(final Graphics2D g2d, final GeneralPath[] boundaryPaths,
                                     final String text, final PixelPos textCenter,
                                     final Color fillColor, final Color borderColor) {
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            for (GeneralPath boundaryPath : boundaryPaths) {
                boundaryPath.transform(transform);
                drawPath(g2d, boundaryPath, fillColor, borderColor);
            }

            drawText(g2d, text, textCenter, 0.0f);
        }

        private void drawGeoBoundary(final Graphics2D g2d, final GeoPos[] geoBoundary,
                                     final String text, final PixelPos textCenter,
                                     final Color fillColor, final Color borderColor) {
            ProductUtils.normalizeGeoPolygon(geoBoundary);
            final List<GeneralPath> boundaryPaths = assemblePathList(geoBoundary);
            final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
            for (GeneralPath boundaryPath : boundaryPaths) {
                boundaryPath.transform(transform);
                //drawPath(g2d, boundaryPath, fillColor, borderColor);
                g2d.setColor(fillColor);
                g2d.fill(boundaryPath);
                g2d.setColor(borderColor);
                g2d.draw(boundaryPath);
            }
            drawText(g2d, text, textCenter, 0.0f);
        }

        private void drawPath(Graphics2D g2d, final GeneralPath gp,
                              final Color fillColor, final Color borderColor) {
            g2d.setColor(fillColor);
            g2d.fill(gp);
            g2d.setColor(borderColor);
            g2d.draw(gp);
        }

        private GeneralPath convertToPixelPath(final GeoPos[] geoBoundary) {
            final GeneralPath gp = new GeneralPath();
            for (int i = 0; i < geoBoundary.length; i++) {
                final GeoPos geoPos = geoBoundary[i];
                final AffineTransform m2vTransform = layerCanvas.getViewport().getModelToViewTransform();
                final Point2D viewPos = m2vTransform.transform(new PixelPos.Double(geoPos.lon, geoPos.lat), null);
                if (i == 0) {
                    gp.moveTo(viewPos.getX(), viewPos.getY());
                } else {
                    gp.lineTo(viewPos.getX(), viewPos.getY());
                }
            }
            gp.closePath();
            return gp;
        }

        private void drawText(Graphics2D g2d, final String text, final PixelPos textCenter, final float offsetX) {
            if (text == null || textCenter == null) {
                return;
            }
            g2d = prepareGraphics2D(offsetX, g2d);
            final FontMetrics fontMetrics = g2d.getFontMetrics();
            final Color color = g2d.getColor();
            g2d.setColor(Color.black);

            g2d.drawString(text,
                           (int)textCenter.x - fontMetrics.stringWidth(text) / 2.0f,
                           (int)textCenter.y + fontMetrics.getAscent() / 2.0f);
            g2d.setColor(color);
        }

        private Graphics2D prepareGraphics2D(final float offsetX, Graphics2D g2d) {
            if (offsetX != 0.0f) {
                g2d = (Graphics2D) g2d.create();
                final AffineTransform transform = g2d.getTransform();
                final AffineTransform offsetTrans = new AffineTransform();
                offsetTrans.setToTranslation(+offsetX, 0);
                transform.concatenate(offsetTrans);
                g2d.setTransform(transform);
            }
            return g2d;
        }

    }

    private class ZoomAllAction extends AbstractAction {

        private ZoomAllAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/ZoomAll24.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layerCanvas.getViewport().zoom(worldMapLayer.getModelBounds());
        }
    }

    private class ZoomToSelectedAction extends AbstractAction {

        private ZoomToSelectedAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/ZoomTo24.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            zoomToProduct(getSelectedProduct());
        }
    }

    private class ZoomToLocationAction extends AbstractAction {

        private ZoomToLocationAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("/org/esa/nest/icons/define-location-24.png", this.getClass()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            zoomToProduct(getSelectedProduct());
        }
    }

    public static ArrayList<GeneralPath> assemblePathList(GeoPos[] geoPoints) {
        final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, geoPoints.length + 8);
        final ArrayList<GeneralPath> pathList = new ArrayList<>(16);

        if (geoPoints.length > 1) {
            double lon, lat;
            double minLon=0, maxLon=0;

            boolean first = true;
            for(GeoPos gp : geoPoints) {
                lon = gp.getLon();
                lat = gp.getLat();
                if(first) {
                    minLon = lon;
                    maxLon = lon;
                    path.moveTo(lon, lat);
                    first = false;
                }
                if (lon < minLon) {
                    minLon = lon;
                }
                if (lon > maxLon) {
                    maxLon = lon;
                }
                path.lineTo(lon, lat);
            }
            path.closePath();

            int runIndexMin = (int) Math.floor((minLon + 180) / 360);
            int runIndexMax = (int) Math.floor((maxLon + 180) / 360);

            if (runIndexMin == 0 && runIndexMax == 0) {
                // the path is completely within [-180, 180] longitude
                pathList.add(path);
                return pathList;
            }

            final Area pathArea = new Area(path);
            final GeneralPath pixelPath = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            for (int k = runIndexMin; k <= runIndexMax; k++) {
                final Area currentArea = new Area(new Rectangle2D.Float(k * 360.0f - 180.0f, -90.0f, 360.0f, 180.0f));
                currentArea.intersect(pathArea);
                if (!currentArea.isEmpty()) {
                    pathList.add(areaToPath(currentArea, -k * 360.0, pixelPath));
                }
            }
        }
        return pathList;
    }

    public static GeneralPath areaToPath(final Area negativeArea, final double deltaX, final GeneralPath pixelPath) {

        final float[] floats = new float[6];
        // move to correct rectangle
        final AffineTransform transform = AffineTransform.getTranslateInstance(deltaX, 0.0);
        final PathIterator iterator = negativeArea.getPathIterator(transform);

        while (!iterator.isDone()) {
            final int segmentType = iterator.currentSegment(floats);
            if (segmentType == PathIterator.SEG_LINETO) {
                pixelPath.lineTo(floats[0], floats[1]);
            } else if (segmentType == PathIterator.SEG_MOVETO) {
                pixelPath.moveTo(floats[0], floats[1]);
            } else if (segmentType == PathIterator.SEG_CLOSE) {
                pixelPath.closePath();
            }
            iterator.next();
        }
        return pixelPath;
    }
}
