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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.jaxb.chronicle.api.SimpleViewCoordinate;
import org.ihtsdo.otf.jaxb.query.ForCollection;
import org.ihtsdo.otf.jaxb.query.LetMap;
import org.ihtsdo.otf.jaxb.query.ReturnTypes;
import org.ihtsdo.otf.jaxb.query.Where;

/**
 *
 * @author kec
 */
public class QueryProcessorForRestXml {

    //private static final String DEFAULT_HOST = "http://api.snomedtools.com";
    private static final String DEFAULT_HOST = "http://localhost:8080";
    
    // Get JAXBContext for converting objects to XML. 
    private static final  JAXBContext ctx = JaxbForClient.get();

    public static String process(SimpleViewCoordinate viewpoint, 
            ForCollection forObject,
            LetMap letMap,
            Where where,
            ReturnTypes returnType) throws JAXBException, IOException {
        return process(viewpoint, forObject, letMap, where, returnType, DEFAULT_HOST);
    }

    public static String process(SimpleViewCoordinate viewpoint, 
            ForCollection forObject,
            LetMap letMap,
            Where where,
            ReturnTypes returnType,
            String host) throws JAXBException, IOException {

        
        
        // create the client
        Client client = ClientBuilder.newClient();
        // specify the host and the path. 
        WebTarget target = client.target(host).path("query-service/query");
        
        
        return target.queryParam("VIEWPOINT", getXmlString(viewpoint)).
                      queryParam("FOR", getXmlString(forObject)).
                      queryParam("LET", getXmlString(letMap)).
                      queryParam("WHERE", getXmlString(where)).
                      queryParam("RETURN", getXmlString(returnType)).
                request(MediaType.TEXT_PLAIN).get(String.class);

    }

    private static String getXmlString(Object obj) throws JAXBException {
        if (obj instanceof SimpleViewCoordinate) {
             org.ihtsdo.otf.jaxb.chronicle.api.ObjectFactory factory = new org.ihtsdo.otf.jaxb.chronicle.api.ObjectFactory();
            obj =  factory.createSimpleViewCoordinate((SimpleViewCoordinate) obj);

        } else if (obj instanceof ForCollection) {
            org.ihtsdo.otf.jaxb.query.ObjectFactory factory = new org.ihtsdo.otf.jaxb.query.ObjectFactory();
            obj = factory.createForCollection((ForCollection) obj);
        } else if (obj instanceof LetMap) {
            org.ihtsdo.otf.jaxb.query.ObjectFactory factory = new org.ihtsdo.otf.jaxb.query.ObjectFactory();
            obj = factory.createLetMap((LetMap) obj);
        } else if (obj instanceof Where) {
            org.ihtsdo.otf.jaxb.query.ObjectFactory factory = new org.ihtsdo.otf.jaxb.query.ObjectFactory();
            obj = factory.createWhere((Where) obj);
        } else if (obj instanceof ReturnTypes) {
            org.ihtsdo.otf.jaxb.query.ObjectFactory factory = new org.ihtsdo.otf.jaxb.query.ObjectFactory();
            obj = factory.createReturnTypes((ReturnTypes) obj);
        }
        StringWriter writer = new StringWriter();
        ctx.createMarshaller().marshal(obj, writer);
        return writer.toString();
    }
    
    
}