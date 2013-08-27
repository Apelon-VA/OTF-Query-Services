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
package org.ihtsdo.otf.query.implementation;

import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.clauses.ConceptIsKindOf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import org.ihtsdo.otf.tcc.api.concept.ConceptFetcherBI;
import org.ihtsdo.otf.tcc.api.concept.ProcessUnfetchedConceptDataBI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.query.implementation.clauses.ChangedFromPreviousVersion;
import org.ihtsdo.otf.query.implementation.clauses.ConceptForComponent;
import org.ihtsdo.otf.query.implementation.clauses.ConceptIsChildOf;
import org.ihtsdo.otf.query.implementation.clauses.ConceptIsDescendentOf;
import org.ihtsdo.otf.query.implementation.clauses.DescriptionActiveLuceneMatch;
import org.ihtsdo.otf.query.implementation.clauses.DescriptionActiveRegexMatch;
import org.ihtsdo.otf.query.implementation.clauses.DescriptionLuceneMatch;
import org.ihtsdo.otf.query.implementation.clauses.DescriptionRegexMatch;
import org.ihtsdo.otf.query.implementation.clauses.FullySpecifiedNameForConcept;
import org.ihtsdo.otf.query.implementation.clauses.PreferredNameForConcept;
import org.ihtsdo.otf.query.implementation.clauses.RefsetLuceneMatch;
import org.ihtsdo.otf.query.implementation.clauses.RelType;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.ddo.concept.ConceptChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.concept.component.description.DescriptionChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RefexPolicy;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RelationshipPolicy;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.VersionPolicy;

/**
 * Executes queries within the terminology hierarchy and returns the nids of the
 * components that match the criterion of query.
 *
 * @author kec
 */
public abstract class Query {

    public String currentViewCoordinateKey = "Current view coordinate";
    private final HashMap<String, Object> letDeclarations =
            new HashMap<>();

    public HashMap<String, Object> getLetDeclarations() {
        return letDeclarations;
    }
    /**
     * Number of Components output in the returnResultSet method.
     */
    int resultSetLimit = 50;

    public HashMap<String, Object> getVCLetDeclarations() throws IOException {
        HashMap<String, Object> letVCDeclarations =
                new HashMap<>();
        letVCDeclarations.put(currentViewCoordinateKey, StandardViewCoordinates.getSnomedInferredLatest());
        return letVCDeclarations;
    }
    /**
     * The concepts, stored as nids in a
     * <code>NativeIdSetBI</code>, that are considered in the query.
     */
    private NativeIdSetBI forSet;
    /**
     * The steps requires to compute the query clause.
     */
    private EnumSet<ClauseComputeType> computeTypes =
            EnumSet.noneOf(ClauseComputeType.class);
    /**
     * The
     * <code>ViewCoordinate</code> used in the query.
     */
    private ViewCoordinate viewCoordinate;

    /**
     * Retrieves what type of iterations are required to compute the clause.
     *
     * @return an <code>EnumSet</code> of the compute types required
     */
    public EnumSet<ClauseComputeType> getComputePhases() {
        return computeTypes;
    }

    public Query(ViewCoordinate viewCoordinate) {
        this.viewCoordinate = viewCoordinate;
    }

    /**
     * Determines the set that will be searched in the query.
     *
     * @return the <code>NativeIdSetBI</code> of the set that will be queried
     * @throws IOException
     */
    protected abstract NativeIdSetBI For() throws IOException;

    public abstract void Let() throws IOException;

    /**
     * Retrieves the root clause of the query.
     *
     * @return root <code>Clause</code> in the query
     */
    public abstract Clause Where();

    public void let(String key, Object object) throws IOException {
        letDeclarations.put(key, object);
    }

    /**
     * Constructs the query and computes the set of concepts that match the
     * criterion specified in the clauses.
     *
     * @return the <code>NativeIdSetBI</code> of nids that meet the criterion of
     * the query
     * @throws IOException
     * @throws Exception
     */
    public NativeIdSetBI compute() throws IOException, Exception {
        forSet = For();
        Let();
        Clause rootClause = Where();
        NativeIdSetBI possibleComponents =
                rootClause.computePossibleComponents(forSet);
        if (computeTypes.contains(ClauseComputeType.ITERATION)) {
            NativeIdSetBI conceptsToIterateOver =
                    Ts.get().getConceptNidsForComponentNids(possibleComponents);
            Iterator itr = new Iterator(rootClause, conceptsToIterateOver);
            Ts.get().iterateConceptDataInParallel(itr);
        }
        return rootClause.computeComponents(possibleComponents);
    }

