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

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.model.cc.P;
import org.ihtsdo.otf.tcc.model.cc.termstore.PersistentStoreI;

/**
 *
 * @author kec
 */
public class TccSingleton {

    static {
        try {
            String directory = "berkeley-db";


            if (System.getProperty("BdbSingleton.BDB_LOCATION") != null) {
                directory = System.getProperty("BdbSingleton.BDB_LOCATION");
            }
            System.out.println("Initializing BdbSingleton from directory: " + directory);
            if (new File(directory).exists()) {
                Ts.setup(Ts.EMBEDDED_BERKELEY_DB_IMPL_CLASS, directory);
            } else {
                Ts.setup(Ts.EMBEDDED_BERKELEY_DB_IMPL_CLASS, directory);
                System.out.println("Loading new files tp: " + directory);

                File[] econFiles = new File[]{new File("/Users/kec/NetBeansProjects/econ/eConcept.econ"),
                    new File("/Users/kec/NetBeansProjects/econ/DescriptionLogicMetadata.econ")};

                Ts.get().loadEconFiles(econFiles);
                System.out.println("Finished load of: " + Arrays.asList(econFiles));
            }
            Ts.get().setGlobalSnapshot(Ts.get().getSnapshot(StandardViewCoordinates.getSnomedInferredLatest()));
            Ts.get().putViewCoordinate(StandardViewCoordinates.getSnomedInferredThenStatedLatest());
            Ts.get().putViewCoordinate(StandardViewCoordinates.getSnomedStatedLatest());
        } catch (Throwable ex) {
            Logger.getLogger(TccSingleton.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static PersistentStoreI get() {
        return P.s;
    }

    public static void close() {
        singleton.closeBdb();
    }
    private static final TccSingleton singleton = new TccSingleton();
    //~--- methods -------------------------------------------------------------

    @PreDestroy
    public void closeBdb() {
        try {
            Ts.close(Ts.EMBEDDED_BERKELEY_DB_IMPL_CLASS);
        } catch (Exception ex) {
            Logger.getLogger(TccSingleton.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
