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
import org.apache.lucene.index.CorruptIndexException;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.ClauseComputeType;
import org.ihtsdo.otf.query.implementation.ClauseSemantic;
import org.ihtsdo.otf.query.implementation.LeafClause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.WhereClause;

/**
 * Calculates the descriptions that match the results from a specified Lucene
 * search.
 *
 *
 * @author kec
 */
public class DescriptionLuceneMatch extends LeafClause {

    String luceneMatch;
    String luceneMatchKey;
    ViewCoordinate vc;

    public DescriptionLuceneMatch(Query enclosingQuery, String luceneMatchKey) {
        super(enclosingQuery);
        this.luceneMatchKey = luceneMatchKey;
        this.luceneMatch = (String) enclosingQuery.getLetDeclarations().get(luceneMatchKey);
        vc = enclosingQuery.getViewCoordinate();
    }

    @Override
    public EnumSet<ClauseComputeType> getComputePhases() {
        return PRE_ITERATION;
    }

    @Override
    public final NativeIdSetBI computePossibleComponents(NativeIdSetBI incomingPossibleComponents) throws CorruptIndexException, IOException {
        
        throw new UnsupportedOperationException("Not supported yet");
//        
//       Collection<Integer> nids = new HashSet<>();
//       try {
//           nids = Ts.get().searchLucene(luceneMatch, SearchType.DESCRIPTION);
//       } catch (org.apache.lucene.queryparser.classic.ParseException ex) {
//           Logger.getLogger(DescriptionLuceneMatch.class.getName()).log(Level.SEVERE, null, ex);
//       }
//
//        NativeIdSetBI outgoingNids = new ConcurrentBitSet();
//        for (Integer nid : nids) {
//            outgoingNids.add(nid);
//
//        }
//
//        getResultsCache().or(outgoingNids);
//
//        return outgoingNids;

    }

    @Override
    public void getQueryMatches(ConceptVersionBI conceptVersion) {
        getResultsCache();
    }

    @Override
    public WhereClause getWhereClause() {
        WhereClause whereClause = new WhereClause();
        whereClause.setSemantic(ClauseSemantic.DESCRIPTION_LUCENE_MATCH);
        for (Clause clause : getChildren()) {
            whereClause.getChildren().add(clause.getWhereClause());
        }
        whereClause.getLetKeys().add(luceneMatchKey);
        return whereClause;
    }
}
