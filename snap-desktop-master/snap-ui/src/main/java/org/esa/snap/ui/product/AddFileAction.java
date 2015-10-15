/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.ui.product;

import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.ui.AppContext;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Thomas Storm
 */
class AddFileAction extends AbstractAction {

    public static final String ALL_FILES_FORMAT = "ALL_FILES";
    private final AppContext appContext;
    private final InputListModel listModel;
    private final String lastOpenInputDir;
    private final String lastOpenedFormat;

    AddFileAction(AppContext appContext, InputListModel listModel, String lastOpenInputDir, String lastOpenedFormat) {
        super("Add product file(s)...");
        this.appContext = appContext;
        this.listModel = listModel;
        this.lastOpenInputDir = lastOpenInputDir;
        this.lastOpenedFormat = lastOpenedFormat;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final PropertyMap preferences = appContext.getPreferences();
        String lastDir = preferences.getPropertyString(lastOpenInputDir,
                                                       SystemUtils.getUserHomeDir().getPath());
        String lastFormat = preferences.getPropertyString(lastOpenedFormat, ALL_FILES_FORMAT);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(lastDir));
        fileChooser.setDialogTitle("Select product(s)");
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
        Iterator<ProductReaderPlugIn> allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
        List<SnapFileFilter> sortedFileFilters = SnapFileFilter.getSortedFileFilters(allReaderPlugIns);
        for (SnapFileFilter productFileFilter : sortedFileFilters) {
            fileChooser.addChoosableFileFilter(productFileFilter);
            if (!ALL_FILES_FORMAT.equals(lastFormat) &&
                productFileFilter.getFormatName().equals(lastFormat)) {
                actualFileFilter = productFileFilter;
            }
        }
        fileChooser.setFileFilter(actualFileFilter);

        int result = fileChooser.showDialog(appContext.getApplicationWindow(), "Select product(s)");
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        preferences.setPropertyString(lastOpenInputDir,
                                      fileChooser.getCurrentDirectory().getAbsolutePath());

        final Object[] selectedProducts = fileChooser.getSelectedFiles();
        try {
            listModel.addElements(selectedProducts);
        } catch (ValidationException ve) {
            // not expected to ever come here
            appContext.handleError("Invalid input path", ve);
        }

        setLastOpenedFormat(preferences, fileChooser.getFileFilter());
    }

    private void setLastOpenedFormat(PropertyMap preferences, FileFilter fileFilter) {
        if (fileFilter instanceof SnapFileFilter) {
            String currentFormat = ((SnapFileFilter) fileFilter).getFormatName();
            if (currentFormat != null) {
                preferences.setPropertyString(lastOpenedFormat, currentFormat);
            }
        } else {
            preferences.setPropertyString(lastOpenedFormat, ALL_FILES_FORMAT);
        }
    }
}
