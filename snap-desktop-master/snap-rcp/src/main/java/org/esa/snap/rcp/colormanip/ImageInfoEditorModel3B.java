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

package org.esa.snap.rcp.colormanip;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.AbstractImageInfoEditorModel;

import java.awt.Color;


class ImageInfoEditorModel3B extends AbstractImageInfoEditorModel {
    private final static Color[] RGB_COLORS = new Color[]{Color.RED, Color.GREEN, Color.BLUE};

    private final int channel;
    private byte[] gammaCurve;

    ImageInfoEditorModel3B(ImageInfo imageInfo, int channel) {
        super(imageInfo);
        Assert.argument(imageInfo.getRgbChannelDef() != null, "imageInfo");
        this.channel = channel;
        updateGammaCurve();
    }

    @Override
    public int getSliderCount() {
        return 2;
    }

    @Override
    public double getSliderSample(int index) {
        if (index == 0) {
            return getImageInfo().getRgbChannelDef().getMinDisplaySample(channel);
        } else {
            return getImageInfo().getRgbChannelDef().getMaxDisplaySample(channel);
        }
    }

    @Override
    public void setSliderSample(int index, double sample) {
        if (index == 0) {
            getImageInfo().getRgbChannelDef().setMinDisplaySample(channel, sample);
        } else {
            getImageInfo().getRgbChannelDef().setMaxDisplaySample(channel, sample);
        }
        fireStateChanged();
    }

    @Override
    public boolean isColorEditable() {
        return false;
    }

    @Override
    public Color getSliderColor(int index) {
        if (index == 0) {
            return Color.BLACK;
        } else {
            return RGB_COLORS[channel];
        }
    }

    @Override
    public void setSliderColor(int index, Color color) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public void createSliderAfter(int index) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public void removeSlider(int removeIndex) {
        throw new IllegalStateException("not implemented for RGB");
    }

    @Override
    public Color[] createColorPalette() {
        Color color = RGB_COLORS[channel];
        Color[] palette = new Color[256];
        final int redMult = color.getRed() / 255;
        final int greenMult = color.getGreen() / 255;
        final int blueMult = color.getBlue() / 255;
        for (int i = 0; i < palette.length; i++) {
            int j = i;
            if (gammaCurve != null) {
                j = gammaCurve[i] & 0xff;
            }
            final int r = j * redMult;
            final int g = j * greenMult;
            final int b = j * blueMult;
            palette[i] = new Color(r, g, b);
        }
        return palette;
    }

    @Override
    public boolean isGammaUsed() {
        return getImageInfo().getRgbChannelDef().isGammaUsed(channel);
    }

    @Override
    public double getGamma() {
        return getImageInfo().getRgbChannelDef().getGamma(channel);
    }

    @Override
    public void setGamma(double gamma) {
        getImageInfo().getRgbChannelDef().setGamma(channel, gamma);
        updateGammaCurve();
        fireStateChanged();
    }

    @Override
    public byte[] getGammaCurve() {
        return gammaCurve;
    }

    private void updateGammaCurve() {
        if (isGammaUsed()) {
            gammaCurve = MathUtils.createGammaCurve(getGamma(), null);
        } else {
            gammaCurve = null;
        }
    }
}
