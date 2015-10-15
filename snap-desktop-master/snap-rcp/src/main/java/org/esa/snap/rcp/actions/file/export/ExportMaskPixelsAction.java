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

package org.esa.snap.rcp.actions.file.export;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.AbstractDialog;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SelectExportMethodDialog;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;


@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportMaskPixelsAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportMaskPixelsAction_MenuText",
        popupText = "#CTL_ExportMaskPixelsAction_PopupText",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 50),
        @ActionReference(path = "Menu/Raster/Export"),
        @ActionReference(path = "Context/ProductSceneView", position = 50)
})

@NbBundle.Messages({
        "CTL_ExportMaskPixelsAction_MenuText=Mask Pixels",
        "CTL_ExportMaskPixelsAction_PopupText=Export Mask Pixels",
        "CTL_ExportMaskPixelsAction_DialogTitle=Export Mask Pixels",
        "CTL_ExportMaskPixelsAction_ShortDescription=Export Mask Pixels."
})
public class ExportMaskPixelsAction extends AbstractAction implements ContextAwareAction, LookupListener, HelpCtx.Provider {

    private static final String HELP_ID = "exportMaskPixels";
    private static final String ERR_MSG_BASE = "Mask pixels cannot be exported:\n";

    private final Lookup.Result<ProductSceneView> result;

    public ExportMaskPixelsAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportMaskPixelsAction(Lookup lkp) {
        super(Bundle.CTL_ExportMaskPixelsAction_MenuText());
        putValue("popupText",Bundle.CTL_ExportMaskPixelsAction_PopupText());
        result = lkp.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        exportMaskPixels();
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new ExportMaskPixelsAction(lkp);
    }

    @Override
    public void resultChanged(LookupEvent le) {
        ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
        boolean enabled = false;
        if (sceneView != null) {
            Product product = sceneView.getProduct();
            enabled = product.getMaskGroup().getNodeCount() > 0;
        }
        setEnabled(enabled);
    }

    /////////////////////////////////////////////////////////////////////////
    // Private implementations for the "export Mask Pixels" command
    /////////////////////////////////////////////////////////////////////////

    /**
     * Performs the actual "export Mask Pixels" command.
     */
    private void exportMaskPixels() {
        ProductSceneView sceneView = SnapApp.getDefault().getSelectedProductSceneView();
        Product product = sceneView.getProduct();
        String[] maskNames = product.getMaskGroup().getNodeNames();
        final String maskName;
        if (maskNames.length == 1) {
            maskName = maskNames[0];
        } else {
            JPanel panel = new JPanel();
            BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.X_AXIS);
            panel.setLayout(boxLayout);
            panel.add(new JLabel("Select Mask: "));
            JComboBox<String> maskCombo = new JComboBox<>(maskNames);
            panel.add(maskCombo);
            ModalDialog modalDialog = new ModalDialog(SnapApp.getDefault().getMainFrame(),
                                                      Bundle.CTL_ExportMaskPixelsAction_DialogTitle(), panel,
                                                      ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP, getHelpCtx().getHelpID());
            if (modalDialog.show() == AbstractDialog.ID_OK) {
                maskName = (String) maskCombo.getSelectedItem();
            } else {
                return;
            }
        }
        Mask mask = product.getMaskGroup().get(maskName);

        final RenderedImage maskImage = mask.getSourceImage();
        if (maskImage == null) {
            SnapDialogs.showError(Bundle.CTL_ExportMaskPixelsAction_DialogTitle(),
                                  ERR_MSG_BASE + "No Mask image available.");
            return;
        }
        // Compute total number of Mask pixels
        final long numMaskPixels = getNumMaskPixels(maskImage, product.getSceneRasterWidth(), product.getSceneRasterHeight());

