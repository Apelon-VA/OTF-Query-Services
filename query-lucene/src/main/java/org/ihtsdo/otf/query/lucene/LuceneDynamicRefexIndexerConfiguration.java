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
import org.jvnet.hk2.annotations.Service;

/**
 * {@link LuceneDynamicRefexIndexerConfiguration}
 * Holds a cache of the configuration for the dynamic refex indexer (which is read from the DB, and may be changed at any point
 * the user wishes).  Keeps track of which assemblage types need to be indexing, and what attributes should be indexed on them.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
@Service
public class LuceneDynamicRefexIndexerConfiguration
{
	public enum INDEXABLE {ASSEMBLAGE, COLUMN_DATA};
	
	//map assemblage nid to the components of the assemblage that should be indexed - and then - for COLUMN_DATA keys, keep the 0 indexed columns 
	//order numbers that need to be indexed.
	private HashMap<Integer, HashMap<INDEXABLE, Integer[]>> whatToIndex_ = new HashMap<>();
		
	private boolean readNeeded = true;
	
	
	protected boolean needsIndexing(int assemblageNid)
	{
		initCheck();
		return whatToIndex_.containsKey(assemblageNid);
	}
	
	protected HashMap<INDEXABLE, Integer[]> whatToIndex(int assemblageNid)
	{
		initCheck();
		return whatToIndex_.get(assemblageNid);
	}
	
	private void initCheck()
	{
		if (readNeeded)
		{
			//TODO implement read
			HashMap<Integer, HashMap<INDEXABLE, Integer[]>> updatedWhatToIndex = new HashMap<>();
			
			whatToIndex_ = updatedWhatToIndex;
		}
	}
	
	/**
	 * true for indexing on, false for indexing off.
	 */
	public void configureAnnotationStyleIndexing(int assemblageNid, boolean enable)
	{
		//TODO implement pref store
	}
	
	/**
	 * for the given assemblage nid, which columns should be indexed?  null or empty list for none.
	 * otherwise, 0 indexed column numbers.
	 */
	public void configureColumnsToIndex(int assemblageNid, Integer[] columnsToIndex)
	{
		//TODO implement pref store
	}

}
