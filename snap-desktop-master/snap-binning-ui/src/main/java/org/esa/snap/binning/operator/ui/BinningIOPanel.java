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

package org.esa.snap.binning.operator.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.product.SourceProductList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * The panel in the binning operator UI which allows for setting input products and the path of the output product.
 */
class BinningIOPanel extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private final TargetProductSelector targetProductSelector;
    private SourceProductList sourceProductList;

    BinningIOPanel(AppContext appContext, BinningFormModel binningFormModel, TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        this.targetProductSelector = targetProductSelector;
        init();
    }

    void prepareClose() {
        sourceProductList.clear();
    }

    private void init() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);
        tableLayout.setRowWeightY(0, 1.0);
        add(createSourceProductsPanel());
        targetProductSelector.getModel().setProductName("level-3");
        add(targetProductSelector.createDefaultPanel());
    }

    private JPanel createSourceProductsPanel() {
        BorderLayout layout = new BorderLayout();

        final JPanel sourceProductPanel = new JPanel(layout);
        sourceProductPanel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        ListDataListener changeListener = new ListDataListener() {

            @Override
            public void contentsChanged(ListDataEvent event) {
                final Product[] sourceProducts = sourceProductList.getSourceProducts();
                try {
                    binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS, sourceProducts);
                } catch (ValidationException e) {
                    appContext.handleError("Unable to set source products.", e);
                }
                if (sourceProducts.length > 0) {
                    binningFormModel.useAsContextProduct(sourceProducts[0]);
                    return;
                }
                String[] sourceProductPath = binningFormModel.getSourceProductPath();
                if (sourceProductPath != null && sourceProductPath.length > 0) {
                    openFirstProduct(sourceProductPath);
                    return;
                }
                binningFormModel.useAsContextProduct(null);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }
        };

        sourceProductList = new SourceProductList(appContext);
        sourceProductList.setLastOpenInputDir("org.esa.snap.binning.lastDir");
        sourceProductList.setLastOpenedFormat("org.esa.snap.binning.lastFormat");
        sourceProductList.addChangeListener(changeListener);
        sourceProductList.setXAxis(false);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCT_PATHS, sourceProductList);
        JComponent[] panels = sourceProductList.getComponents();
        sourceProductPanel.add(panels[0], BorderLayout.CENTER);
        sourceProductPanel.add(panels[1], BorderLayout.EAST);

        return sourceProductPanel;
    }

    private void openFirstProduct(final String[] inputPaths) {
        final SwingWorker<Product, Void> worker = new SwingWorker<Product, Void>() {
            @Override
            protected Product doInBackground() throws Exception {
                for (String inputPath : inputPaths) {
                    if (inputPath == null || inputPath.trim().length() == 0) {
                        continue;
                    }
                    try {
                        final TreeSet<File> fileSet = new TreeSet<>();
                        WildcardMatcher.glob(inputPath, fileSet);
                        for (File file : fileSet) {
                            final Product product = ProductIO.readProduct(file);
                            if (product != null) {
                                return product;
                            }
                        }
                    } catch (IOException e) {
                        Logger logger = SystemUtils.LOG;
                        logger.severe("I/O problem occurred while scanning source product files: " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Product firstProduct = get();
                    if (firstProduct != null) {
                        binningFormModel.useAsContextProduct(firstProduct);
                        firstProduct.dispose();
                    }
                } catch (Exception ex) {
                    String msg = String.format("Cannot open source products.\n%s", ex.getMessage());
                    appContext.handleError(msg, ex);
                }
            }
        };
        worker.execute();
    }
}
