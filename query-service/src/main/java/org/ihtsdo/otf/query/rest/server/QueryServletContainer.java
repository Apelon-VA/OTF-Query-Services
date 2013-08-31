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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.servlet.ServletException;
import org.ihtsdo.otf.tcc.datastore.BdbTerminologyStore;

/**
 *
 * @author kec
 */
public class QueryServletContainer extends ServletContainer {
    BdbTerminologyStore termStore;
    public QueryServletContainer() {
    }

    public QueryServletContainer(ResourceConfig resourceConfig) {
        super(resourceConfig);
    }

    @Override
    public void destroy() {
        System.out.println("Destroy QueryServletContainer");
        termStore.shutdown();
        super.destroy(); 
    }

    @Override
    public void init() throws ServletException {
        System.out.println("Initialize QueryServletContainer");
        termStore = new BdbTerminologyStore();
        super.init(); 
    }    
}
