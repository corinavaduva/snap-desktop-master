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
package org.esa.snap.productlibrary.rcp.toolviews;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductDB;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.db.SQLUtils;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseQueryListener;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.UIUtils;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**

 */
public final class DatabasePane extends JPanel {

    private final JTextField nameField = new JTextField();
    private final JList missionJList = new JList();
    private final JList productTypeJList = new JList();
    private final JComboBox acquisitionModeCombo = new JComboBox(new String[]{DBQuery.ALL_MODES});
    private final JComboBox passCombo = new JComboBox(new String[]{
            DBQuery.ALL_PASSES, DBQuery.ASCENDING_PASS, DBQuery.DESCENDING_PASS});
    private final JTextField trackField = new JTextField();

    private final JXDatePicker startDateBox = new JXDatePicker();
    private final JXDatePicker endDateBox = new JXDatePicker();
    private final JComboBox polarizationCombo = new JComboBox(new String[]{
            DBQuery.ANY, DBQuery.QUADPOL, DBQuery.DUALPOL, DBQuery.HHVV, DBQuery.HHHV, DBQuery.VVVH, "HH", "VV", "HV", "VH"});
    private final JComboBox calibrationCombo = new JComboBox(new String[]{
            DBQuery.ANY, DBQuery.CALIBRATED, DBQuery.NOT_CALIBRATED});
    private final JComboBox orbitCorrectionCombo = new JComboBox(new String[]{
            DBQuery.ANY, DBQuery.ORBIT_PRELIMINARY, DBQuery.ORBIT_PRECISE, DBQuery.ORBIT_VERIFIED});

    private final JComboBox metadataNameCombo = new JComboBox();
    private final JTextField metdataValueField = new JTextField();
    private final JTextArea metadataArea = new JTextArea();
    private final JButton addMetadataButton = new JButton("+");
    private final JButton updateButton = new JButton(UIUtils.loadImageIcon("icons/ViewRefresh16.png"));
    private final JTextArea productText = new JTextArea();

    private ProductDB db;
    private DBQuery dbQuery = new DBQuery();
    private ProductEntry[] productEntryList = null;
    boolean modifyingCombos = false;

    private final List<DatabaseQueryListener> listenerList = new ArrayList<>(1);

