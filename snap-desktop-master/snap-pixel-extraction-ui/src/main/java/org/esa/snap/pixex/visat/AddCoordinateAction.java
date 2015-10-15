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

package org.esa.snap.pixex.visat;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

class AddCoordinateAction extends AbstractAction {

    private final CoordinateTableModel tableModel;

    AddCoordinateAction(CoordinateTableModel tableModel) {
        super("Add coordinate");
        this.tableModel = tableModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(),
                                                                   "Coord_" + tableModel.getRowCount(), "", "",
                                                                   new PixelPos(), new GeoPos(0, 0),
                                                                   null);
        tableModel.addPlacemark(placemark);
    }
}
