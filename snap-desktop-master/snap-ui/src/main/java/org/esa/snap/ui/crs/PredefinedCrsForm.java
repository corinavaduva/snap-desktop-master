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

package org.esa.snap.ui.crs;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class PredefinedCrsForm extends CrsForm {

    private CrsInfo selectedCrsInfo;

    public PredefinedCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    protected String getLabelText() {
        return "Predefined CRS";
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        if (selectedCrsInfo != null) {
            return selectedCrsInfo.getCrs(referencePos);
        } else {
            return null;
        }
    }


    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }

    @Override
    protected JComponent createCrsComponent() {
        final TableLayout tableLayout = new TableLayout(2);
        final JPanel panel = new JPanel(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);

        final JTextField crsCodeField = new JTextField();
        crsCodeField.setEditable(false);
        final JButton crsButton = new JButton("Select...");
        final PredefinedCrsPanel predefinedCrsForm = new PredefinedCrsPanel(
                new CrsInfoListModel(CrsInfo.generateCRSList()));
        crsButton.addActionListener(e -> {
            final ModalDialog dialog = new ModalDialog(null,
                                                       "Select Coordinate Reference System",
                                                       predefinedCrsForm,
                                                       ModalDialog.ID_OK_CANCEL, null);
            if (dialog.show() == ModalDialog.ID_OK) {
                selectedCrsInfo = predefinedCrsForm.getSelectedCrsInfo();
                if (selectedCrsInfo != null) {
                    crsCodeField.setText(selectedCrsInfo.toString());
                    fireCrsChanged();
                }
            }
        });
        panel.add(crsCodeField);
        panel.add(crsButton);
        panel.addPropertyChangeListener("enabled", evt -> {
            crsCodeField.setEnabled((Boolean) evt.getNewValue());
            crsButton.setEnabled((Boolean) evt.getNewValue());
        });
        return panel;
    }

}
