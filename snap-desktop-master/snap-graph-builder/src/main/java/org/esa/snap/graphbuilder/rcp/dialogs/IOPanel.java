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
package org.esa.snap.graphbuilder.rcp.dialogs;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.ui.AppContext;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * IO Panel to handle source and target selection
 * User: lveci
 * Date: Feb 5, 2009
 */
public class IOPanel {

    private final TargetProductSelector targetProductSelector;
    private final boolean useSourceSelector;
    private final List<SourceProductSelector> sourceProductSelectorList = new ArrayList<>(3);
    private String targetProductNameSuffix = "";

    IOPanel(final AppContext theAppContext, final JTabbedPane tabbedPane, boolean createSourceSelector) {
        this.useSourceSelector = createSourceSelector;

        targetProductSelector = new TargetProductSelector();
        final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        final String saveDir = SnapApp.getDefault().getPreferences().get(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
        targetProductSelector.getModel().setProductDir(new File(saveDir));
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + theAppContext.getApplicationName());

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(1, 1);

        final JPanel ioParametersPanel = new JPanel(tableLayout);

        if (useSourceSelector) {
            // Fetch source products
            sourceProductSelectorList.add(new SourceProductSelector(theAppContext));

            for (SourceProductSelector selector : sourceProductSelectorList) {
                ioParametersPanel.add(selector.createDefaultPanel());
            }
            ioParametersPanel.add(tableLayout.createVerticalSpacer());
            sourceProductSelectorList.get(0).addSelectionChangeListener(new AbstractSelectionChangeListener() {
                public void selectionChanged(SelectionChangeEvent event) {
                    final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                    if (selectedProduct != null) {
                        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
                        targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
                    }
                }
            });
        }

        ioParametersPanel.add(targetProductSelector.createDefaultPanel());
        if (useSourceSelector) {
            tabbedPane.add("I/O Parameters", ioParametersPanel);
        } else {
            tabbedPane.add("Target Product", ioParametersPanel);
        }
    }

    public void setTargetProductName(final String name) {
        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
        targetProductSelectorModel.setProductName(name + getTargetProductNameSuffix());
    }

    public void initProducts() {
        if (useSourceSelector) {
            for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
                sourceProductSelector.initProducts();
            }
        }
    }

    public void releaseProducts() {
        if (!useSourceSelector) {
            for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
                sourceProductSelector.releaseProducts();
            }
        }
    }

    public void onApply() {
        final String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        SnapApp.getDefault().getPreferences().put(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDir);
    }

    public Product getSelectedSourceProduct() {
        if (useSourceSelector)
            return sourceProductSelectorList.get(0).getSelectedProduct();
        return null;
    }

    public File getTargetFile() {
        return targetProductSelector.getModel().getProductFile();
    }

    public String getTargetFormat() {
        return targetProductSelector.getModel().getFormatName();
    }

    String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        targetProductNameSuffix = suffix;
    }

    public boolean isOpenInAppSelected() {
        return targetProductSelector.getModel().isOpenInAppSelected();
    }
}
