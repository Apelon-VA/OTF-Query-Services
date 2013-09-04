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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.servlet.ServletException;
import org.ihtsdo.otf.tcc.datastore.BdbTerminologyStore;

/**
 * Overriding ServletContainer to enable access to
 * <code>init()</code> and
 * <code>destroy()</code> methods.
 *
 * @author kec
 */
public class QueryServletContainer extends ServletContainer {

    BdbTerminologyStore termStore;
    CountDownLatch started = new CountDownLatch(1);

    public QueryServletContainer() {
    }

    public QueryServletContainer(ResourceConfig resourceConfig) {
        super(resourceConfig);
    }

    @Override
    public void destroy() {
        System.out.println("Destroy QueryServletContainer");
        try {
            started.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(QueryServletContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (termStore != null) {
             termStore.shutdown();
        }
       
        super.destroy();
    }

    @Override
    public void init() throws ServletException {
        
        Thread bdbStartupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting BdbTerminologyStore in backbround thread. ");
                try {
                    termStore = new BdbTerminologyStore();
                } finally {
                    started.countDown();
                }

            }
        }, "Bdb startup thread");
        bdbStartupThread.start();
        super.init();
    }
}
