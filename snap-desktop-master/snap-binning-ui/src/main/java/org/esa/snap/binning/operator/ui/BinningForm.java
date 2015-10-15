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

import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.ui.AppContext;

import javax.swing.JTabbedPane;

/**
 * The form for the {@link BinningDialog}.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningForm extends JTabbedPane {

    private final BinningIOPanel ioPanel;
    private BinningConfigurationPanel configurationPanel;

    BinningForm(AppContext appContext, BinningFormModel binningFormModel, TargetProductSelector targetProductSelector) {
        ioPanel = new BinningIOPanel(appContext, binningFormModel, targetProductSelector);
        addTab("I/O Parameters", ioPanel);
        addTab("Filter", new BinningFilterPanel(binningFormModel));
        configurationPanel = new BinningConfigurationPanel(appContext, binningFormModel);
        addTab("Configuration", configurationPanel);
    }

    void prepareClose() {
        ioPanel.prepareClose();
    }

    public BinningConfigurationPanel getBinningConfigurationPanel() {
        return configurationPanel;
    }
}
