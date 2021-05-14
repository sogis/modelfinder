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
        
        container.appendChild(div().id("suggestbox-div").add(suggestBox).element());

        HTMLElement resultContainer = div().id("result-container").element();       
        container.appendChild(resultContainer);

        suggestBox.addSelectionHandler(new SelectionHandler() {
            @Override
            public void onSelection(Object value) {
                if (resultContainer.firstChild != null) {
                    resultContainer.removeChild(resultContainer.firstChild);
                }
                  
                SuggestItem<ModelInfo> item = (SuggestItem<ModelInfo>) value;
                ModelInfo result = (ModelInfo) item.getValue();
                
                HTMLElement resultContent = div().id("result-content").element();
                resultContent.appendChild(h(4, "Name").element());
                resultContent.appendChild(p().textContent(result.getName()).element());
                
                resultContent.appendChild(h(4, "Version").element());
                resultContent.appendChild(p().textContent(result.getVersion()).element());

                if (result.getTitle() != null) {
                    resultContent.appendChild(h(4, "Title").element());
                    resultContent.appendChild(p().textContent(result.getTitle()).element());
                }
                                
                resultContent.appendChild(h(4, "File").element());
                resultContent.appendChild(p().add(
                        a().css("result")
                        .attr("href", result.getFile())
                        .attr("target", "_blank").add(result.getFile()))
                        .element());
                
                if (result.getIdgeoiv() != null) {
                    resultContent.appendChild(h(4, "ID GeoIV").element());
                    resultContent.appendChild(p().textContent(result.getIdgeoiv()).element());
                }
                
                if (result.getFurtherInformation() != null) {
                    resultContent.appendChild(h(4, "Further information").element());
                    resultContent.appendChild(p().add(
                            a().css("result")
                            .attr("href", result.getFurtherInformation())
                            .attr("target", "_blank").add(result.getFurtherInformation()))
                            .element());
                }

                if (result.getIssuer() != null) {
                    resultContent.appendChild(h(4, "Issuer").element());
                    resultContent.appendChild(p().add(
                            a().css("result")
                            .attr("href", result.getIssuer())
                            .attr("target", "_blank").add(result.getIssuer()))
                            .element());
                }

                if (result.getTechnicalContact() != null) {
                    resultContent.appendChild(h(4, "Technical contact").element());
                    resultContent.appendChild(p().add(
                            a().css("result")
                            .attr("href", result.getTechnicalContact())
                            .attr("target", "_blank").add(result.getTechnicalContact()))
                            .element());
                }

                if (result.getPrecursorVersion() != null) {
                    resultContent.appendChild(h(4, "Precursor version").element());
                    resultContent.appendChild(p().textContent(result.getPrecursorVersion()).element());
                }
                
                resultContainer.appendChild(resultContent);                
            }
        });

	    
	    
	    body().add(container);        
	}
}