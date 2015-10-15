/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.graphbuilder.rcp.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.db.CommonReaders;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.gpf.ProcessTimeMonitor;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileTable;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphDialog;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphExecuter;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphNode;
import org.esa.snap.graphbuilder.rcp.dialogs.support.GraphsMenu;
import org.esa.snap.graphbuilder.rcp.dialogs.support.ProgressBarProgressMonitor;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.FileChooserFactory;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides the dialog for executing a graph on a list of products
 */
public class BatchGraphDialog extends ModelessDialog implements GraphDialog {

    private final AppContext appContext;
    protected final ProductSetPanel productSetPanel;
    protected final List<GraphExecuter> graphExecutorList = new ArrayList<>(10);

    protected final static Path defaultGraphPath = ResourceUtils.getGraphFolder("");

    private final JTabbedPane tabbedPane;
    private final JLabel statusLabel;
    private final JLabel bottomStatusLabel;
    private final JPanel progressPanel;
    private final JProgressBar progressBar;
    private final JLabel progressMsgLabel;
    private ProgressBarProgressMonitor progBarMonitor = null;

    private Map<File, File[]> slaveFileMap = null;
    private final List<BatchProcessListener> listenerList = new ArrayList<>(1);
    private final boolean closeOnDone;
    private boolean skipExistingTargetFiles = false;

    private boolean isProcessing = false;
    protected File graphFile;
    protected boolean openProcessedProducts = true;

