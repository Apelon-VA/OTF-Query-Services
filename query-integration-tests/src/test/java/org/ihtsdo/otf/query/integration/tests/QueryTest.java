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
import java.util.ArrayList;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.ConcurrentBitSet;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.junit.BdbTestRunner;
import org.ihtsdo.otf.tcc.junit.BdbTestRunnerConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
public class QueryTest {

    public QueryTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRegexQuery() throws IOException, Exception {
        System.out.println("Sequence: " + Ts.get().getSequence());
        try {
            Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
                @Override
                protected NativeIdSetBI For() throws IOException {
                    NativeIdSetBI forSet = new ConcurrentBitSet();
                    forSet.add(Snomed.MOTION.getNid());
                    forSet.add(Snomed.ACCELERATION.getNid());
                    forSet.add(Snomed.CENTRIFUGAL_FORCE.getNid());
                    forSet.add(Snomed.CONTINUED_MOVEMENT.getNid());
                    forSet.add(Snomed.DECELERATION.getNid());
                    forSet.add((Snomed.MOMENTUM.getNid()));
                    forSet.add(Snomed.VIBRATION.getNid());
                    return forSet;
                }

                @Override
                public void Let() throws IOException {
                    let("regex", "[Vv]ibration");
                }

                @Override
                public Clause Where() {
                    return Or(ConceptForComponent(DescriptionRegexMatch("regex")));
                }
            };

            NativeIdSetBI results = q.compute();
            System.out.println("Regex query result count: " + results.size());
            Assert.assertEquals(1, results.size());


        } catch (IOException ex) {
            Assert.fail(ex.toString());
        } catch (Exception ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void testXor() throws IOException, Exception {

        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("Acceleration", Snomed.ACCELERATION);
                let("Motion", Snomed.MOTION);
            }

            @Override
            public Clause Where() {
                return Xor(ConceptIsChildOf("Acceleration"), ConceptIsChildOf("Motion"));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Xor result size: " + results.size());
        Assert.assertEquals(6, results.size());

        ArrayList<Object> resultSet = q.returnDisplayObjects(results, ReturnTypes.DESCRIPTION_VERSION_FSN);
        for (Object id : resultSet) {
            System.out.println(id);
        }

        Assert.assertEquals(6, resultSet.size());

        ArrayList<Object> resultSet2 = q.returnDisplayObjects(results, ReturnTypes.DESCRIPTION_VERSION_PREFERRED);
        for (Object id : resultSet2) {
            System.out.println(id);
        }

        Assert.assertEquals(6, resultSet2.size());


    }

    @Test
    public void testPreferredTerm() throws IOException, Exception {
        System.out.println("Sequence: " + Ts.get().getSequence());

        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                NativeIdSetBI forSet = new ConcurrentBitSet();
                forSet.add(Snomed.STATUS.getNid());
                return forSet;
            }

            @Override
            public void Let() throws IOException {
                let("status", Snomed.STATUS);
            }

            @Override
            public Clause Where() {
                return PreferredNameForConcept(ConceptIsKindOf("status"));
            }
        };
        NativeIdSetBI results = q.compute();
        System.out.println("Preferred query result count: " + results.size());
        System.out.println("Preferred nids: " + results.getMin());
        Assert.assertEquals(1, results.size());
    }

    @Ignore
    @Test
    public void testLuceneMatch() throws IOException, Exception {
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();

            }

            @Override
            public void Let() throws IOException {
                let("Centrifugal", "Centrifugal");
            }

            @Override
            public Clause Where() {
                return Or(DescriptionLuceneMatch("Centrifugal"));

            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Description lucene search test: " + results.size());
        Assert.assertEquals(18, results.size());
    }

    @Ignore
    @Test
    public void testRelType() throws IOException, Exception {
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("Finding site", Snomed.FINDING_SITE);
               // let("Endocrine", Snomed.STRUCTURE_OF_ENDOCRINE_SYSTEM);
            }

            @Override
            public Clause Where() {
                return Or(RelType("Finding site", "Endocrine"));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Relationship test: " + results.size());
        Assert.assertEquals(501, results.size());
    }
    
    @Ignore
    @Test
    public void TestRelTypeRestriction() throws IOException, Exception{
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {

            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("Physical force", Snomed.PHYSICAL_FORCE);
                let("Motion", Snomed.MOTION);
                let("Is a", Snomed.IS_A);
            }

            @Override
            public Clause Where() {
                return Or(RelType("Is a", "Physical force", "Motion", true));
            }
        };
        
        NativeIdSetBI results = q.compute();
        System.out.println("Rel restriction count: " + results.size());
        Assert.assertEquals(6, results.size());
        
    }

    @Test
    public void testFullySpecifiedName() throws IOException, Exception {
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();

            }

            @Override
            public void Let() throws IOException {
                let("motion", Snomed.MOTION);
            }

            @Override
            public Clause Where() {
                return FullySpecifiedNameForConcept(ConceptIsKindOf("motion"));

            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Fully specified name test: " + results.size());
        Assert.assertEquals(7, results.size());

    }

    @Test
    public void testQuery() throws IOException, Exception {
        System.out.println("Sequence: " + Ts.get().getSequence());
        try {
            Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
                @Override
                public NativeIdSetBI For() throws IOException {
                    return Ts.get().getAllConceptNids();
                }

                @Override
                public void Let() throws IOException {
                    let("allergic-asthma", Snomed.ALLERGIC_ASTHMA);
                }

                @Override
                public Clause Where() {
                    return And(ConceptIsKindOf("allergic-asthma"));
                }
            };

            NativeIdSetBI results = q.compute();
            System.out.println("Query result count: " + results.size());
            Assert.assertEquals(11, results.size());


        } catch (IOException ex) {
            Assert.fail(ex.toString());
        } catch (Exception ex) {
            Assert.fail(ex.toString());
        }

        IsChildOfTest isChildOfTest = new IsChildOfTest();
        Query q3 = isChildOfTest.getQuery();
        NativeIdSetBI results3 = q3.compute();
        System.out.println("Query result count " + results3.size());
        Assert.assertEquals(21, results3.size());

        IsDescendentOfTest isDescendent = new IsDescendentOfTest();
        Query q4 = isDescendent.getQuery();
        NativeIdSetBI results4 = q4.compute();
        System.out.println("ConceptIsDescendentOf query result count " + results4.size());
        Assert.assertEquals(6, results4.size());

        ArrayList<Object> resultSet = q4.returnDisplayObjects(results4, ReturnTypes.UUIDS);
        for (Object id : resultSet) {
            System.out.println(id);
        }

        Assert.assertEquals(6, resultSet.size());

        for (int id : results4.getSetValues()) {
            System.out.println(id);
        }

        IsKindOfTest kindOf = new IsKindOfTest();
        Query kindOfQuery = kindOf.getQuery();
        NativeIdSetBI kindOfResults = kindOfQuery.compute();
        System.out.println("Kind of results: " + kindOfResults.size());
        Assert.assertEquals(171, kindOfResults.size());
        /*
         ChangedFromPreviousVersionTest previous = new ChangedFromPreviousVersionTest();
         Query changesQuery = previous.getQuery();
         NativeIdSetBI changesResults = changesQuery.compute();
         System.out.println("Query result count " + changesResults.size());
         Assert.assertEquals(6, changesResults.size());
         */

        /*DescriptionRegexMatchTest regexMatch = new DescriptionRegexMatchTest();
         Query regexMatchQuery = regexMatch.getQuery();
         NativeIdSetBI regexResults = regexMatchQuery.compute();
         System.out.println("Regex query result count " + regexResults.size());
         Assert.assertEquals(2, regexResults.size());
         */
        /*
         ConceptForComponentTest componentTest = new ConceptForComponentTest();
         Query componentQuery = componentTest.getQuery();
         NativeIdSetBI componentResults = componentQuery.compute();
         System.out.println("\n\nQuery result count: " + componentResults.size());
         Assert.assertEquals(1, componentResults.size());
         */
    }
}