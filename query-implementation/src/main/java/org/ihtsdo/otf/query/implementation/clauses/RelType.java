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
import org.ihtsdo.otf.tcc.api.nid.ConcurrentBitSet;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.ClauseSemantic;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.datastore.Bdb;

/**
 * Computes all concepts that have a source relationship matching the input
 * target concept and relationship type. If the relationship type subsumption is
 * true, then the clause computes the matching concepts using all relationship
 * types that are a kind of the input relationship type. Queries that specify a
 * relationship target restriction can be constructed using the
 * <code>RelRestriction</code> clause.
 *
 * @author dylangrald
 */
public class RelType extends LeafClause {

    ConceptSpec targetSpec;
    String targetSpecKey;
    String viewCoordinateKey;
    ViewCoordinate vc;
    Query enclosingQuery;
    ConceptSpec relType;
    String relTypeSpecKey;
    NativeIdSetBI cache;
    Boolean relTypeSubsumption;

    public RelType(Query enclosingQuery, String relTypeSpecKey, String targetSpecKey, String viewCoordinateKey, Boolean relTypeSubsumption) {
        super(enclosingQuery);
        this.targetSpecKey = targetSpecKey;
        this.targetSpec = (ConceptSpec) enclosingQuery.getLetDeclarations().get(targetSpecKey);
        this.viewCoordinateKey = viewCoordinateKey;
        this.enclosingQuery = enclosingQuery;
        this.relTypeSpecKey = relTypeSpecKey;
        this.relType = (ConceptSpec) enclosingQuery.getLetDeclarations().get(relTypeSpecKey);
        this.relTypeSubsumption = relTypeSubsumption;

    }

    @Override
    public WhereClause getWhereClause() {
        WhereClause whereClause = new WhereClause();
        whereClause.setSemantic(ClauseSemantic.REL_TYPE);
        whereClause.getLetKeys().add(relTypeSpecKey);
        whereClause.getLetKeys().add(targetSpecKey);
        whereClause.getLetKeys().add(viewCoordinateKey);
        return whereClause;
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return PRE_AND_POST_ITERATION;
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        this.vc = (ViewCoordinate) this.enclosingQuery.getLetDeclarations().get(viewCoordinateKey);
        NativeIdSetBI relTypeSet = new ConcurrentBitSet();
        relTypeSet.add(this.relType.getNid());
        if (this.relTypeSubsumption) {
            relTypeSet.or(Ts.get().isKindOfSet(this.relType.getNid(), vc));
        }
        NativeIdSetBI relationshipSet = Bdb.getMemoryCache().getDestRelNids(this.targetSpec.getNid(), relTypeSet, this.vc);
        getResultsCache().or(relationshipSet);
        int relTypetNid = this.relType.getNid();
        if (this.relTypeSubsumption) {
            NativeIdSetBI relTypeSubsumptionSet = Ts.get().isKindOfSet(relTypetNid, vc);
            getResultsCache().or(Bdb.getMemoryCache().getDestRelNids(this.targetSpec.getNid(), relTypeSubsumptionSet, vc));
        }

        return getResultsCache();
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        //Nothing to do here...
    }
}
