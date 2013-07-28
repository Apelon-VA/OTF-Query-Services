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
package org.ihtsdo.otf.query.service;

import java.io.IOException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 *
 * @author kec
 */
@Path("/query")
public class QueryResource {
//    static {
//        TccSingleton.get();
//    }
    @GET
    @Path("test")
    @Produces("text/plain")
    public String getSequence() throws IOException {
        //return Long.toString(TccSingleton.get().getSequence());
        return "33";
    }

    @GET
    @Path("process")
    @Produces("text/plain")
    public String doQuery(@QueryParam("FOR") String forValue,
                          @QueryParam("LET") String letValue, 
                          @QueryParam("WHERE") String whereValue) throws IOException  {
        String queryString = forValue + "|" + letValue+ "|" + whereValue;
        System.out.println("Recieved: " + queryString);
        return queryString;
    }
    
}
