package org.ihtsdo.otf.query.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.ihtsdo.otf.tcc.model.cc.description.Description;
import static org.ihtsdo.otf.query.lucene.LuceneManager.logger;
import static org.ihtsdo.otf.query.lucene.LuceneManager.root;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.nid.ConcurrentBitSet;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.model.cc.P;
import org.ihtsdo.tcc.model.index.service.DescriptionIndexer;
import org.jvnet.hk2.annotations.Service;

/**
 * Lucene Manager for a Description index. Provides the description indexing service.
 * May need to surface more of these methods in the Indexer interface. 
 * @author aimeefurber
 */
@Service
public class DescriptionLuceneManager extends LuceneManager implements DescriptionIndexer {

    protected static File descLuceneMutableDirFile = new File(root.getPath() + "/mutable/lucene");
    protected static File descLuceneReadOnlyDirFile = new File(root.getPath() + "/read-only/lucene");
    protected static String descMutableDirectorySuffix = "mutable/lucene";
    protected static String descReadOnlyDirectorySuffix = "read-only/lucene";
    public static Directory descLuceneMutableDir;
    public static Directory descLuceneReadOnlyDir;
    public static int matchLimit = 10000;
    private static NativeIdSetBI uncommittedDescNids = new ConcurrentBitSet();
    private static Semaphore initSemaphore = new Semaphore(1);
    private static Semaphore luceneWriterPermit = new Semaphore(50);
    protected static DescriptionIndexGenerator descIndexer = null;
    protected static IndexReader descReadOnlyReader;
    public static IndexWriter descWriter;
    protected static DirectoryReader mutableSearcher;
    private static CountDownLatch writerLatch;

    public DescriptionLuceneManager() {
        setupLuceneDir();
    }
    
