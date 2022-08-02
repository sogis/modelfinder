package ch.so.agi;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties.Jetty.Threads;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.so.agi.search.LuceneSearcher;
import ch.so.agi.search.Result;
import ch.so.agi.search.InvalidLuceneQueryException;
import ch.so.agi.search.LuceneSearcherException;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Settings settings;
    
    @Autowired
    LuceneSearcher searcher;
    
    @Autowired
    ObjectMapper objectMapper;

    // TODO: move to yml?
//    @Value("${lucene.query.default.records:20}")
//    private Integer QUERY_DEFAULT_RECORDS;

    @Value("${lucene.query.max.records:5000}")
    private Integer QUERY_MAX_RECORDS;   

    @Value("${lucene.query.max.records:5000}")
    private Integer QUERY_MAX_ALL_RECORDS;   

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok().body(settings);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping()  {
        return new ResponseEntity<String>("modelfinder", HttpStatus.OK);
    }

    // TODO:
    // Eventuell gesamte Liste speichern / cachen:
    // - Beim Aufstarten 
    // - Beim Indexieren
    // Cache durch Webserver? Wie lange?
    
    @GetMapping("/search")
    public Map<String, List<ModelInfo>> searchModel(@RequestParam(value="query", required=false) String queryString, 
            @RequestParam(value="ilisite", required=false) String iliSite) {
        Result results = null;
                
        try {
            results = searcher.searchIndex(queryString, iliSite, QUERY_MAX_RECORDS, QUERY_MAX_ALL_RECORDS, false);
            log.debug("Search for '" + queryString +"' found " + results.getAvailable() + " and retrieved " + results.getRetrieved() + " records");            
        } catch (LuceneSearcherException | InvalidLuceneQueryException e) {
            throw new IllegalStateException(e);
        }

        List<Map<String, String>> records = results.getRecords();        
        Map<String, List<ModelInfo>> resultMap = records.stream()
                .map(r -> {                    
                    ModelInfo modelInfo = new ModelInfo();
                    modelInfo.setRepository(r.get("repository"));
                    modelInfo.setRepositoryDomain(r.get("repository").replaceAll("http(s)?://|www\\.|/.*", ""));
                    modelInfo.setDisplayName(r.get("dispname"));
                    modelInfo.setName(r.get("name"));
                    modelInfo.setTitle(r.get("title"));
                    modelInfo.setVersion(r.get("version"));
                    modelInfo.setFile(r.get("file"));
                    modelInfo.setIssuer(r.get("issuer"));
                    modelInfo.setFurtherInformation(r.get("furtherinformation"));
                    modelInfo.setPrecursorVersion(r.get("precursorversion"));
                    modelInfo.setMd5(r.get("md5"));
                    modelInfo.setTechnicalContact(r.get("technicalcontact"));
                    modelInfo.setTag(r.get("tag"));
                    modelInfo.setIdgeoiv(r.get("idgeoiv"));
                    return modelInfo;
                })
                .collect(Collectors.groupingBy(ModelInfo::getRepositoryDomain, Collectors.collectingAndThen(
                        Collectors.toList(),
                        models -> models.stream()
                            .sorted(Comparator.comparing(ModelInfo::getName, (m1, m2) -> {
                                return m1.toLowerCase().compareTo(m2.toLowerCase());
                            }))
                            .collect(Collectors.toList())
                        )));

        Map<String, List<ModelInfo>> sortedResultMap = resultMap.entrySet().stream()
            .sorted(Map.Entry.<String, List<ModelInfo>>comparingByKey())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                    LinkedHashMap::new)); 

        return sortedResultMap;
    }

    // TODO reicht getHost wegen reverse proxy?
    @GetMapping(value="/opensearchdescription.xml", produces=MediaType.APPLICATION_XML_VALUE) 
    public ResponseEntity<?> opensearchdescription() {
        String xml = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/">
  <ShortName>INTERLIS model finder</ShortName>
  <Url type="text/html" method="get" template="%s?query={searchTerms}"/>
  <Url type="application/x-suggestions+json" method="get" template="%s/search/suggestions?q={searchTerms}"/>
  <LongName>INTERLIS model finder</LongName>
  <Image height="16" width="16" type="image/x-icon">data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAMtJREFUeNpi/P//PwMhwOjaJQCkHKDYAFmOhYBGBSDVAMTxuNSw4NE8AUjlE3IdCw7nHgBifQYiAAsWzReAWJ6BSMCExWaiNaMYAA0sfQYSASMoGoG2g6JnPwMZgAXJdqJA0JcbDJq/3jJo/n7DcJ1VhIGRwaUTFNf3CWk0//GMoePtPgbpP58xXJBASLPLt/sM017vwBmIDvg08/37ydAJtBlfLBjgMyD+0yUG3n+/8BrAj9fvP58RnQ6wAs1fbygz4CkLL155gAADAPBrNaa8wGmqAAAAAElFTkSuQmCC</Image>
</OpenSearchDescription>   
        """.formatted(getHost(), getHost());
        
        return new ResponseEntity<String>(xml, HttpStatus.OK);
    }

    @GetMapping(value="/search/suggestions", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> suggestModels(@RequestParam(value="q", required=false) String searchTerms) {
        Result results = null;

        try {
            results = searcher.searchIndex(searchTerms, null, 50, QUERY_MAX_ALL_RECORDS, false);
            log.debug("Search for '" + searchTerms +"' found " + results.getAvailable() + " and retrieved " + results.getRetrieved() + " records");            
        } catch (LuceneSearcherException | InvalidLuceneQueryException e) {
            throw new IllegalStateException(e);
        }

        ArrayNode suggestions = objectMapper.createArrayNode();
        suggestions.add(searchTerms);

        List<Map<String, String>> records = results.getRecords();   
        
        ArrayNode completions = objectMapper.createArrayNode();
        records.forEach(it -> {
           completions.add(it.get("name")); 
        });
        suggestions.add(completions);
        log.debug(suggestions.toPrettyString());
        
        return new ResponseEntity<JsonNode>(suggestions, HttpStatus.OK);
    }
    
    
    private String getHost() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
        
    //@Scheduled(cron="0 */2 * * *")
    @Scheduled(cron="*/2 * * * *")
    private void rebuildIndex() {
        System.out.println("******************REINDEX...");
        try {
            searcher.createIndex();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        } 
    }

    @PostConstruct
    public void init() throws Exception {
    }
}
