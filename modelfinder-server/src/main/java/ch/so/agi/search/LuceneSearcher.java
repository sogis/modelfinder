package ch.so.agi.search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.ilirepository.impl.ModelLister;
import ch.interlis.ilirepository.impl.ModelMetadata;
import ch.interlis.ilirepository.impl.RepositoryAccess;
import ch.interlis.ilirepository.impl.RepositoryAccessException;
import ch.interlis.ilirepository.impl.RepositoryVisitor;
import ch.so.agi.Settings;

@Repository("LuceneSearcher")
public class LuceneSearcher {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private Settings settings;

    @Value("${app.index-directory}")
    private String INDEX_DIR;

    private NIOFSDirectory fsIndex;
    private IndexWriter writer;
    private StandardAnalyzer analyzer;
    private QueryParser queryParser;
    
    private IliManager manager;

    // PostConstruct: Anwendung ist noch nicht fertig gestartet / ready -> Abklären wegen liveness und readyness probes.
    // PostConstruct wird vor dem CommandLineRunner ausgeführt. Die Variablen müssen für das Erstellen
    // des Index instanziert werden.
    @PostConstruct
    public void init() throws IOException {
//      Path indexDir = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "modelfinder_idx");
      Path indexDir = Paths.get(INDEX_DIR);
      log.info("Index folder: " + indexDir);
      
      this.fsIndex = new NIOFSDirectory(indexDir);
      this.analyzer = new StandardAnalyzer();   
    }
    
    public void createIndex() throws IOException {
        log.info("Building index ...");
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(fsIndex, indexWriterConfig);
        writer.prepareCommit();  
        
        writer.deleteAll();
        
        try {
//            manager = new IliManager();
//            manager.setRepositories(settings.getDefaultRepositories().toArray(new String[0]));

            List<String> repositories = settings.getRepositories();
            RepositoryAccess repoAccess = new RepositoryAccess();
            
            // Mit RepositoryVisitor ist "file" qualifiziert, d.h. mit Repo-Basis-URL, d.h.
            // eine vollständige URL.
            ModelLister modelLister=new ModelLister();
            modelLister.setIgnoreDuplicates(true);
            
            RepositoryVisitor visitor=new RepositoryVisitor(repoAccess, modelLister);
            visitor.setRepositories(repositories.toArray(new String[repositories.size()]));
            visitor.visitRepositories();
            
            List<ModelMetadata> mergedModelMetadatav = modelLister.getResult2();
            log.debug("mergedModelMetadatav: {}", mergedModelMetadatav.size());
                      
            List<ModelMetadata> latestMergedModelMetadatav = RepositoryAccess.getLatestVersions2(mergedModelMetadatav);
            log.debug("latestMergedModelMetadatav: {}", latestMergedModelMetadatav.size());
            
            List<ModelMetadata> precursorModelMetadata = new ArrayList<ModelMetadata>(mergedModelMetadatav);
            precursorModelMetadata.removeAll(latestMergedModelMetadatav);
            log.debug("precursorModelMetadata: {}", precursorModelMetadata.size());

            for (ModelMetadata modelMetadata : latestMergedModelMetadatav) {
                addDocument(modelMetadata, false);
            }
            
            // Nur aktuell gültige in den Index schreiben.
//            for (ModelMetadata modelMetadata : precursorModelMetadata) {
//                addDocument(modelMetadata, true);
//            }
            
        } catch (RepositoryAccessException | Ili2cException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            writer.rollback();
        }
        
        writer.commit();
        writer.close();       
        
        log.info("Index built.");
    }

