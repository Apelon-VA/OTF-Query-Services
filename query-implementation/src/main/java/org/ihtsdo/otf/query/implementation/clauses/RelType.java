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
import org.ihtsdo.otf.query.implementation.Where;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.datastore.Bdb;

/**
 * Allows the user to specify a
 * <code>Relationship</code> source and a
 * <code>Relationship</code> type. Queries that specify a
 * <code>Relationship</code> source restriction can be constructed using the
 * <code>RelRestriction</code> clause.
 *
 * @author dylangrald
 */
public class RelType extends LeafClause {

    ConceptSpec sourceSpec;
    String sourceSpecKey;
    String viewCoordinateKey;
    ViewCoordinate vc;
    Query enclosingQuery;
    ConceptSpec relType;
    String relTypeKey;
    NativeIdSetBI cache;
    Boolean relTypeSubsumption;

    public RelType(Query enclosingQuery, String relTypeKey, String sourceSpecKey, String viewCoordinateKey, Boolean relTypeSubsumption) {
        super(enclosingQuery);
        this.sourceSpecKey = sourceSpecKey;
        this.sourceSpec = (ConceptSpec) enclosingQuery.getLetDeclarations().get(sourceSpecKey);
        this.viewCoordinateKey = viewCoordinateKey;
        this.enclosingQuery = enclosingQuery;
        this.relTypeKey = relTypeKey;
        this.relType = (ConceptSpec) enclosingQuery.getLetDeclarations().get(relTypeKey);
        this.relTypeSubsumption = relTypeSubsumption;

    }

    @Override
    public WhereClause getWhereClause() {
        WhereClause whereClause = new WhereClause();
        whereClause.setSemantic(ClauseSemantic.REL_TYPE);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(sourceSpecKey);
        return whereClause;
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return PRE_AND_POST_ITERATION;
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        if (this.viewCoordinateKey.equals(enclosingQuery.currentViewCoordinateKey)) {
            this.vc = (ViewCoordinate) this.enclosingQuery.getVCLetDeclarations().get(viewCoordinateKey);
        } else {
            this.vc = (ViewCoordinate) this.enclosingQuery.getLetDeclarations().get(viewCoordinateKey);
        }
        NativeIdSetBI relTypeSet = new ConcurrentBitSet();
        relTypeSet.add(this.relType.getNid());
        if (this.relTypeSubsumption) {
            relTypeSet.or(Ts.get().isKindOfSet(this.relType.getNid(), vc));
        }
        NativeIdSetBI relationshipSet = Bdb.getNidCNidMap().getDestRelNids(this.sourceSpec.getNid(), relTypeSet, this.vc);
        getResultsCache().or(relationshipSet);
        int relTypetNid = this.relType.getNid();
        if (this.relTypeSubsumption) {
            NativeIdSetBI relTypeSubsumptionSet = Ts.get().isKindOfSet(relTypetNid, vc);
            getResultsCache().or(Bdb.getNidCNidMap().getDestRelNids(this.sourceSpec.getNid(), relTypeSubsumptionSet, vc));
        }

        return getResultsCache();
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        //Nothing to do here...
    }
}
