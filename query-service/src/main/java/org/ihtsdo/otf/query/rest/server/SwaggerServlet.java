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

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

/**
 *
 * @author aimeefurber
 */
public class SwaggerServlet extends HttpServlet {
    @Override
    public void init(ServletConfig config){
        String url = System.getProperty("host.url");
        url = "http://this-should-never-be-used";
        if(url.contains("/otf")){
            url = url.substring(0, url.indexOf("/otf") + 4) + "/query-service";
        }
        ConfigFactory.config().setBasePath(url);
        ConfigFactory.config().setApiVersion(config.getInitParameter("api.version"));
        ScannerFactory.setScanner(new DefaultJaxrsScanner());
        ClassReaders.setReader(new DefaultJaxrsApiReader());
    }
}