        String numPixelsText;
        if (numMaskPixels == 1) {
            numPixelsText = "One Mask pixel will be exported.\n";
        } else {
            numPixelsText = numMaskPixels + " Mask pixels will be exported.\n";
        }
        // Get export method from user
        final String questionText = "How do you want to export the pixel values?\n";
        final JCheckBox createHeaderBox = new JCheckBox("Create header");
        final JCheckBox exportTiePointsBox = new JCheckBox("Export tie-points");
        final JCheckBox exportWavelengthsAndSFBox = new JCheckBox("Export wavelengths + solar fluxes");
        final int method = SelectExportMethodDialog.run(SnapApp.getDefault().getMainFrame(), getWindowTitle(),
                                                        questionText + numPixelsText, new JCheckBox[]{
                        createHeaderBox,
                        exportTiePointsBox,
                        exportWavelengthsAndSFBox
                }, getHelpCtx().getHelpID());

        final boolean mustCreateHeader = createHeaderBox.isSelected();
        final boolean mustExportTiePoints = exportTiePointsBox.isSelected();
        final boolean mustExportWavelengthsAndSF = exportWavelengthsAndSFBox.isSelected();
//
        final PrintWriter out;
        final StringBuffer clipboardText;
        final int initialBufferSize = 256000;
        if (method == SelectExportMethodDialog.EXPORT_TO_CLIPBOARD) {
            // Write into string buffer
            final StringWriter stringWriter = new StringWriter(initialBufferSize);
            out = new PrintWriter(stringWriter);
            clipboardText = stringWriter.getBuffer();
        } else if (method == SelectExportMethodDialog.EXPORT_TO_FILE) {
            // Write into file, get file from user
            final File file = promptForFile(createDefaultFileName(product, maskName));
            if (file == null) {
                return; // Cancel
            }
            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                SnapDialogs.showError(Bundle.CTL_ExportMaskPixelsAction_DialogTitle(),
                                      ERR_MSG_BASE + "Failed to create file '" + file + "':\n" + e.getMessage());
                return; // Error
            }
            out = new PrintWriter(new BufferedWriter(fileWriter, initialBufferSize));
            clipboardText = null;
        } else {
            return; // Cancel
        }

        final ProgressMonitorSwingWorker<Exception, Object> swingWorker = new ProgressMonitorSwingWorker<Exception, Object>(
                SnapApp.getDefault().getMainFrame(), Bundle.CTL_ExportMaskPixelsAction_DialogTitle()) {

            @Override
            protected Exception doInBackground(ProgressMonitor pm) throws Exception {
                Exception returnValue = null;
                try {
                    boolean success = exportMaskPixels(out, product, maskImage, maskName,
                                                       mustCreateHeader, mustExportTiePoints, mustExportWavelengthsAndSF, pm);
                    if (success && clipboardText != null) {
                        SystemUtils.copyToClipboard(clipboardText.toString());
                        clipboardText.setLength(0);
                    }
                } catch (Exception e) {
                    returnValue = e;
                } finally {
                    out.close();
                }
                return returnValue;
            }

            @Override
            public void done() {
//                 clear status bar
                SnapApp.getDefault().setStatusBarMessage("");
//                 show default-cursor
                UIUtils.setRootFrameDefaultCursor(SnapApp.getDefault().getMainFrame());
//                 On error, show error message
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception != null) {
                    SnapDialogs.showError(Bundle.CTL_ExportMaskPixelsAction_DialogTitle(),
                                          ERR_MSG_BASE + exception.getMessage());
                }
            }

        };

        // show wait-cursor
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        // show message in status bar
        SnapApp.getDefault().setStatusBarMessage("Exporting Mask pixels...");

        // Start separate worker thread.
        swingWorker.execute();
    }

    private static String createDefaultFileName(final Product raster, String maskName) {
        String productName = FileUtils.getFilenameWithoutExtension(raster.getProduct().getName());
        return productName + "_" + maskName + "_Mask.txt";
    }

    private static String getWindowTitle() {
        return SnapApp.getDefault().getInstanceName() + " - " + Bundle.CTL_ExportMaskPixelsAction_DialogTitle();
    }

    /*
     * Opens a modal file chooser dialog that prompts the user to select the output file name.
     *
     * @param visatApp the VISAT application
     * @return the selected file, <code>null</code> means "Cancel"
     */
    private static File promptForFile(String defaultFileName) {
        final SnapFileFilter fileFilter = new SnapFileFilter("TXT", "txt", "Text");
        return SnapDialogs.requestFileForSave(Bundle.CTL_ExportMaskPixelsAction_DialogTitle(),
                                              false,
                                              fileFilter,
                                              ".txt",
                                              defaultFileName,
                                              null,
                                              "exportMaskPixels.lastDir");
    }

    /*
     * Writes all pixel values of the given product within the given Mask to the specified out.
     *
     * @param out      the data output writer
     * @param product  the product providing the pixel values
     * @param maskImage the mask image for the Mask
     * @return <code>true</code> for success, <code>false</code> if export has been terminated (by user)
     */
    private static boolean exportMaskPixels(final PrintWriter out,
                                            final Product product,
                                            final RenderedImage maskImage,
                                            String maskName,
                                            boolean mustCreateHeader,
                                            boolean mustExportTiePoints,
                                            boolean mustExportWavelengthsAndSF,
                                            ProgressMonitor pm) throws IOException {

        final Band[] bands = product.getBands();
        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        final GeoCoding geoCoding = product.getSceneGeoCoding();

        final int minTileX = maskImage.getMinTileX();
        final int minTileY = maskImage.getMinTileY();

        final int numXTiles = maskImage.getNumXTiles();
        final int numYTiles = maskImage.getNumYTiles();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        final Rectangle imageRect = new Rectangle(0, 0, w, h);

        pm.beginTask("Writing pixel data...", numXTiles * numYTiles + 2);
        try {
            if (mustCreateHeader) {
                createHeader(out, product, maskName, mustExportWavelengthsAndSF);
            }
            pm.worked(1);
            writeColumnNames(out, geoCoding, bands, mustExportTiePoints, tiePointGrids);
            pm.worked(1);

            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                    if (pm.isCanceled()) {
                        return false;
                    }
                    final Rectangle tileRectangle = new Rectangle(maskImage.getTileGridXOffset() + tileX * maskImage.getTileWidth(),
                                                                  maskImage.getTileGridYOffset() + tileY * maskImage.getTileHeight(),
                                                                  maskImage.getTileWidth(), maskImage.getTileHeight());

                    final Rectangle r = imageRect.intersection(tileRectangle);
                    if (!r.isEmpty()) {
                        Raster maskTile = maskImage.getTile(tileX, tileY);
                        for (int y = r.y; y < r.y + r.height; y++) {
                            for (int x = r.x; x < r.x + r.width; x++) {
                                if (maskTile.getSample(x, y, 0) != 0) {
                                    writeDataLine(out, geoCoding, bands, mustExportTiePoints, tiePointGrids, x, y);
                                }
                            }
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }

        return true;
    }

    private static void createHeader(PrintWriter out, Product product, String maskName, boolean mustExportWavelengthsAndSF) {
        out.write("# Exported mask '" + maskName + "' on " +
                          new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.mmmmmm").format(new GregorianCalendar().getTime()) + "\n");
        out.write("# Product name: " + product.getName() + "\n");
        if (product.getFileLocation() != null) {
            out.write("# Product file location: " + product.getFileLocation() + "\n");
        }
        out.write("\n");
        if (mustExportWavelengthsAndSF) {
            out.write("# Wavelength:");
            out.write("\t\t\t"); // account for pixel-x, pixel-y, lon, lat columns
            for (final Band band : product.getBands()) {
                out.print("\t");
                out.print("" + band.getSpectralWavelength());
            }
            out.print("\n");
            out.write("# Solar flux:");
            out.write("\t\t\t"); // account for pixel-x, pixel-y, lon, lat columns
            for (final Band band : product.getBands()) {
                out.print("\t");
                out.print("" + band.getSolarFlux());
            }
            out.print("\n");
        }
    }

    /*
     * Writes the header line of the dataset to be exported.
     *
     * @param out                     the data output writer
     * @param geoCoding               the product's geo-coding
     * @param bands                   the array of bands to be considered
     * @param mustExportTiePointGrids if tie-point grids shall be considered
     * @param tiePointGrids           the array of tie-point grids to be considered
     */
    private static void writeColumnNames(final PrintWriter out,
                                         final GeoCoding geoCoding,
                                         final Band[] bands,
                                         boolean mustExportTiePointGrids,
                                         TiePointGrid[] tiePointGrids) {
        out.print("Pixel-X");
        out.print("\t");
        out.print("Pixel-Y");
        if (geoCoding != null) {
            out.print("\t");
            out.print("Longitude");
            out.print("\t");
            out.print("Latitude");
        }
        for (final Band band : bands) {
            out.print("\t");
            out.print(band.getName());
        }
        if (mustExportTiePointGrids) {
            for (final TiePointGrid grid : tiePointGrids) {
                out.print("\t");
                out.print(grid.getName());
            }
        }
        out.print("\n");
    }

    /*
     * Writes a data line of the dataset to be exported for the given pixel position.
     *
     * @param out                     the data output writer
     * @param geoCoding               the product's geo-coding
     * @param bands                   the array of bands that provide pixel values
     * @param mustExportTiePointGrids if tie-point grids shall be exported
     * @param tiePointGrids           the array of tie-point grids that provide pixel values
     * @param x                       the current pixel's X coordinate
     * @param y                       the current pixel's Y coordinate
     */
    private static void writeDataLine(final PrintWriter out,
                                      final GeoCoding geoCoding,
                                      final Band[] bands,
                                      boolean mustExportTiePoints,
                                      TiePointGrid[] tiePointGrids, int x,
                                      int y) throws IOException {
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);

        out.print(String.valueOf(pixelPos.x));
        out.print("\t");
        out.print(String.valueOf(pixelPos.y));
        if (geoCoding != null) {
            final GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
            out.print("\t");
            out.print(String.valueOf(geoPos.lon));
            out.print("\t");
            out.print(String.valueOf(geoPos.lat));
        }
        final int[] intPixel = new int[1];
        final float[] floatPixel = new float[1];
        for (final Band band : bands) {
            out.print("\t");
            if (band.isPixelValid(x, y)) {
                if (band.isFloatingPointType()) {
                    band.readPixels(x, y, 1, 1, floatPixel, ProgressMonitor.NULL);
                    out.print(floatPixel[0]);
                } else {
                    band.readPixels(x, y, 1, 1, intPixel, ProgressMonitor.NULL);
                    out.print(intPixel[0]);
                }
            } else {
                out.print("NaN");
            }
        }
        if (mustExportTiePoints) {
            for (final TiePointGrid grid : tiePointGrids) {
                grid.readPixels(x, y, 1, 1, floatPixel, ProgressMonitor.NULL);
                out.print("\t");
                out.print(floatPixel[0]);
            }
        }
        out.print("\n");
    }


    /*
     * Computes the total number of pixels within the specified Mask.
     *
     * @param raster   the raster data node
     * @param maskImage the rendered image masking out the Mask
     * @return the total number of pixels in the Mask
     */
    private static long getNumMaskPixels(final RenderedImage maskImage, int sceneRasterWidth, int sceneRasterHeight) {
        final int minTileX = maskImage.getMinTileX();
        final int minTileY = maskImage.getMinTileY();

        final int numXTiles = maskImage.getNumXTiles();
        final int numYTiles = maskImage.getNumYTiles();

        final Rectangle imageRect = new Rectangle(0, 0, sceneRasterWidth, sceneRasterHeight);

        long numMaskPixels = 0;
        for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
            for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                final Rectangle tileRectangle = new Rectangle(maskImage.getTileGridXOffset() + tileX * maskImage.getTileWidth(),
                                                              maskImage.getTileGridYOffset() + tileY * maskImage.getTileHeight(),
                                                              maskImage.getTileWidth(), maskImage.getTileHeight());

                final Rectangle r = imageRect.intersection(tileRectangle);
                if (!r.isEmpty()) {
                    Raster maskTile = maskImage.getTile(tileX, tileY);
                    for (int y = r.y; y < r.y + r.height; y++) {
                        for (int x = r.x; x < r.x + r.width; x++) {
                            if (maskTile.getSample(x, y, 0) != 0) {
                                numMaskPixels++;
                            }
                        }
                    }
                }
            }
        }
        return numMaskPixels;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }
}