    public static void setupLuceneDir(){
        IndexWriter writer;

        descLuceneMutableDirFile = new File(root,
                descMutableDirectorySuffix);
        descLuceneReadOnlyDirFile = new File(root,
                descReadOnlyDirectorySuffix);
        writer = descWriter;

        if (writer != null) {
            try {
                writer.close(true);
            } catch (CorruptIndexException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        descLuceneMutableDir = null;
        descLuceneReadOnlyDir = null;
    }

    @Override
    public void addDescription(Description description) {
        uncommittedDescNids.setMember(description.getNid());
    }

    @Override
    public void writeToIndex(Collection<Description> items) throws IOException {
        write(items, null);
    }

    @Override
    public void writeToIndex(Collection<Description> items, ViewCoordinate viewCoordinate) throws IOException {
        write(items, viewCoordinate);
    }

    @Override
    public void commitToLucene() throws InterruptedException {
        luceneWriterPermit.acquire();

        NativeIdSetBI descNidsToCommit = new ConcurrentBitSet(uncommittedDescNids);

        uncommittedDescNids.clear();
        luceneWriterService.execute(new DescLuceneWriter(descNidsToCommit));
    }

    @Override //needs to go to a concept Indexer
    public void commitToLucene(ConceptChronicleBI c) throws InterruptedException, IOException {
        luceneWriterPermit.acquire();

        NativeIdSetBI descNidsToCommit = new ConcurrentBitSet();

        for (DescriptionChronicleBI dnid : c.getDescriptions()) {
            descNidsToCommit.setMember(dnid.getNid());
            uncommittedDescNids.setNotMember(dnid.getNid());
        }

        luceneWriterService.execute(new DescLuceneWriter(descNidsToCommit));
    }

    @Override
    public void commitWriter() throws IOException {
        if (descWriter != null) {
            descWriter.commit();
        }
    }

    @Override
    public void createIndex() throws Exception {
        createIndex(null);
    }

    public static void init() throws IOException {
        // Only do if not first time
        if (descLuceneReadOnlyDir == null) {
            initSemaphore.acquireUninterruptibly();

            try {
                if (descLuceneReadOnlyDir == null) {
                    descLuceneReadOnlyDir = initDirectory(DescriptionLuceneManager.descLuceneReadOnlyDirFile, false);
                    descReadOnlyReader = DirectoryReader.open(descLuceneReadOnlyDir);

                }
            } catch (IndexNotFoundException ex) {
                System.out.println(ex.toString());
                descReadOnlyReader = null;
            } finally {
                initSemaphore.release();
            }
        }

        if (descLuceneMutableDir == null) {
            initSemaphore.acquireUninterruptibly();

            try {
                if (descLuceneMutableDir == null) {
                    descLuceneMutableDir = initDirectory(DescriptionLuceneManager.descLuceneMutableDirFile, true);
                }
            } finally {
                initSemaphore.release();
            }
        }
    }

    public void createIndex(ViewCoordinate viewCoord) throws Exception {
        IndexWriter writer;

        init();
        writer = descWriter;

        if (writer == null) {
            descLuceneMutableDirFile.mkdirs();
            descLuceneMutableDir = setupWriter(descLuceneMutableDirFile, descLuceneMutableDir);
        }

        descIndexer = new DescriptionIndexGenerator(writer);
        P.s.iterateConceptDataInSequence(descIndexer);
        writer.commit();
    }

    @Override
    public void closeWriter() {
        IndexWriter writer;

        writer = descWriter;

        if (writer != null) {
            try {
                writer.commit();
                writer.close(true);
                writer = null;
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Exception during lucene writer close", e);
            }
        }

        descWriter = writer;
        logger.info("Shutting down luceneWriterService.");
        luceneWriterService.shutdown();
        logger.info("Awaiting termination of luceneWriterService.");

        try {
            luceneWriterService.awaitTermination(90, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    public static SearchResult search(Query q) throws CorruptIndexException, IOException {
        IndexSearcher searcher;
        init();

        int matchLimit = getMatchLimit();


        TtkMultiReader mr = null;
        if (descReadOnlyReader != null) {
            DirectoryReader newMutableSearcher =
                    DirectoryReader.openIfChanged(mutableSearcher, descWriter, true);
            if (newMutableSearcher != null) {
                mutableSearcher.close();
                mutableSearcher = newMutableSearcher;
            }
            mr = new TtkMultiReader(descReadOnlyReader, mutableSearcher);
            searcher = new IndexSearcher(mr);
            searcher.setSimilarity(new ShortTextSimilarity());
        } else {
            DirectoryReader newMutableSearcher =
                    DirectoryReader.openIfChanged(mutableSearcher, descWriter, true);
            if (newMutableSearcher != null) {
                mutableSearcher.close();
                mutableSearcher = newMutableSearcher;
            }
            searcher = new IndexSearcher(mutableSearcher);
            searcher.setSimilarity(new ShortTextSimilarity());
        }


        TopDocs topDocs = searcher.search(q, null, matchLimit);

        // Suppress duplicates in the read-only index
        List<ScoreDoc> newDocs = new ArrayList<>(topDocs.scoreDocs.length);
        HashSet<Integer> ids = new HashSet<>(topDocs.scoreDocs.length);
        String searchTerm = "dnid";

        if (mr != null) {
            for (ScoreDoc sd : topDocs.scoreDocs) {
                if (!mr.isFirstIndex(sd.doc)) {
                    newDocs.add(sd);

                    Document d = searcher.doc(sd.doc);
                    int nid = Integer.parseInt(d.get(searchTerm));

                    ids.add(nid);
                }
            }
        }

        for (ScoreDoc sd : topDocs.scoreDocs) {
            if ((mr == null) || mr.isFirstIndex(sd.doc)) {
                Document d = searcher.doc(sd.doc);
                int nid = Integer.parseInt(d.get(searchTerm));

                if (!ids.contains(nid)) {
                    newDocs.add(sd);
                }
            }
        }

        // Lucene match explainer code, useful to tweak the lucene score, uncomment for debug purposes only
        boolean explainMatch = false;

        if (explainMatch) {
            for (ScoreDoc sd : newDocs) {
                Document d = searcher.doc(sd.doc);
                DescriptionChronicleBI desc = null;
                try {
                    desc = (DescriptionChronicleBI) Ts.get().getComponent(Integer.valueOf(d.get("dnid")));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (desc != null) {
                    System.out.println("-------------------------" + desc.getPrimordialVersion().getText());
                } else {
                    System.out.println("------------------------- Null");
                }
                System.out.println(searcher.explain(q, sd.doc).toString());
            }
        }
        topDocs.scoreDocs = newDocs.toArray(new ScoreDoc[newDocs.size()]);
        topDocs.totalHits = topDocs.scoreDocs.length;

        return new SearchResult(topDocs, searcher);
    }

    private synchronized void write(Collection<Description> items, ViewCoordinate viewCoord) throws IOException {
        init();
        try {
            writeToLuceneNoLock(items);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    protected static void writeToLuceneNoLock(Collection<Description> descriptions) throws CorruptIndexException, IOException {
        if (descWriter == null) {
            descLuceneMutableDir = setupWriter(descLuceneMutableDirFile, descLuceneMutableDir);
        }

        if (descWriter != null) {
            for (Description desc : descriptions) {
                if (desc != null) {
                    descWriter.deleteDocuments(new Term("dnid", Integer.toString(desc.getNid())));
                    descWriter.addDocument(DescriptionIndexGenerator.createDoc(desc));
                }
            }
        }
    }

    private class DescLuceneWriter implements Runnable {

        private int batchSize = 200;
        private NativeIdSetBI descNidsToWrite;

        public DescLuceneWriter(NativeIdSetBI descNidsToCommit) {
            super();
            this.descNidsToWrite = descNidsToCommit;
            writerLatch = new CountDownLatch(1);
        }

        public CountDownLatch getWriterLatch() {
            return writerLatch;
        }

        @Override
        public void run() {
            try {
                ArrayList<Description> toIndex = new ArrayList<>(batchSize + 1);
                NativeIdSetItrBI idItr = descNidsToWrite.getIterator();
                int count = 0;

                while (idItr.next()) {
                    count++;

                    Description d = (Description) P.s.getComponent(idItr.nid());

                    toIndex.add(d);

                    if (count > batchSize) {
                        count = 0;
                        writeToIndex(toIndex);
                        toIndex = new ArrayList<>(batchSize + 1);
                    }
                }

                writeToIndex(toIndex);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

            luceneWriterPermit.release();
            writerLatch.countDown();
        }
    }
    public static CountDownLatch getWriterLatch(){
        return writerLatch;
    }
    public static void addUncommittedDescNid(int dNid) {
        uncommittedDescNids.setMember(dNid);
    }

    protected static Directory setupWriter(File luceneDirFile, Directory luceneDir)
            throws IOException, CorruptIndexException, LockObtainFailedException {
        if (luceneDir == null) {
            luceneDir = new SimpleFSDirectory(luceneDirFile);
        }

        luceneDir.clearLock("write.lock");

        IndexWriter writer;
        IndexWriterConfig config = new IndexWriterConfig(version, new StandardAnalyzer(version));
        MergePolicy mergePolicy = new LogByteSizeMergePolicy();

        config.setMergePolicy(mergePolicy);
        config.setSimilarity(new ShortTextSimilarity());

        if (new File(luceneDirFile, "segments.gen").exists()) {
            writer = new IndexWriter(luceneDir, config);
        } else {
            writer = new IndexWriter(luceneDir, config);
        }
        descWriter = writer;
        mutableSearcher = DirectoryReader.open(writer, true);

        return luceneDir;
    }
    
    protected static Directory initDirectory(File luceneDirFile, boolean mutable)
            throws IOException, CorruptIndexException, LockObtainFailedException {
        Directory luceneDir;

        if (luceneDirFile.exists()) {
            luceneDir = new SimpleFSDirectory(luceneDirFile);

            if (mutable) {
                DescriptionLuceneManager.setupWriter(luceneDirFile, luceneDir);
            }
        } else {
            luceneDirFile.mkdirs();
            luceneDir = new SimpleFSDirectory(luceneDirFile);

            if (mutable) {
                DescriptionLuceneManager.setupWriter(luceneDirFile, luceneDir);
            }
        }

        return luceneDir;
    }
    
    public static boolean indexExists() {
        return descLuceneMutableDirFile.exists();
    }

    public static int getMatchLimit() {
        return matchLimit;
    }

    public static void setMatchLimit(int limit) {
        matchLimit = limit;

    }
}
