package org.esa.snap.opendap.utils;

import opendap.dap.DDS;
import org.esa.snap.opendap.datamodel.DAPVariable;
import org.esa.snap.opendap.datamodel.OpendapLeaf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class VariableCollector {


    private final ArrayList<DAPVariable> variableList;
    private VariableExtractor variableExtractor;

    public VariableCollector() {
        variableList = new ArrayList<DAPVariable>();
        variableExtractor = new VariableExtractor();
    }

    public DAPVariable[] collectDAPVariables(DDS dds) {
        final DAPVariable[] dapVariables = variableExtractor.extractVariables(dds);
        storeDAPVariables(dapVariables);
        return dapVariables;
    }

    public DAPVariable[] collectDAPVariables(OpendapLeaf leaf) {
        final DAPVariable[] dapVariables = variableExtractor.extractVariables(leaf);
        storeDAPVariables(dapVariables);
        return dapVariables;
    }

    private void storeDAPVariables(DAPVariable[] dapVariables) {
        for (int i = 0; i < dapVariables.length; i++) {
            boolean contained = false;
            for (int j = 0; j < variableList.size(); j++) {
                if (dapVariables[i].equals(variableList.get(j))) {
                    dapVariables[i] = variableList.get(j);
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                variableList.add(dapVariables[i]);
            }
        }
    }

    public Set<DAPVariable> getVariables() {
        Set<DAPVariable> variables = new HashSet<DAPVariable>(variableList);
        return variables;
    }

}
