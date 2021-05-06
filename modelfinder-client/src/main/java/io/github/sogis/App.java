package io.github.sogis;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.dom.HTMLElement;
import ol.Extent;
import ol.Map;
import ol.OLFactory;
import ol.proj.Projection;
import ol.proj.ProjectionOptions;
import proj4.Proj4;

public class App implements EntryPoint {

    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    // Projection
    private static final String EPSG_2056 = "EPSG:2056";
    private static final String EPSG_4326 = "EPSG:4326"; 
    private Projection projection;

    private String MAP_DIV_ID = "map";
    private Map map;

	public void onModuleLoad() {
	    init();
	}
	
	public void init() {
	    // Registering EPSG:2056 / LV95 reference frame.
        Proj4.defs(EPSG_2056, "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs");
        ol.proj.Proj4.register(Proj4.get());

        ProjectionOptions projectionOptions = OLFactory.createOptions();
        projectionOptions.setCode(EPSG_2056);
        projectionOptions.setUnits("m");
        projectionOptions.setExtent(new Extent(2420000, 1030000, 2900000, 1350000));
        projection = new Projection(projectionOptions);
        Projection.addProjection(projection);
        
        // Change Domino UI color scheme.
        Theme theme = new Theme(ColorScheme.WHITE);
        theme.apply();

        // Add the Openlayers map (element) to the body.
        HTMLElement mapElement = div().id(MAP_DIV_ID).element();
        body().add(mapElement);
        map = MapPresets.getColorMap(MAP_DIV_ID);

        
        console.log("fubar foo bar");
	}
}