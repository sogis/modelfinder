package io.github.sogis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ilirepository.impl.RepositoryAccessException;
import io.github.sogis.search.InvalidLuceneQueryException;
import io.github.sogis.search.LuceneSearcher;
import io.github.sogis.search.LuceneSearcherException;
import io.github.sogis.search.Result;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Settings settings;
    
    @Autowired
    LuceneSearcher searcher;
    
    @Value("${lucene.query.default.records:20}")
    private Integer QUERY_DEFAULT_RECORDS;

    @Value("${lucene.query.max.records:50}")
    private Integer QUERY_MAX_RECORDS;   
    
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok().body(settings);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping()  {
        
        try {
            searcher.init();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        
        return new ResponseEntity<String>("modelfinder", HttpStatus.OK);
    }
    
    @GetMapping("/search")
    public List<ModelInfo> searchModel(@RequestParam(value="query", required=false) String queryString) {
        Result results = null;
        
        log.info(queryString);
        
        try {
            results = searcher.searchIndex(queryString, QUERY_DEFAULT_RECORDS, false);
            log.info("Search for '" + queryString +"' found " + results.getAvailable() + " and retrieved " + results.getRetrieved() + " records");            
        } catch (LuceneSearcherException | InvalidLuceneQueryException e) {
            throw new IllegalStateException(e);
        }

        List<Map<String, String>> records = results.getRecords();
        
        List<ModelInfo> resultList = records.stream()
                //.distinct()
                .map(r -> {                    
                    ModelInfo modelInfo = new ModelInfo();
                    modelInfo.setName(r.get("name"));
                    modelInfo.setVersion(r.get("version"));
                    modelInfo.setFile(r.get("file"));
                    modelInfo.setRepository(r.get("repository"));
                    modelInfo.setIssuer(r.get("issuer"));
                    modelInfo.setFurtherInformation(r.get("furtherinformation"));
                    modelInfo.setPrecursorVersion(r.get("precursorversion"));
                    modelInfo.setMd5(r.get("md5"));
                    modelInfo.setTechnicalContact(r.get("technicalcontact"));
                    modelInfo.setIdgeoiv(r.get("idgeoiv"));
                    return modelInfo;
                })
                .collect(Collectors.toList());

        
        return resultList;
    }
    
}