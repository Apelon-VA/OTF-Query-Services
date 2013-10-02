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
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.implementation.RegexQueryFromJaxb;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.ddo.ResultList;

/**
 * Creates a simple REST call for a regex description match. Encode the regex at
 * the following URL: http://www.url-encode-decode.com and select UTF-8.
 *
 * @author dylangrald
 */
public class RegexResource {

    @Path("query-service/Regex")
    @GET
    @Produces("text/plain")
    public String doQuery(@QueryParam("TEXT") String regex) throws IOException, JAXBException, Exception {
        String queryString = "TEXT: " + regex;
        System.out.println("Received: \n   " + queryString);
        if (regex == null) {
            return "Malformed query. Lucene query must have input regular expression. \n"
                    + "Found: " + queryString
                    + "\n See: the section on Query Client in the query documentation: \n"
                    + "http://ihtsdo.github.io/OTF-Query-Services/query-documentation/docbook/query-documentation.html";
        }

        //Decode the queryText
        regex = URLDecoder.decode(regex, "UTF-8");

        RegexQueryFromJaxb query = new RegexQueryFromJaxb(regex);
        NativeIdSetBI resultSet = query.compute();
        
        //The default result type is DESCRIPTION_VERSION_FSN
        ArrayList<Object> objectList = query.returnResults();

        ResultList resultList = new ResultList();
        resultList.setTheResults(objectList);
        StringWriter writer = new StringWriter();

        JaxbForQuery.get().createMarshaller().marshal(resultList, writer);
        return writer.toString();

    }
}