    public DatabasePane() {
        try {
            missionJList.setFixedCellWidth(100);
            createPanel();

            missionJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (modifyingCombos || event.getValueIsAdjusting()) return;
                    updateProductTypeCombo();
                    queryDatabase();
                }
            });
            productTypeJList.setFixedCellWidth(100);
            productTypeJList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    if (modifyingCombos || event.getValueIsAdjusting()) return;
                    queryDatabase();
                }
            });
            addComboListener(acquisitionModeCombo);
            addComboListener(passCombo);
            addComboListener(polarizationCombo);
            addComboListener(calibrationCombo);
            addComboListener(orbitCorrectionCombo);
            
            addMetadataButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addMetadataText();
                }
            });
            updateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    queryDatabase();
                }
            });
        } catch (Throwable t) {
            handleException(t);
        }
    }

    private void addComboListener(final JComboBox combo) {
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (modifyingCombos || event.getStateChange() == ItemEvent.DESELECTED) return;
                queryDatabase();
            }
        });
    }

    /**
     * Adds a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be added.
     */
    public void addListener(final DatabaseQueryListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a <code>DatabasePaneListener</code>.
     *
     * @param listener the <code>DatabasePaneListener</code> to be removed.
     */
    public void removeListener(final DatabaseQueryListener listener) {
        listenerList.remove(listener);
    }

    private void notifyQuery() {
        for (final DatabaseQueryListener listener : listenerList) {
            listener.notifyNewEntryListAvailable();
        }
    }

    private static void handleException(Throwable t) {
        t.printStackTrace();
        final SnapApp app = SnapApp.getDefault();
        if (app != null) {
            SnapDialogs.showError(t.getMessage());
        }
    }

    private void createPanel() {
        setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        JLabel label;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(new JLabel("Mission:"), gbc);
        gbc.gridx = 1;
        this.add(new JLabel("Product Type:"), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        this.add(new JScrollPane(missionJList), gbc);
        gbc.gridx = 1;
        this.add(new JScrollPane(productTypeJList), gbc);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Product Name:", nameField);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Acquisition Mode:", acquisitionModeCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Pass:", passCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Track:", trackField);
        label.setHorizontalAlignment(JLabel.RIGHT);

        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Start Date:", startDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "End Date:", endDateBox);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Polarization:", polarizationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Calibration:", calibrationCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        label = DialogUtils.addComponent(this, gbc, "Orbit Correction:", orbitCorrectionCombo);
        label.setHorizontalAlignment(JLabel.RIGHT);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        this.add(createFreeSearchPanel(), gbc);

        gbc.gridy++;
        final JPanel productDetailsPanel = new JPanel(new BorderLayout());
        productDetailsPanel.setBorder(BorderFactory.createTitledBorder("Product Details"));
        productText.setLineWrap(true);
        productText.setRows(4);
        productText.setBackground(getBackground());
        productDetailsPanel.add(productText, BorderLayout.CENTER);
        this.add(productDetailsPanel, gbc);

        //DialogUtils.fillPanel(this, gbc);
    }

    private JPanel createFreeSearchPanel() {
        final JPanel freeSearchPanel = new JPanel(new GridBagLayout());
        freeSearchPanel.setBorder(BorderFactory.createTitledBorder("Metadata SQL Query"));
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        freeSearchPanel.add(metadataNameCombo, gbc);
        metadataNameCombo.setPrototypeDisplayValue("123456789012");
        gbc.gridx = 1;
        freeSearchPanel.add(metdataValueField, gbc);
        metdataValueField.setColumns(10);
        gbc.gridx = 2;
        freeSearchPanel.add(addMetadataButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        freeSearchPanel.add(metadataArea, gbc);
        metadataArea.setBorder(new LineBorder(Color.BLACK));
        metadataArea.setLineWrap(true);
        metadataArea.setRows(4);
        metadataArea.setToolTipText("Use AND,OR,NOT and =,<,>,<=,>-");
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        freeSearchPanel.add(updateButton, gbc);

        DialogUtils.fillPanel(freeSearchPanel, gbc);
        return freeSearchPanel;
    }

    private void connectToDatabase() throws Exception {
        db = ProductDB.instance();

        refresh();
    }

    public ProductDB getDB() {
        if (db == null) {
            queryDatabase();
        }
        return db;
    }

    public void refresh() {
        try {
            if(!db.isReady())
                return;

            boolean origState = lockCombos(true);

            if (metadataNameCombo.getItemCount() == 0) {
                final String[] metadataNames = db.getMetadataNames();
                for (String name : metadataNames) {
                    metadataNameCombo.insertItemAt(name, metadataNameCombo.getItemCount());
                }
            }

            updateMissionCombo();
            lockCombos(origState);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    private boolean lockCombos(boolean flag) {
        final boolean origState = modifyingCombos;
        modifyingCombos = flag;
        return origState;
    }

    private void updateMissionCombo() throws SQLException {
        boolean origState = lockCombos(true);
        try {
            missionJList.removeAll();
            missionJList.setListData(SQLUtils.prependString(DBQuery.ALL_MISSIONS, db.getAllMissions()));
        } finally {
            lockCombos(origState);
        }
    }

    private void updateProductTypeCombo() {
        boolean origState = lockCombos(true);
        try {
            productTypeJList.removeAll();
            acquisitionModeCombo.removeAllItems();

            final String selectedMissions[] = toStringArray(missionJList.getSelectedValuesList());
            String[] productTypeList;
            String[] acquisitionModeList;
            if (StringUtils.contains(selectedMissions, DBQuery.ALL_MISSIONS)) {
                productTypeList = db.getAllProductTypes();
                acquisitionModeList = db.getAllAcquisitionModes();
            } else {
                productTypeList = db.getProductTypes(selectedMissions);
                acquisitionModeList = db.getAcquisitionModes(selectedMissions);
            }
            productTypeJList.setListData(SQLUtils.prependString(DBQuery.ALL_PRODUCT_TYPES, productTypeList));
            final String[] modeItems = SQLUtils.prependString(DBQuery.ALL_MODES, acquisitionModeList);
            for (String item : modeItems) {
                acquisitionModeCombo.addItem(item);
            }

        } catch (Throwable t) {
            handleException(t);
        } finally {
            lockCombos(origState);
        }
    }

    private static String[] toStringArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public void setBaseDir(final File dir) {
        dbQuery.setBaseDir(dir);
        if (db != null)
            queryDatabase();
    }

    private void addMetadataText() {
        final String name = (String) metadataNameCombo.getSelectedItem();
        final String value = metdataValueField.getText();
        if (!name.isEmpty() && !value.isEmpty()) {
            if (metadataArea.getText().length() > 0) {
                metadataArea.append(" AND ");
            }
            if(value.matches("-?\\d+(\\.\\d+)?")) {     // isNumeric
                metadataArea.append(name + "=" + value + " ");
            } else {
                metadataArea.append(name + "='" + value + "' ");
            }
        }
    }

    private Calendar getDate(final JXDatePicker dateField) {
        final Date date = dateField.getDate();
        if(date == null)
            return null;
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    private void setData() {
        dbQuery.setSelectedMissions(toStringArray(missionJList.getSelectedValuesList()));
        dbQuery.setSelectedProductTypes(toStringArray(productTypeJList.getSelectedValuesList()));
        dbQuery.setSelectedName(nameField.getText());
        dbQuery.setSelectedAcquisitionMode((String) acquisitionModeCombo.getSelectedItem());
        dbQuery.setSelectedPass((String) passCombo.getSelectedItem());
        dbQuery.setSelectedTrack(trackField.getText());

        dbQuery.setStartEndDate(getDate(startDateBox), getDate(endDateBox));

        dbQuery.setSelectedPolarization((String) polarizationCombo.getSelectedItem());
        dbQuery.setSelectedCalibration((String) calibrationCombo.getSelectedItem());
        dbQuery.setSelectedOrbitCorrection((String) orbitCorrectionCombo.getSelectedItem());

        dbQuery.clearMetadataQuery();
        dbQuery.setFreeQuery(metadataArea.getText());
    }

    public void queryDatabase() {
        if (db == null) {
            try {
                connectToDatabase();
            } catch (Throwable t) {
                handleException(t);
            }
        }
        if(metadataNameCombo.getItemCount() == 0) {
            refresh();
        }

        setData();

        if (productEntryList != null) {
            ProductEntry.dispose(productEntryList);
        }
        try {
            if(db.isReady()) {
                productEntryList = dbQuery.queryDatabase(db);
                notifyQuery();
            }
        } catch (Throwable t) {
            handleException(t);
        }
    }

    public void setSelectionRect(final GeoPos[] selectionBox) {
        dbQuery.setSelectionRect(selectionBox);
        dbQuery.setReturnAllIfNoIntersection(true);
        queryDatabase();
    }

    public ProductEntry[] getProductEntryList() {
        return productEntryList;
    }

    public DBQuery getDBQuery() {
        setData();
        return dbQuery;
    }

    public void findSlices(final int dataTakeId) {
        metadataArea.setText(AbstractMetadata.data_take_id+"="+dataTakeId);

        dbQuery.setSelectionRect(null);
        queryDatabase();

        metadataArea.setText("");
    }

    public void setDBQuery(final DBQuery query) throws Exception {
        if (query == null) return;
        dbQuery = query;
        if (db == null) {
            connectToDatabase();
        }
        boolean origState = lockCombos(true);
        try {
            missionJList.setSelectedIndices(findIndices(missionJList, dbQuery.getSelectedMissions()));
            updateProductTypeCombo();
            productTypeJList.setSelectedIndices(findIndices(productTypeJList, dbQuery.getSelectedProductTypes()));
            acquisitionModeCombo.setSelectedItem(dbQuery.getSelectedAcquisitionMode());
            passCombo.setSelectedItem(dbQuery.getSelectedPass());
            startDateBox.setDate(dbQuery.getStartDate().getTime());
            endDateBox.setDate(dbQuery.getEndDate().getTime());

            polarizationCombo.setSelectedItem(dbQuery.getSelectedPolarization());
            calibrationCombo.setSelectedItem(dbQuery.getSelectedCalibration());
            orbitCorrectionCombo.setSelectedItem(dbQuery.getSelectedOrbitCorrection());

            metadataArea.setText(dbQuery.getFreeQuery());
        } finally {
            lockCombos(origState);
        }
    }

    private static int[] findIndices(final JList list, final String[] values) {
        final int size = list.getModel().getSize();
        final List<Integer> indices = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final String str = (String) list.getModel().getElementAt(i);
            if (StringUtils.contains(values, str)) {
                indices.add(i);
            }
        }
        final int[] intIndices = new int[indices.size()];
        for (int i = 0; i < indices.size(); ++i) {
            intIndices[i] = indices.get(i);
        }
        return intIndices;
    }

    public void updateProductSelectionText(final ProductEntry[] selections) {
        if (selections != null && selections.length == 1) {
            final ProductEntry entry = selections[0];
            final StringBuilder text = new StringBuilder(255);

            final MetadataElement absRoot = entry.getMetadata();
            final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, AbstractMetadata.NO_METADATA_STRING);
            final ProductData.UTC acqTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
            final int absOrbit = absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, AbstractMetadata.NO_METADATA);
            final int relOrbit = absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, AbstractMetadata.NO_METADATA);
            final String map = absRoot.getAttributeString(AbstractMetadata.map_projection, AbstractMetadata.NO_METADATA_STRING).trim();
            final int cal = absRoot.getAttributeInt(AbstractMetadata.abs_calibration_flag, AbstractMetadata.NO_METADATA);
            final int tc = absRoot.getAttributeInt(AbstractMetadata.is_terrain_corrected, AbstractMetadata.NO_METADATA);
            final int coreg = absRoot.getAttributeInt(AbstractMetadata.coregistered_stack, AbstractMetadata.NO_METADATA);

            text.append(entry.getName());
            text.append("\n\n");
            text.append(entry.getAcquisitionMode() + "   " + sampleType + '\n');
            text.append(acqTime.format());
            text.append('\n');

            text.append("Orbit: " + absOrbit);
            if (relOrbit != AbstractMetadata.NO_METADATA)
                text.append("  Track: " + relOrbit);
            text.append('\n');
            if (!map.isEmpty()) {
                text.append(map);
                text.append('\n');
            }
            if (cal == 1)
                text.append("Calibrated ");
            if (coreg == 1)
                text.append("Coregistered ");
            if (tc == 1)
                text.append("Terrain Corrected ");

            productText.setText(text.toString());
        } else {
            productText.setText("");
        }
    }
}
