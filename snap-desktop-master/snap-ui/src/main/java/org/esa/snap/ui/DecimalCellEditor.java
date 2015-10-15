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

package org.esa.snap.ui;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;

/**
 * A {@code DecimalCellEditor} which is able to validate the entered value.
 * If the value is not valid the cell is marked with a red border and the value is rejected.
 * The cell value is right aligned.
 */
public class DecimalCellEditor extends DefaultCellEditor {

    private Border defaultBorder;
    private double minValue;
    private double maxValue;

    /**
     * Creates a new editor. The bounds of the valid value range are set
     * to {@link Double#MIN_VALUE} and {@link Double#MAX_VALUE}
     */
    public DecimalCellEditor() {
        this(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * Creates a new editor. The bounds of the valid value range are set
     * to specified {@code minValue} and {@code maxValue}
     *
     * @param minValue the minimum value of the valid range
     * @param maxValue the maximum value of the valid range
     */
    public DecimalCellEditor(double minValue, double maxValue) {
        super(new JTextField());
        this.minValue = minValue;
        this.maxValue = maxValue;
        JTextField textField = (JTextField) getComponent();
        defaultBorder = textField.getBorder();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                 int column) {
        JComponent component = (JComponent) super.getTableCellEditorComponent(table, value, isSelected, row,
                                                                              column);
        final JTextField textField = (JTextField) component;
        textField.selectAll();
        textField.setBorder(defaultBorder);
        textField.setHorizontalAlignment(JTextField.RIGHT);
        return component;
    }

    @Override
    public boolean stopCellEditing() {
        JTextField textField = (JTextField) getComponent();
        double value;
        try {
            value = Double.parseDouble(textField.getText());
        } catch (NumberFormatException ignored) {
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));
            return false;
        }

        boolean validValue = validateValue(value);
        if (!validValue) {
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));
            return false;
        }

        if (!super.stopCellEditing()) {
            ((JComponent) getComponent()).setBorder(new LineBorder(Color.red));
            return false;
        }

        return true;
    }

    protected boolean validateValue(double value) {
        return value >= minValue && value <= maxValue;
    }

    @Override
    public Object getCellEditorValue() {
        JTextField textField = (JTextField) getComponent();
        try {
            return Double.parseDouble(textField.getText());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }
}
