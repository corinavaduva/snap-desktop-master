/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import junit.framework.TestCase;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.esa.snap.core.datamodel.PlainFeatureFactory.*;

public class SimpleFeatureShapeFigureTest extends TestCase {

    private final GeometryFactory gf = new GeometryFactory();

    public void testSpecificGeometryType() {
        SimpleFeatureType sft = createPlainFeatureType("Polygon", Polygon.class, DefaultGeographicCRS.WGS84);

        Polygon polygon = createPolygon();
        SimpleFeature simpleFeature = createPlainFeature(sft, "_1", polygon, "");

        SimpleFeatureShapeFigure shapeFigure = new SimpleFeatureShapeFigure(simpleFeature, new DefaultFigureStyle());
        assertEquals(polygon, shapeFigure.getGeometry());
        assertNotNull(shapeFigure.getShape());
        assertEquals(Figure.Rank.AREA, shapeFigure.getRank());
    }

    @Ignore
    @Test
    public void testMixedGeometries_2() {
        //ignore does not work

//        SimpleFeatureType sft = createPlainFeatureType("Geometry", Geometry.class, DefaultGeographicCRS.WGS84);
//
//        Geometry geometry;
//        SimpleFeature feature;
//        SimpleFeatureShapeFigure figure;
//
//        geometry = createPoint();
//        feature = createPlainFeature(sft, "_4", geometry, "");
//        figure = new SimpleFeatureShapeFigure(feature, SceneRasterTransform.IDENTITY, new DefaultFigureStyle());
//        assertEquals(geometry, figure.getGeometry());
//        assertNotNull(figure.getShape());
//        assertEquals(Figure.Rank.POINT, figure.getRank());
//
//        geometry = createGeometryCollection();
//        feature = createPlainFeature(sft, "_5", geometry, "");
//        figure = new SimpleFeatureShapeFigure(feature, SceneRasterTransform.IDENTITY, new DefaultFigureStyle());
//        assertEquals(geometry, figure.getGeometry());
//        assertNotNull(figure.getShape());
//        assertEquals(Figure.Rank.NOT_SPECIFIED, figure.getRank());
    }

    public void testMixedGeometries_1() {
        SimpleFeatureType sft = createPlainFeatureType("Geometry", Geometry.class, DefaultGeographicCRS.WGS84);

        Geometry geometry;
        SimpleFeature feature;
        SimpleFeatureShapeFigure figure;

        geometry = createPolygon();
        feature = createPlainFeature(sft, "_1", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, new DefaultFigureStyle());
        assertEquals(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.AREA, figure.getRank());

        geometry = createLinearRing();
        feature = createPlainFeature(sft, "_2", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, new DefaultFigureStyle());
        assertEquals(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.LINE, figure.getRank());

        geometry = createLineString();
        feature = createPlainFeature(sft, "_3", geometry, "");
        figure = new SimpleFeatureShapeFigure(feature, new DefaultFigureStyle());
        assertEquals(geometry, figure.getGeometry());
        assertNotNull(figure.getShape());
        assertEquals(Figure.Rank.LINE, figure.getRank());
    }

    public void testRank() {
        assertEquals(Figure.Rank.POINT, SimpleFeatureShapeFigure.getRank(createPoint()));
        assertEquals(Figure.Rank.POINT, SimpleFeatureShapeFigure.getRank(createMultiPoint()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createLineString()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createLinearRing()));
        assertEquals(Figure.Rank.LINE, SimpleFeatureShapeFigure.getRank(createMultiLineString()));
        assertEquals(Figure.Rank.AREA, SimpleFeatureShapeFigure.getRank(createPolygon()));
        assertEquals(Figure.Rank.AREA, SimpleFeatureShapeFigure.getRank(createMultiPolygon()));
        assertEquals(Figure.Rank.NOT_SPECIFIED, SimpleFeatureShapeFigure.getRank(createGeometryCollection()));
    }

    private Point createPoint() {
        return gf.createPoint(new Coordinate(0, 0));
    }

    private LineString createLineString() {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
        });
    }

    private LinearRing createLinearRing() {
        return gf.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
                new Coordinate(0, 0),
        });
    }

    private Polygon createPolygon() {
        return gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0, 1),
                new Coordinate(0, 0),
        }), null);
    }

    private MultiPoint createMultiPoint() {
        return gf.createMultiPoint(new Point[0]);
    }

    private MultiPolygon createMultiPolygon() {
        return gf.createMultiPolygon(new Polygon[0]);
    }

    private MultiLineString createMultiLineString() {
        return gf.createMultiLineString(new LineString[0]);
    }

    private GeometryCollection createGeometryCollection() {
        return gf.createGeometryCollection(new Geometry[0]);
    }
}
