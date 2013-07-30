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
package org.ihtsdo.otf.query.client;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.glassfish.jersey.client.ClientConfig;
import org.ihtsdo.otf.tcc.api.query.ForCollection;
import org.ihtsdo.otf.tcc.api.query.JaxbForQuery;
import org.ihtsdo.otf.tcc.api.query.LetMap;
import org.ihtsdo.otf.tcc.api.query.Where;

/**
 *
 * @author kec
 */
public class QueryRestClient {

    public static final String defaultLocalHostServer = "http://localhost:8080/query-service/query/";
    private static String serverUrlStr = defaultLocalHostServer;
    private static Client restClient;

    public static void setup() throws IOException {
        setup(defaultLocalHostServer);
    }

    public static void setup(String serverUrlStr) throws IOException {
        QueryRestClient.serverUrlStr = serverUrlStr;

        ClientConfig cc = new ClientConfig();

        restClient = ClientBuilder.newClient(cc);
    }
   
   public static String process(String vcValue, String forValue, String letValue, String whereValue) {
        try {
            WebTarget r = restClient.target(serverUrlStr + "process").
                    queryParam("VIEWPOINT", vcValue).
                    queryParam("FOR", forValue).
                    queryParam("LET", letValue).
                    queryParam("WHERE", whereValue);
            final AsyncInvoker asyncInvoker = r.request(MediaType.TEXT_PLAIN).async();
 
            final Future<Response> responseFuture = 
                    asyncInvoker.get();
            final Response response = responseFuture.get();
            return response.readEntity(String.class);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(QueryRestClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "failed";
    }
    

    public static void main(String[] args) {
        try {
            setup();
            ExampleQuery q = new ExampleQuery(null);
            
            JAXBContext ctx = JaxbForQuery.get();
            
            String viewpointXml = 
"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
"<view-coordinate>\n" +
"    <allowedStatusAsString>ACTIVE</allowedStatusAsString>\n" +
"    <classifierSpec>\n" +
"        <description>IHTSDO Classifier</description>\n" +
"        <uuidStrs>7e87cc5b-e85f-3860-99eb-7a44f2b9e6f9</uuidStrs>\n" +
"    </classifierSpec>\n" +
"    <contradictionManagerPolicy>IDENTIFY_ALL_CONFLICTS</contradictionManagerPolicy>\n" +
"    <langPrefConceptSpecList>\n" +
"        <description>United States of America English language reference set (foundation metadata concept)</description>\n" +
"        <uuidStrs>bca0a686-3516-3daf-8fcf-fe396d13cfad</uuidStrs>\n" +
"    </langPrefConceptSpecList>\n" +
"    <languageSort>RF2_LANG_REFEX</languageSort>\n" +
"    <languageSpec>\n" +
"        <description>United States of America English language reference set (foundation metadata concept)</description>\n" +
"        <uuidStrs>bca0a686-3516-3daf-8fcf-fe396d13cfad</uuidStrs>\n" +
"    </languageSpec>\n" +
"    <name>SNOMED Infered-Latest</name>\n" +
"    <precedence>PATH</precedence>\n" +
"    <relationshipAssertionType>INFERRED</relationshipAssertionType>\n" +
"    <vcUuid>0c734870-836a-11e2-9e96-0800200c9a66</vcUuid>\n" +
"    <viewPosition>\n" +
"        <path>\n" +
"            <conceptSpec>\n" +
"                <description>SNOMED Core</description>\n" +
"                <uuidStrs>8c230474-9f11-30ce-9cad-185a96fd03a2</uuidStrs>\n" +
"            </conceptSpec>\n" +
"            <origins>\n" +
"                <path>\n" +
"                    <conceptSpec>\n" +
"                        <description>Workbench Auxiliary</description>\n" +
"                        <uuidStrs>2faa9260-8fb2-11db-b606-0800200c9a66</uuidStrs>\n" +
"                    </conceptSpec>\n" +
"                </path>\n" +
"                <time>9223372036854775807</time>\n" +
"            </origins>\n" +
"        </path>\n" +
"        <time>9223372036854775807</time>\n" +
"    </viewPosition>\n" +
"</view-coordinate>";
            
            
            String forXml = getXmlString(ctx, new ForCollection());
            
            q.Let();
            Map<String, Object> map = q.getLetDeclarations();
            LetMap wrappedMap = new LetMap(map);
            String letMapXml = getXmlString(ctx, wrappedMap);
            
 
            Where.WhereClause where = q.Where().getWhereClause();

            String whereXml = getXmlString(ctx, where);

            
            System.out.println("Process: " + process(viewpointXml, forXml, letMapXml, whereXml));
        } catch (IOException | JAXBException ex) {
            Logger.getLogger(QueryRestClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String getXmlString(JAXBContext ctx, Object obj) throws JAXBException {
        StringWriter writer;
        writer = new StringWriter();
        ctx.createMarshaller().marshal(obj, writer);
        String letMapXml = writer.toString();
        return letMapXml;
    }
}
