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
package org.ihtsdo.otf.query.integration.tests.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import junit.framework.Assert;
import org.apache.lucene.queryparser.classic.ParseException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.ihtsdo.otf.query.rest.server.QueryResource;
import org.ihtsdo.otf.query.implementation.ForCollection;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.implementation.LetMap;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.query.integration.tests.ConceptForComponentTest;
import org.ihtsdo.otf.query.integration.tests.ConceptIsTest;
import org.ihtsdo.otf.query.integration.tests.FullySpecifiedNameForConceptTest;
import org.ihtsdo.otf.query.integration.tests.IsChildOfTest;
import org.ihtsdo.otf.query.integration.tests.IsDescendentOfTest;
import org.ihtsdo.otf.query.integration.tests.IsKindOfTest;
import org.ihtsdo.otf.query.integration.tests.NotTest;
import org.ihtsdo.otf.query.integration.tests.OrTest;
import org.ihtsdo.otf.query.integration.tests.PreferredNameForConceptTest;
import org.ihtsdo.otf.query.integration.tests.QueryClauseTest;
import org.ihtsdo.otf.query.integration.tests.QueryTest;
import org.ihtsdo.otf.query.integration.tests.RefsetContainsConceptTest;
import org.ihtsdo.otf.query.integration.tests.RefsetContainsKindOfConceptTest;
import org.ihtsdo.otf.query.integration.tests.RefsetContainsStringTest;
import org.ihtsdo.otf.query.integration.tests.RefsetLuceneMatchTest;
import org.ihtsdo.otf.query.integration.tests.RelRestriction2Test;
import org.ihtsdo.otf.query.integration.tests.RelRestrictionTest;
import org.ihtsdo.otf.query.integration.tests.RelTypeTest;
import org.ihtsdo.otf.query.integration.tests.XorTest;
import org.ihtsdo.otf.query.rest.server.LuceneResource;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.blueprint.TerminologyBuilderBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.nid.ConcurrentBitSet;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.junit.BdbTestRunner;
import org.ihtsdo.otf.tcc.junit.BdbTestRunnerConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author kec
 */
@RunWith(BdbTestRunner.class)
@BdbTestRunnerConfig()
public class RestQueryTest extends JerseyTest {

    public RestQueryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    @Override
    public void setUp() {
    }

    @After
    @Override
    public void tearDown() {
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(QueryResource.class);
    }

