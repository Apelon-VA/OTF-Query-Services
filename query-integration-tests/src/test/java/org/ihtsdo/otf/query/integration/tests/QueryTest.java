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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.blueprint.TerminologyBuilderBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
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
 * Class that handles integration tests for <code>Query</code> clauses.
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
    public void setUp() throws ValidationException, IOException {
    }

    @After
    public void tearDown() {
    }

//    @Test
//    public void testChangedFromPreviousVersion() throws IOException, Exception {
//        System.out.println("Changed from previous version");
//        ChangedFromPreviousVersionTest changeTest = new ChangedFromPreviousVersionTest();
//        NativeIdSetBI results = changeTest.computeQuery();
//        Assert.assertEquals(1, results.size());
//
//    }

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
        XorVersionTest xorTest = new XorVersionTest();
        NativeIdSetBI results = xorTest.computeQuery();
        System.out.println("Different query size: " + results.size());
        Assert.assertEquals(55271, results.size());

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
        for (Object o : descLuceneMatch.q.returnDisplayObjects(results, ReturnTypes.DESCRIPTION)) {
            System.out.println(o);
        }
        Assert.assertEquals(6, results.size());
    }

    @Test
    public void testOr() throws IOException, Exception {
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
        Assert.assertEquals(4, results.size());
    }

    @Test
    public void testRelType() throws IOException, Exception {

        RelTypeTest relTest = new RelTypeTest();
        NativeIdSetBI results = relTest.getQuery().compute();
        System.out.println("Relationship test: " + results.size());
        Assert.assertEquals(228, results.size());

    }

    @Test
    public void testRelTypeVersioning() throws IOException, Exception {
        final SetViewCoordinate setViewCoordinate = new SetViewCoordinate(2002, 1, 31, 0, 0);

        Query q = new Query() {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("endocrine system", Snomed.STRUCTURE_OF_ENDOCRINE_SYSTEM);
                let("finding site", Snomed.FINDING_SITE);
                let("v2", setViewCoordinate.getViewCoordinate());
            }

            @Override
            public Clause Where() {
                return And(RelType("finding site", "endocrine system"), Not(RelType("finding site", "endocrine system", "v2")));
            }
        };

        NativeIdSetBI results = q.compute();
        Assert.assertEquals(228 - 17, results.size());
    }

    @Test
    public void testRelRestrictionSubsumptionTrue() throws IOException, Exception {
        System.out.println("Rel restriction subsumption true");
        RelRestrictionTest rrTest = new RelRestrictionTest();
        NativeIdSetBI results = rrTest.computeQuery();
        for (Object o : rrTest.q.returnDisplayObjects(results, ReturnTypes.COMPONENT)) {
            System.out.println(o);
        }
        Assert.assertEquals(3, results.size());

    }

    @Test
    public void testRelRestrictionSubsumptionFalse() throws IOException, Exception {
        System.out.println("RelRestriction subsumption false test");
        RelRestriction2Test test = new RelRestriction2Test();
        NativeIdSetBI results = test.computeQuery();
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testRelRestrictionSubsumptionNull() throws IOException, Exception {

        Query q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().getAllConceptNids();
            }

            @Override
            public void Let() throws IOException {
                let("physical force", Snomed.PHYSICAL_FORCE);
                let("is a", Snomed.IS_A);
                let("motion", Snomed.MOTION);
            }

            @Override
            public Clause Where() {
                return Or(RelRestriction("motion", "is a", "physical force"));
            }
        };
        NativeIdSetBI results = q.compute();
        int[] setValues = results.getSetValues();
        int count = 0;
        for (Object o : q.returnDisplayObjects(results, ReturnTypes.NIDS)) {
            Assert.assertEquals(setValues[count], Integer.parseInt(o.toString()));
            count++;
        }
        System.out.println("Rel restriction subsumption null results: " + results.size());
        Assert.assertEquals(7, results.size());

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
        Assert.assertEquals(7, fsnTest.getQuery().returnDisplayObjects(results, ReturnTypes.NIDS).size());

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
    public void notTest() throws IOException, Exception {
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

    @Test
    public void conceptForComponentTest() throws IOException, Exception {
        System.out.println("ConceptForComponentTest");
        ConceptForComponentTest cfcTest = new ConceptForComponentTest();
        NativeIdSetBI results = cfcTest.computeQuery();
        Assert.assertEquals(3, results.size());
    }

    @Test
    public void refsetLuceneMatchTest() throws IOException, Exception {
        System.out.println("RefsetLuceneMatch test");
        RefsetLuceneMatchTest rlmTest = new RefsetLuceneMatchTest();
        NativeIdSetBI ids = rlmTest.computeQuery();
        for (Object o : rlmTest.q.returnDisplayObjects(ids, ReturnTypes.COMPONENT)) {
            System.out.println(o);
            Assert.assertTrue(o.toString().contains("Virtual medicinal product simple reference set"));
        }
        Assert.assertEquals(1, ids.size());
    }

    @Test
    public void refsetContainsConceptTest() throws IOException, Exception {
        this.addRefsetMember();
        System.out.println("RefsetContainsConcept test");
        RefsetContainsConceptTest rccTest = new RefsetContainsConceptTest();
        NativeIdSetBI ids = rccTest.computeQuery();
        Assert.assertEquals(1, ids.size());

    }

    @Test
    public void refsetContainsStringTest() throws Exception {
        System.out.println("RefsetContainsString test");
        this.addRefsetMember();
        RefsetContainsStringTest rcsTest = new RefsetContainsStringTest();
        NativeIdSetBI ids = rcsTest.computeQuery();
        Assert.assertEquals(1, ids.size());
    }

    @Test
    public void refsetContainsKindOfConceptTest() throws Exception {
        System.out.println("RefsetContainsKindOfConcept test");
        this.addRefsetMember();
        RefsetContainsKindOfConceptTest rckocTest = new RefsetContainsKindOfConceptTest();
        NativeIdSetBI nids = rckocTest.computeQuery();
        Assert.assertEquals(1, nids.size());
    }

    @Test
    public void orTest() throws IOException, Exception{
        System.out.println("Or test");
        OrTest orTest = new OrTest();
        NativeIdSetBI results = orTest.computeQuery();
        Assert.assertEquals(3, results.size());
    }
    
    @Test
    public void notTest2() throws IOException, Exception{
        System.out.println("Not test2");
        NotTest notTest = new NotTest();
        NativeIdSetBI results = notTest.computeQuery();
        Assert.assertEquals(6, results.size());
    }
    
    public void addRefsetMember() throws IOException {
        try {
            RefexCAB refex = new RefexCAB(RefexType.STR, Snomed.MILD.getLenient().getNid(), Snomed.SEVERITY_REFSET.getLenient().getNid(), IdDirective.GENERATE_HASH, RefexDirective.INCLUDE);
            refex.put(ComponentProperty.STRING_EXTENSION_1, "Mild severity");
            int authorNid = TermAux.USER.getLenient().getConceptNid();
            int editPathNid = TermAux.WB_AUX_PATH.getLenient().getConceptNid();

            EditCoordinate ec = new EditCoordinate(authorNid, Snomed.CORE_MODULE.getLenient().getNid(), editPathNid);
            TerminologyBuilderBI tb = Ts.get().getTerminologyBuilder(ec, StandardViewCoordinates.getSnomedInferredLatest());
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
