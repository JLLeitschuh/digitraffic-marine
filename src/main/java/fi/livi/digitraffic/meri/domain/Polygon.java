package fi.livi.digitraffic.meri.domain;

import java.util.List;

public class Polygon implements Geometry {

    public final List<List<Point>> points;

    public Polygon(List<List<Point>> points) {
        this.points = points;
    }

}