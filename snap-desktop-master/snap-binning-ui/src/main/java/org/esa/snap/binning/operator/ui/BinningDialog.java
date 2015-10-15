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

package org.esa.snap.binning.operator.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.TypedDescriptorsRegistry;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.ui.AppContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UI for binning operator.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class BinningDialog extends SingleTargetProductDialog {

    private static final String OPERATOR_NAME = "Binning";

    private final BinningForm form;
    private final BinningFormModel formModel;

    protected BinningDialog(AppContext appContext, String title, String helpID) {
        super(appContext, title, ID_APPLY_CLOSE_HELP, helpID, new TargetProductSelectorModel(), true);

        formModel = new BinningFormModel();
        form = new BinningForm(appContext, formModel, getTargetProductSelector());

        OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(OPERATOR_NAME);

        ParameterUpdater parameterUpdater = new BinningParameterUpdater();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 formModel.getPropertySet(),
                                                                                 formModel.getParameterMap(),
                                                                                 parameterUpdater);
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected boolean verifyUserInput() {
        AggregatorConfig[] aggregatorConfigs = formModel.getAggregatorConfigs();
        if (!isAtLeastOneAggreatorConfigDefined(aggregatorConfigs)) {
            return false;
        }

        if (!doUsedVariablesStillExist(aggregatorConfigs)) {
            return false;
        }

        if (!areTargetNamesUnique(aggregatorConfigs)) {
            return false;
        }

        if (!isTimeFilterWellConfigured()) {
            return false;
        }
        return true;
    }


    @Override
    protected Product createTargetProduct() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new BinningOp.Spi());
        final TargetProductCreator targetProductCreator = new TargetProductCreator();
        targetProductCreator.executeWithBlocking();
        return targetProductCreator.get();
    }

    @Override
    public int show() {
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareClose();
        super.hide();
    }

    private boolean doUsedVariablesStillExist(AggregatorConfig[] aggregatorConfigs) {
        Product contextProduct = formModel.getContextProduct();
        // assuming that the variables are defined in the context product
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            String aggregatorConfigName = aggregatorConfig.getName();
            AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorConfigName);
            String[] sourceVarNames = descriptor.getSourceVarNames(aggregatorConfig);
            for (String sourceVarName : sourceVarNames) {
                if (!contextProduct.containsBand(sourceVarName)) {
                    String msg = String.format(
                            "Source band name '%s' of aggregator '%s' is unknown.\nIt is neither one of the bands of the source products,\n" +
                            "nor is it defined by an intermediate source band.", sourceVarName, aggregatorConfigName
                    );
                    showErrorDialog(msg);
                    return false;
                }
            }

        }
        return true;
    }

    private boolean isTimeFilterWellConfigured() {
        if (formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY ||
            formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.TIME_RANGE) {
            if (formModel.getStartDateTime() == null) {
                showErrorDialog("Start date/time must be provided when time filter method 'spatiotemporal data day' or 'time range' is chosen.");
                return false;
            }
            if (formModel.getPeriodDuration() == null) {
                showErrorDialog("Period duration must be provided when time filter method 'spatiotemporal data day' or 'time range' is chosen.");
                return false;
            }
        }
        if (formModel.getTimeFilterMethod() == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
            if (formModel.getMinDataHour() == null) {
                showErrorDialog("Min data hour must be provided when time filter method 'spatiotemporal data day' is chosen.");
                return false;
            }
        }
        return true;
    }

    private boolean isAtLeastOneAggreatorConfigDefined(AggregatorConfig[] aggregatorConfigs) {
        if (aggregatorConfigs.length == 0) {
            showErrorDialog("Please configure at least a single aggregator.");
            return false;
        }
        return true;
    }

    private boolean areTargetNamesUnique(AggregatorConfig[] aggregatorConfigs) {
        List<String> targetVarNameList = new ArrayList<>();
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, aggregatorConfig.getName());
            String[] targetNames = descriptor.getTargetVarNames(aggregatorConfig);
            for (String targetName : targetNames) {
                if (targetVarNameList.contains(targetName)) {
                    showErrorDialog(String.format("The target band with the name '%s' is defined twice.", targetName));
                    return false;
                } else {
                    targetVarNameList.add(targetName);
                }
            }
        }
        return true;
    }

    private void updateParameterMap(Map<String, Object> parameters) {
        parameters.put("variableConfigs", formModel.getVariableConfigs());
        parameters.put("aggregatorConfigs", formModel.getAggregatorConfigs());

        parameters.put("outputFile", getTargetProductSelector().getModel().getProductFile().getPath());

        parameters.put("maskExpr", formModel.getMaskExpr());
        parameters.put("region", formModel.getRegion());
        parameters.put("numRows", formModel.getNumRows());
        parameters.put("superSampling", formModel.getSuperSampling());
        parameters.put("sourceProductPaths", formModel.getSourceProductPath());

        BinningOp.TimeFilterMethod method = formModel.getTimeFilterMethod();
        parameters.put("timeFilterMethod", method);
        if (method == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
            parameters.put("minDataHour", formModel.getMinDataHour());
            parameters.put("startDateTime", formModel.getStartDateTime());
            parameters.put("periodDuration", formModel.getPeriodDuration());
        } else if (method == BinningOp.TimeFilterMethod.TIME_RANGE) {
            parameters.put("startDateTime", formModel.getStartDateTime());
            parameters.put("periodDuration", formModel.getPeriodDuration());
        }
    }

    private void updateFormModel(Map<String, Object> parameterMap) throws ValidationException {
        final PropertySet propertySet = formModel.getBindingContext().getPropertySet();
        final Set<Map.Entry<String, Object>> entries = parameterMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Property property = propertySet.getProperty(entry.getKey());
            if (property != null) {
                property.setValue(entry.getValue());
            }
        }

        if (parameterMap.containsKey("outputFile")) {
            File outputFile = new File((String) parameterMap.get("outputFile"));
            File outputDir = outputFile.getParentFile();
            if (outputDir != null) {
                getTargetProductSelector().getModel().setProductDir(outputDir);
            }
            getTargetProductSelector().getModel().setProductName(FileUtils.getFilenameWithoutExtension(outputFile));

        }

        BinningConfigurationPanel configurationPanel = form.getBinningConfigurationPanel();

        VariableTableController variableTableController = configurationPanel.getVariableTableController();
        VariableConfig[] variableConfigs = new VariableConfig[0];
        if (parameterMap.containsKey("variableConfigs")) {
            variableConfigs = (VariableConfig[]) parameterMap.get("variableConfigs");
        }
        variableTableController.setVariableConfigs(variableConfigs);

        AggregatorTableController aggregatorTableController = configurationPanel.getAggregatorTableController();
        AggregatorConfig[] aggregatorConfigs = new AggregatorConfig[0];
        if (parameterMap.containsKey("aggregatorConfigs")) {
            aggregatorConfigs = (AggregatorConfig[]) parameterMap.get("aggregatorConfigs");
        }
        aggregatorTableController.setAggregatorConfigs(aggregatorConfigs);
    }

    private class TargetProductCreator extends ProgressMonitorSwingWorker<Product, Void> {

        protected TargetProductCreator() {
            super(BinningDialog.this.getJDialog(), "Creating target product");
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Binning...", 100);
            final Map<String, Object> parameters = new HashMap<>();
            updateParameterMap(parameters);
            final Product targetProduct = GPF.createProduct("Binning", parameters, formModel.getSourceProducts());
            pm.done();
            return targetProduct;
        }
    }

    private class BinningParameterUpdater implements ParameterUpdater {

        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            formModel.getBindingContext().adjustComponents();
            updateParameterMap(parameterMap);
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            updateFormModel(parameterMap);
            formModel.getBindingContext().adjustComponents();
        }
    }
}
