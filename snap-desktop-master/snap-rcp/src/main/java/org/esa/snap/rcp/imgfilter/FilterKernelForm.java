package org.esa.snap.rcp.imgfilter;


import org.esa.snap.rcp.imgfilter.model.Filter;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * A form containing a {link @FilterKernelCanvas} and components to change size and fill value of the kernel.
 *
 * @author Norman
 */
public class FilterKernelForm extends JPanel implements Filter.Listener {

    private Filter filter;
    private CanvasMouseListener canvasMouseListener;
    private FilterKernelCanvas kernelCanvas;
    private JSpinner kernelWidthSpinner;
    private JSpinner kernelHeightSpinner;
    private JComboBox<Number> fillValueCombo;
    private double fillValue;

    private final DefaultComboBoxModel<Number> structuringFillValueModel = new DefaultComboBoxModel<>(new Number[]{0, 1});
    private final DefaultComboBoxModel<Number> kernelFillValueModel = new DefaultComboBoxModel<>(new Number[]{-5., -4., -3., -2., -1., 0., 1., 2., 3., 4., 5.});

    public FilterKernelForm(Filter filter) {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));

        this.filter = null;
        this.fillValue = 0.0;
        setFilter(filter);
    }

    public double getFillValue() {
        return fillValue;
    }

    public void setFillValue(double fillValue) {
        double fillValueOld = this.fillValue;
        this.fillValue = fillValue;
        firePropertyChange("fillValue", fillValueOld, fillValue);
    }

    @Override
    public void filterChanged(Filter filter, String propertyName) {

        if (this.filter != filter) {
            return;
        }

        boolean structureElement = filter.getOperation() != Filter.Operation.CONVOLVE;
        fillValueCombo.setModel(structureElement ? structuringFillValueModel : kernelFillValueModel);

        int kernelWidth = filter.getKernelWidth();
        int kernelHeight = filter.getKernelHeight();

        if (kernelWidthSpinner != null
                && ((Number) kernelWidthSpinner.getValue()).intValue() != kernelWidth) {
            kernelWidthSpinner.setValue(kernelWidth);
        }
        if (kernelHeightSpinner != null
                && ((Number) kernelHeightSpinner.getValue()).intValue() != kernelHeight) {
            kernelHeightSpinner.setValue(kernelHeight);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 320);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {

        Filter filterOld = this.filter;
        if (filterOld != filter) {
            if (this.filter != null) {
                this.filter.removeListener(this);
            }

            this.filter = filter;
            initUI();

            if (this.filter != null) {
                this.filter.addListener(this);
            }

            firePropertyChange("filter", filterOld, this.filter);
        }
    }

    private void initUI() {
        removeAll();

        if (kernelCanvas != null && canvasMouseListener != null) {
            kernelCanvas.removeMouseListener(canvasMouseListener);
            kernelCanvas.removeMouseMotionListener(canvasMouseListener);
            kernelCanvas = null;
        }
        fillValueCombo = null;
        kernelHeightSpinner = null;
        kernelWidthSpinner = null;

        if (filter == null) {
            invalidate();
            revalidate();
            repaint();
            return;
        }

        kernelCanvas = new FilterKernelCanvas(filter);

        if (canvasMouseListener == null) {
            canvasMouseListener = new CanvasMouseListener();
        }
        kernelCanvas.addMouseListener(canvasMouseListener);
        if (filter.isEditable()) {
            kernelCanvas.addMouseMotionListener(canvasMouseListener);
        }

        if (filter.isEditable()) {
            boolean structureElement = filter.getOperation() != Filter.Operation.CONVOLVE;
            if (structureElement) {
                fillValueCombo = new JComboBox<>(structuringFillValueModel);
                ((JTextField) fillValueCombo.getEditor().getEditorComponent()).setColumns(1);
                fillValueCombo.setEditable(false);
                fillValueCombo.setSelectedItem((int) getFillValue());
            } else {
                fillValueCombo = new JComboBox<>(kernelFillValueModel);
                ((JTextField) fillValueCombo.getEditor().getEditorComponent()).setColumns(3);
                fillValueCombo.setEditable(true);
                fillValueCombo.setSelectedItem(getFillValue());
            }
            fillValueCombo.addItemListener(e -> setFillValue(((Number) fillValueCombo.getSelectedItem()).doubleValue()));
            fillValueCombo.setToolTipText("Value that will be used to set a kernel element when clicking into the matrix");

            kernelWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            kernelWidthSpinner.setValue(filter.getKernelHeight());
            kernelWidthSpinner.addChangeListener(e -> {
                Integer kernelWidth = (Integer) kernelWidthSpinner.getValue();
                filter.setKernelSize(kernelWidth, filter.getKernelHeight());
            });
            kernelWidthSpinner.setToolTipText("Width of the kernel (number of matrix columns)");


            kernelHeightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            kernelHeightSpinner.setValue(filter.getKernelWidth());
            kernelHeightSpinner.addChangeListener(e -> {
                Integer kernelHeight = (Integer) kernelHeightSpinner.getValue();
                filter.setKernelSize(filter.getKernelWidth(), kernelHeight);
            });
            kernelHeightSpinner.setToolTipText("Height of the kernel (number of matrix rows)");

            JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
            toolBar.setFloatable(false);
            toolBar.setRollover(true);
            toolBar.add(new JLabel("Fill:"));
            toolBar.add(fillValueCombo);
            toolBar.add(Box.createHorizontalStrut(32));
            toolBar.add(new JLabel(" W:"));
            toolBar.add(kernelWidthSpinner);
            toolBar.add(new JLabel(" H:"));
            toolBar.add(kernelHeightSpinner);

            add(kernelCanvas, BorderLayout.CENTER);
            add(toolBar, BorderLayout.SOUTH);
        } else {
            add(kernelCanvas, BorderLayout.CENTER);
        }

        invalidate();
        revalidate();
        repaint();
    }

    private class CanvasMouseListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            FilterKernelCanvas kernelCanvas = (FilterKernelCanvas) e.getComponent();
            if (e.isPopupTrigger()) {
                showPopup(e, kernelCanvas);
            } else {
                kernelCanvas.getFilter().adjustKernelQuotient();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            FilterKernelCanvas kernelCanvas = (FilterKernelCanvas) e.getComponent();
            if (e.isPopupTrigger()) {
                showPopup(e, kernelCanvas);
            } else if (e.getButton() == 1 && kernelCanvas.getFilter().isEditable()) {
                setElement(kernelCanvas, e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            boolean button1 = (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
            if (button1 && kernelCanvas.getFilter().isEditable()) {
                FilterKernelCanvas kernelCanvas = (FilterKernelCanvas) e.getComponent();
                setElement(kernelCanvas, e);
            }
        }

        private void showPopup(MouseEvent e, FilterKernelCanvas kernelCanvas) {
            e.consume();
            JPopupMenu popupMenu = createPopupMenu(kernelCanvas);
            popupMenu.show(kernelCanvas, e.getX(), e.getY());
        }

        private void setElement(FilterKernelCanvas kernelCanvas, MouseEvent e) {
            int index = kernelCanvas.getKernelElementIndex(e.getX(), e.getY());
            if (index >= 0) {
                kernelCanvas.getFilter().setKernelElement(index, getFillValue());
            }
        }
    }


    protected JPopupMenu createPopupMenu(final FilterKernelCanvas kernelCanvas) {

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> {
            Clipboard systemClip = Toolkit.getDefaultToolkit().getSystemClipboard();
            systemClip.setContents(new StringSelection(kernelCanvas.getFilter().getKernelElementsAsText()), null);
        });
        popupMenu.add(copyItem);

        if (!filter.isEditable()) {
            return popupMenu;
        }

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setEnabled(isPastePossible());
        pasteItem.addActionListener(e -> {
            Clipboard systemClip = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transfer = systemClip.getContents(null);
            try {
                String data = (String) transfer.getTransferData(DataFlavor.stringFlavor);
                kernelCanvas.getFilter().setKernelElementsFromText(data);
                kernelCanvas.getFilter().adjustKernelQuotient();
            } catch (Error | RuntimeException e1) {
                e1.printStackTrace();
                throw e1;
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        });
        popupMenu.add(pasteItem);

        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> kernelCanvas.getFilter().fillRectangle(0.0));
        popupMenu.add(clearItem);

        popupMenu.addSeparator();

        final double fillValue = getFillValue();
        String fillValueText = fillValue == (int) fillValue ? String.valueOf((int) fillValue) : String.valueOf(fillValue);

        JMenuItem fillRectangleItem = new JMenuItem(String.format("Fill Rectangle by <%s>", fillValueText));
        fillRectangleItem.addActionListener(e -> {
            kernelCanvas.getFilter().fillRectangle(fillValue);
            kernelCanvas.getFilter().adjustKernelQuotient();
        });
        popupMenu.add(fillRectangleItem);

        JMenuItem fillEllipseItem = new JMenuItem(String.format("Fill Ellipse by <%s>", fillValueText));
        fillEllipseItem.addActionListener(e -> {
            kernelCanvas.getFilter().fillEllipse(fillValue);
            kernelCanvas.getFilter().adjustKernelQuotient();
        });
        popupMenu.add(fillEllipseItem);

        JMenuItem fillGaussItem = new JMenuItem("Fill Gaussian");
        fillGaussItem.addActionListener(e -> {
            kernelCanvas.getFilter().fillGaussian();
            kernelCanvas.getFilter().adjustKernelQuotient();
        });
        popupMenu.add(fillGaussItem);

        JMenuItem fillLaplaceItem = new JMenuItem("Fill Laplacian");
        fillLaplaceItem.addActionListener(e -> {
            kernelCanvas.getFilter().fillLaplacian();
            kernelCanvas.getFilter().adjustKernelQuotient();
        });
        popupMenu.add(fillLaplaceItem);

        JMenuItem fillRandomItem = new JMenuItem("Fill Random");
        fillRandomItem.addActionListener(e -> {
            kernelCanvas.getFilter().fillRandom();
            kernelCanvas.getFilter().adjustKernelQuotient();
        });
        popupMenu.add(fillRandomItem);

        return popupMenu;
    }

    private boolean isPastePossible() {
        boolean enabled = false;
        Clipboard systemClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transfer = systemClip.getContents(null);
        if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) transfer.getTransferData(DataFlavor.stringFlavor);
                enabled = Filter.isKernelDataText(data);
            } catch (UnsupportedFlavorException | IOException ignored) {
            }
        }
        return enabled;
    }
}
