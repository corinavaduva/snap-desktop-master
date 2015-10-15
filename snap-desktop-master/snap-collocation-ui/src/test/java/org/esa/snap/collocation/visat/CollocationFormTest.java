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

package org.esa.snap.collocation.visat;

import org.esa.snap.collocation.ResamplingType;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import javax.swing.DefaultComboBoxModel;

import static org.junit.Assert.*;

/**
 * Tests for class {@link CollocationForm}.
 *
 * @author Ralf Quast
 */
public class CollocationFormTest {

    @Test
    public void testAdaptResamplingComboBoxModel() {
        final Product product = new Product("name", "type", 10, 10);
        final Band band1 = product.addBand("band1", ProductData.TYPE_INT32);
        final Band band2 = product.addBand("band2", ProductData.TYPE_INT32);

        DefaultComboBoxModel<ResamplingType> resamplingComboBoxModel = new DefaultComboBoxModel<>(ResamplingType.values());

        boolean validPixelExpressionUsed = CollocationForm.isValidPixelExpressionUsed(product);
        assertFalse(validPixelExpressionUsed);
        CollocationForm.adaptResamplingComboBoxModel(resamplingComboBoxModel, validPixelExpressionUsed);
        assertEquals(5, resamplingComboBoxModel.getSize());

        band1.setValidPixelExpression("true");
        validPixelExpressionUsed = CollocationForm.isValidPixelExpressionUsed(product);
        assertTrue(validPixelExpressionUsed);
        CollocationForm.adaptResamplingComboBoxModel(resamplingComboBoxModel, validPixelExpressionUsed);
        assertEquals(1, resamplingComboBoxModel.getSize());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, resamplingComboBoxModel.getSelectedItem());

        band1.setValidPixelExpression(null);
        band2.setValidPixelExpression("  ");
        validPixelExpressionUsed = CollocationForm.isValidPixelExpressionUsed(product);
        assertFalse(validPixelExpressionUsed);
        CollocationForm.adaptResamplingComboBoxModel(resamplingComboBoxModel, validPixelExpressionUsed);
        assertEquals(5, resamplingComboBoxModel.getSize());
        assertEquals(ResamplingType.NEAREST_NEIGHBOUR, resamplingComboBoxModel.getSelectedItem());
    }
}
