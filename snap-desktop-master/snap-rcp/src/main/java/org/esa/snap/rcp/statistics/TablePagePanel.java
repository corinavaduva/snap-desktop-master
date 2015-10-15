/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.rcp.statistics;

import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.util.StringUtils;
import org.openide.windows.TopComponent;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
abstract class TablePagePanel extends PagePanel {

    private final TableModel emptyTableModel;
    private JTable table;

    TablePagePanel(TopComponent parentDialog, String helpId, String titlePrefix, String defaultInformationText) {
        super(parentDialog, helpId, titlePrefix);
        emptyTableModel = new DefaultTableModel(1, 1);
        emptyTableModel.setValueAt(defaultInformationText, 0, 0);
        table = new JTable(emptyTableModel);
    }

    /**
     * Notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeChanged(final ProductNodeEvent event) {
        if (event.getSourceNode() == getRaster() || event.getSourceNode() == getProduct()) {
            updateComponents();
        }
    }

    protected JTable getTable() {
        return table;
    }

    protected void showNoInformationAvailableMessage() {
        table.setModel(emptyTableModel);
    }

    protected void setColumnRenderer(int column, TableCellRenderer renderer) {
        getTable().getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    static class RendererFactory {

        final static int ALTERNATING_ROWS = 1;
        final static int TOOLTIP_AWARE = 2;
        final static int WRAP_TEXT = 4;

        static TableCellRenderer createRenderer(int spec) {
            return createRenderer(spec, null);
        }

        static TableCellRenderer createRenderer(int spec, Object configurator) {
            final List<RendererStrategy> strategies = new ArrayList<>();
            if ((spec & WRAP_TEXT) == WRAP_TEXT) {
                strategies.add(new WrapTextRenderer((ArrayList<Integer>) configurator));
            }
            if ((spec & ALTERNATING_ROWS) == ALTERNATING_ROWS) {
                strategies.add(new AlternatingRowsRenderer());
            }
            if ((spec & TOOLTIP_AWARE) == TOOLTIP_AWARE) {
                strategies.add(new TooltipAwareRenderer());
            }

            return new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JTextArea textArea = new JTextArea(value.toString());
                    for (RendererStrategy strategy : strategies) {
                        strategy.customise(table, textArea, value.toString(), row, column);
                    }
                    careForEmptyLines(value, textArea);
                    return textArea;
                }

                public void careForEmptyLines(Object value, JTextArea textArea) {
                    if (StringUtils.isNullOrEmpty(value.toString())) {
                        // imbecile code, but necessary in order to show empty lines
                        textArea.setText("____");
                        textArea.setForeground(textArea.getBackground());
                    }
                }
            };
        }
    }

    private static interface RendererStrategy {

        void customise(JTable table, JTextArea component, String value, int rowIndex, int colIndex);
    }

    private static class AlternatingRowsRenderer implements RendererStrategy {

        private Color brightBackground;
        private Color mediumBackground;

        public AlternatingRowsRenderer() {
            brightBackground = Color.white;
            mediumBackground = new Color((14 * brightBackground.getRed()) / 15,
                                         (14 * brightBackground.getGreen()) / 15,
                                         (14 * brightBackground.getBlue()) / 15);
        }

        protected Color getBackground(int row) {
            if (row % 2 == 0) {
                return mediumBackground;
            } else {
                return brightBackground;
            }
        }

        @Override
        public void customise(JTable table, JTextArea component, String value, int rowIndex, int colIndex) {
            component.setBorder(new EmptyBorder(0, 0, 0, 0));
            component.setBackground(getBackground(rowIndex));
        }
    }

    private static class TooltipAwareRenderer implements RendererStrategy {

        @Override
        public void customise(JTable table, JTextArea component, String value, int rowIndex, int colIndex) {
            component.setToolTipText(value);
        }
    }

    private static class WrapTextRenderer implements RendererStrategy {

        List<Integer> wrappingRows;

        private WrapTextRenderer(ArrayList<Integer> wrappingRows) {
            this.wrappingRows = wrappingRows;
        }

        @Override
        public void customise(JTable table, JTextArea component, String value, int rowIndex, int colIndex) {
            if (!wrappingRows.contains(rowIndex)) {
                return;
            }
            component.setLineWrap(true);
            component.setWrapStyleWord(true);

            int max = 230;
//            if ((table).getCellSpanAt(rowIndex, colIndex).getColumnSpan() == table.getColumnCount()) {
//                max *= 2;
//            }

            int numRows = countLines(component, value, max);
            if (numRows > 1) {
                int rowHeight = table.getRowHeight() * numRows;
                table.setRowHeight(rowIndex, Math.max(table.getRowHeight(rowIndex), rowHeight));
            }
        }

        private static int countLines(JTextArea component, String value, int max) {
            int lineCount = 0;
            FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
            for (String s : value.split("\n")) {
                lineCount += 1 + fontMetrics.stringWidth(s) / max;
            }
            return lineCount;
        }
    }

    static interface TableRow {}

    protected static abstract class TablePagePanelModel implements TableModel {

        private List<TableModelListener> listeners = new ArrayList<>();
        protected List<TableRow> rows = new ArrayList<>();

        public void addRow(TableRow row) {
            rows.add(row);
            notifyListeners();
        }

        public void clear() {
            rows.clear();
            notifyListeners();
        }

        private void notifyListeners() {
            for (TableModelListener listener : listeners) {
                listener.tableChanged(new TableModelEvent(this));
            }
        }

        @Override
        public void addTableModelListener(TableModelListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTableModelListener(TableModelListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void setValueAt(Object invalid1, int invalid2, int invalid3) {
            throw new IllegalStateException("Table must be non-editable!");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

    }

}
