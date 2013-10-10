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
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
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
 * Class that handles integration tests for
 * <code>Query</code> clauses.
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
    public void testSimpleQuery() throws IOException, Exception {
        System.out.println("Simple query: ");
        Query q = new Query() {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return null;
            }

            @Override
            public void Let() throws IOException {
                let("motion", Snomed.MOTION);
            }

            @Override
            public Clause Where() {
                return ConceptIs("motion");
            }
        };
        System.out.println(q.returnResults().size());
        for (Object o : q.returnResults()) {
            System.out.println(o);
        };
        Assert.assertEquals(1, q.returnResults().size());
    }

    @Test
    public void testRegexQuery() throws IOException, Exception {
        System.out.println("Sequence: " + Ts.get().getSequence());
        DescriptionRegexMatchTest regexTest = new DescriptionRegexMatchTest();
        NativeIdSetBI results = regexTest.getQuery().compute();
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testDifferenceQuery() throws IOException, Exception {
        XorTest xorTest = new XorTest();
        NativeIdSetBI results = xorTest.computeQuery();
        System.out.println("Different query size: " + results.size());
        Assert.assertEquals(25686, results.size());

    }

    @Test
    public void testConceptIs() throws IOException, Exception {
        ConceptIsTest test = new ConceptIsTest();
        NativeIdSetBI results = test.computeQuery();
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testDescriptionLuceneMatch() throws IOException, Exception {
        DescriptionLuceneMatchTest descLuceneMatch = new DescriptionLuceneMatchTest();
        NativeIdSetBI results = descLuceneMatch.computeQuery();
        System.out.println("Description Lucene match test size: " + results.size());
        for (Object o : descLuceneMatch.q.returnDisplayObjects(results, ReturnTypes.COMPONENT)) {
            System.out.println(o);
        }
        for(Object o: descLuceneMatch.q.returnDisplayObjects(results, ReturnTypes.DESCRIPTION)){
            System.out.println(o);
        }
        Assert.assertEquals(6, results.size());
    }
    
    @Test
    public void testOr() throws IOException, Exception{
        Query q = new Query() {

            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("motion", Snomed.MOTION);
                let("acceleration", Snomed.ACCELERATION);
            }

            @Override
            public Clause Where() {
                return Or(ConceptIs("motion"),
                            ConceptIs("acceleration"));
            }
            
        };
        
        NativeIdSetBI results = q.compute();
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testDescriptionLuceneMatch2() throws IOException, ContradictionException, Exception {
        Query q = new Query() {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("leg", "leg");
            }

            @Override
            public Clause Where() {
                return DescriptionLuceneMatch("leg");
            }
        };
        NativeIdSetBI results = q.compute();
        System.out.println("Description lucene match (leg) size: " + results.size());
        for (Object o : q.returnDisplayObjects(results, ReturnTypes.DESCRIPTION)) {
            Assert.assertTrue(o != null);
        }
        Assert.assertTrue(results.size() > 800);
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
                return Xor(ConceptIsDescendentOf("Acceleration"),
                        ConceptIsKindOf("Motion"));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Xor result size: " + results.size());
        Assert.assertEquals(6, results.size());
    }

    @Test
    public void testPreferredTerm() throws IOException, Exception {
        System.out.println("Sequence: " + Ts.get().getSequence());

        PreferredNameForConceptTest preferredNameTest = new PreferredNameForConceptTest();
        NativeIdSetBI results = preferredNameTest.computeQuery();
        System.out.println("Preferred query result count: " + results.size());
        for (Object o : preferredNameTest.getQuery().returnDisplayObjects(results, ReturnTypes.UUIDS)) {
            System.out.println(o);
        }
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testRelType() throws IOException, Exception {

        RelTypeTest relTest = new RelTypeTest();
        NativeIdSetBI results = relTest.getQuery().compute();
        System.out.println("Relationship test: " + results.size());
        Assert.assertEquals(210, results.size());

    }

    @Ignore
    @Test
    public void testRelRestrictionSubsumptionTrue() throws IOException, Exception {
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("Finding site", Snomed.FINDING_SITE);
                let("Disease", Snomed.DISEASE);
                let("Eye structure", Snomed.EYE_STRUCTURE);
            }

            @Override
            public Clause Where() {
                return Or(RelRestriction("Disease", "Finding site", "Eye structure", true));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Rel restriction count: " + results.size());
        Assert.assertEquals(290, results.size());

    }

    @Test
    public void testRelRestrictionSubsumptionFalse() throws IOException, Exception {
        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("Is a", Snomed.IS_A);
                let("Motion", Snomed.MOTION);
                let("Acceleration", Snomed.ACCELERATION);
            }

            @Override
            public Clause Where() {
                return Or(RelRestriction("Acceleration", "Is a", "Motion", false));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Rel restriction subsumption false count " + results.size());
        for (Object o : q.returnDisplayObjects(results, ReturnTypes.CONCEPT_VERSION)) {
            System.out.println(o);
        }
        Assert.assertEquals(1, results.size());
    }

    @Ignore
    @Test
    public void testRelRestrictionSubsumptionNull() throws IOException, Exception {

        RelRestrictionTest relRestriction = new RelRestrictionTest();
        NativeIdSetBI results = relRestriction.computeQuery();
        System.out.println("Rel restriction subsumption null results: " + results.size());
        Assert.assertEquals(84, results.size());

    }

    @Test
    public void testFullySpecifiedName() throws IOException, Exception {
        FullySpecifiedNameForConceptTest fsnTest = new FullySpecifiedNameForConceptTest();

        NativeIdSetBI results = fsnTest.computeQuery();
        System.out.println("Fully specified name test: " + results.size());
        for (Object o : fsnTest.getQuery().returnDisplayObjects(results, ReturnTypes.UUIDS)) {
            System.out.println(o);
        }
        Assert.assertEquals(7, results.size());

    }

    @Test
    public void testAnd() throws IOException, Exception {
        AndTest andTest = new AndTest();
        NativeIdSetBI results = andTest.computeQuery();
        System.out.println("And query test results: " + results.size());
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void isChildOfTest() throws IOException, Exception {
        IsChildOfTest isChildOfTest = new IsChildOfTest();
        Query q3 = isChildOfTest.getQuery();
        NativeIdSetBI results3 = q3.compute();
        System.out.println("Query result count " + results3.size());
        Assert.assertEquals(21, results3.size());
    }

    @Test
    public void isDescendentOfTest() throws IOException, Exception {
        IsDescendentOfTest isDescendent = new IsDescendentOfTest();
        Query q4 = isDescendent.getQuery();
        NativeIdSetBI results4 = q4.compute();
        System.out.println("ConceptIsDescendentOf query result count " + results4.size());
        Assert.assertEquals(6, results4.size());

    }

    @Test
    public void isKindOfTest() throws IOException, Exception {
        IsKindOfTest kindOf = new IsKindOfTest();
        Query kindOfQuery = kindOf.getQuery();
        NativeIdSetBI kindOfResults = kindOfQuery.compute();
        System.out.println("Kind of results: " + kindOfResults.size());
        Assert.assertEquals(171, kindOfResults.size());
    }

    @Test
    public void queryTest() throws IOException, Exception {
        Query q = new Query() {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().isKindOfSet(Snomed.MOTION.getNid(), StandardViewCoordinates.getSnomedInferredLatest());
            }

            @Override
            public void Let() throws IOException {
                let("acceleration", Snomed.ACCELERATION);
                let("is a", Snomed.IS_A);
                let("motion", Snomed.MOTION);
                let("deceleration", "[Dd]eceleration.*");
                let("continued movement", Snomed.CONTINUED_MOVEMENT);
                let("centrifugal", "[CC]entrifugal.*");
            }

            @Override
            public Clause Where() {
                return And(ConceptForComponent(DescriptionRegexMatch("deceleration")),
                        And(Or(RelType("is a", "motion"),
                        ConceptForComponent(DescriptionRegexMatch("centrifugal"))),
                        ConceptIsKindOf("motion"),
                        Not(Or(ConceptIsChildOf("acceleration"),
                        ConceptIs("continued movement")))));
            }
        };

        NativeIdSetBI results = q.compute();
        System.out.println("Query test results: " + results.size());
        for (Object o : q.returnDisplayObjects(results, ReturnTypes.UUIDS)) {
            System.out.println(o);
        }
        Assert.assertEquals(1, results.size());


    }
    
    @Test
    public void notTest() throws IOException, Exception{
        Query q = new Query() {

            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("acceleration", Snomed.ACCELERATION);
                        
            }

            @Override
            public Clause Where() {
                return Not(ConceptIs("acceleration"));
            }
        };
        
        NativeIdSetBI results = q.compute();
        System.out.println("Not test result size: " + results.size());
        Assert.assertEquals(Ts.get().getAllConceptNids().size() - 1, results.size());
    }
}