/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.query.lucene;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.StandardViewCoordinates;
import org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicChronicleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicVersionBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicStringBI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link LuceneDynamicRefexIndexerConfiguration}
 * Holds a cache of the configuration for the dynamic refex indexer (which is read from the DB, and may be changed at any point
 * the user wishes).  Keeps track of which assemblage types need to be indexing, and what attributes should be indexed on them.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
@SuppressWarnings("deprecation")
@Service
public class LuceneDynamicRefexIndexerConfiguration
{
	private static final Logger logger = Logger.getLogger(LuceneDynamicRefexIndexer.class.getName());
	
	//store assemblage nids that should be indexed - and then - for COLUMN_DATA keys, keep the 0 indexed column order numbers that need to be indexed.
	private HashMap<Integer, Integer[]> whatToIndex_ = new HashMap<>();
		
	private volatile boolean readNeeded = true;
	
	protected boolean needsIndexing(int assemblageNid)
	{
		initCheck();
		return whatToIndex_.containsKey(assemblageNid);
	}
	
	protected Integer[] whatColumnsToIndex(int assemblageNid)
	{
		initCheck();
		return whatToIndex_.get(assemblageNid);
	}
	
	private void initCheck()
	{
		if (readNeeded)
		{
			try
			{
				HashMap<Integer, Integer[]> updatedWhatToIndex = new HashMap<>();
				
				ConceptVersionBI c = Ts.get().getConceptVersion(StandardViewCoordinates.getWbAuxiliary(), RefexDynamic.REFEX_DYNAMIC_INDEX_CONFIGURATION.getUuids()[0]);
				
				for (RefexDynamicChronicleBI<?> r : c.getRefexDynamicMembers())
				{
					RefexDynamicVersionBI<?> rdv = r.getVersion(StandardViewCoordinates.getWbAuxiliary());
					int assemblageToIndex = rdv.getReferencedComponentNid();
					Integer[] finalCols = null;
					RefexDynamicDataBI[] data = rdv.getData();
					if (data != null && data.length > 0)
					{
						String colsToIndex = ((RefexDynamicStringBI) data[0]).getDataString();
						String[] split = colsToIndex.split(",");
						finalCols = new Integer[split.length];
						for (int i = 0; i < split.length; i++)
						{
							finalCols[i] = Integer.parseInt(split[i]);
						}
					}
					updatedWhatToIndex.put(assemblageToIndex, finalCols);
				}
				
				whatToIndex_ = updatedWhatToIndex;
			}
			catch (Exception e)
			{
				logger.log(Level.SEVERE, "Unexpected error reading Dynamic Refex Index Configuration - generated index will be incomplete!", e);
			}
		}
	}
	
	
	/**
	 * for the given assemblage nid, which columns should be indexed?  null or empty list for none.
	 * otherwise, 0 indexed column numbers.
	 * 
	 * Note - columnsToIndex must be provided for member-style assemblage NIDs - it doesn't make any 
	 * sense to index the assemblageID of a member style refex.
	 * 
	 * So, for member style - you can configure which columns to index.
	 * For annotation style - you can configure just indexing the assemblage itself, or you can also 
	 * index individual data columns.
	 */
	public void configureColumnsToIndex(int assemblageNid, Integer[] columnsToIndex)
	{
		readNeeded = true;
		//TODO implement pref store
	}

}