    /**
     *
     * @return the <code>ViewCoordinate</code> in the query
     */
    public ViewCoordinate getViewCoordinate() {
        return viewCoordinate;
    }

    protected RefsetLuceneMatch RefsetLuceneMatch(String luceneMatch) {
        return new RefsetLuceneMatch(this, luceneMatch);
    }

    private class Iterator implements ProcessUnfetchedConceptDataBI {

        NativeIdSetBI conceptsToIterate;
        Clause rootClause;

        public Iterator(Clause rootClause, NativeIdSetBI conceptsToIterate) {
            this.rootClause = rootClause;
            this.conceptsToIterate = conceptsToIterate;
        }

        @Override
        public boolean allowCancel() {
            return true;
        }

        @Override
        public void processUnfetchedConceptData(int cNid, ConceptFetcherBI fetcher) throws Exception {
            if (conceptsToIterate.contains(cNid)) {
                ConceptVersionBI concept = fetcher.fetch(viewCoordinate);
                for (Clause c : rootClause.getChildren()) {
                    c.getQueryMatches(concept);
                }
            }
        }

        @Override
        public NativeIdSetBI getNidSet() throws IOException {
            return conceptsToIterate;
        }

        @Override
        public String getTitle() {
            return "Query Iterator";
        }

        @Override
        public boolean continueWork() {
            return true;
        }
    }

