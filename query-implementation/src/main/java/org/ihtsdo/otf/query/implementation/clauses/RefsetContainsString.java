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

/**
 *
 * @author dylangrald
 */
import java.io.IOException;
import java.util.EnumSet;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.ClauseSemantic;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.WhereClause;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import static org.ihtsdo.otf.tcc.api.refex.RefexType.CID_CID_CID_STRING;
import static org.ihtsdo.otf.tcc.api.refex.RefexType.CID_CID_STR;
import static org.ihtsdo.otf.tcc.api.refex.RefexType.CID_STR;
import static org.ihtsdo.otf.tcc.api.refex.RefexType.STR;
import org.ihtsdo.otf.tcc.api.refex.RefexVersionBI;
import org.ihtsdo.otf.tcc.api.refex.type_string.RefexStringVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 * TODO: not implemented yet.
 *
 * @author dylangrald
 */
public class RefsetContainsString extends LeafClause {

    Query enclosingQuery;
    String queryText;
    String viewCoordinateKey;
    ViewCoordinate vc;
    NativeIdSetBI cache;
    ConceptSpec refsetSpec;
    String refsetSpecKey;

    public RefsetContainsString(Query enclosingQuery, String refsetSpecKey, String queryText, String viewCoordinateKey) {
        super(enclosingQuery);
        this.enclosingQuery = enclosingQuery;
        this.refsetSpecKey = refsetSpecKey;
        this.refsetSpec = (ConceptSpec) this.enclosingQuery.getLetDeclarations().get(refsetSpecKey);
        this.queryText = queryText;
        this.viewCoordinateKey = viewCoordinateKey;

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
        int refsetNid = this.refsetSpec.getNid();
        //ConceptVersionBI conceptVersion = Ts.get().getConceptVersion(vc, refsetNid);
        ComponentChronicleBI cc = Ts.get().getComponent(refsetNid);
        for(RefexChronicleBI rc : cc.getRefexes()){
            System.out.println(rc.toString());
        }
        
        /*for(NidPairForRefex i:P.s.getRefexPairs(refsetNid)){
         ConceptChronicleBI memberConcept = Ts.get().getConcept(i.getMemberNid());
         if(memberConcept.g)
         getResultsCache().add(memberConcept.getConceptNid());
            
//         }*/
//        for (RefexChronicleBI rc : conceptVersion.getChronicle().getRefexes()) {
//
//            for (RefexVersionBI rv : rc.getRefexMembersActive(vc)) {
//                switch (rc.getRefexType()) {
//                    case CID_STR:
//                    case CID_CID_CID_STRING:
//                    case CID_CID_STR:
//                    case STR:
//                        RefexStringVersionBI rsv = (RefexStringVersionBI) rv;
//                        if (rsv.getString1().matches(queryText)) {
//                            getResultsCache().add(rv.getNid());
//                        }
//                    default:
//                    // do nothing... 
//
//                }
//            }
//        }
        /*for(RefexVersionBI r: conceptVersion.getRefsetMembersActive()){
         if(r.toUserString().matches(queryText)){
         getResultsCache().add(r.getConceptNid());
         }
         }*/
        /*for(RefexChronicleBI r :conceptVersion.getEnclosingConcept().getRefsetMembers()){
         if(r.toUserString().matches(queryText)){
         getResultsCache().add(r.getConceptNid());
         }
         }*/

        return getResultsCache();
    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) throws IOException, ContradictionException {

    }

    @Override
    public WhereClause getWhereClause() {
        WhereClause whereClause = new WhereClause();
        whereClause.setSemantic(ClauseSemantic.REFSET_CONTAINS_KIND_OF_CONCEPT);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(refsetSpecKey);
        whereClause.getLetKeys().add(queryText);
        return whereClause;
    }
}
