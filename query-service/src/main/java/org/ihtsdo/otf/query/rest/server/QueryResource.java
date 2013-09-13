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
package org.ihtsdo.otf.query.rest.server;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.implementation.QueryFromJaxb;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetItrBI;
import org.ihtsdo.otf.tcc.ddo.ResultList;

/**
 * Perform a query and return results. 
 * @author kec
 */
@Path("query-service/query")
public class QueryResource {

    @GET
    @Produces("text/plain")
    public String doQuery(@QueryParam("VIEWPOINT") String viewValue,
            @QueryParam("FOR") String forValue,
            @QueryParam("LET") String letValue,
            @QueryParam("WHERE") String whereValue,
            @QueryParam("RETURN") String returnValue) throws IOException, JAXBException, Exception {
        String queryString = "VIEWPOINT: " + viewValue + "\n   "
                + "FOR: " + forValue + "\n   "
                + "LET: " + letValue + "\n   "
                + "WHERE: " + whereValue + "\n   "
                + "RETURN: " + returnValue;
        System.out.println("Received: \n   " + queryString);
        if (viewValue == null || forValue == null || letValue == null
                || whereValue == null || returnValue == null) {
            return "Malformed query. Query must have VIEWPOINT, FOR, LET, WHERE, and RETURN values. \n"
                    + "Found: " + queryString +
                    "\n See: the section on Query Client in the query documentation: \n" +
                    "http://ihtsdo.github.io/OTF-Query-Services/query-documentation/docbook/query-documentation.html";
        }
        
        QueryFromJaxb query = new QueryFromJaxb(viewValue, forValue, letValue, whereValue);
        NativeIdSetBI resultSet = query.compute();

        if (!returnValue.equals("null")) {
            ReturnTypes returnType;
            if (returnValue.startsWith("<?xml")) {
                Unmarshaller unmarshaller = JaxbForQuery.get().createUnmarshaller();
                returnType = (ReturnTypes) unmarshaller.unmarshal(new StringReader(returnValue));
            } else {
                returnType = ReturnTypes.valueOf(returnValue);
            }
            ArrayList<Object> objectList = query.returnDisplayObjects(resultSet,
                    returnType);

            ResultList resultList = new ResultList();
            resultList.setTheResults(objectList);
            StringWriter writer = new StringWriter();

            JaxbForQuery.get().createMarshaller().marshal(resultList, writer);
            return writer.toString();
        }
        
        NativeIdSetItrBI iterator = resultSet.getIterator();
        List<Integer> results = new ArrayList<>(resultSet.size());
        while (iterator.next()) {
            results.add(iterator.nid());
        }
        
        
        return results.toString();


    }
}
