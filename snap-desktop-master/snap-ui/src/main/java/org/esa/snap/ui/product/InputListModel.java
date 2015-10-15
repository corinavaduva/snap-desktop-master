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

package org.esa.snap.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.AbstractListModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Thomas Storm
 */
class InputListModel extends AbstractListModel<Object> {

    private final List<Object> list = new ArrayList<>();
    private List<Product> sourceProducts = new ArrayList<>();
    private Property sourceProductPaths;
    private boolean internalPropertyChange;

    @Override
    public Object getElementAt(int index) {
        return list.get(index);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    Product[] getSourceProducts() {
        return sourceProducts.toArray(new Product[sourceProducts.size()]);
    }

    void setElements(String[] elements) throws ValidationException {
        if (!list.isEmpty()) {
            final int endIndex = list.size() - 1;
            list.clear();
            fireIntervalRemoved(this, 0, endIndex);
        }
        final File[] files = new File[elements.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(elements[i]);
        }
        addElements(files);
    }

    void addElements(Object... elements) throws ValidationException {
        final int startIndex = list.size();
        for (Object element : elements) {
            if (!(element instanceof File || element instanceof Product)) {
                throw new IllegalStateException(
                        "Only java.io.File or Product allowed.");
            }
            if (mayAdd(element)) {
                list.add(element);
            }
        }
        updateProperty();
        fireIntervalAdded(this, startIndex, list.size() - 1);
    }

    void clear() {
        if (!list.isEmpty()) {
            final int endIndex = list.size() - 1;
            list.clear();
            try {
                updateProperty();
            } catch (ValidationException ignored) {
            }
            fireIntervalRemoved(this, 0, endIndex);
        }
    }

    void removeElementsAt(int[] selectedIndices) {
        List<Object> toRemove = new ArrayList<>();
        int startIndex = Integer.MAX_VALUE;
        int endIndex = Integer.MIN_VALUE;
        for (int selectedIndex : selectedIndices) {
            startIndex = Math.min(startIndex, selectedIndex);
            endIndex = Math.max(endIndex, selectedIndex);
            toRemove.add(list.get(selectedIndex));
        }
        if (list.removeAll(toRemove)) {
            try {
                updateProperty();
            } catch (ValidationException ignored) {
            }
            fireIntervalRemoved(this, startIndex, endIndex);
        }
    }

    private void updateProperty() throws ValidationException {
        final List<String> files = new ArrayList<>();
        final List<Product> products = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof File) {
                files.add(((File) element).getPath());
            } else if (element instanceof Product) {
                products.add((Product) element);
            }
        }
        internalPropertyChange = true;
        sourceProductPaths.setValue(files.toArray(new String[files.size()]));
        internalPropertyChange = false;
        sourceProducts = products;
    }

    private boolean mayAdd(Object element) {
        if (list.contains(element)) {
            return false;
        }

        if (element instanceof Product) {
            return true;
        }

        File file = (File) element;
        return file.isDirectory() || !alreadyContained(file);
    }

    private boolean alreadyContained(File file) {
        for (Product sourceProduct : sourceProducts) {
            File fileLocation = sourceProduct.getFileLocation();
            if (fileLocation != null && fileLocation.getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    public void setProperty(Property property) {
        this.sourceProductPaths = property;
        if (sourceProductPaths != null && sourceProductPaths.getContainer() != null) {
            sourceProductPaths.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!internalPropertyChange) {
                        Object newValue = evt.getNewValue();
                        try {
                            if (newValue == null) {
                                clear();
                            } else {
                                setElements((String[]) newValue);
                            }
                        } catch (ValidationException e) {
                            SystemUtils.LOG.log(Level.SEVERE, "Problems at setElements.", e);
                        }
                    }
                }
            });
        }
    }

    public Property getProperty() {
        return sourceProductPaths;
    }
}
