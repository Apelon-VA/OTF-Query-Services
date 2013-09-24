package org.ihtsdo.otf.query.lucene;

//~--- non-JDK imports --------------------------------------------------------
import java.io.File;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.Version;

import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;

//~--- JDK imports ------------------------------------------------------------


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.thread.NamedThreadFactory;


public abstract class LuceneManager {

    protected static final Logger logger = Logger.getLogger(ConceptChronicleBI.class.getName());
    public final static Version version = Version.LUCENE_40;
    public static File root = new File("berkeley-db"); //TODO consider if this should differ based of index type
    protected static ExecutorService luceneWriterService =
            Executors.newFixedThreadPool(1, new NamedThreadFactory(new ThreadGroup("Lucene group"), "Lucene writer"));

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
