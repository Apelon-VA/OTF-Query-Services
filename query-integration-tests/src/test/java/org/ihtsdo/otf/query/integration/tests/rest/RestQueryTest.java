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
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.query.rest.server.LuceneResource;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.junit.BdbTestRunner;
import org.ihtsdo.otf.tcc.junit.BdbTestRunnerConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

//    @Override
//    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
//        return super.getTestContainerFactory(); 
//    }
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
                    queryParam("RETURN", ReturnTypes.NIDS.name()).
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

    private static String getXmlString(JAXBContext ctx, Object obj) throws JAXBException {
        StringWriter writer;
        writer = new StringWriter();
        ctx.createMarshaller().marshal(obj, writer);
        String letMapXml = writer.toString();
        return letMapXml;
    }
}