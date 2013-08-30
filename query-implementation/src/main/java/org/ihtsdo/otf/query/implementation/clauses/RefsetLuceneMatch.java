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
package org.ihtsdo.otf.query.implementation.clauses;

import java.io.IOException;
import java.util.EnumSet;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.Where;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;

/**
 * TODO: Not implemented yet.
 * @author dylangrald
 */
public class RefsetLuceneMatch extends LeafClause {

    String luceneMatch;
    String luceneMatchKey;
    ViewCoordinate vc;

    public RefsetLuceneMatch(Query enclosingQuery, String luceneMatchKey) {
        super(enclosingQuery);
        this.luceneMatchKey = luceneMatchKey;
        this.luceneMatch = (String) enclosingQuery.getLetDeclarations().get(luceneMatchKey);
        vc = enclosingQuery.getViewCoordinate();
    }

/*    @Override
    public Where.WhereClause getWhereClause() {
        Where.WhereClause whereClause = new Where.WhereClause();
        whereClause.setSemantic(Where.ClauseSemantic.REFSET_LUCENE_MATCH);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(luceneMatchKey);
        return whereClause;
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return PRE_ITERATION;
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        Collection<Integer> nids = new HashSet<>();
        try {
            nids = Ts.get().searchLuceneRefset(luceneMatch, SearchType.REFSET);
            //nids = store.searchLucene(luceneMatch, SearchType.DESCRIPTION);
        } catch (ParseException ex) {
            Logger.getLogger(DescriptionLuceneMatch.class.getName()).log(Level.SEVERE, null, ex);
        }

        NativeIdSetBI outgoingNids = new ConcurrentBitSet();
        for (Integer nid : nids) {
            outgoingNids.add(nid);

        }

        getResultsCache().or(outgoingNids);

        return outgoingNids;


    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        getResultsCache();
    }*/

    @Override
    public WhereClause getWhereClause() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
