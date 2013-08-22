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
package org.ihtsdo.otf.query.rest.client.examples;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.rest.client.QueryProcessorForRestXml;
import org.ihtsdo.otf.query.rest.client.ViewCoordinateExample;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 *
 * @author kec
 */
public class KindOfQueryExample extends Query {

    public KindOfQueryExample(ViewCoordinate viewCoordinate) {
        super(viewCoordinate);
    }

    @Override
    protected NativeIdSetBI For() throws IOException {
        return Ts.get().getAllConceptNids();
    }

    @Override
    public void Let() throws IOException {
        let("allergic-asthma", Snomed.ALLERGIC_ASTHMA);
    }

    @Override
    public Clause Where() {
        return And(ConceptIsKindOf("allergic-asthma"));
    }

    /**
     *
     * @param args args[0] is an optional server url.
     */
    public static void main(String[] args) {
        try {
            // Construct an example query. 


            KindOfQueryExample query = new KindOfQueryExample(ViewCoordinateExample.getSnomedInferredLatest());

            // if host is provided, override default host.
            String results;
            if (args.length > 0) {
                results = QueryProcessorForRestXml.process(query, args[0]);
            } else {
                results = QueryProcessorForRestXml.process(query);
            }
            Logger.getLogger(KindOfQueryExample.class.getName()).log(Level.INFO, "Results: {0}", results);



        } catch (IOException | JAXBException ex) {
            Logger.getLogger(KindOfQueryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
