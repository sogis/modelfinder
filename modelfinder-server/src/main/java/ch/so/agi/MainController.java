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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    
    // TODO: move to yml?
    @Value("${lucene.query.default.records:20}")
    private Integer QUERY_DEFAULT_RECORDS;

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
    public Map<String, List<ModelInfo>> searchModel(@RequestParam(value="query", required=false) String queryString) {
        Result results = null;
                
        try {
            results = searcher.searchIndex(queryString, QUERY_MAX_RECORDS, QUERY_MAX_ALL_RECORDS, false);
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
