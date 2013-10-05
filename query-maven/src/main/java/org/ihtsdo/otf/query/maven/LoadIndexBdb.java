package org.ihtsdo.ttk.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.io.File;
import java.util.Collection;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.query.maven.IsKindOfMetrics;
import org.ihtsdo.otf.tcc.api.time.TimeHelper;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.io.FileIO;
import org.ihtsdo.otf.tcc.api.metadata.binding.Taxonomies;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;

/**
 * Goal which touches a timestamp file.
 *
 * @goal load-index-bdb
 *
 * @phase process-sources
 */
public class LoadIndexBdb extends AbstractMojo {

    /**
     * true if the mutable database should replace the read-only database after
     * load is complete.
     *
     * @parameter default-value=true
     * @required
     */
    private boolean moveToReadOnly = true;
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}/berkeley-db"
     * @required
     */
    private String bdbFolderLocation;
    /**
     * <code>eConcept format</code> files to import.
     *
     * @parameter
     * @required
     */
    private String[] econFileStrings;

    //~--- methods -------------------------------------------------------------
    @Override
    public void execute() throws MojoExecutionException {
        try {
            File bdbFolderFile = new File(bdbFolderLocation);
            boolean dbExists = bdbFolderFile.exists();
            System.setProperty("org.ihtsdo.otf.tcc.datastore.bdb-location", bdbFolderLocation);
            
            TerminologyStoreDI store = Hk2Looker.get().getService(TerminologyStoreDI.class);

            if (!dbExists) {
                store.loadEconFiles(econFileStrings);
            }

            store.index();

            //Metrics option:
            /*
            getLog().info("Starting metrics");

            long start = System.currentTimeMillis();
            IsKindOfMetrics metrics = new IsKindOfMetrics(Taxonomies.SNOMED.getLenient().getNid(),
                    StandardViewCoordinates.getSnomedInferredLatest());

            store.iterateConceptDataInParallel(metrics);

            long end = System.currentTimeMillis();

            getLog().info("\n\nParallel: " + metrics.getReport());
            getLog().info("Finished parallel metrics. Elapsed time: "
                    + TimeHelper.getElapsedTimeString(end - start)
                    + " (" + (end - start) + " ms)");
            start = System.currentTimeMillis();
            metrics = new IsKindOfMetrics(Taxonomies.SNOMED.getLenient().getNid(),
                    StandardViewCoordinates.getSnomedInferredLatest());
            store.iterateConceptDataInSequence(metrics);
            end = System.currentTimeMillis();
            getLog().info("\nSequential: " + metrics.getReport());
            getLog().info("Finished sequential metrics. Elapsed time: "
                    + TimeHelper.getElapsedTimeString(end - start)
                    + " (" + (end - start) + " ms)");
            start = System.currentTimeMillis();
            metrics = new IsKindOfMetrics(Taxonomies.SNOMED.getLenient().getNid(),
                    StandardViewCoordinates.getSnomedInferredLatest());

            store.iterateConceptDataInParallel(metrics);

            end = System.currentTimeMillis();

            getLog().info("\n\nParallel: " + metrics.getReport());
            getLog().info("Finished parallel metrics. Elapsed time: "
                    + TimeHelper.getElapsedTimeString(end - start)
                    + " (" + (end - start) + " ms)");
            start = System.currentTimeMillis();
            metrics = new IsKindOfMetrics(Taxonomies.SNOMED.getLenient().getNid(),
                    StandardViewCoordinates.getSnomedInferredLatest());
            store.iterateConceptDataInSequence(metrics);
            end = System.currentTimeMillis();
            getLog().info("\nSequential: " + metrics.getReport());
            getLog().info("Finished sequential metrics. Elapsed time: "
                    + TimeHelper.getElapsedTimeString(end - start)
                    + " (" + (end - start) + " ms)");
            getLog().info("\n\n");


            int nid = store.getNidFromAlternateId(TermAux.SNOMED_IDENTIFIER.getUuids()[0], "138875005");
            ConceptChronicleBI snomedConcept = store.getConceptForNid(nid);
            System.out.println("Found concept from alt id: " + snomedConcept);
            System.out.println(snomedConcept.toLongString()); */

//          TODO turn classifier back on. ??
//         System.out.println("\nStart classify");
//         Classifier.classify();
//         System.out.println("End classify");


            ConceptChronicleBI motion = store.getConcept(UUID.fromString("45a8fde8-535d-3d2a-b76b-95ab67718b41"));

            ConceptVersionBI centrifugalForceVersion = store.getConceptVersion(
                    StandardViewCoordinates.getSnomedInferredLatest(), UUID.fromString("2b684fe1-8baf-34ef-9d2a-df03142c915a"));

            ConceptVersionBI motionVersion = store.getConceptVersion(
                    StandardViewCoordinates.getSnomedInferredLatest(),
                    UUID.fromString("45a8fde8-535d-3d2a-b76b-95ab67718b41"));

            Collection<? extends ConceptVersionBI> descendents = motionVersion.getRelationshipsIncomingOrigins();

            boolean childOf = centrifugalForceVersion.isKindOf(motionVersion);
            childOf = centrifugalForceVersion.isKindOf(motionVersion);
            childOf = centrifugalForceVersion.isKindOf(motionVersion);

            NativeIdSetBI kindOfNids = store.isKindOfSet(motion.getNid(),
                    StandardViewCoordinates.getSnomedInferredLatest());
            if (kindOfNids.contains(centrifugalForceVersion.getNid())) {
                System.out.println("kindOfNids contains centrifugalForce");
            } else {
                System.out.println("kindOfNids DOES NOT contains centrifugalForce");

            }

            Ts.close();

            if (!dbExists && moveToReadOnly) {
                getLog().info("moving mutable to read-only");

                File readOnlyDir = new File(bdbFolderLocation, "read-only");

                FileIO.recursiveDelete(readOnlyDir);

                File mutableDir = new File(bdbFolderLocation, "mutable");

                mutableDir.renameTo(readOnlyDir);
            }
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
        }
    }
}
