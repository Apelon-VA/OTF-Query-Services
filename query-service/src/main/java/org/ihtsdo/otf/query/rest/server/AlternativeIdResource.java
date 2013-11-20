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

//~--- non-JDK imports --------------------------------------------------------
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;
import org.ihtsdo.otf.tcc.model.index.service.IndexerBI;

//~--- JDK imports ------------------------------------------------------------
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.xml.bind.JAXBException;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.query.lucene.LuceneRefexIndexer;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.type_long.RefexLongVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.ddo.ResultList;
import org.ihtsdo.otf.tcc.ddo.concept.ConceptChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.concept.component.description.DescriptionChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.concept.component.description.SimpleDescriptionVersionDdo;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RefexPolicy;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RelationshipPolicy;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.VersionPolicy;
import org.ihtsdo.otf.tcc.model.index.service.SearchResult;

/**
 * This resource accepts SNOMED IDs, and returns the associated UUID for that
 * SNOMED ID, or it accepts UUIDs and returns the associated SNOMED ID for that
 * UUID.
 *
 * @author kec
 */
@Path("query-service")
public class AlternativeIdResource {

    private static IndexerBI sctIdIndexer;
    private static volatile int snomedAssemblageNid = Integer.MIN_VALUE;
    private static final TerminologyStoreDI ts = Ts.get();

    static {
        List<IndexerBI> lookers = Hk2Looker.get().getAllServices(IndexerBI.class);

        for (IndexerBI li : lookers) {
            System.out.println("AlternativeIdResource found indexer: " + li.getIndexerName());
            if (li.getIndexerName().equals("refex")) {
                sctIdIndexer = li;
            }
        }
    }

    @GET
    @Path("uuid/{id}")
    @Produces("text/plain")
    public String getUuidFromSctid(@PathParam("id") String id) throws IOException, JAXBException, Exception {
        System.out.println("Getting UUID for: " + id);
        System.out.println("SCTID indexer: " + sctIdIndexer);

        List<SearchResult> result = sctIdIndexer.query(id, ComponentProperty.LONG_EXTENSION_1, 1);
        System.out.println("result: " + result);
        for (SearchResult r : result) {
            System.out.println("nid: " + r.nid + " score:" + r.score);
        }
        System.out.println("result: " + result);

        if (!result.isEmpty()) {
            ComponentChronicleBI cc = Ts.get().getComponent(result.get(0).nid);
            RefexChronicleBI rx = (RefexChronicleBI) cc;
            UUID uuid = Ts.get().getUuidPrimordialForNid(rx.getReferencedComponentNid());
            return uuid.toString();
        }
        return "no entry found";
    }

    @GET
    @Path("uuid")
    @Produces("text/plain")
    public String getUuidInfo() throws IOException, JAXBException, Exception {
        return "Add the SNOMED ID to the end of the URL";
    }

    @GET
    @Path("sctid")
    @Produces("text/plain")
    public String getSctidInfo() throws IOException, JAXBException, Exception {
        return "Add the UUID to the end of the URL";
    }

    @GET
    @Path("sctid/{id}")
    @Produces("text/plain")
    public String getSctidFromUuid(@PathParam("id") String id) throws IOException, JAXBException, Exception {
        System.out.println("Getting sctid for: " + id);
        if (snomedAssemblageNid == Integer.MIN_VALUE) {
            try {
                snomedAssemblageNid = TermAux.SNOMED_IDENTIFIER.getNid();
            } catch (ValidationException ex) {
                Logger.getLogger(LuceneRefexIndexer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LuceneRefexIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        ComponentChronicleBI<?> component = Ts.get().getComponent(UUID.fromString(id));

        for (RefexChronicleBI<?> annotation : component.getAnnotations()) {
            if (annotation.getAssemblageNid() == snomedAssemblageNid) {
                RefexLongVersionBI sctid = (RefexLongVersionBI) annotation.getPrimordialVersion();
                return Long.toString(sctid.getLong1());
            }

        }

        return "no entry found";
    }

    @GET
    @Path("descriptions/{id}")
    @Produces("text/plain")
    public String getDescFromSctid(@PathParam("id") String id) throws IOException, JAXBException, Exception {
        System.out.println("Getting descriptions for: " + id);
        System.out.println("SCTID indexer: " + sctIdIndexer);

        if (!id.matches("[0-9]*") || id.length() > 19) {
            return "Incorrect SNOMED id.";
        }

        List<SearchResult> result = sctIdIndexer.query(id, ComponentProperty.LONG_EXTENSION_1, 1);
        System.out.println("result: " + result);
        for (SearchResult r : result) {
            System.out.println("nid: " + r.nid + " score:" + r.score);
        }
        System.out.println("result: " + result);

        if (!result.isEmpty()) {
            ViewCoordinate vc = StandardViewCoordinates.getSnomedInferredLatest();
            ComponentChronicleBI cc = Ts.get().getComponent(result.get(0).nid);
            UUID uuid = Ts.get().getUuidPrimordialForNid(cc.getNid());
            ConceptChronicleBI concept = Ts.get().getComponent(uuid).getEnclosingConcept();
            ConceptVersionBI cv = concept.getVersion(vc);

            ArrayList<Object> list = new ArrayList<>();

            for (DescriptionChronicleBI dc : concept.getVersion(vc).getDescriptions()) {
                if (dc.getVersion(vc) != null) {
                    DescriptionVersionBI dv = dc.getVersion(vc);
                    ConceptChronicleDdo ccDdo = new ConceptChronicleDdo(ts.getSnapshot(vc), concept, VersionPolicy.ACTIVE_VERSIONS, RefexPolicy.REFEX_MEMBERS, RelationshipPolicy.DESTINATION_RELATIONSHIPS);
                    DescriptionChronicleDdo dcDdo = new DescriptionChronicleDdo(ts.getSnapshot(vc), ccDdo, dc);
                    list.add(new SimpleDescriptionVersionDdo(dcDdo, ts.getSnapshot(vc), dv, cv));
                }
            }
            if (list.size() > 0) {

                ResultList resultList = new ResultList();
                resultList.setTheResults(list);
                StringWriter writer = new StringWriter();

                JaxbForQuery.get().createMarshaller().marshal(resultList, writer);
                return writer.toString();
            }
        }
        return "No descriptions found for " + id + ". Please ensure you have the correct SNOMED id.";

    }

}
