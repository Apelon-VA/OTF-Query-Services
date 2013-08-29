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
import java.util.Collection;
import java.util.EnumSet;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.Where;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 * <code>LeafClause</code> that computes refsets that contain concepts that are
 * a kind of concept specified by the input
 * <code>ConceptSpec</code>.
 *
 * @author dylangrald
 */
public class RefsetContainsKindOfConcept extends LeafClause {

    Query enclosingQuery;
    String conceptSpecKey;
    ConceptSpec conceptSpec;
    String viewCoordinateKey;
    ViewCoordinate vc;

    public RefsetContainsKindOfConcept(Query enclosingQuery, String conceptSpecKey, String viewCoordinateKey) {
        super(enclosingQuery);
        this.enclosingQuery = enclosingQuery;
        this.conceptSpecKey = conceptSpecKey;
        this.conceptSpec = (ConceptSpec) this.enclosingQuery.getLetDeclarations().get(conceptSpecKey);
        this.viewCoordinateKey = viewCoordinateKey;

    }

    @Override
    public Where.WhereClause getWhereClause() {
        Where.WhereClause whereClause = new Where.WhereClause();
        whereClause.setSemantic(Where.ClauseSemantic.REFSET_CONTAINS_KIND_OF_CONCEPT);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(conceptSpecKey);
        return whereClause;

    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return PRE_ITERATION;
    }

    @Override
    public NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws IOException, ValidationException, ContradictionException {
        if (this.viewCoordinateKey.equals(enclosingQuery.currentViewCoordinateKey)) {
            this.vc = (ViewCoordinate) this.enclosingQuery.getVCLetDeclarations().get(viewCoordinateKey);
        } else {
            this.vc = (ViewCoordinate) this.enclosingQuery.getLetDeclarations().get(viewCoordinateKey);
        }
        int parentNid = this.conceptSpec.getNid();
        NativeIdSetBI kindOfSet = Ts.get().isKindOfSet(parentNid, vc);
        NativeIdSetItrBI iter = kindOfSet.getIterator();
        while (iter.next()) {
            ConceptVersionBI conceptVersion = Ts.get().getConceptVersion(vc, iter.nid());
            Collection<? extends RefexChronicleBI<?>> refexes = conceptVersion.getRefexes();
            for (RefexChronicleBI r : refexes) {
                this.getResultsCache().add(r.getConceptNid());
            }

        }
        return getResultsCache();
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {
        //Nothing to do here
    }
}
