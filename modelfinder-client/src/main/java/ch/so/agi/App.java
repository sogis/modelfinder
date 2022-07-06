package ch.so.agi;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import org.dominokit.domino.ui.forms.SwitchButton;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.TextNode;
import org.dominokit.domino.ui.forms.TextBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.dom.AbortController;
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
import elemental2.dom.RequestInit;
import elemental2.dom.URL;
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
    private String ilisite = "";

    private AbortController abortController = null;

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

        // Get search params to handle "switch expand button" and to restrict the search.
        Location location = DomGlobal.window.location;
        URLSearchParams searchParams = new URLSearchParams(location.search);
        
        String paramExpanded = searchParams.get("expanded");
        if (searchParams.has("expanded") && searchParams.get("expanded").toLowerCase() == "true") {
            expanded = true;
        }
        
        if (searchParams.has("ilisite")) {
             ilisite = searchParams.get("ilisite");
        }
        
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
                        if (value) {
                            updateUrlLocation("expanded", "true");
                        } else  {
                            updateUrlLocation("expanded", "false");
                        }

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
            
            if (abortController != null) {
                abortController.abort();
            }
            
            abortController = new AbortController();
            final RequestInit init = RequestInit.create();
            init.setSignal(abortController.signal);
            
            DomGlobal.fetch(pathname + "search?query=" + textBox.getValue().toLowerCase() + "&ilisite=" + ilisite, init)
            .then(response -> {
                if (!response.ok) {
                    return null;
                }
                return response.text();
            }).then(json -> {                
                JsPropertyMap<?> parsed = Js.cast(Global.JSON.parse(json));
                
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
                    }
                });
                
                abortController = null;

                return null;
            }).catch_(error -> {
                //console.log(error);
                
                // Das war falsch. Das führt dazu, dass es einmalig funktioniert und bei
                // weiteren darauffolgenden Abbrüchen nicht mehr funktioniert (oder
                // dann nur bei jedem zweiten).
                //abortController = null;
                return null;
            });
        });
    }
    
    private void removeResults() {
        while(resultContent.firstChild != null) {
            resultContent.removeChild(resultContent.firstChild);
        }
    }
    
    private void updateUrlLocation(String key, String value) {
        URL url = new URL(DomGlobal.location.href);
        String host = url.host;
        String protocol = url.protocol;
        String pathname = url.pathname;
        URLSearchParams params = url.searchParams;
        params.set(key, value);

        String newUrl = protocol + "//" + host + pathname + "?" + params.toString(); 
        updateUrlWithoutReloading(newUrl);
    }

    // Update the URL in the browser without reloading the page.
    private static native void updateUrlWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;
}