package fi.livi.digitraffic.meri.model.winternavigation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import fi.livi.digitraffic.meri.model.geojson.Feature;
import fi.livi.digitraffic.meri.model.geojson.Geometry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonPropertyOrder({ "name",
                     "type",
                     "geometry",
                     "properties" })
@ApiModel(description = "GeoJSON Feature object")
public class WinterNavigationDirwayFeature extends Feature<Geometry, WinterNavigationDirwayProperties> {

    @ApiModelProperty(value = "Name of the dirway", required = true)
    public final String name;

    public WinterNavigationDirwayFeature(final String name,
                                         final WinterNavigationDirwayProperties properties,
                                         final Geometry geometry) {
        super(geometry, properties);
        this.name = name;
    }
}