    /**
     * Return the desired Display Objects, which are specified by
     * <code>ReturnTypes</code>.
     *
     * @param resultSet results from the Query
     * @param returnType an <code>EnumSet</code> of <code>ReturnTypes</code>,
     * the desired Display Object types
     * @return an <code>ArrayList</code> of the Display Objects
     * @throws IOException
     * @throws ContradictionException
     */
    public ArrayList<Object> returnDisplayObjects(NativeIdSetBI resultSet, ReturnTypes returnType) throws IOException, ContradictionException {
        ArrayList<Object> results = new ArrayList<>();
        
            NativeIdSetItrBI iter = resultSet.getIterator();
            switch (returnType) {
                case UUIDS:
                    while (iter.next()) {
                        results.add(Ts.get().getComponent(iter.nid()).getVersion(viewCoordinate).getPrimordialUuid());
                    }
                    break;
                case NIDS:
                    while (iter.next()) {
                        results.add(iter.nid());
                    }
                    break;
                case CONCEPT_VERSION:
                    while (iter.next()) {
                        ConceptChronicleDdo cc = new ConceptChronicleDdo(Ts.get().getSnapshot(viewCoordinate), Ts.get().getConcept(iter.nid()), VersionPolicy.ACTIVE_VERSIONS,
                                RefexPolicy.REFEX_MEMBERS_AND_REFSET_MEMBERS, RelationshipPolicy.DESTINATION_RELATIONSHIPS);
                        results.add(cc);
                    }
                    break;
                case DESCRIPTION_VERSION_FSN:
                    while (iter.next()) {
                        DescriptionChronicleBI desc = Ts.get().getConceptVersion(viewCoordinate, iter.nid()).getFullySpecifiedDescription();
                        ConceptChronicleDdo cc = new ConceptChronicleDdo(Ts.get().getSnapshot(viewCoordinate), Ts.get().getConcept(iter.nid()), VersionPolicy.ACTIVE_VERSIONS,
                                RefexPolicy.REFEX_MEMBERS_AND_REFSET_MEMBERS, RelationshipPolicy.DESTINATION_RELATIONSHIPS);
                        DescriptionChronicleDdo descChronicle = new DescriptionChronicleDdo(Ts.get().getSnapshot(viewCoordinate), cc, desc);
                        results.add(descChronicle);
                    }
                    break;
                case DESCRIPTION_VERSION_PREFERRED:
                    while (iter.next()) {
                        DescriptionChronicleBI desc = Ts.get().getConceptVersion(viewCoordinate, iter.nid()).getPreferredDescription();
                        ConceptChronicleDdo cc = new ConceptChronicleDdo(Ts.get().getSnapshot(viewCoordinate), Ts.get().getConcept(iter.nid()), VersionPolicy.ACTIVE_VERSIONS,
                                RefexPolicy.REFEX_MEMBERS_AND_REFSET_MEMBERS, RelationshipPolicy.DESTINATION_RELATIONSHIPS);
                        DescriptionChronicleDdo descChronicle = new DescriptionChronicleDdo(Ts.get().getSnapshot(viewCoordinate), cc, desc);
                        results.add(descChronicle);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Return type not supported.");
            }
        

        return results;

    }

    public void setResultSetLimit(int limit) {
        this.resultSetLimit = limit;
    }

    protected ConceptIsKindOf ConceptIsKindOf(String conceptSpecKey) {
        return new ConceptIsKindOf(this, conceptSpecKey, this.currentViewCoordinateKey);
    }

    protected ConceptIsKindOf ConceptIsKindOf(String conceptSpecKey, String viewCoordinateKey) {
        return new ConceptIsKindOf(this, conceptSpecKey, viewCoordinateKey);
    }

    protected DescriptionRegexMatch DescriptionRegexMatch(String regexKey) {
        return new DescriptionRegexMatch(this, regexKey);
    }

    protected DescriptionActiveRegexMatch DescriptionActiveRegexMatch(String regexKey) {
        return new DescriptionActiveRegexMatch(this, regexKey);
    }

    protected ConceptForComponent ConceptForComponent(Clause child) {
        return new ConceptForComponent(this, child);
    }

    protected ConceptIsDescendentOf ConceptIsDescendentOf(String conceptSpecKey) {
        return new ConceptIsDescendentOf(this, conceptSpecKey, this.currentViewCoordinateKey);
    }

    protected ConceptIsDescendentOf ConceptIsDescendentOf(String conceptSpecKey, String viewCoordinateKey) {
        return new ConceptIsDescendentOf(this, conceptSpecKey, viewCoordinateKey);
    }

    protected ConceptIsChildOf ConceptIsChildOf(String conceptSpecKey) {
        return new ConceptIsChildOf(this, conceptSpecKey, this.currentViewCoordinateKey);
    }

    protected ConceptIsChildOf ConceptIsChildOf(String conceptSpecKey, String viewCoordinateKey) {
        return new ConceptIsChildOf(this, conceptSpecKey, viewCoordinateKey);
    }

    protected DescriptionActiveLuceneMatch DescriptionActiveLuceneMatch(String queryTextKey) {
        return new DescriptionActiveLuceneMatch(this, queryTextKey);
    }

    protected DescriptionLuceneMatch DescriptionLuceneMatch(String queryTextKey) {
        return new DescriptionLuceneMatch(this, queryTextKey);
    }

    protected And And(Clause... clauses) {
        return new And(this, clauses);
    }

    protected RelType RelType(String relTypeKey, String conceptSpecKey) {
        return new RelType(this, relTypeKey, conceptSpecKey, null, this.currentViewCoordinateKey, null);
    }

    protected RelType RelType(String relTypeKey, String conceptSpecKey, String viewCoordinateKey) {
        return new RelType(this, relTypeKey, conceptSpecKey, null, viewCoordinateKey, null);
    }
    
    protected RelType RelType(String relTypeKey, String conceptSpecKey, String relRestrictionKey, Boolean subsumption){
        return new RelType(this, relTypeKey, conceptSpecKey, null, this.currentViewCoordinateKey, subsumption);
    }
    
    protected RelType RelType(String relTypeKey, String conceptSpecKey, String relRestrictionKey, String viewCoordinateKey, Boolean subsumption){
        return new RelType(this, relTypeKey, conceptSpecKey, relRestrictionKey, viewCoordinateKey, subsumption);
    }

    protected PreferredNameForConcept PreferredNameForConcept(Clause clause) {
        return new PreferredNameForConcept(this, clause);
    }

    protected And Intersection(Clause... clauses) {
        return new And(this, clauses);
    }

    protected FullySpecifiedNameForConcept FullySpecifiedNameForConcept(Clause clause) {
        return new FullySpecifiedNameForConcept(this, clause);
    }

    public Not Not(Clause clause) {
        return new Not(this, clause);
    }

    /**
     * Getter for the For set.
     *
     * @return the <code>NativeIdSetBI</code> of the concepts that will be
     * searched in the query
     */
    public NativeIdSetBI getForSet() {
        return forSet;
    }

    protected Or Or(Clause... clauses) {
        return new Or(this, clauses);
    }

    protected Or Union(Clause... clauses) {
        return new Or(this, clauses);
    }

    protected ChangedFromPreviousVersion ChangedFromPreviousVersion(String previousCoordinateKey) {
        return new ChangedFromPreviousVersion(this, previousCoordinateKey);
    }

    protected Xor Xor(Clause... clauses) {
        return new Xor(this, clauses);
    }
}
