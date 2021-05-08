package io.github.sogis;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.forms.SuggestBox;
import org.dominokit.domino.ui.forms.SuggestBoxStore;
import org.dominokit.domino.ui.forms.SuggestItem;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.HasSelectionHandler.SelectionHandler;
import org.dominokit.domino.ui.forms.SuggestBox.DropDownPositionDown;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.Headers;
import elemental2.dom.RequestInit;

public class App implements EntryPoint {
    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

//    // Projection
//    private static final String EPSG_2056 = "EPSG:2056";
//    private static final String EPSG_4326 = "EPSG:4326"; 
//    private Projection projection;
//
//    private String MAP_DIV_ID = "map";
//    private Map map;

	public void onModuleLoad() {
	    init();
	}
	
	@SuppressWarnings("unchecked")
    public void init() {
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();
	    
	    HTMLElement container = div().id("container").element();

	    HTMLElement topLevelContent = div().id("top-level-content").element();
	    container.appendChild(topLevelContent);
	    
        RequestInit requestInit = RequestInit.create();
        Headers headers = new Headers();
        headers.append("Content-Type", "application/x-www-form-urlencoded");
        requestInit.setHeaders(headers);

        SuggestBoxStore dynamicStore = new SuggestBoxStore() {

            @Override
            public void filter(String value, SuggestionsHandler suggestionsHandler) {
                if (value.trim().length() == 0) {
                    return;
                }

                DomGlobal.fetch("search?query=" + value.trim().toLowerCase(), requestInit)
                .then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                })
                .then(json -> {                    
                    ModelInfo[] searchResults = (ModelInfo[]) Global.JSON.parse(json);
                    List<ModelInfo> searchResultList = Arrays.asList(searchResults);
                    
                    List<SuggestItem<ModelInfo>> suggestItems = new ArrayList<SuggestItem<ModelInfo>>();

                    for (ModelInfo modelInfo : searchResultList) {
                        SuggestItem<ModelInfo> suggestItem = SuggestItem.create(modelInfo, modelInfo.getDisplayName(), Icons.ALL.file_document_outline_mdi());
                        suggestItems.add(suggestItem);
                    }
                   
                    suggestionsHandler.onSuggestionsReady(suggestItems);                    
                    return null;
                }).catch_(error -> {
                    console.log(error);
                    return null;
                });
            }

            @Override
            public void find(Object searchValue, Consumer handler) {
                if (searchValue == null) {
                    return;
                }
                ModelInfo modelInfo = (ModelInfo) searchValue;
                SuggestItem<ModelInfo> suggestItem = SuggestItem.create(modelInfo, null);
                handler.accept(suggestItem);
            }
        };
	    
        SuggestBox suggestBox = SuggestBox.create("Search for INTERLIS models...", dynamicStore);
        suggestBox.addLeftAddOn(Icons.ALL.search());
        suggestBox.getInputElement().setAttribute("autocomplete", "off");
        suggestBox.getInputElement().setAttribute("spellcheck", "false");
        suggestBox.setFocusOnClose(false);
        suggestBox.setFocusColor(Color.RED);        
        DropDownMenu suggestionsMenu = suggestBox.getSuggestionsMenu();
        suggestionsMenu.setPosition(new DropDownPositionDown());
        
        suggestBox.addSelectionHandler(new SelectionHandler() {
            @Override
            public void onSelection(Object value) {
                SuggestItem<ModelInfo> item = (SuggestItem<ModelInfo>) value;
                ModelInfo result = (ModelInfo) item.getValue();
                console.log(result.getDisplayName());
                
//                HTMLInputElement el =(HTMLInputElement) suggestBox.getInputElement().element();
//                el.value = result.getLabel();
//                
//                CustomEventInit eventInit = CustomEventInit.create();
//                eventInit.setDetail(result);
//                eventInit.setBubbles(true);
//                CustomEvent customEvent = new CustomEvent("startingPointChanged", eventInit);
//                root.dispatchEvent(customEvent);
            }
        });

        
        
	    container.appendChild(div().id("suggestbox-div").add(suggestBox).element());
	            
	    body().add(container);
        
        
        console.log("fubar foo bar");
	}
}