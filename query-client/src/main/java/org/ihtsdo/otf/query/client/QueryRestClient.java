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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;

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

    public static String test() {
        WebTarget r = restClient.target(serverUrlStr + "test");
        return r.request(MediaType.TEXT_PLAIN).get(String.class);
        
    }
   
   public static String process(String forValue, String letValue, String whereValue) {
        try {
            WebTarget r = restClient.target(serverUrlStr + "process").
                    queryParam("FOR", forValue).
                    queryParam("LET", letValue).
                    queryParam("WHERE", whereValue);
            final AsyncInvoker asyncInvoker = r.request(MediaType.TEXT_PLAIN).async();
 
            final Future<Response> responseFuture = 
                    asyncInvoker.get();
            final Response response = responseFuture.get();
            return response.readEntity(String.class);
        } catch (InterruptedException ex) {
            Logger.getLogger(QueryRestClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(QueryRestClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "failed";
    }
    

    public static void main(String[] args) {
        try {
            setup();
            System.out.println("Test: " + test());
            System.out.println("Process: " + process("for", "let", "where"));
        } catch (IOException ex) {
            Logger.getLogger(QueryRestClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