    @Test
    public void testQuery() {
        try {
            ExampleQuery q = new ExampleQuery(null);

            JAXBContext ctx = JaxbForQuery.get();

            String viewCoordinateXml = getXmlString(ctx,
                    StandardViewCoordinates.getSnomedInferredLatest());

            String forXml = getXmlString(ctx, new ForCollection());

            q.Let();
            Map<String, Object> map = q.getLetDeclarations();
            LetMap wrappedMap = new LetMap(map);
            String letMapXml = getXmlString(ctx, wrappedMap);

            WhereClause where = q.Where().getWhereClause();

            String whereXml = getXmlString(ctx, where);

            final String resultString = target("query-service/query").
                    queryParam("VIEWPOINT", viewCoordinateXml).
                    queryParam("FOR", forXml).
                    queryParam("LET", letMapXml).
                    queryParam("WHERE", whereXml).
                    queryParam("RETURN", ReturnTypes.UUIDS.name()).
                    request(MediaType.TEXT_PLAIN).get(String.class);

            Logger.getLogger(RestQueryTest.class.getName()).log(Level.INFO,
                    "Result: {0}", resultString);

        } catch (JAXBException | IOException ex) {
            Logger.getLogger(RestQueryTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testLuceneRestQuery() throws IOException, ContradictionException, ParseException, JAXBException {
        LuceneResource lr = new LuceneResource();
        String queryText = "oligophrenia";
        String luceneResource = null;
        try {
            luceneResource = lr.doQuery(queryText);
        } catch (Exception ex) {
            Logger.getLogger(RestQueryTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Assert.assertTrue(luceneResource != null);
        System.out.println(luceneResource);
    }

    @Test
    public void conceptForComponentTest() throws IOException, JAXBException {
        System.out.println("ConceptForComponentTest");
        ConceptForComponentTest test = new ConceptForComponentTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(3, getNidSet(resultString).size());
    }

    @Test
    public void conceptIsTest() throws IOException, JAXBException {
        System.out.println("ConceptIsTest");
        ConceptIsTest test = new ConceptIsTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(1, getNidSet(resultString).size());
    }

    @Test
    public void fsnTest() throws JAXBException, IOException, Exception {
        System.out.println("FSN test");
        FullySpecifiedNameForConceptTest fsnTest = new FullySpecifiedNameForConceptTest();
        String resultString = returnResultString(fsnTest);
        System.out.println(resultString);
        Assert.assertEquals(7, getNidSet(resultString).size());
    }

    @Test
    public void isChildOfTest() throws IOException, JAXBException {
        System.out.println("IsChildOf test");
        IsChildOfTest icoTest = new IsChildOfTest();
        String resultString = returnResultString(icoTest);
        Assert.assertEquals(21, getNidSet(resultString).size());
    }

    @Test
    public void IsDescendentOfTest() throws IOException, JAXBException {
        System.out.println("IsDescendentOf test");
        IsDescendentOfTest idoTest = new IsDescendentOfTest();
        String resultString = returnResultString(idoTest);
        Assert.assertEquals(6, getNidSet(resultString).size());
    }

    @Test
    public void IsKindOfTest() throws IOException, JAXBException {
        System.out.println("IsKindOf test");
        IsKindOfTest ikoTest = new IsKindOfTest();
        String resultString = returnResultString(ikoTest);
        Assert.assertEquals(171, getNidSet(resultString).size());
    }

    @Ignore
    @Test
    public void NotTest() throws IOException, JAXBException {
        System.out.println("Not test");
        NotTest notTest = new NotTest();
        String resultString = returnResultString(notTest);
        Assert.assertEquals(5, getNidSet(resultString).size());
    }

    @Test
    public void OrTest() throws IOException, JAXBException {
        System.out.println("Or test");
        OrTest orTest = new OrTest();
        String resultString = returnResultString(orTest);
        Assert.assertEquals(3, getNidSet(resultString).size());
    }

    @Test
    public void PreferredNameForConceptTest() throws IOException, JAXBException {
        System.out.println("PreferredNameForConcept test");
        PreferredNameForConceptTest pnfcTest = new PreferredNameForConceptTest();
        String resultString = returnResultString(pnfcTest);
        Assert.assertEquals(4, getNidSet(resultString).size());
    }

    @Test
    public void RefsetContainsConceptTest() throws IOException, JAXBException {
        System.out.println("RefsetContainsConcept test");
        addRefsetMember();
        RefsetContainsConceptTest rccTest = new RefsetContainsConceptTest();
        String resultString = returnResultString(rccTest);
        Assert.assertEquals(1, getNidSet(resultString).size());
    }

    @Test
    public void RefsetContainsKindOfConceptTest() throws JAXBException, IOException {
        System.out.println("RefsetContainsKindOfConcept test");
        addRefsetMember();
        RefsetContainsKindOfConceptTest test = new RefsetContainsKindOfConceptTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(1, getNidSet(resultString).size());
    }

    @Test
    public void RefsetContainsStringTest() throws JAXBException, IOException {
        System.out.println("RefsetContainsString test");
        addRefsetMember();
        RefsetContainsStringTest test = new RefsetContainsStringTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(1, getNidSet(resultString).size());
    }

    @Test
    public void RefsetLuceneMatchTest() throws JAXBException, IOException {
        System.out.println("RefsetLuceneMatch test");
        RefsetLuceneMatchTest test = new RefsetLuceneMatchTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(1, getNidSet(resultString).size());
    }

    @Test
    public void RelRestrictionTest() throws IOException, JAXBException {
        System.out.println("RelRestriction test");
        RelRestrictionTest test = new RelRestrictionTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(3, getNidSet(resultString).size());
    }
    
    /**
     * TODO
     * @throws Exception 
     */
    @Ignore
    @Test
    public void RelRestrictionSubFalseTest() throws Exception{
        System.out.println("RelRestriction subsumption false test");
        RelRestriction2Test test = new RelRestriction2Test();
        String resultsString = returnResultString(test);
        Assert.assertEquals(1, getNidSet(resultsString).size());
    }

    @Test
    public void RelTypeTest() throws JAXBException, IOException {
        System.out.println("RelType test");
        RelTypeTest test = new RelTypeTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(228, getNidSet(resultString).size());
    }

    @Test
    public void XorTest() throws IOException, JAXBException {
        System.out.println("Xor test");
        XorTest test = new XorTest();
        String resultString = returnResultString(test);
        Assert.assertEquals(2, getNidSet(resultString).size());
    }

    public String returnResultString(QueryClauseTest test, ReturnTypes returnType) throws JAXBException, IOException {
        JAXBContext ctx = JaxbForQuery.get();
        String viewCoordinateXml = getXmlString(ctx,
                StandardViewCoordinates.getSnomedInferredLatest());

        String forXml = getXmlString(ctx, new ForCollection());

        Query q = test.getQuery();

        q.Let();
        Map<String, Object> map = q.getLetDeclarations();
        LetMap wrappedMap = new LetMap(map);
        String letMapXml = getXmlString(ctx, wrappedMap);

        WhereClause where = q.Where().getWhereClause();

        String whereXml = getXmlString(ctx, where);

        final String resultString = target("query-service/query").
                queryParam("VIEWPOINT", viewCoordinateXml).
                queryParam("FOR", forXml).
                queryParam("LET", letMapXml).
                queryParam("WHERE", whereXml).
                queryParam("RETURN", returnType.name()).
                request(MediaType.TEXT_PLAIN).get(String.class);

        return resultString;
    }
    
    public String returnResultString(QueryClauseTest test) throws JAXBException, IOException{
        return returnResultString(test, ReturnTypes.NIDS);
    }

    private NativeIdSetBI getNidSet(String resultString) {
        StringTokenizer st = new StringTokenizer(resultString, "<>");
        NativeIdSetBI results = new ConcurrentBitSet();
        while (st.hasMoreElements()) {
            String nextToken = st.nextToken();
            if (nextToken.matches("-[0-9]*")) {
                results.add(Integer.parseInt(nextToken));
            }
        }
        return results;
    }

    private static String getXmlString(JAXBContext ctx, Object obj) throws JAXBException {
        StringWriter writer;
        writer = new StringWriter();
        ctx.createMarshaller().marshal(obj, writer);
        String letMapXml = writer.toString();
        return letMapXml;
    }
    
        
    public void addRefsetMember() throws IOException {
        try {
            RefexCAB refex = new RefexCAB(RefexType.STR, Snomed.MILD.getLenient().getNid(), Snomed.SEVERITY_REFSET.getLenient().getNid(), IdDirective.GENERATE_HASH, RefexDirective.INCLUDE);
            refex.put(ComponentProperty.STRING_EXTENSION_1, "Mild severity");
            int authorNid = TermAux.USER.getLenient().getConceptNid();
            int editPathNid = TermAux.WB_AUX_PATH.getLenient().getConceptNid();

            EditCoordinate ec = new EditCoordinate(authorNid, Snomed.CORE_MODULE.getLenient().getNid(), editPathNid);
            TerminologyBuilderBI tb = Ts.get().getTerminologyBuilder(ec, org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates.getSnomedInferredLatest());
            RefexChronicleBI rc = tb.construct(refex);
            Ts.get().addUncommitted(Snomed.SEVERITY_REFSET.getLenient());
            Ts.get().commit();
        } catch (InvalidCAB ex) {
            Logger.getLogger(QueryTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ContradictionException ex) {
            Logger.getLogger(QueryTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
