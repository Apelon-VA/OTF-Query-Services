package org.ihtsdo.otf.query.integration.tests;

/*
 * Copyright 2013 International Health Terminology Standards Development Organisation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.ihtsdo.otf.query.lucene.DescriptionLuceneManager;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.query.lucene.LuceneManager;
import org.ihtsdo.otf.query.lucene.SearchResult;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.TerminologyBuilderBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.junit.BdbTestRunner;
import org.ihtsdo.otf.tcc.junit.BdbTestRunnerConfig;
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;
import org.ihtsdo.otf.tcc.model.cc.concept.ConceptDataManager;
import org.ihtsdo.otf.tcc.model.cc.concept.ConceptDataSimpleReference;
import org.ihtsdo.otf.tcc.model.cc.termstore.SearchType;
import org.ihtsdo.tcc.model.index.service.DescriptionIndexer;
import org.ihtsdo.tcc.model.index.service.IdIndexer;
import org.ihtsdo.tcc.model.index.service.RefsetIndexer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Class that handles integration tests for
 * <code>Query</code> clauses.
 *
 * @author kec
 */
@RunWith(BdbTestRunner.class)
@BdbTestRunnerConfig()
public class LuceneTest {
    EditCoordinate ec;
    ViewCoordinate vc;

    public LuceneTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {
            int authorNid = TermAux.USER.getLenient().getConceptNid();
                int editPathNid = TermAux.WB_AUX_PATH.getLenient().getConceptNid();
                ec = new EditCoordinate(authorNid, Snomed.CORE_MODULE.getLenient().getNid(), editPathNid);
                vc = Ts.get().getMetadataVC();
        } catch (IOException ex) {
            Logger.getLogger(LuceneTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLuceneDescriptionIndex() throws IOException, Exception {
        
        //add test description to concept
        ConceptChronicleBI concept = Ts.get().getConcept(Snomed.BODY_STRUCTURE.getLenient().getNid());
        String testDescription = "Test description lucene index";
        DescriptionCAB descBp = new DescriptionCAB(concept.getPrimordialUuid(),
                SnomedMetadataRf2.SYNONYM_RF2.getLenient().getPrimordialUuid(),
                LanguageCode.EN,
                testDescription,
                false,
                IdDirective.GENERATE_HASH);
        TerminologyBuilderBI builder = Ts.get().getTerminologyBuilder(ec, vc);
        DescriptionChronicleBI newDesc = builder.construct(descBp);
        Ts.get().commit();

        //search for test description in lucene index
        String[] parts = testDescription.split(" ");
        HashSet<String> wordSet = new HashSet<String>();
        for (String word : parts) {
            if (!wordSet.contains(word) && word.length() > 1
                    && !word.startsWith("(") && !word.endsWith(")")) {
                word = QueryParser.escape(word);
                wordSet.add(word);
            }
        }
        String queryTerm = null;
        for (String word : wordSet) {
            if (queryTerm == null) {
                queryTerm = "+" + word;
            } else {
                queryTerm = queryTerm + " " + "+" + word;
            }
        }
        Query q = new QueryParser(LuceneManager.version, "desc",
                new StandardAnalyzer(LuceneManager.version)).parse(queryTerm);
        SearchResult result = DescriptionLuceneManager.search(q);
        boolean found = false;
        for (int i = 0; i < result.topDocs.totalHits; i++) {
            Document doc = result.searcher.doc(result.topDocs.scoreDocs[i].doc);
            int dnid = Integer.parseInt(doc.get("dnid"));
            DescriptionVersionBI description =
                    (DescriptionVersionBI) Ts.get().getComponentVersion(Ts.get().getMetadataVC(), dnid);
            if(description.getText().equals(testDescription)){
                found = true;
            }
        }
        Assert.assertTrue(found);
    }
    
    @Test
    public void testLuceneRefsetIndex(){
        // TODO need to write test once refset lucene index is working
        // currently, just checking that refset index service is available
        RefsetIndexer refsetIndexer = Hk2Looker.get().getService(RefsetIndexer.class);
        Assert.assertNotNull(refsetIndexer);
    }
    
    @Test
    public void testLuceneIdIndex(){
        // TODO need to write test once ID lucene index is working
        // currently, just checking that ID index service is available
        IdIndexer idIndexer = Hk2Looker.get().getService(IdIndexer.class);
        Assert.assertNotNull(idIndexer);
    }
}