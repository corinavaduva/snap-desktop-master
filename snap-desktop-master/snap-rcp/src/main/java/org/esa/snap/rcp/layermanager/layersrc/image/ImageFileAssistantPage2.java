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

package org.esa.snap.rcp.layermanager.layersrc.image;

import org.esa.snap.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.snap.ui.layer.LayerSourcePageContext;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.AffineTransform;

class ImageFileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JTextField[] numberFields;

    ImageFileAssistantPage2() {
        super("Edit Affine Transformation");
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 2);

        double[] flatmatrix = new double[6];
        numberFields = new JTextField[flatmatrix.length];
        AffineTransform transform = (AffineTransform) getContext().getPropertyValue(
                ImageFileLayerSource.PROPERTY_NAME_WORLD_TRANSFORM);
        transform.getMatrix(flatmatrix);

// see http://support.esri.com/index.cfm?fa=knowledgebase.techarticles.articleShow&d=17489
        String[] labels = new String[]{
                "X-dimension of a pixel in map units: ",
                "Rotation parameter for row: ",
                "Rotation parameter for column: ",
                "Negative of Y-dimension of a pixel in map units: ",
                "X-coordinate of center of upper left pixel: ",
                "Y-coordinate of centre of upper left pixel: "
        };
        numberFields[0] = addRow(panel, labels[0], gbc);
        numberFields[0].setText(String.valueOf(flatmatrix[0]));
        numberFields[1] = addRow(panel, labels[1], gbc);
        numberFields[1].setText(String.valueOf(flatmatrix[1]));
        numberFields[2] = addRow(panel, labels[2], gbc);
        numberFields[2].setText(String.valueOf(flatmatrix[2]));
        numberFields[3] = addRow(panel, labels[3], gbc);
        numberFields[3].setText(String.valueOf(flatmatrix[3]));

        numberFields[4] = addRow(panel, labels[4], gbc);
        numberFields[4].setText(String.valueOf(flatmatrix[4]));

        numberFields[5] = addRow(panel, labels[5], gbc);
        numberFields[5].setText(String.valueOf(flatmatrix[5]));

        return panel;
    }

    @Override
    public boolean validatePage() {
        try {
            return createTransform().getDeterminant() != 0.0;
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean performFinish() {
        AffineTransform transform = createTransform();
        final LayerSourcePageContext context = getContext();
        context.setPropertyValue(ImageFileLayerSource.PROPERTY_NAME_WORLD_TRANSFORM, transform);
        return ImageFileLayerSource.insertImageLayer(context);
    }


    private JTextField addRow(JPanel panel, String label, GridBagConstraints gbc) {
        gbc.gridy++;

        gbc.weightx = 0.2;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.weightx = 0.8;
        gbc.gridx = 1;
        final JTextField fileField = new JTextField(12);
        fileField.setHorizontalAlignment(JTextField.RIGHT);
        panel.add(fileField, gbc);
        fileField.getDocument().addDocumentListener(new MyDocumentListener());
        return fileField;
    }

    private AffineTransform createTransform() {
        double[] flatmatrix = new double[numberFields.length];
        for (int i = 0; i < flatmatrix.length; i++) {
            flatmatrix[i] = Double.parseDouble(getText(numberFields[i]));
        }
        return new AffineTransform(flatmatrix);
    }

    private String getText(JTextComponent textComponent) {
        String s = textComponent.getText();
        return s != null ? s.trim() : "";
    }

    private class MyDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            getContext().updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            getContext().updateState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            getContext().updateState();
        }
    }


}