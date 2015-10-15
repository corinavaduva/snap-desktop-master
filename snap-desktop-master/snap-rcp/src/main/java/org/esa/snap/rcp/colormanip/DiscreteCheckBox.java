package org.esa.snap.rcp.colormanip;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DiscreteCheckBox extends JCheckBox {

    private boolean shouldFireDiscreteEvent;

    DiscreteCheckBox(final ColorManipulationForm parentForm) {
        super("Discrete colours");

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (shouldFireDiscreteEvent) {
                    parentForm.getFormModel().getModifiedImageInfo().getColorPaletteDef().setDiscrete(isSelected());
                    parentForm.applyChanges();
                }
            }
        });
    }

    void setDiscreteColorsMode(boolean discrete) {
        shouldFireDiscreteEvent = false;
        setSelected(discrete);
        shouldFireDiscreteEvent = true;
    }
}
