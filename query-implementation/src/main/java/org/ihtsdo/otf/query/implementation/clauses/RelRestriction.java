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
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.ClauseSemantic;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.ConcurrentBitSet;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.datastore.Bdb;

/**
 * Allows the user to define a restriction on the destination set of a
 * relationship query. Also allows the user to specify subsumption on the
 * destination restriction and relType.
 *
 * @author dylangrald
 */
public class RelRestriction extends LeafClause {

    ViewCoordinate vc;
    Query enclosingQuery;
    String relTypeKey;
    ConceptSpec relType;
    String sourceSpecKey;
    ConceptSpec sourceSpec;
    String relRestrictionSpecKey;
    ConceptSpec relRestrictionSpec;
    String viewCoordinateKey;
    Boolean destinationSubsumption;
    Boolean relTypeSubsumption;

    public RelRestriction(Query enclosingQuery, String relRestrictionSpecKey, String relTypeKey, String sourceSpecKey,
            String viewCoordinateKey, Boolean destinationSubsumption, boolean relTypeSubsumption) {
        super(enclosingQuery);
        this.enclosingQuery = enclosingQuery;
        this.sourceSpecKey = sourceSpecKey;
        this.sourceSpec = (ConceptSpec) enclosingQuery.getLetDeclarations().get(sourceSpecKey);
        this.relTypeKey = relTypeKey;
        this.relType = (ConceptSpec) enclosingQuery.getLetDeclarations().get(relTypeKey);
        this.relRestrictionSpecKey = relRestrictionSpecKey;
        this.relRestrictionSpec = (ConceptSpec) enclosingQuery.getLetDeclarations().get(relRestrictionSpecKey);
        this.viewCoordinateKey = viewCoordinateKey;
        this.relTypeSubsumption = relTypeSubsumption;
        this.destinationSubsumption = destinationSubsumption;

    }

    @Override
    public WhereClause getWhereClause() {
        WhereClause whereClause = new WhereClause();
        whereClause.setSemantic(ClauseSemantic.REL_RESTRICTION);
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
        NativeIdSetBI relationshipSet = Bdb.getMemoryCache().getDestRelNids(this.sourceSpec.getNid(), relTypeSet, this.vc);
        getResultsCache().or(relationshipSet);
        int parentNid = relRestrictionSpec.getNid();
        NativeIdSetBI restrictionSet = new ConcurrentBitSet();
        restrictionSet.add(parentNid);
        restrictionSet.or(Ts.get().isKindOfSet(parentNid, vc));
        if (!this.destinationSubsumption) {
            getResultsCache().and(restrictionSet);
            return getResultsCache();
        } else {
            //Default is to compute using subsumption
            NativeIdSetBI kindOfSet = Ts.get().isKindOfSet(parentNid, vc);
            NativeIdSetItrBI iter = kindOfSet.getSetBitIterator();
            while (iter.next()) {
                getResultsCache().or(Bdb.getMemoryCache().getDestRelNids(iter.nid(), relTypeSet, vc));
            }
        }
        getResultsCache().and(restrictionSet);

        return getResultsCache();

    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        //Nothing to do here...
    }
}
