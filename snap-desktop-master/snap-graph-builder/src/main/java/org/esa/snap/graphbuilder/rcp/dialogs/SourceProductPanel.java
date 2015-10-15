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
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * Source product selection panel
 */
public class SourceProductPanel extends JPanel {

    private final List<SourceProductSelector> sourceProductSelectorList = new ArrayList<>(3);

    public SourceProductPanel(final AppContext appContext) {

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(1, 1);

        setLayout(tableLayout);

        // Fetch source products
        sourceProductSelectorList.add(new SourceProductSelector(appContext));

        for (SourceProductSelector selector : sourceProductSelectorList) {
            add(selector.createDefaultPanel());
        }
        add(tableLayout.createVerticalSpacer());

    }

    public void initProducts() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    public void releaseProducts() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    public Product getSelectedSourceProduct() {
        return sourceProductSelectorList.get(0).getSelectedProduct();
    }
}
