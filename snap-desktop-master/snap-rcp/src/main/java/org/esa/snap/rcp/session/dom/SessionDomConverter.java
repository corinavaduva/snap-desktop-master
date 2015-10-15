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

package org.esa.snap.rcp.session.dom;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.HashMap;
import java.util.Map;

public class SessionDomConverter extends DefaultDomConverter {

    private final Map<Class<?>, DomConverter> domConverterMap;

    public SessionDomConverter(ProductManager productManager) {
        super(PropertyContainer.class);
        this.domConverterMap = new HashMap<>(33);
        setDomConverter(Product.class, new ProductDomConverter(productManager));
        setDomConverter(RasterDataNode.class, new RasterDataNodeDomConverter(productManager));
        setDomConverter(PlacemarkDescriptor.class, new PlacemarkDescriptorDomConverter());
    }

    private SessionDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory, PropertySetDescriptor propertySetDescriptor, Map<Class<?>, DomConverter> domConverterMap) {
        super(valueType, propertyDescriptorFactory, propertySetDescriptor);
        this.domConverterMap = domConverterMap;
    }

    final void setDomConverter(Class<?> type, DomConverter domConverter) {
        domConverterMap.put(type, domConverter);
    }

    @Override
    protected DomConverter createChildDomConverter(Class<?> valueType, PropertyDescriptorFactory propertyDescriptorFactory, PropertySetDescriptor propertySetDescriptor) {
        return new SessionDomConverter(valueType, getPropertyDescriptorFactory(), propertySetDescriptor, domConverterMap);
    }

    @Override
    protected DomConverter findChildDomConverter(PropertyDescriptor descriptor) {
        return findDomConverter(descriptor.getType());
    }

    private DomConverter findDomConverter(Class<?> type) {
        DomConverter domConverter = domConverterMap.get(type);
        while (domConverter == null && type != null && type != Object.class) {
            type = type.getSuperclass();
            domConverter = domConverterMap.get(type);
        }
        return domConverter;
    }
}
