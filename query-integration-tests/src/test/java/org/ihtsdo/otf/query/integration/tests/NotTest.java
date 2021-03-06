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
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 * Creates a test for the
 * <code>Not</code> clause.
 *
 * @author dylangrald
 *
 */
public class NotTest extends QueryClauseTest {

    public NotTest() throws IOException {

        this.q = new Query(StandardViewCoordinates.getSnomedInferredLatest()) {
            @Override
            protected NativeIdSetBI For() throws IOException {
                return Ts.get().isKindOfSet(Snomed.MOTION.getNid(), StandardViewCoordinates.getSnomedInferredLatest());

            }

            @Override
            public void Let() throws IOException {
                let("motion", Snomed.MOTION);
                let("regex", "[Vv]ibration.*");
                NativeIdSetBI kindOfSet = Ts.get().isKindOfSet(Snomed.MOTION.getNid(), StandardViewCoordinates.getSnomedInferredLatest());
                NativeIdSetItrBI iter = kindOfSet.getSetBitIterator();
                StringBuilder forSet = new StringBuilder("");
                while(iter.next()){
                    forSet.append(Ts.get().getComponent(iter.nid()).getPrimordialUuid().toString()).append(",");
                }
                let("Custom FOR set", forSet.toString());
            }

            @Override
            public Clause Where() {
                return Not(ConceptForComponent(DescriptionRegexMatch("regex")));
            }
        };

    }
}
