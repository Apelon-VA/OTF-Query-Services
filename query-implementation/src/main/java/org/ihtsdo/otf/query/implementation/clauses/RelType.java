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
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.Where;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;

/**
 * TODO: Not implemented yet.
 * @author dylangrald
 */
public class RelType extends LeafClause {

    ConceptSpec conceptSpec;
    String conceptSpecKey;
    String viewCoordinateKey;
    ViewCoordinate vc;
    Query enclosingQuery;
    ConceptSpec relType;
    String relTypeKey;
    NativeIdSetBI cache;
    

    public RelType(Query enclosingQuery, String relTypeKey, String conceptSpecKey, String viewCoordinateKey) {
        super(enclosingQuery);
        this.conceptSpecKey = conceptSpecKey;
        this.conceptSpec = (ConceptSpec) enclosingQuery.getLetDeclarations().get(conceptSpecKey);
        this.viewCoordinateKey = viewCoordinateKey;
        this.enclosingQuery = enclosingQuery;
        this.relTypeKey = relTypeKey;
        this.relType = (ConceptSpec) enclosingQuery.getLetDeclarations().get(relTypeKey);

    }

    @Override
    public Where.WhereClause getWhereClause() {
        Where.WhereClause whereClause = new Where.WhereClause();
        whereClause.setSemantic(Where.ClauseSemantic.REL_TYPE);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(conceptSpecKey);
        return whereClause;
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return ITERATION;
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        if (this.viewCoordinateKey.equals(enclosingQuery.currentViewCoordinateKey)) {
            this.vc = (ViewCoordinate) this.enclosingQuery.getVCLetDeclarations().get(viewCoordinateKey);
        } else {
            this.vc = (ViewCoordinate) this.enclosingQuery.getLetDeclarations().get(viewCoordinateKey);
        }
        NativeIdSetBI resultSet = new ConcurrentBitSet();
        resultSet = incomingPossibleComponents;
        NativeIdSetBI relTypeSet = new ConcurrentBitSet();
        relTypeSet.add(this.relType.getNid());
        //NativeIdSetBI relationshipSet = Bdb.getNidCNidMap().getDestRelNids(this.conceptSpec.getNid(), relTypeSet);
        //resultSet.and(relationshipSet);
        return resultSet;
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        //TO DO

    }
}
