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
package org.ihtsdo.otf.query.implementation;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.store.Ts;

/**
 * Finds concepts with descriptions that match the input string using Lucene.
 *
 * @author dylangrald
 */
public class LuceneQueryFromJaxb extends Query {

    private String queryText;

    public LuceneQueryFromJaxb(String queryText) throws JAXBException, IOException {
        super();
        this.queryText = queryText;
    }

    @Override
    protected NativeIdSetBI For() throws IOException {
        return Ts.get().getAllConceptNids();
    }

    @Override
    public void Let() throws IOException {
        let(queryText, queryText);
    }

    @Override
    public Clause Where() {
        return ConceptForComponent(DescriptionLuceneMatch(queryText));
    }
}
