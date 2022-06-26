package io.github.sogis;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.forms.SuggestBox;
import org.dominokit.domino.ui.forms.SuggestBoxStore;
import org.dominokit.domino.ui.forms.SuggestItem;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.TextNode;
import org.dominokit.domino.ui.utils.HasSelectionHandler.SelectionHandler;
import org.dominokit.domino.ui.forms.AbstractSuggestBox.DropDownPositionDown;
import org.dominokit.domino.ui.forms.TextBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.dom.Document;
import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLDocument;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLParagraphElement;
import elemental2.dom.Headers;
import elemental2.dom.Location;
import elemental2.dom.RequestInit;
import jsinterop.base.Js;
import jsinterop.base.JsForEachCallbackFn;
import jsinterop.base.JsPropertyMap;

public class App implements EntryPoint {
    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    private String pathname;
    private HTMLElement resultContent;
    
	public void onModuleLoad() {
	    init();
	}
	
	@SuppressWarnings("unchecked")
    public void init() {
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();
	    
	    HTMLElement container = div().id("container").element();        
        body().add(container);        

        Location location = DomGlobal.window.location;
        pathname = location.pathname;

        if (pathname.contains("index.html")) {
            pathname = pathname.replace("index.html", "");
        }

        HTMLElement logoDiv = div().css("logo")
                .add(div()
                        .add(img().attr("src", location.protocol + "//" + location.host + location.pathname + "Logo.png").attr("alt", "Logo Kanton")).element()).element();
        container.appendChild(logoDiv);

	    HTMLElement topLevelContent = div().id("top-level-content").element();
	    container.appendChild(topLevelContent);
	    
	    
        TextBox textBox = TextBox.create().setLabel("Search for INTERLIS models...");
        textBox.addLeftAddOn(Icons.ALL.search());
        textBox.setFocusColor(Color.RED_DARKEN_3);
        textBox.getInputElement().setAttribute("autocomplete", "off");
        textBox.getInputElement().setAttribute("spellcheck", "false");

        HTMLElement resetIcon = Icons.ALL.close().style().setCursor("pointer").get().element();
        resetIcon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                textBox.clear();
                while(resultContent.firstChild != null) {
                    resultContent.removeChild(resultContent.firstChild);
                }
            }
        });
        textBox.addRightAddOn(resetIcon);
        topLevelContent.appendChild(div().id("filterbox-div").add(textBox).element());
        
        resultContent = div().id("result-content").element(); 
        container.appendChild(resultContent);
        
        HTMLDocument document = DomGlobal.document;
        
        textBox.addEventListener("keyup", event -> {
            if (textBox.getValue().trim().length() == 0) {
                //listStore.setData(datasets);
                return;
            }

            DomGlobal.fetch(pathname + "search?query=" + textBox.getValue().toLowerCase()).then(response -> {
                if (!response.ok) {
                    return null;
                }
                return response.text();
            }).then(json -> {                
                JsPropertyMap<?> parsed = Js.cast(Global.JSON.parse(json));
                DomGlobal.console.info(parsed);
                
                while(resultContent.firstChild != null) {
                    resultContent.removeChild(resultContent.firstChild);
                }
                
                parsed.forEach(new JsForEachCallbackFn() {
                    @Override
                    public void onKey(String key) {
                        JsArray modelInfoArray = (JsArray) parsed.get(key);
                                                
                        HTMLElement details = (HTMLElement) document.createElement("details");
                        details.className = "repo-details";
                        
                        HTMLElement summary = (HTMLElement) document.createElement("summary");
                        summary.className = "repo-summary";
                        summary.appendChild(span().add(key + " (" + modelInfoArray.length + ")").element());

                        HTMLParagraphElement paragraph = p().add(TextNode.of("fubar")).element();
                        
                        details.append(summary, paragraph);
                        resultContent.appendChild(details);
                        
                        
                        
                        
//                        console.log("value: " + value);
//                        if (value instanceof String) {
//                            console.log("key: " + key);
//                            console.log("value: " + value);
//
//                        }
                    }
                });
                
                
                
                // models.geo.admin.ch [13]
                // geo.so.ch [2]
                

//                JsArray<JsPropertyMap<String>> paymentMethods = Js.uncheckedCast(map.get("paymentMethods"));
//                JsPropertyMap<String> method = paymentMethods.getAt(0);
                
//                LinkedHashMap<String, List<ModelInfo>> modelInfoMap = Js.uncheckedCast(json);                
//                console.log(modelInfoMap);
//                
//                
//                modelInfoMap.get("a");
                
//                for (var entry : modelInfoMap.entrySet()) {
//                    
//                    console.log(entry.getKey());
//                }


                //List<Dataset> filteredDatasets = mapper.read(json);
                
//                Collections.sort(filteredDatasets, new Comparator<Dataset>() {
//                    @Override
//                    public int compare(Dataset o1, Dataset o2) {
//                        return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
//                    }
//                });

                //listStore.setData(filteredDatasets);
                
                return null;
            }).catch_(error -> {
                console.log(error);
                return null;
            });
        });


	    
	    
