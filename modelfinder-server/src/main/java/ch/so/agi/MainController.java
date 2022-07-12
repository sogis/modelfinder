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
            log.info("Search for '" + queryString +"' found " + results.getAvailable() + " and retrieved " + results.getRetrieved() + " records");            
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
  <!--<Url type="text/html" method="get" template="%s/search?q={searchTerms}"/>-->
  <Url type="application/x-suggestions+json" method="get" template="%s/search/suggestions?q={searchTerms}"/>
  <LongName>INTERLIS model finder</LongName>
  <Image height="16" width="16" type="image/x-icon">data:image/x-icon;base64,AAABAAEAEBAAAAEAIABoBAAAFgAAACgAAAAQAAAAIAAAAAEAIAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTAADJP8IAiL/NAEh/0kBIf9JAyP/JgMk/whxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAAMk/wgBIf9dASD/vgUn/+cTM//4EzP/+AUn/+cBIP+rAiL/NHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAAMk/wgBH/+OEzP/+G2A//++xv//3uL//9PY//+irv//PFX//wUn/+cBIf9dcWlMAHFpTABxaUwAcWlMAHFpTAABIP92EzP/+KKu///6+///7vD//8rR///T2P//+vv//+7w//9tgP//BSf/5wIi/zRxaUwAcWlMAHFpTAADI/8mBSf/546c///6+///sLr//zxV//8QMf//EDH//1Bn///e4v//7vD//1Bn//8BIP+rAyT/CHFpTABxaUwAASH/XSZC///e4v//09j//yZC//8CIv//AiL//wIi//8CIv//UGf///r7//+irv//BSf/5wMj/yZxaUwAcWlMAAEf/448Vf//+vv//46c//8CIv//AiL//wIi//8CIv//AiL//xAx///T2P//09j//xMz//gBIf9JcWlMAHFpTAABH/+OUGf///r7//+OnP//AiL//wIi//8CIv//AiL//wIi//8QMf//ytH//9PY//8TM//4ASH/SXFpTABxaUwAASD/diZC///u8P//vsb//xAx//8CIv//AiL//wIi//8CIv//PFX//+7w//+wuv//BSf/5wIi/zRxaUwAcWlMAAIi/zQFJ//noq7///r7//+OnP//EDH//wIi//8CIv//JkL//77G///6+///UGf//wEg/74DJP8IcWlMAHFpTABxaUwAAR//jiZC///K0f//+vv//8rR//+irv//oq7//97i///6+///jpz//xMz//gBIf9JcWlMAHFpTABxaUwAcWlMAAMk/xkBIP+rJkL//46c///e4v//+vv//+7w///T2P//bYD//xMz//gBIP92AyT/CHFpTABxaUwAcWlMAHFpTABxaUwAAyT/GQEf/44FJ//nJkL//zxV//88Vf//EzP/+AIi/84BIf9dAyT/CHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAAyP/JgEh/10BIP92ASD/dgEh/0kDJP8ZcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwAcWlMAHFpTABxaUwA//8AAPgfAADgDwAAwAcAAMADAACAAQAAgAEAAIABAACAAQAAgAEAAIABAADAAwAAwAMAAOAHAAD4HwAA//8AAA==</Image>
</OpenSearchDescription>   
        """.formatted(getHost(), getHost());
        
        return new ResponseEntity<String>(xml, HttpStatus.OK);
    }

    
    @GetMapping(value="/search/suggestions", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> suggestModels(@RequestParam(value="q", required=false) String searchTerms) {
        Result results = null;

        try {
            results = searcher.searchIndex(searchTerms, null, 20, QUERY_MAX_ALL_RECORDS, false);
            log.debug("Search for '" + searchTerms +"' found " + results.getAvailable() + " and retrieved " + results.getRetrieved() + " records");            
        } catch (LuceneSearcherException | InvalidLuceneQueryException e) {
            throw new IllegalStateException(e);
        }

        ArrayNode suggestions = objectMapper.createArrayNode();
        suggestions.add(searchTerms);

        
        log.info(suggestions.toPrettyString());
        return new ResponseEntity<JsonNode>(suggestions, HttpStatus.OK);
    }
    
    
    private String getHost() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
        
    @Scheduled(cron="0 */2 * * *")
    private void rebuildIndex() {
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
