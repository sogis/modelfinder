package ch.so.agi;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dominokit.domino.ui.forms.SwitchButton;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.notifications.Notification;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.TextNode;
import org.dominokit.domino.ui.forms.TextBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.core.JsMap;
import elemental2.core.JsMap.ForEachCallbackFn;
import elemental2.dom.Document;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLCollection;
import elemental2.dom.HTMLDocument;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLParagraphElement;
import elemental2.dom.HTMLTableElement;
import elemental2.dom.HTMLTableSectionElement;
import elemental2.dom.Location;
import elemental2.dom.URLSearchParams;
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
    private boolean expanded = false;

    public void onModuleLoad() {
        init();
    }
    
    @SuppressWarnings("unchecked")
    public void init() {
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();
          
        // Main container element.
        HTMLElement container = div().id("container").element();        
        body().add(container);        

        // Get search params to restrict search.
        Location location = DomGlobal.window.location;
        URLSearchParams searchParams = new URLSearchParams(location.search);
        console.log(searchParams.get("expanded"));
        
        String paramExpanded = searchParams.get("expanded");
        if (searchParams.has("expanded") && searchParams.get("expanded").toLowerCase() == "true") {
            expanded = true;
        }
        
        
        // TODO: on server exact match? nur ilisite?
        
        // Get pathname to handle url correctly for resources (e.g. logos and server requests)
        pathname = location.pathname;

        if (pathname.contains("index.html")) {
            pathname = pathname.replace("index.html", "");
        }

        // Add logo
        HTMLElement logoDiv = div().css("logo")
                .add(div()
                        .add(img().attr("src", location.protocol + "//" + location.host + location.pathname + "Logo.png").attr("alt", "Logo Kanton")).element()).element();
        container.appendChild(logoDiv);

        // Content element for filter / search element and switch button.
        HTMLElement topLevelContent = div().id("top-level-content").element();
        container.appendChild(topLevelContent);
        
        // Textbox that acts as filter / search element.
        TextBox textBox = TextBox.create().setLabel("Search for INTERLIS models...");
        textBox.addLeftAddOn(Icons.ALL.search());
        textBox.setFocusColor(Color.RED_DARKEN_3);
        textBox.getInputElement().setAttribute("autocomplete", "off");
        textBox.getInputElement().setAttribute("spellcheck", "false");

        HTMLElement resetIcon = Icons.ALL.close().style().setCursor("pointer").get().element();
        textBox.addRightAddOn(resetIcon);
        
        topLevelContent.appendChild(div().id("filterbox-div").add(textBox).element());
        
        // Switch button for toggling results.
        SwitchButton switchButton = SwitchButton.create("Expand results", "off", "on")
            .addChangeHandler(
                    value -> {      
                        HTMLCollection<Element> repoDetailElements = resultContent.getElementsByClassName("repo-details");
                        for (Element element : repoDetailElements.asList()) {
                            if (value) {
                                element.setAttribute("open", true); 
                            } else  {
                                element.removeAttribute("open"); 
                            }
                        } 
                    })
            .setOffTitle("OFF")
            .setOnTitle("ON")
            .setColor(Color.RED_DARKEN_3);
        
        if (expanded) {
            switchButton.check(expanded);
        }
        
        topLevelContent.appendChild(switchButton.element());
        
        // Reset search: clear textbox, remove results, uncheck switch button.
        resetIcon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                textBox.clear();
                
                removeResults();
                
                switchButton.uncheck();
            }
        });

        // Content element for search results.
        resultContent = div().id("result-content").element(); 
        container.appendChild(resultContent);
        
        // HTML document: used for creating html elements that are not
        // available in elemento (e.g. summary, details).
        HTMLDocument document = DomGlobal.document;
        
        // Search models in the lucene index on the server.
        // See pathname: should handle paths that are set in 
        // reverse proxies and/or api gateways.
        textBox.addEventListener("keyup", event -> {
            if (textBox.getValue().trim().length() > 0 && textBox.getValue().trim().length() <=2) {
                return;
            }
            
            if (textBox.getValue().trim().length() == 0) {
                removeResults();          
                return;
            }

            DomGlobal.fetch(pathname + "search?query=" + textBox.getValue().toLowerCase()).then(response -> {
                if (!response.ok) {
                    return null;
                }
                return response.text();
            }).then(json -> {                
                JsPropertyMap<?> parsed = Js.cast(Global.JSON.parse(json));
                //DomGlobal.console.info(parsed);
                
                // TODO: remember which repo was opened?
                
                removeResults();
                
                parsed.forEach(new JsForEachCallbackFn() {
                    @Override
                    public void onKey(String key) {
                        JsArray<ModelInfo> modelInfoArray = (JsArray) parsed.get(key);
                                                                        
                        HTMLElement details = (HTMLElement) document.createElement("details");
                        details.className = "repo-details";
                        
                        if (switchButton.isChecked()) {
                            details.setAttribute("open", true);
                        }
                        
                        HTMLElement summary = (HTMLElement) document.createElement("summary");
                        summary.className = "repo-summary";
                        summary.appendChild(span().add(key + " (" + modelInfoArray.length + ")").element());
                        
                        HTMLParagraphElement paragraph = p().element(); 
                        
                        details.append(summary, paragraph);
                        resultContent.appendChild(details);

                        for (ModelInfo modelInfo : modelInfoArray.asList()) {                            
                            HTMLElement modelDetails = (HTMLElement) document.createElement("details");
                            modelDetails.className = "model-details";
                                                        
                            HTMLElement modelSummary = (HTMLElement) document.createElement("summary");
                            modelSummary.className = "model-summary";
                            
                            HTMLElement launchIcon = Icons.ALL.launch_mdi().style().addCss("model-launch-icon").get().element();
                            HTMLElement modelLink = a()
                                    .attr("class", "icon-link")
                                    .attr("href", modelInfo.getFile())
                                    .attr("target", "_blank").add(launchIcon).element();
                                                                                    
                            modelSummary.appendChild(span().add(TextNode.of(modelInfo.getName() + " ")).add(modelLink).element());

                            HTMLParagraphElement modelParagraph = p().style("margin-top: 10px;").element();
                            
                            HTMLTableElement modelTable = table().element();
                            modelTable.id = "model-table";
                            modelTable.appendChild(
                                    colgroup().add(col().attr("span", "1").style("width: 20%")).add(col().attr("span", "1").style("width: 80%")).element());
                            HTMLTableSectionElement modelTableBody = tbody().element();
                            modelTableBody.appendChild(
                                    tr().add(
                                            td().add("Version:"))
                                        .add(
                                            td().add(modelInfo.getVersion())).element());
                            
                            modelTableBody.appendChild(
                                    tr().add(
                                            td().add("File:"))
                                        .add(
                                            td().add(
                                                    a()
                                                        .attr("class", "result")
                                                        .attr("href", modelInfo.getFile())
                                                        .attr("target", "_blank")
                                                            .add(modelInfo.getFile())
                                                    )).element());

                            if (modelInfo.getFurtherInformation() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("Further information:"))
                                            .add(
                                                td().add(
                                                        a()
                                                            .attr("class", "result")
                                                            .attr("href", modelInfo.getFurtherInformation())
                                                            .attr("target", "_blank")
                                                                .add(modelInfo.getFurtherInformation())
                                                        )).element());
                            }
                            
                            if (modelInfo.getIssuer() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("Issuer:"))
                                            .add(
                                                td().add(
                                                        a()
                                                            .attr("class", "result")
                                                            .attr("href", modelInfo.getIssuer())
                                                            .attr("target", "_blank")
                                                                .add(modelInfo.getIssuer())
                                                        )).element());
                            }

                            if (modelInfo.getTechnicalContact() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("Technical contact:"))
                                            .add(
                                                td().add(
                                                        a()
                                                            .attr("class", "result")
                                                            .attr("href", modelInfo.getTechnicalContact())
                                                            .attr("target", "_blank")
                                                                .add(modelInfo.getTechnicalContact())
                                                        )).element());
                            }

                            if (modelInfo.getFile().contains("geo.admin.ch") && modelInfo.getIdgeoiv() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("ID GeoIV:"))
                                            .add(
                                                td().add(modelInfo.getIdgeoiv())).element());
                            }

                            if (!modelInfo.getFile().contains("geo.admin.ch") && modelInfo.getTag() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("Tags:"))
                                            .add(
                                                td().add(modelInfo.getTag())).element());
                            }
                            
                            if (modelInfo.getPrecursorVersion() != null) {
                                modelTableBody.appendChild(
                                        tr().add(
                                                td().add("Precursor version:"))
                                            .add(
                                                td().add(modelInfo.getPrecursorVersion())).element());
                            }

                            modelTable.appendChild(modelTableBody);
                            modelParagraph.appendChild(modelTable);
                            
                            modelDetails.append(modelSummary, modelParagraph);
                            paragraph.append(modelDetails);
                        }
                                                

                        
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
    
    private void removeResults() {
        while(resultContent.firstChild != null) {
            resultContent.removeChild(resultContent.firstChild);
        }
    }
    
    // Update the URL in the browser without reloading the page.
    private static native void updateUrlWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;
}