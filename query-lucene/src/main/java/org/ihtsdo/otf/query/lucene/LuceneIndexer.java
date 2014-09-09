package org.ihtsdo.otf.query.lucene;

//~--- non-JDK imports --------------------------------------------------------
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NRTManager;
import org.apache.lucene.search.NRTManagerReopenThread;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.thread.NamedThreadFactory;
import org.ihtsdo.otf.tcc.model.cc.termstore.TermstoreLogger;
import org.ihtsdo.otf.tcc.model.index.service.IndexedGenerationCallable;
import org.ihtsdo.otf.tcc.model.index.service.IndexerBI;
import org.ihtsdo.otf.tcc.model.index.service.SearchResult;

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LuceneIndexer implements IndexerBI {

    public static final String LUCENE_ROOT_LOCATION_PROPERTY
            = "org.ihtsdo.otf.tcc.query.lucene-root-location";
    public static final String DEFAULT_LUCENE_LOCATION = "lucene";
    protected static final Logger logger
            = Logger.getLogger(LuceneIndexer.class.getName());
    public static final Version luceneVersion = Version.LUCENE_43;
    private static final UnindexedFuture unindexedFuture = new UnindexedFuture();
    private static final ThreadGroup threadGroup = new ThreadGroup("Lucene");
    public static File root;
    private static final FieldType indexedComponentNidType;
    private static final FieldType referencedComponentNidType;

    static {
        indexedComponentNidType = new FieldType();
        indexedComponentNidType.setNumericType(FieldType.NumericType.INT);
        indexedComponentNidType.setIndexed(false);
        indexedComponentNidType.setStored(true);
        indexedComponentNidType.setTokenized(false);
        indexedComponentNidType.freeze();
        referencedComponentNidType = new FieldType();
        referencedComponentNidType.setNumericType(FieldType.NumericType.INT);
        referencedComponentNidType.setIndexed(true);
        referencedComponentNidType.setStored(false);
        referencedComponentNidType.setTokenized(false);
        referencedComponentNidType.freeze();
    }

    private final ConcurrentHashMap<Integer, IndexedGenerationCallable> componentNidLatch = new ConcurrentHashMap<>();
    private boolean enabled = true;
    protected final ExecutorService luceneWriterService;
    protected ExecutorService luceneWriterFutureCheckerService;
    private final NRTManagerReopenThread reopenThread;
    private final NRTManager.TrackingIndexWriter trackingIndexWriter;
    private final NRTManager searcherManager;
    private final String indexName;

    public LuceneIndexer(String indexName) throws IOException {
        this.indexName = indexName;
        luceneWriterService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory(threadGroup, indexName + " Lucene writer"));
        luceneWriterFutureCheckerService = Executors.newFixedThreadPool(1,
                new NamedThreadFactory(threadGroup, indexName + " Lucene future checker"));
        setupRoot();

        File indexDirectoryFile = new File(root.getPath() + "/" + indexName);
        System.out.println("Index: " + indexDirectoryFile);
        Directory indexDirectory = initDirectory(indexDirectoryFile);

        indexDirectory.clearLock("write.lock");

        IndexWriterConfig config = new IndexWriterConfig(luceneVersion, new StandardAnalyzer(luceneVersion));
        MergePolicy mergePolicy = new LogByteSizeMergePolicy();

        config.setMergePolicy(mergePolicy);
        config.setSimilarity(new ShortTextSimilarity());

        IndexWriter indexWriter = new IndexWriter(indexDirectory, config);

        trackingIndexWriter = new NRTManager.TrackingIndexWriter(indexWriter);

        boolean applyAllDeletes = false;

        searcherManager = new NRTManager(trackingIndexWriter, null, applyAllDeletes);
        
        // Refreshes searcher every 5 seconds when nobody is waiting, and up to 100 msec delay
        // when somebody is waiting:
        reopenThread = new NRTManagerReopenThread(searcherManager, 5.0, 0.1);
        this.startThread();

    }

    private static void setupRoot() {
        String rootLocation = System.getProperty(LUCENE_ROOT_LOCATION_PROPERTY);

        if (rootLocation != null) {
            root = new File(rootLocation, DEFAULT_LUCENE_LOCATION);
        } else {
            rootLocation = System.getProperty("org.ihtsdo.otf.tcc.datastore.bdb-location");

            if (rootLocation != null) {
                root = new File(rootLocation, DEFAULT_LUCENE_LOCATION);
            } else {
                root = new File(DEFAULT_LUCENE_LOCATION);
            }
        }
    }

    private void startThread() {
        reopenThread.setName("Lucene NRT " + indexName + " Reopen Thread");
        reopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
        reopenThread.setDaemon(true);
        reopenThread.start();
    }

    @Override
    public String getIndexerName() {
        return indexName;
    }

    protected abstract boolean indexChronicle(ComponentChronicleBI<?> chronicle);

    private static Directory initDirectory(File luceneDirFile)
            throws IOException, CorruptIndexException, LockObtainFailedException {
        if (luceneDirFile.exists()) {
            return new SimpleFSDirectory(luceneDirFile);
        } else {
            luceneDirFile.mkdirs();

            return new SimpleFSDirectory(luceneDirFile);
        }
    }

    /**
     * Query index with no specified target generation of the index.
     * 
     * Calls {@link #query(String, ComponentProperty, int, long)} with the targetGeneration 
     * field set to Long.MIN_VALUE
     *
     * @param query The query to apply.
     * @param field The component field to be queried.
     * @param sizeLimit The maximum size of the result list.
     * @return a List of <code>SearchResult</codes> that contins the nid of the
     * component that matched, and the score of that match relative to other
     * matches.
     * @throws IOException
     * @throws ParseException
     */
    @Override
    public final List<SearchResult> query(String query, ComponentProperty field, int sizeLimit)
            throws IOException, ParseException {
        return query(query, field, sizeLimit, Long.MIN_VALUE);
    }
    
    /**
    *
    *Calls {@link #query(String, boolean, ComponentProperty, int, long)} with the prefixSearch field set to 
    * false.
    *
    * @param query The query to apply.
    * @param field The component field to be queried.
    * @param sizeLimit The maximum size of the result list.
    * @param targetGeneration target generation that must be included in the
    * search or Long.MIN_VALUE if there is no need to wait for a target
    * generation.  Long.MAX_VALUE can be passed in to force this query to wait until 
    * any inprogress indexing operations are completed - and then use the latest index.
    * @return a List of <code>SearchResult</codes> that contins the nid of the
    * component that matched, and the score of that match relative to other
    * matches.
    * @throws IOException
    * @throws ParseException
    */
   @Override
    public final List<SearchResult> query(String query, ComponentProperty field, int sizeLimit, long targetGeneration)
           throws IOException, ParseException {
       return query(query, false, field, sizeLimit, targetGeneration);
   }

    /**
     * A generic query API that handles most common cases.  The cases handled for various component property types
     * are details below.
     * 
     * NOTE - subclasses of LuceneIndexer may have other query(...) methods that allow for more specific and or complex
     * queries.  Specifically, {@link LuceneDynamicRefexIndexer} has its own query(...) methods.
     *
     *
     * @param query The query to apply.
     * @param field The component field to be queried.
     * @param sizeLimit The maximum size of the result list.
     * @param targetGeneration target generation that must be included in the
     * search or Long.MIN_VALUE if there is no need to wait for a target
     * generation.  Long.MAX_VALUE can be passed in to force this query to wait until 
     * any inprogress indexing operations are completed - and then use the latest index.
     * @param prefixSearch if true, utilize a search algorithm that is optimized 
     * for prefix searching, such as the searching that would be done to implement 
     * a type-ahead style search.  This is currently only applicable to 
     * {@link ComponentProperty#DESCRIPTION_TEXT} cases - is ignored for all other 
     * field types.  Does not use the Lucene Query parser.  Every term (or token) 
     * that is part of the query string will be required to be found in the result.
     * 
     * Note, it is useful to NOT trim the text of the query before it is sent in - 
     * if the last word of the query has a space character following it, that word 
     * will be required as a complete term.  If the last word of the query does not 
     * have a space character following it, that word will be required as a prefix 
     * match only.
     * 
     * For example:
     * The query "family test" will return results that contain 'Family Testudinidae'
     * The query "family test " will not match on  'Testudinidae', so that will be excluded.
     * 
     * At the moment, the only supported ComponentProperty types for a search are:
     * 
     * - {@link ComponentProperty#LONG_EXTENSION_1} - currently, only used by the 
     *     {@link LuceneRefexIndexer} to index SCTIDs.
     * 
     * - {@link ComponentProperty#DESCRIPTION_TEXT} - this is the property value you 
     *     pass in to search all indexed description types.
     *     
     * - {@link ComponentProperty#ASSEMBLAGE_ID} - This is the property value you 
     *     pass in to search for all concepts which have references to a particular
     *     Dynamic Refex Assemblage - and that particular Dynamic Refex Assemblage is 
     *     defined as an annotation style refex.  
     *
     * @return a List of <code>SearchResult</codes> that contins the nid of the
     * component that matched, and the score of that match relative to other
     * matches.
     * @throws IOException
     * @throws ParseException
     */
    public final List<SearchResult> query(String query, boolean prefixSearch, ComponentProperty field, int sizeLimit, Long targetGeneration)
            throws IOException, ParseException, NumberFormatException {

        switch (field) {
            case LONG_EXTENSION_1:
                long long1 = Long.parseLong(query);
                //TODO this should be redone as a string field... indexing these as long's is needless and slower when it comes 
                // to indexing purely SCTIDs, or NIDs.  Plus, the API above doesn't give you any ability to create a complex query 
                // anyway... which is rather limiting.  What is the point of a range query, if we can't pass a range?
                Query long1query = NumericRangeQuery.newLongRange(field.name(), long1, long1, true, true);
                return search(long1query, sizeLimit, targetGeneration);

            case DESCRIPTION_TEXT:
                return search(buildTokenizedStringQuery(query, field.name(), prefixSearch), sizeLimit, targetGeneration);

            case ASSEMBLAGE_ID:
                Query termQuery = new TermQuery(new Term(LuceneDynamicRefexIndexer.COLUMN_FIELD_ASSEMBLAGE, query));
                return search(termQuery, sizeLimit, targetGeneration);

            default:
                throw new IOException("Can't handle: " + field.name());
        }

    }

    @Override
    public final void clearIndex() {
        try {
            trackingIndexWriter.deleteAll();
        } catch (IOException ex) {
            Logger.getLogger(LuceneRefexIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public final void closeWriter() {
        try {
            reopenThread.close();
            luceneWriterService.shutdown();
            luceneWriterService.awaitTermination(15, TimeUnit.MINUTES);
            luceneWriterFutureCheckerService.shutdown();
            luceneWriterFutureCheckerService.awaitTermination(15, TimeUnit.MINUTES);
            trackingIndexWriter.getIndexWriter().close(true);
        } catch (IOException ex) {
            Logger.getLogger(LuceneRefexIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(LuceneIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public final void commitWriter() {
        try {
            trackingIndexWriter.getIndexWriter().commit();
        } catch (IOException ex) {
            Logger.getLogger(LuceneRefexIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected abstract void addFields(ComponentChronicleBI<?> chronicle, Document doc);

    @Override
    public final Future<Long> index(ComponentChronicleBI<?> chronicle) {
        if (!enabled) {
            return null;
        }

        if (indexChronicle(chronicle)) {
            Future<Long> future = luceneWriterService.submit(new AddDocument(chronicle));

            luceneWriterFutureCheckerService.execute(new FutureChecker(future));

            return future;
        }

        return unindexedFuture;
    }

    /**
     * Subclasses may call this method with much more specific queries than this generic class is capable of constructing.
     */
    protected final List<SearchResult> search(Query q, int sizeLimit, Long targetGeneration) throws IOException {
        if (targetGeneration != null && targetGeneration != Long.MIN_VALUE) {
            if (targetGeneration == Long.MAX_VALUE)
            {
                searcherManager.maybeRefreshBlocking();
            }
            else
            {
                searcherManager.waitForGeneration(targetGeneration);
            }
        }
        
        IndexSearcher searcher = searcherManager.acquire();

        try {
            if (TermstoreLogger.logger.isLoggable(Level.FINE)) {
               TermstoreLogger.logger.log(Level.FINE, "Running query: " + q.toString());
             }
            
            //Since the index carries some duplicates by design, which we will remove - get a few extra results up front.
            //so we are more likely to come up with the requested number of results
            long limitWithExtras = sizeLimit + (long)((double)sizeLimit * 0.25d);
            
            int adjustedLimit = (limitWithExtras > Integer.MAX_VALUE ? sizeLimit : (int)limitWithExtras);
            
            TopDocs topDocs = searcher.search(q, adjustedLimit);
            List<SearchResult> results = new ArrayList<>(topDocs.totalHits);
            HashSet<Integer> includedComponentIDs = new HashSet<>();

            for (ScoreDoc hit : topDocs.scoreDocs) {
                if (TermstoreLogger.logger.isLoggable(Level.FINEST)) {
                    TermstoreLogger.logger.log(Level.FINEST, "Hit: {0} Score: {1}", new Object[]{hit.doc, hit.score});
                }

                Document doc = searcher.doc(hit.doc);
                int componentId = doc.getField(ComponentProperty.COMPONENT_ID.name()).numericValue().intValue();
                if (includedComponentIDs.contains(componentId))
                {
                    continue;
                }
                else
                {
                    includedComponentIDs.add(componentId);
                    results.add(new SearchResult(componentId, hit.score));
                    if (results.size() == sizeLimit)
                    {
                        break;
                    }
                }
            }
            if (TermstoreLogger.logger.isLoggable(Level.FINE)) {
                TermstoreLogger.logger.log(Level.FINE, "Returning " + results.size() + " results from query");
              }
            return results;
        } finally {
            searcherManager.release(searcher);
        }
    }

    /**
     *
     * @param nid for the component that the caller wished to wait until it's
     * document is added to the index.
     * @return a <code>Callable&lt;Long&gt;</code> object that will block until
     * this indexer has added the document to the index. The <code>call()</code>
     * method on the object will return the index generation that contains the
     * document, which can be used in search calls to make sure the generation
     * is available to the searcher.
     */
    @Override
    public IndexedGenerationCallable getIndexedGenerationCallable(int nid) {
        IndexedGenerationCallable indexedLatch = new IndexedGenerationCallable();
        IndexedGenerationCallable existingIndexedLatch = componentNidLatch.putIfAbsent(nid, indexedLatch);

        if (existingIndexedLatch != null) {
            return existingIndexedLatch;
        }

        return indexedLatch;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private class AddDocument implements Callable<Long> {

        ComponentChronicleBI<?> chronicle;

        public AddDocument(ComponentChronicleBI<?> chronicle) {
            this.chronicle = chronicle;
        }

        @Override
        public Long call() throws Exception {
            IndexedGenerationCallable latch = componentNidLatch.remove(chronicle.getNid());
            Document doc = new Document();

            doc.add(new IntField(ComponentProperty.COMPONENT_ID.name(), chronicle.getNid(),
                    LuceneIndexer.indexedComponentNidType));
            addFields(chronicle, doc);

            // Note that the addDocument operation could cause duplicate documents to be
            // added to the index if a new luceneVersion is added after initial index
            // creation. It does this to avoid the performance penalty of
            // finding and deleting documents prior to inserting a new one.
            //
            // At this point, the number of duplicates should be
            // small, and we are willing to accept a small number of duplicates
            // because the new versions are additive (we don't allow deletion of content)
            // so the search results will be the same. Duplicates can be removed
            // by regenerating the index.
            long indexGeneration = trackingIndexWriter.addDocument(doc);

            if (latch != null) {
                latch.setIndexGeneration(indexGeneration);
            }

            return indexGeneration;
        }
    }

    /**
     * Class to ensure that any exceptions associated with indexingFutures are
     * properly logged.
     */
    private static class FutureChecker implements Runnable {

        Future<Long> future;

        public FutureChecker(Future<Long> future) {
            this.future = future;
        }

        @Override
        public void run() {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(LuceneIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static class UnindexedFuture implements Future<Long> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Long get() throws InterruptedException, ExecutionException {
            return Long.MIN_VALUE;
        }

        @Override
        public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return Long.MIN_VALUE;
        }
    }
    
    /**
     * Create a query that will match on the specified text using either the WhitespaceAnalyzer or the StandardAnalyzer.
     * Uses the Lucene Query Parser if prefixSearch is false, otherwise, uses a custom prefix algorithm.  
     * See {@link LuceneIndexer#query(String, boolean, ComponentProperty, int, Long)} for details on the prefix search algorithm. 
     */
    protected Query buildTokenizedStringQuery(String query, String field, boolean prefixSearch) throws IOException, ParseException
    {
        BooleanQuery bq = new BooleanQuery();
        
        if (prefixSearch) 
        {
            bq.add(buildPrefixQuery(query,field, new StandardAnalyzer(LuceneIndexer.luceneVersion)), Occur.SHOULD);
            bq.add(buildPrefixQuery(query,field, new WhitespaceAnalyzer(LuceneIndexer.luceneVersion)), Occur.SHOULD);
        }
        else {
            bq.add(new QueryParser(LuceneIndexer.luceneVersion, field, new StandardAnalyzer(LuceneIndexer.luceneVersion)).parse(query), Occur.SHOULD);
            bq.add(new QueryParser(LuceneIndexer.luceneVersion, field, new WhitespaceAnalyzer(LuceneIndexer.luceneVersion)).parse(query), Occur.SHOULD);
        }
        BooleanQuery wrap = new BooleanQuery();
        wrap.add(bq, Occur.MUST);
        return wrap;
    }
    
    protected static Query buildPrefixQuery(String searchString, String field, Analyzer analyzer) throws IOException
    {
        StringReader textReader = new StringReader(searchString);
        TokenStream tokenStream = analyzer.tokenStream(field, textReader);
        tokenStream.reset();
        List<String> terms = new ArrayList<>();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        
        while (tokenStream.incrementToken())
        {
            terms.add(charTermAttribute.toString());
        }
        textReader.close();
        tokenStream.close();
        analyzer.close();
        
        BooleanQuery bq = new BooleanQuery();
        if (terms.size() > 0 && !searchString.endsWith(" "))
        {
            String last = terms.remove(terms.size() - 1);
            bq.add(new PrefixQuery((new Term(field, last))), Occur.MUST);
        }
        for (String s : terms)
        {
            bq.add(new TermQuery(new Term(field, s)), Occur.MUST);
        }
        
        return bq;
    }
}
