package fi.livi.digitraffic.meri.model.geojson;

import java.util.Arrays;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "GeoJSON Point Geometry object")
public class Point extends Geometry<List<Double>> {

    public Point() {
        super(GeometryType.Point);
    }

    /**
     *
     * @param x longitude
     * @param y latitude
     */
    public Point(final double x, final double y) {
        super(GeometryType.Point, Arrays.asList(x, y));
    }


    @ApiModelProperty(required = true, allowableValues = "Point", example = "Point")
    @Override
    public String getType() {
        return super.getType();
    }

    @ApiModelProperty(required = true, position = 2, example = "[26.97677492, 65.34673850]",
                      value = "An array of coordinates. " + COORD_FORMAT_WGS84_LONG_INC_ALT)
    @Override
    public List<Double> getCoordinates() {
        return super.getCoordinates();
    }

}