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
package org.ihtsdo.otf.query.rest.client;

import java.io.StringReader;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.ihtsdo.otf.query.implementation.JaxbForQuery;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;

/**
 *
 * @author kec
 */
public class ViewCoordinateExample {

    public static final String snomedInferredLatestXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<ns2:view-coordinate xmlns:ns2=\"http://api.chronicle.otf.ihtsdo.org\">\n"
            + "    <allowedStatusAsString>ACTIVE</allowedStatusAsString>\n"
            + "    <classifierSpec>\n"
            + "        <description>IHTSDO Classifier</description>\n"
            + "        <uuidStrs>7e87cc5b-e85f-3860-99eb-7a44f2b9e6f9</uuidStrs>\n"
            + "    </classifierSpec>\n"
            + "    <contradictionManagerPolicy>IDENTIFY_ALL_CONFLICTS</contradictionManagerPolicy>\n"
            + "    <langPrefConceptSpecList>\n"
            + "        <description>United States of America English language reference set (foundation metadata concept)</description>\n"
            + "        <uuidStrs>bca0a686-3516-3daf-8fcf-fe396d13cfad</uuidStrs>\n"
            + "    </langPrefConceptSpecList>\n"
            + "    <languageSort>RF2_LANG_REFEX</languageSort>\n"
            + "    <languageSpec>\n"
            + "        <description>United States of America English language reference set (foundation metadata concept)</description>\n"
            + "        <uuidStrs>bca0a686-3516-3daf-8fcf-fe396d13cfad</uuidStrs>\n"
            + "    </languageSpec>\n"
            + "    <name>SNOMED Infered-Latest</name>\n"
            + "    <precedence>PATH</precedence>\n"
            + "    <relationshipAssertionType>INFERRED</relationshipAssertionType>\n"
            + "    <vcUuid>0c734870-836a-11e2-9e96-0800200c9a66</vcUuid>\n"
            + "    <viewPosition>\n"
            + "        <path>\n"
            + "            <conceptSpec>\n"
            + "                <description>SNOMED Core</description>\n"
            + "                <uuidStrs>8c230474-9f11-30ce-9cad-185a96fd03a2</uuidStrs>\n"
            + "            </conceptSpec>\n"
            + "            <origins>\n"
            + "                <path>\n"
            + "                    <conceptSpec>\n"
            + "                        <description>Workbench Auxiliary</description>\n"
            + "                        <uuidStrs>2faa9260-8fb2-11db-b606-0800200c9a66</uuidStrs>\n"
            + "                    </conceptSpec>\n"
            + "                </path>\n"
            + "                <time>9223372036854775807</time>\n"
            + "            </origins>\n"
            + "        </path>\n"
            + "        <time>9223372036854775807</time>\n"
            + "    </viewPosition>\n"
            + "</ns2:view-coordinate>";

    public static ViewCoordinate getSnomedInferredLatest() throws JAXBException {
        Unmarshaller unmarshaller = JaxbForQuery.get().createUnmarshaller();
        return (ViewCoordinate) unmarshaller.unmarshal(new StringReader(snomedInferredLatestXml));

    }
}