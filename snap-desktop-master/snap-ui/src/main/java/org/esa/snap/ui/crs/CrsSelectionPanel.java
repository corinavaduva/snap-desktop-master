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
import org.esa.snap.core.datamodel.Product;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @author Marco Zühlke
 * @since BEAM 4.7
 */
public class CrsSelectionPanel extends JPanel {

    private CrsChangeListener crsChangeListener;
    private final CrsForm[] crsForms;

    public CrsSelectionPanel(CrsForm... crsForms) {
        this.crsForms = crsForms;
        createUI();
        crsChangeListener = new CrsChangeListener();
        addPropertyChangeListener("enabled", new EnabledChangeListener());

    }

    public void setReferenceProduct(Product product) {
        for (CrsForm crsForm : crsForms) {
            crsForm.setReferenceProduct(product);
        }
    }

    public CoordinateReferenceSystem getCrs(GeoPos referencePos) throws FactoryException {
        for (CrsForm crsForm : crsForms) {
            if (crsForm.getRadioButton().isSelected()) {
                return crsForm.getCRS(referencePos);
            }
        }
        return null;
    }

    public void prepareShow() {
        for (CrsForm crsForm : crsForms) {
            crsForm.prepareShow();
            crsForm.addCrsChangeListener(crsChangeListener);
        }
        updateUIState();
    }

    public void prepareHide() {
        for (CrsForm crsForm : crsForms) {
            crsForm.prepareHide();
            crsForm.removeCrsChangeListener(crsChangeListener);
        }
    }

    private void updateUIState() {
        for (CrsForm crsForm : crsForms) {
            crsForm.getCrsUI().setEnabled(crsForm.getRadioButton().isSelected());
        }
    }

    private void createUI() {
        ButtonGroup buttonGroup = new ButtonGroup();


        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);

        setLayout(tableLayout);
        setBorder(BorderFactory.createTitledBorder("Coordinate Reference System (CRS)"));

        final UpdateStateListener updateStateListener = new UpdateStateListener();


        int rowCount = 0;
        for (int i = 0, crsFormsLength = crsForms.length; i < crsFormsLength; i++) {
            CrsForm crsForm = crsForms[i];
            JRadioButton crsRadioButton = crsForm.getRadioButton();
            crsRadioButton.setSelected(i == 0);
            crsRadioButton.addActionListener(updateStateListener);
            JComponent crsComponent = crsForm.getCrsUI();
            crsComponent.setEnabled(i == 0);
            buttonGroup.add(crsRadioButton);
            if (crsForm.wrapAfterButton()) {
                tableLayout.setCellColspan(rowCount, 0, 2);
                rowCount++;
                tableLayout.setCellColspan(rowCount, 0, 2);
                tableLayout.setCellPadding(rowCount, 0, new Insets(4, 24, 4, 4));
                rowCount++;
            } else {
                tableLayout.setCellWeightX(rowCount, 0, 0.0);
                rowCount++;
            }

            add(crsRadioButton);
            add(crsComponent);
        }
    }

    private class UpdateStateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
            fireCrsChanged();
        }
    }

    private class CrsChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            fireCrsChanged();
        }
    }

    private void fireCrsChanged() {
        firePropertyChange("crs", null, null);
    }

    private class EnabledChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final Boolean enabled = (Boolean) evt.getNewValue();

            for (CrsForm crsForm : crsForms) {
                final JRadioButton button = crsForm.getRadioButton();
                button.setEnabled(enabled);
                final boolean selected = button.isSelected();
                crsForm.getCrsUI().setEnabled(selected && enabled);
            }
        }
    }
}
