package org.esa.snap.productlibrary.rcp.toolviews.timeline;

import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseQueryListener;
import org.esa.snap.productlibrary.rcp.toolviews.model.DatabaseStatistics;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * shows products on a time line
 */
public class TimelinePanel extends JPanel implements DatabaseQueryListener {

    private final DatabaseStatistics stats;
    private JPanel currentPanel = null;
    private JPanel timelinePanel;
    private JPanel yearsPanel;
    private JPanel monthsPanel;

    public TimelinePanel(final DatabaseStatistics stats) {
        this.stats = stats;
        createPanel();
        setMaximumSize(new Dimension(500, 30));
    }

    private void createPanel() {
        setLayout(new BorderLayout());

        final JPanel centrePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        timelinePanel = new TimelinePlot(stats);
        yearsPanel = new YearsPlot(stats);
        monthsPanel = new MonthsPlot(stats);
        gbc.weightx = 10;
        gbc.weighty = 10;
        centrePanel.add(timelinePanel, gbc);
        //centrePanel.add(yearsPanel, gbc);
        centrePanel.add(monthsPanel, gbc);
        hideShowPanels(timelinePanel);

        this.add(centrePanel, BorderLayout.CENTER);
        this.add(createControlPanel(), BorderLayout.WEST);
    }

    private JPanel createControlPanel() {
        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
        final JRadioButton timelineButton = new JRadioButton("Timeline", true);
        final JRadioButton yearsButton = new JRadioButton("Years", false);
        final JRadioButton monthsButton = new JRadioButton("Months", false);

        final ButtonGroup group = new ButtonGroup();
        group.add(timelineButton);
        group.add(yearsButton);
        group.add(monthsButton);

        timelineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideShowPanels(timelinePanel);
            }
        });

        yearsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideShowPanels(yearsPanel);
            }
        });

        monthsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideShowPanels(monthsPanel);
            }
        });

        controlPanel.add(timelineButton);
        //controlPanel.add(yearsButton);
        controlPanel.add(monthsButton);

        return controlPanel;
    }

    private void hideShowPanels(final JPanel selectedPanel) {
        currentPanel = selectedPanel;
        timelinePanel.setVisible(currentPanel.equals(timelinePanel));
        yearsPanel.setVisible(currentPanel.equals(yearsPanel));
        monthsPanel.setVisible(currentPanel.equals(monthsPanel));
    }

    public void notifyNewEntryListAvailable() {
        currentPanel.updateUI();
    }

    public void notifyNewMapSelectionAvailable() {

    }
}
