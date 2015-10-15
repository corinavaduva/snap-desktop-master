/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.ui.mosaic;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.MosaicOp;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.AppContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Map;

class MosaicDialog extends SingleTargetProductDialog {

    private final MosaicForm form;

    MosaicDialog(final String title, final String helpID, AppContext appContext) {
        super(appContext, title, ID_APPLY_CLOSE, helpID);
        final TargetProductSelector selector = getTargetProductSelector();
        selector.getModel().setSaveToFileSelected(true);
        selector.getModel().setProductName("mosaic");
        selector.getSaveToFileCheckBox().setEnabled(false);
        form = new MosaicForm(selector, appContext);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Mosaic");

        MosaicFormModel formModel = form.getFormModel();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 formModel.getPropertySet(),
                                                                                 formModel.getParameterMap(),
                                                                                 null);
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected boolean verifyUserInput() {
        final MosaicFormModel mosaicModel = form.getFormModel();
        if (!verifySourceProducts(mosaicModel)) {
            return false;
        }
        if (!verfiyTargetCrs(mosaicModel)) {
            return false;
        }
        if (!verifyVariablesAndConditions(mosaicModel)) {
            return false;
        }
        if (mosaicModel.isUpdateMode() && mosaicModel.getUpdateProduct() == null) {
            showErrorDialog("No product to update specified.");
            return false;
        }
        final String productName = getTargetProductSelector().getModel().getProductName();
        if (!mosaicModel.isUpdateMode() && StringUtils.isNullOrEmpty(productName)) {
            showErrorDialog("No name for the target product specified.");
            return false;
        }
        final boolean varsNotSpecified = mosaicModel.getVariables() == null || mosaicModel.getVariables().length == 0;
        final boolean condsNotSpecified =
                mosaicModel.getConditions() == null || mosaicModel.getConditions().length == 0;
        if (varsNotSpecified && condsNotSpecified) {
            showErrorDialog("No variables or conditions specified.");
            return false;
        }
        if (!verifyDEM(mosaicModel)) {
            return false;
        }
        return true;
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final MosaicFormModel formModel = form.getFormModel();
        return GPF.createProduct("Mosaic", formModel.getParameterMap(), formModel.getSourceProductMap());
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }


    private boolean verifyVariablesAndConditions(MosaicFormModel mosaicModel) {
        final Map<String, Product> sourceProductMap = mosaicModel.getSourceProductMap();
        final MosaicOp.Variable[] variables = mosaicModel.getVariables();
        final MosaicOp.Condition[] conditions = mosaicModel.getConditions();
        for (Map.Entry<String, Product> entry : sourceProductMap.entrySet()) {
            if (conditions != null) {
                for (MosaicOp.Variable variable : variables) {
                    try {
                        BandArithmetic.parseExpression(variable.getExpression(), new Product[]{entry.getValue()}, 0);
                    } catch (ParseException e) {
                        final String msg = String.format("Expression '%s' is invalid for product '%s'.\n%s",
                                                         variable.getName(),
                                                         entry.getKey(),
                                                         e.getMessage());
                        showErrorDialog(msg);
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            if (conditions != null) {
                for (MosaicOp.Condition condition : conditions) {
                    try {
                        BandArithmetic.parseExpression(condition.getExpression(), new Product[]{entry.getValue()}, 0);
                    } catch (ParseException e) {
                        final String msg = String.format("Expression '%s' is invalid for product '%s'.\n%s",
                                                         condition.getName(),
                                                         entry.getKey(),
                                                         e.getMessage());
                        showErrorDialog(msg);
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean verfiyTargetCrs(MosaicFormModel formModel) {
        try {
            final CoordinateReferenceSystem crs = formModel.getTargetCRS();
            if (crs == null) {
                showErrorDialog("No 'Coordinate Reference System' selected.");
                return false;
            }
        } catch (FactoryException e) {
            e.printStackTrace();
            showErrorDialog("No 'Coordinate Reference System' selected.\n" + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean verifySourceProducts(MosaicFormModel formModel) {
        final Map<String, Product> sourceProductMap = formModel.getSourceProductMap();
        if (sourceProductMap == null || sourceProductMap.isEmpty()) {
            showErrorDialog("No source products specified.");
            return false;
        }
        return true;
    }

    private boolean verifyDEM(MosaicFormModel formModel) {
        String externalDemName = formModel.getElevationModelName();
        if (externalDemName != null) {
            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(externalDemName);
            if (demDescriptor == null) {
                showErrorDialog("The DEM '" + externalDemName + "' is not supported.");
                return false;
            }
        }
        return true;
    }
}