    private void addDocument(ModelMetadata modelMetadata, boolean isPrecursorVersion) throws IOException, Ili2cException {
        Document document = new Document();
//        if (isPrecursorVersion) {
//            document.add(new StoredField("dispname", modelMetadata.getName() + " (" + modelMetadata.getVersion() + ") precursor version"));
//        } else {
//            document.add(new StoredField("dispname", modelMetadata.getName() + " (" + modelMetadata.getVersion() + ")"));
//        }
        
//        document.add(new StoredField("dispname", modelMetadata.getName()));

        try {
            // Es gibt ILI-Dateien mit Leerschlag.
            // Man kann jedoch nicht URLEncoder verwenden, weil dann der
            // gesamte String encoded wird.
            URI uri = new URI(modelMetadata.getFile().replace(" ", "%20"));
            String host = uri.getHost();
            String scheme = uri.getScheme();
            document.add(new TextField("repository", scheme+"://"+host, Store.YES));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        document.add(new StoredField("dispname", modelMetadata.getName() + " (" + modelMetadata.getVersion() + ")"));
        document.add(new TextField("name", modelMetadata.getName(), Store.YES));
        if (modelMetadata.getShortDescription() != null) {
            document.add(new TextField("shortdescription", modelMetadata.getShortDescription(), Store.YES));
        }
        document.add(new TextField("version", modelMetadata.getVersion(), Store.YES));
        document.add(new TextField("file", modelMetadata.getFile(), Store.YES));
        if (modelMetadata.getFile().contains("replaced") || modelMetadata.getFile().contains("obsolete")) {
            document.add(new DoubleDocValuesField("boost", 0.5));
        }
        if(modelMetadata.getTitle() != null) {
            document.add(new TextField("title", modelMetadata.getTitle(), Store.YES));
        }
        if (modelMetadata.getIssuer() != null) {
            document.add(new TextField("issuer", modelMetadata.getIssuer(), Store.YES));
        }
        if (modelMetadata.getPrecursorVersion() != null) {
            document.add(new TextField("precursorversion", modelMetadata.getPrecursorVersion(), Store.YES));
        }
        if (modelMetadata.getTechnicalContact() != null) {
            document.add(new TextField("technicalcontact", modelMetadata.getTechnicalContact(), Store.YES));
        }
        if (modelMetadata.getFurtherInformation() != null) {
            document.add(new TextField("furtherinformation", modelMetadata.getFurtherInformation(), Store.YES));
        }
        if (modelMetadata.getMd5() != null) {
            document.add(new TextField("md5", modelMetadata.getMd5(), Store.YES));
        }
       
        if (modelMetadata.getTags() != null) {
            document.add(new TextField("tag", modelMetadata.getTags(), Store.YES));
        }
        
        if (!modelMetadata.getSchemaLanguage().equalsIgnoreCase(modelMetadata.ili1)) {
            // Nur MGDM haben eine IDGeoIV.
            if (modelMetadata.getFile().contains("geo.admin.ch")) {
                // Weil es Rolf Z. gibt, der das pflegt, kann man
                // den Tag auswerten, um an die IDGeoIV zu kommen.
                // Ist zwar nur durch "Wissen" möglich, ist aber 
                // momentan so und ich erspare mir das Parsen des
                // Modelles. Wenn man später u.U. die Klassen kennen
                // will, kann man immer noch darauf zurückommen.
                                
                if (modelMetadata.getTags() != null) {
                    document.add(new TextField("idgeoiv", modelMetadata.getTags(), Store.YES));
                }
            }
        }
        
        String fileUrl = modelMetadata.getFile();
        if (fileUrl.contains("geo.so")) {
            document.add(new TextField("administration", "solothurn", Store.YES));
        } else if (fileUrl.contains("geo.ai") || fileUrl.contains("geo.ar")) {
            document.add(new TextField("administration", "appenzell", Store.YES));            
        } else if (fileUrl.contains("geo.be")) {
            document.add(new TextField("administration", "bern", Store.YES));            
        } else if (fileUrl.contains("geo.bl") || fileUrl.contains("geo.bs")) {
            document.add(new TextField("administration", "basel", Store.YES));            
        } else if (fileUrl.contains("geo.gl")) {
            document.add(new TextField("administration", "glarus", Store.YES));            
        } else if (fileUrl.contains("geo.gr")) {
            document.add(new TextField("administration", "graubünden", Store.YES));            
        } else if (fileUrl.contains("geo.llv")) {
            document.add(new TextField("administration", "liechenstein", Store.YES));            
        } else if (fileUrl.contains("geo.lu")) {
            document.add(new TextField("administration", "luzern", Store.YES));            
        } else if (fileUrl.contains("geo.sg")) {
            document.add(new TextField("administration", "st gallen", Store.YES));            
        } else if (fileUrl.contains("geo.sh")) {
            document.add(new TextField("administration", "schaffhausen", Store.YES));            
        } else if (fileUrl.contains("geo.sz")) {
            document.add(new TextField("administration", "schwyz", Store.YES));            
        } else if (fileUrl.contains("geo.ti")) {
            document.add(new TextField("administration", "tessin", Store.YES));            
        } else if (fileUrl.contains("geo.zg")) {
            document.add(new TextField("administration", "zug", Store.YES));            
        } else if (fileUrl.contains("geo.zh")) {
            document.add(new TextField("administration", "zürich", Store.YES));            
        }
        
        // TODO: whole model as text

        writer.addDocument(document);
    }
    
    @PreDestroy
    private void close() {
        try {
            fsIndex.close();
            log.info("Lucene Index closed");
        } catch (IOException e) {
            log.warn("Issue closing Lucene Index: " + e.getMessage());
        }
    }
    
    /**
     * Search Lucene Index for records matching querystring
     * @param queryString - human written query string from e.g. a search form
     * @param iliSite - restrict search to this interlis repository
     * @param numRecords - number of requested records 
     * @param showAvailable - check for number of matching available records 
     * @return Top Lucene query results as a Result object
     * @throws LuceneSearcherException 
     * @throws InvalidLuceneQueryException 
     */
    public Result searchIndex(String queryString, String iliSite, int numRecords, int maxAllRecords, boolean showAvailable)
            throws LuceneSearcherException, InvalidLuceneQueryException {
        log.debug("queryString: {}, iliSite: {}", queryString, iliSite);
        IndexReader reader = null;
        IndexSearcher indexSearcher = null;
        Query query;
        TopDocs documents;
        TotalHitCountCollector collector = null;
        try {            
            reader = DirectoryReader.open(this.fsIndex);
            indexSearcher = new IndexSearcher(reader);
            
            if (queryString == null || queryString.trim().length() == 0) {
                query = new MatchAllDocsQuery();
            } else {
                queryParser = new QueryParser("name", analyzer); // 'name' is default field if we don't prefix search string
                queryParser.setAllowLeadingWildcard(true);
                
                String luceneQueryString = "";
                String[] splitedQuery = queryString.split("\\s+");
                for (int i=0; i<splitedQuery.length; i++) {
                    String token = QueryParser.escape(splitedQuery[i]);
                    log.debug("token: " + token);
                    
                    // TODO: tag und shortdescription auswerten.
                    
                    // Das Feld, welches bestimmend sein soll (also in der Suche zuoberst gelistet), bekommt
                    // einen sehr hohen Boost.
                    luceneQueryString += "(name:*" + token + "*^10 OR "
                            //+ "version:*" + token + "* OR "
                            + "file:*" + token + "* OR "
                            + "title:*" + token + "* OR "
                            + "issuer:*" + token + "* OR "
                            + "administration:*" + token + "* OR "
                            + "technicalcontact:*" + token + "* OR "
                            + "furtherinformation:*" + token + "* OR "
                            + "idgeoiv:" + token + "*^20";
                    luceneQueryString += ")";
                    if (i<splitedQuery.length-1) {
                        luceneQueryString += " AND ";
                    }
                    
                    if (iliSite != null && iliSite.trim().length() > 0) {
                        luceneQueryString += " AND (repository: " + QueryParser.escape(iliSite) + "^100)";
                    }
                }
                            
                //log.info("luceneQueryString: " + luceneQueryString);
                
                Query tmpQuery = queryParser.parse(luceneQueryString);
                query = FunctionScoreQuery.boostByValue(tmpQuery, DoubleValuesSource.fromDoubleField("boost"));
                
                log.debug("'" + luceneQueryString + "' ==> '" + query.toString() + "'");
            }
            
            if (showAvailable) {
                collector = new TotalHitCountCollector();
                indexSearcher.search(query, collector);
            }
            
            // Sorting: https://stackoverflow.com/questions/21965778/sorting-search-result-in-lucene-based-on-a-numeric-field
            // Scheint noch bissle mühsam zu sein.
            // Eventuell muss man es eh ausserhalb machen, wenn wir noch gruppieren etc.
            if (queryString == null || queryString.trim().length() == 0) {
                documents = indexSearcher.search(query, maxAllRecords);
            } else {
                documents = indexSearcher.search(query, numRecords);
            }
            
            log.debug("documents.totalHits.value: ", documents.totalHits.value);
            List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
            for (ScoreDoc scoreDoc : documents.scoreDocs) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                Map<String, String> docMap = new HashMap<String, String>();
                List<IndexableField> fields = document.getFields();
                for (IndexableField field : fields) {
                    docMap.put(field.name(), field.stringValue());
                }
                mapList.add(docMap);
            }
            
            Result result = new Result(mapList, mapList.size(),
                    collector == null ? (mapList.size() < numRecords ? mapList.size() : -1) : collector.getTotalHits());
            return result;
        } catch (ParseException e) {
            e.printStackTrace();            
            throw new InvalidLuceneQueryException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new LuceneSearcherException(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                log.warn("Could not close IndexReader: " + ioe.getMessage());
            }
        }
    }
}