    public BatchGraphDialog(final AppContext theAppContext, final String title, final String helpID,
                            final boolean closeOnDone) {
        super(theAppContext.getApplicationWindow(), title, ID_YES | ID_APPLY_CLOSE_HELP, helpID);
        this.appContext = theAppContext;
        this.closeOnDone = closeOnDone;

        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                ValidateAllNodes();
            }
        });
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // status
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        bottomStatusLabel = new JLabel("");
        getButtonPanel().add(bottomStatusLabel, 0);

        // progress Bar
        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout(2, 2));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressMsgLabel = new JLabel();
        progressPanel.add(progressMsgLabel, BorderLayout.NORTH);
        final JButton progressCancelBtn = new JButton("Cancel");
        progressCancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                CancelProcessing();
            }
        });
        progressPanel.add(progressCancelBtn, BorderLayout.EAST);
        progressPanel.setVisible(false);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        productSetPanel = new ProductSetPanel(appContext, null, new FileTable(), true, true);
        tabbedPane.add("I/O Parameters", productSetPanel);

        getButton(ID_APPLY).setText("Run");
        getButton(ID_YES).setText("Load Graph");

        if (getJDialog().getJMenuBar() == null) {
            final GraphsMenu operatorMenu =  new GraphsMenu(getJDialog(), this);

            getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        }

        setContent(mainPanel);
        super.getJDialog().setMinimumSize(new Dimension(400, 300));
    }

    @Override
    public int show() {
        return super.show();
    }

    @Override
    public void hide() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
        notifyMSG(BatchProcessListener.BatchMSG.CLOSE);
        super.hide();
    }

    @Override
    public void onApply() {
        if (isProcessing) return;

        productSetPanel.onApply();

        skipExistingTargetFiles = productSetPanel.isSkippingExistingTargetFiles();

        try {
            DoProcessing();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void addListener(final BatchProcessListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(final BatchProcessListener listener) {
        listenerList.remove(listener);
    }

    private void notifyMSG(final BatchProcessListener.BatchMSG msg, final String text) {
        for (final BatchProcessListener listener : listenerList) {
            listener.notifyMSG(msg, text);
        }
    }

    private void notifyMSG(final BatchProcessListener.BatchMSG msg) {
        for (final BatchProcessListener listener : listenerList) {
            listener.notifyMSG(msg, productSetPanel.getFileList(), getAllBatchProcessedTargetProducts());
        }
    }

    /**
     * OnLoad
     */
    @Override
    protected void onYes() {
        LoadGraph();
    }

    public void setInputFiles(final File[] productFileList) {
        productSetPanel.setProductFileList(productFileList);
    }

    public void setInputFiles(final ProductEntry[] productEntryList) {
        productSetPanel.setProductEntryList(productEntryList);
    }

    public void setTargetFolder(final File path) {
        productSetPanel.setTargetFolder(path);
    }

    public void LoadGraph() {
        if (isProcessing) return;

        final File file = getFilePath(this.getContent(), "Graph File");
        if (file != null) {
            LoadGraph(file);
        }
    }

    public void LoadGraph(final File file) {
        try {
            graphFile = file;

            initGraphs();
            addGraphTabs("", true);

            setTitle(file.getName());
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to load graph "+file.toString(), e);
        }
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle("Batch Processing : " + title);
    }

    public boolean canSaveGraphs() {
        return false;
    }

    public void SaveGraph() {
    }

    public String getGraphAsString() throws GraphException, IOException {
        if(graphExecutorList.size() > 0) {
            return graphExecutorList.get(0).getGraphAsString();
        }
        return "";
    }

    private static File getFilePath(Component component, String title) {

        final File graphPath = new File(getPref("batch.last_graph_path", defaultGraphPath.toFile().getAbsolutePath()));
        final JFileChooser chooser = FileChooserFactory.getInstance().createFileChooser(graphPath);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(title);
        if (chooser.showDialog(component, "ok") == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            setPref("batch.last_graph_path", file.getAbsolutePath());
            return file;
        }
        return null;
    }

    private static String getPref(final String id, final String defaultStr) {
        return SnapApp.getDefault().getPreferences().get(id, defaultStr);
    }

    private static void setPref(final String id, final String value) {
        SnapApp.getDefault().getPreferences().put(id, value);
    }

    @Override
    protected void onClose() {
        CancelProcessing();

        super.onClose();
    }

    void initGraphs() {
        try {
            deleteGraphs();
            createGraphs();
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
        }
    }

    /**
     * Validates the input and then call the GPF to execute the graph
     */
    private void DoProcessing() {

        if (ValidateAllNodes()) {

            MemUtils.freeAllMemory();

            progressBar.setValue(0);
            progBarMonitor = new ProgressBarProgressMonitor(progressBar, progressMsgLabel, progressPanel);

            final SwingWorker processThread = new ProcessThread(progBarMonitor);
            processThread.execute();

        } else {
            if (statusLabel.getText() != null && !statusLabel.getText().isEmpty())
                showErrorDialog(statusLabel.getText());
        }
    }

    private void CancelProcessing() {
        if (progBarMonitor != null)
            progBarMonitor.setCanceled(true);
    }

    private void deleteGraphs() {
        for (GraphExecuter gex : graphExecutorList) {
            gex.ClearGraph();
        }
        graphExecutorList.clear();
    }

    /**
     * Loads a new graph from a file
     *
     * @param executer the GraphExcecuter
     * @param graphFile     the graph file to load
     * @param addUI    add a user interface
     */
    protected void LoadGraph(final GraphExecuter executer, final File graphFile, final boolean addUI) {
        try {
            executer.loadGraph(graphFile, addUI);

        } catch (GraphException e) {
            showErrorDialog(e.getMessage());
        }
    }

    private boolean ValidateAllNodes() {
        if (isProcessing) return false;
        if (productSetPanel == null)
            return false;
        if (graphExecutorList.isEmpty())
            return false;

        boolean result;
        statusLabel.setText("");
        try {
            cloneGraphs();

            assignParameters();

            // first graph must pass
            result = graphExecutorList.get(0).InitGraph();

        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
            bottomStatusLabel.setText("");
            result = false;
        }
        return result;
    }

    private void openTargetProducts() {
        final File[] fileList = getAllBatchProcessedTargetProducts();
        if (fileList != null && fileList.length > 0) {
            for (File file : fileList) {
                try {

                    final Product product = CommonReaders.readProduct(file);
                    if (product != null) {
                        appContext.getProductManager().addProduct(product);
                    }
                } catch (IOException e) {
                    showErrorDialog(e.getMessage());
                }
            }
        }
    }

    protected ProductSetPanel getProductSetPanel() {
        return productSetPanel;
    }

    public void setTargetProductNameSuffix(final String suffix) {
        productSetPanel.setTargetProductNameSuffix(suffix);
    }

    void createGraphs() throws GraphException {
        try {
            final GraphExecuter graphEx = new GraphExecuter();
            LoadGraph(graphEx, graphFile, true);
            graphExecutorList.add(graphEx);
        } catch (Exception e) {
            throw new GraphException(e.getMessage());
        }
    }

    private void addGraphTabs(final String title, final boolean addUI) {

        if (graphExecutorList.isEmpty()) {
            return;
        }

        tabbedPane.setSelectedIndex(0);
        while (tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(tabbedPane.getTabCount() - 1);
        }

        final GraphExecuter graphEx = graphExecutorList.get(0);
        for (GraphNode n : graphEx.GetGraphNodes()) {
            if (n.GetOperatorUI() == null)
                continue;
            if (n.getNode().getOperatorName().equals("Read") || n.getNode().getOperatorName().equals("Write")
                    || n.getNode().getOperatorName().equals("ProductSet-Reader")) {
                n.setOperatorUI(null);
                continue;
            }

            if (addUI) {
                String tabTitle = title;
                if (tabTitle.isEmpty())
                    tabTitle = n.getOperatorName();
                tabbedPane.addTab(tabTitle, null,
                        n.GetOperatorUI().CreateOpTab(n.getOperatorName(), n.getParameterMap(), appContext),
                        n.getID() + " Operator");
            }
        }
    }

    public void setSlaveFileMap(Map<File, File[]> fileMap) {
        slaveFileMap = fileMap;
    }

    protected void assignParameters() {
        final File[] fileList = productSetPanel.getFileList();
        int graphIndex = 0;
        for (File f : fileList) {
            String name;
            final Object o = productSetPanel.getValueAt(graphIndex, 0);
            if (o instanceof String)
                name = (String) o;
            else
                name = FileUtils.getFilenameWithoutExtension(f);

            final File targetFolder = productSetPanel.getTargetFolder();
            if(!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            final File targetFile = new File(targetFolder, name);
            final String targetFormat = productSetPanel.getTargetFormat();

            setIO(graphExecutorList.get(graphIndex),
                    "Read", f,
                    "Write", targetFile, targetFormat);
            if (slaveFileMap != null) {
                final File[] slaveFiles = slaveFileMap.get(f);
                if (slaveFiles != null) {
                    setSlaveIO(graphExecutorList.get(graphIndex),
                            "ProductSet-Reader", f, slaveFiles);
                }
            }
            ++graphIndex;
        }
    }

    protected static void setIO(final GraphExecuter graphEx,
                                final String readID, final File readPath,
                                final String writeID, final File writePath,
                                final String format) {
        final GraphNode readNode = graphEx.getGraphNodeList().findGraphNodeByOperator(readID);
        if (readNode != null) {
            graphEx.setOperatorParam(readNode.getID(), "file", readPath.getAbsolutePath());
        }

        if (writeID != null) {
            final GraphNode writeNode = graphEx.getGraphNodeList().findGraphNodeByOperator(writeID);
            if (writeNode != null) {
                if (format != null)
                    graphEx.setOperatorParam(writeNode.getID(), "formatName", format);
                graphEx.setOperatorParam(writeNode.getID(), "file", writePath.getAbsolutePath());
            }
        }
    }

    /**
     * For coregistration
     *
     * @param graphEx      the graph executer
     * @param productSetID the product set reader
     * @param masterFile   master file
     * @param slaveFiles   slave file list
     */
    protected static void setSlaveIO(final GraphExecuter graphEx, final String productSetID,
                                     final File masterFile, final File[] slaveFiles) {
        final GraphNode productSetNode = graphEx.getGraphNodeList().findGraphNodeByOperator(productSetID);
        if (productSetNode != null) {
            StringBuilder str = new StringBuilder(masterFile.getAbsolutePath());
            for (File slaveFile : slaveFiles) {
                str.append(',');
                str.append(slaveFile.getAbsolutePath());
            }
            graphEx.setOperatorParam(productSetNode.getID(), "fileList", str.toString());
        }
    }

    protected void cloneGraphs() throws Exception {
        final GraphExecuter graphEx = graphExecutorList.get(0);
        for (int graphIndex = 1; graphIndex < graphExecutorList.size(); ++graphIndex) {
            final GraphExecuter cloneGraphEx = graphExecutorList.get(graphIndex);
            cloneGraphEx.ClearGraph();
        }
        graphExecutorList.clear();
        graphExecutorList.add(graphEx);

        final File[] fileList = productSetPanel.getFileList();
        for (int graphIndex = 1; graphIndex < fileList.length; ++graphIndex) {

            final GraphExecuter cloneGraphEx = new GraphExecuter();
            LoadGraph(cloneGraphEx, graphFile, false);
            graphExecutorList.add(cloneGraphEx);

            // copy UI parameter to clone
            final List<GraphNode> cloneGraphNodes = cloneGraphEx.GetGraphNodes();
            for (GraphNode cloneNode : cloneGraphNodes) {
                final GraphNode node = graphEx.getGraphNodeList().findGraphNode(cloneNode.getID());
                if (node != null)
                     cloneNode.setOperatorUI(node.GetOperatorUI());
            }
        }
    }

    public File[] getAllBatchProcessedTargetProducts() {
        final List<File> targetFileList = new ArrayList<>();
        for (GraphExecuter graphEx : graphExecutorList) {
            targetFileList.addAll(graphEx.getProductsToOpenInDAT());
        }
        return targetFileList.toArray(new File[targetFileList.size()]);
    }

    void cleanUpTempFiles() {

    }

    /////

    private class ProcessThread extends SwingWorker<Boolean, Object> {

        private final ProgressMonitor pm;
        private ProcessTimeMonitor timeMonitor = new ProcessTimeMonitor();
        private boolean errorOccured = false;
        final List<String> errMsgs = new ArrayList<>();

        public ProcessThread(final ProgressMonitor pm) {
            this.pm = pm;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            pm.beginTask("Processing Graph...", 100 * graphExecutorList.size());
            try {
                timeMonitor.start();
                isProcessing = true;

                final File[] existingFiles = productSetPanel.getTargetFolder().listFiles();

                final File[] fileList = productSetPanel.getFileList();
                int graphIndex = 0;
                for (GraphExecuter graphEx : graphExecutorList) {
                    if (pm.isCanceled()) break;

                    if(shouldSkip(graphEx, existingFiles)) {
                        continue;
                    }

                    try {
                        final String nOfm = String.valueOf(graphIndex + 1) + " of " + graphExecutorList.size() + ' ';
                        final String statusText = "Processing " + nOfm + fileList[graphIndex].getName();
                        statusLabel.setText(statusText);
                        notifyMSG(BatchProcessListener.BatchMSG.UPDATE, statusText);

                        MemUtils.freeAllMemory();

                        graphEx.InitGraph();

                        graphEx.executeGraph(new SubProgressMonitor(pm, 100));

                        graphEx.disposeGraphContext();
                    } catch (Exception e) {
                        System.out.print(e.getMessage());
                        String filename = fileList[graphIndex].getName();
                        errMsgs.add(filename + " -> " + e.getMessage());
                    }

                    graphEx = null;
                    ++graphIndex;

                    // calculate time remaining
                    final long duration = timeMonitor.getCurrentDuration();
                    final double timePerGraph = duration / (double) graphIndex;
                    final long timeLeft = (long) (timePerGraph * (graphExecutorList.size() - graphIndex));
                    if (timeLeft > 0) {
                        String remainingStr = "Estimated " + ProcessTimeMonitor.formatDuration(timeLeft) + " remaining";
                        if (!errMsgs.isEmpty())
                            remainingStr += " (Errors occurred)";
                        bottomStatusLabel.setText(remainingStr);
                    }
                }

                MemUtils.freeAllMemory();

            } catch (Exception e) {
                System.out.print(e.getMessage());
                if (e.getMessage() != null && !e.getMessage().isEmpty())
                    statusLabel.setText(e.getMessage());
                else
                    statusLabel.setText(e.toString());
                errorOccured = true;
            } finally {
                isProcessing = false;
                pm.done();
            }
            return true;
        }

        @Override
        public void done() {
            if (!errorOccured) {
                final long duration = timeMonitor.stop();
                statusLabel.setText("Processing completed in " + ProcessTimeMonitor.formatDuration(duration));
                bottomStatusLabel.setText("");

                if(openProcessedProducts) {
                    openTargetProducts();
                }
            }
            if (!errMsgs.isEmpty()) {
                final StringBuilder msg = new StringBuilder("The following errors occurred:\n");
                for (String errStr : errMsgs) {
                    msg.append(errStr);
                    msg.append('\n');
                }
                showErrorDialog(msg.toString());
            }
            cleanUpTempFiles();
            notifyMSG(BatchProcessListener.BatchMSG.DONE);
            if (closeOnDone)
                close();
        }

        private boolean shouldSkip(final GraphExecuter graphEx, final File[] existingFiles) {
            if(skipExistingTargetFiles) {
                final File[] targetFiles = graphEx.getPotentialOutputFiles();

                if(existingFiles != null) {
                    boolean allTargetsExist = true;
                    for (File targetFile : targetFiles) {
                        boolean fileExists = false;
                        for(File existingFile : existingFiles) {
                            if(existingFile.getAbsolutePath().startsWith(targetFile.getAbsolutePath())) {
                                fileExists = true;
                                break;
                            }
                        }
                        if (!fileExists) {
                            allTargetsExist = false;
                            break;
                        }
                    }
                    return allTargetsExist;
                }
            }
            return false;
        }
    }

    public interface BatchProcessListener {

        public enum BatchMSG {DONE, UPDATE, CLOSE}

        public void notifyMSG(final BatchMSG msg, final File[] inputFileList, final File[] targetFileList);

        public void notifyMSG(final BatchMSG msg, final String text);
    }

}
