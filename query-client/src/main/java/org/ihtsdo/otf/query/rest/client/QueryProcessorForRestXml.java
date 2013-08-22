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
package org.ihtsdo.otf.query.rest.client;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.query.implementation.ForCollection;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.implementation.LetMap;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.query.implementation.Where;

/**
 *
 * @author kec
 */
public class QueryProcessorForRestXml {

    private static final String DEFAULT_HOST = "http://localhost:8080";

    public static String process(Query query) throws JAXBException, IOException {
        return process(query, DEFAULT_HOST);
    }

    public static String process(Query query, String host) throws JAXBException, IOException {
        JAXBContext ctx = JaxbForQuery.get();
        
        String viewpointXml = getXmlString(ctx, query.getViewCoordinate());

        String forXml = getXmlString(ctx, new ForCollection());

        query.Let();
        Map<String, Object> map = query.getLetDeclarations();
        LetMap wrappedMap = new LetMap(map);
        String letMapXml = getXmlString(ctx, wrappedMap);


        Where.WhereClause where = query.Where().getWhereClause();

        String whereXml = getXmlString(ctx, where);

        // create the client
        Client client = ClientBuilder.newClient();
        // specify the host and the path. 
        WebTarget target = client.target(host).path("query-service/query");
        return target.queryParam("VIEWPOINT", viewpointXml).
                      queryParam("FOR", forXml).
                      queryParam("LET", letMapXml).
                      queryParam("WHERE", whereXml).
                request(MediaType.TEXT_PLAIN).get(String.class);

    }

    private static String getXmlString(JAXBContext ctx, Object obj) throws JAXBException {
        StringWriter writer = new StringWriter();
        ctx.createMarshaller().marshal(obj, writer);
        return writer.toString();
    }
}
