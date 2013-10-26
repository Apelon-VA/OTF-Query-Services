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

import org.ihtsdo.otf.query.implementation.QueryFromJaxb;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.implementation.ReturnTypes;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.ddo.ResultList;

/**
 * Perform a query and return results.
 *
 * @author kec
 */
@Path("query-service/query")
public class QueryResource {

    @GET
    @Produces("text/plain")
    public String doQuery(HttpServletRequest request, @QueryParam("VIEWPOINT") String viewValue,
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
        
        //TODO: check to make sure db is open.
//        if(ChronicleServletContainer.status.equals("The project is building.")){
//            throw new QueryApplicationException(HttpErrorType.ERROR422, "The project is building");
//        }
        
        System.out.println(request.getRequestURL());
        
        QueryFromJaxb query;
        try {
            query = new QueryFromJaxb(viewValue, forValue, letValue, whereValue);
        } catch (NullPointerException e) {
            throw new QueryApplicationException(HttpErrorType.ERROR503, "Please contact system administrator.");
        }
        if (query.getViewCoordinate() == null) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed VIEWPOINT value.");
        } else if (query.getForCollection() == null) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed FOR value.");
        } else if (query.getLetDeclarations() == null) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed LET value.");
        } else if (query.getRootClause() == null) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed WHERE value.");
        } else if (query.nullSpec == true) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Null ConceptSpec.");
        }
        NativeIdSetBI resultSet = null;
        try {
            resultSet = query.compute();
        } catch (ValidationException e) {
            throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed input concept in LET value. See below for ValidationException details.", e);
        }

        if (!returnValue.equals("null") && !returnValue.equals("")) {
            ReturnTypes returnType;
            if (returnValue.startsWith("<?xml")) {
                try {
                    Unmarshaller unmarshaller = JaxbForQuery.get().createUnmarshaller();
                    returnType = (ReturnTypes) unmarshaller.unmarshal(new StringReader(returnValue));
                } catch (JAXBException e) {
                    throw new QueryApplicationException(HttpErrorType.ERROR422, "Malformed RETURN value.");
                }
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
        } else {
            //The default result type is DESCRIPTION_VERSION_FSN
            ArrayList<Object> objectList = query.returnResults();

            ResultList resultList = new ResultList();
            resultList.setTheResults(objectList);
            StringWriter writer = new StringWriter();

            JaxbForQuery.get().createMarshaller().marshal(resultList, writer);
            return writer.toString();
        }
    }
}