//        RequestInit requestInit = RequestInit.create();
//        Headers headers = new Headers();
//        headers.append("Content-Type", "application/x-www-form-urlencoded");
//        requestInit.setHeaders(headers);
//
//        SuggestBoxStore dynamicStore = new SuggestBoxStore() {
//
//            @Override
//            public void filter(String value, SuggestionsHandler suggestionsHandler) {
//                if (value.trim().length() == 0) {
//                    return;
//                }
//
//                DomGlobal.fetch(pathname + "search?query=" + value.trim().toLowerCase(), requestInit)
//                .then(response -> {
//                    if (!response.ok) {
//                        return null;
//                    }
//                    return response.text();
//                })
//                .then(json -> {                    
//                    ModelInfo[] searchResults = (ModelInfo[]) Global.JSON.parse(json);
//                    List<ModelInfo> searchResultList = Arrays.asList(searchResults);
//                    
//                    List<SuggestItem<ModelInfo>> suggestItems = new ArrayList<SuggestItem<ModelInfo>>();
//
//                    for (ModelInfo modelInfo : searchResultList) {
//                        SuggestItem<ModelInfo> suggestItem = SuggestItem.create(modelInfo, modelInfo.getDisplayName(), Icons.ALL.file_document_outline_mdi());
//                        suggestItems.add(suggestItem);
//                    }
//                   
//                    suggestionsHandler.onSuggestionsReady(suggestItems);                    
//                    return null;
//                }).catch_(error -> {
//                    console.log(error);
//                    return null;
//                });
//            }
//
//            @Override
//            public void find(Object searchValue, Consumer handler) {
//                if (searchValue == null) {
//                    return;
//                }
//                ModelInfo modelInfo = (ModelInfo) searchValue;
//                SuggestItem<ModelInfo> suggestItem = SuggestItem.create(modelInfo, null);
//                handler.accept(suggestItem);
//            }
//        };
//	    
//        SuggestBox suggestBox = SuggestBox.create("Search for INTERLIS models...", dynamicStore);
//        suggestBox.addLeftAddOn(Icons.ALL.search());
//        suggestBox.getInputElement().setAttribute("autocomplete", "off");
//        suggestBox.getInputElement().setAttribute("spellcheck", "false");
//        suggestBox.setFocusOnClose(false);
//        suggestBox.setFocusColor(Color.RED);        
//        DropDownMenu suggestionsMenu = suggestBox.getSuggestionsMenu();
//        suggestionsMenu.setPosition(new DropDownPositionDown());
//        
//        container.appendChild(div().id("suggestbox-div").add(suggestBox).element());
//
//        HTMLElement resultContainer = div().id("result-container").element();       
//        container.appendChild(resultContainer);
//
//        suggestBox.addSelectionHandler(new SelectionHandler() {
//            @Override
//            public void onSelection(Object value) {
//                if (resultContainer.firstChild != null) {
//                    resultContainer.removeChild(resultContainer.firstChild);
//                }
//                  
//                SuggestItem<ModelInfo> item = (SuggestItem<ModelInfo>) value;
//                ModelInfo result = (ModelInfo) item.getValue();
//                
//                HTMLElement resultContent = div().id("result-content").element();
//                resultContent.appendChild(h(4, "Name").element());
//                resultContent.appendChild(p().textContent(result.getName()).element());
//                
//                resultContent.appendChild(h(4, "Version").element());
//                resultContent.appendChild(p().textContent(result.getVersion()).element());
//
//                if (result.getTitle() != null) {
//                    resultContent.appendChild(h(4, "Title").element());
//                    resultContent.appendChild(p().textContent(result.getTitle()).element());
//                }
//                                
//                resultContent.appendChild(h(4, "File").element());
//                resultContent.appendChild(p().add(
//                        a().css("result")
//                        .attr("href", result.getFile())
//                        .attr("target", "_blank").add(result.getFile()))
//                        .element());
//                
//                if (result.getIdgeoiv() != null) {
//                    resultContent.appendChild(h(4, "ID GeoIV").element());
//                    resultContent.appendChild(p().textContent(result.getIdgeoiv()).element());
//                }
//                
//                if (result.getFurtherInformation() != null) {
//                    resultContent.appendChild(h(4, "Further information").element());
//                    resultContent.appendChild(p().add(
//                            a().css("result")
//                            .attr("href", result.getFurtherInformation())
//                            .attr("target", "_blank").add(result.getFurtherInformation()))
//                            .element());
//                }
//
//                if (result.getIssuer() != null) {
//                    resultContent.appendChild(h(4, "Issuer").element());
//                    resultContent.appendChild(p().add(
//                            a().css("result")
//                            .attr("href", result.getIssuer())
//                            .attr("target", "_blank").add(result.getIssuer()))
//                            .element());
//                }
//
//                if (result.getTechnicalContact() != null) {
//                    resultContent.appendChild(h(4, "Technical contact").element());
//                    resultContent.appendChild(p().add(
//                            a().css("result")
//                            .attr("href", result.getTechnicalContact())
//                            .attr("target", "_blank").add(result.getTechnicalContact()))
//                            .element());
//                }
//
//                if (result.getPrecursorVersion() != null) {
//                    resultContent.appendChild(h(4, "Precursor version").element());
//                    resultContent.appendChild(p().textContent(result.getPrecursorVersion()).element());
//                }
//                
//                resultContainer.appendChild(resultContent);                
//            }
//        });
	}
}