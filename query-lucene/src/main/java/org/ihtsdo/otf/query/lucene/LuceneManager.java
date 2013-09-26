package org.ihtsdo.otf.query.lucene;

//~--- non-JDK imports --------------------------------------------------------
import java.io.File;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.Version;

import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;

//~--- JDK imports ------------------------------------------------------------


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.thread.NamedThreadFactory;


public abstract class LuceneManager {

    public static final String LUCENE_ROOT_LOCATION_PROPERTY = "org.ihtsdo.otf.tcc.query.lucene-root-location";
    public static final String DEFAULT_LUCENE_LOCATION = "lucene";

    protected static final Logger logger = Logger.getLogger(ConceptChronicleBI.class.getName());
    public final static Version version = Version.LUCENE_43;
    public static File root = new File(DEFAULT_LUCENE_LOCATION);
    protected static ExecutorService luceneWriterService =
            Executors.newFixedThreadPool(1, new NamedThreadFactory(new ThreadGroup("Lucene group"), "Lucene writer"));
    
    static {
        String rootLocation = System.getProperty(LUCENE_ROOT_LOCATION_PROPERTY);
        
        if (rootLocation != null) {
            root = new File(rootLocation, DEFAULT_LUCENE_LOCATION);
        }
    }

    public static void setRoot(File root) {
        LuceneManager.root = root;
    }
    
    protected static class ShortTextSimilarity extends DefaultSimilarity {

        public ShortTextSimilarity() {
        }

        @Override
        public float coord(int overlap, int maxOverlap) {
            return 1.0f;
        }

        @Override
        public float tf(float freq) {
            return 1.0f;
        }

        @Override
        public float tf(int freq) {
            return 1.0f;
        }
    }
}
