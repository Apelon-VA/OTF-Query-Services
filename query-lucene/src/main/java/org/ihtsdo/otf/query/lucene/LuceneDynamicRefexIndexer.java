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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ihtsdo.otf.query.lucene;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.ihtsdo.otf.query.lucene.LuceneDynamicRefexIndexerConfiguration.INDEXABLE;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicChronicleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicVersionBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicBooleanBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicByteArrayBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicDoubleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicFloatBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicIntegerBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicLongBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicNidBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicPolymorphicBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicStringBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicString;
import org.ihtsdo.otf.tcc.model.index.service.SearchResult;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link LuceneDynamicRefexIndexer} An indexer that can be used both to index the reverse mapping of annotation style refexes, and to index
 * the attached data of dynamic refexes.
 *
 * This class provides specialized query methods for handling very specific queries against RefexDynamic data.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@SuppressWarnings("rawtypes")
public class LuceneDynamicRefexIndexer extends LuceneIndexer
{
	public static final String INDEX_NAME = "dynamicRefex";
	private final Logger logger = Logger.getLogger(LuceneDynamicRefexIndexer.class.getName());
	private final String COLUMN_FIELD_ID = "colID";
	private final String COLUMN_FIELD_DATA = "colData";

	@Inject 
	private LuceneDynamicRefexIndexerConfiguration lric;

	public LuceneDynamicRefexIndexer() throws IOException
	{
		super(INDEX_NAME);
	}

	@Override
	protected boolean indexChronicle(ComponentChronicleBI chronicle)
	{
		if (chronicle instanceof RefexDynamicChronicleBI)
		{
			RefexDynamicChronicleBI<?> rdc = (RefexDynamicChronicleBI<?>) chronicle;
			return lric.needsIndexing(rdc.getAssemblageNid());
		}

		return false;
	}

	@Override
	protected void addFields(ComponentChronicleBI chronicle, Document doc)
	{
		RefexDynamicChronicleBI<?> rdc = (RefexDynamicChronicleBI<?>) chronicle;
		for (@SuppressWarnings("unchecked") Iterator<RefexDynamicVersionBI<?>> it = (Iterator<RefexDynamicVersionBI<?>>) rdc.getVersions().iterator(); it.hasNext();)
		{
			RefexDynamicVersionBI<?> rdv = it.next();

			for (Entry<INDEXABLE, Integer[]> indexSpec : lric.whatToIndex(rdv.getAssemblageNid()).entrySet())
			{
				if (indexSpec.getKey() == INDEXABLE.ASSEMBLAGE)
				{
					//Yes, this is a long, but we never do anything other than exact matches, so it performs better to index it as a string
					//rather that indexing it as a long... as we never need to match things like nid > X
					doc.add(new StringField(INDEXABLE.ASSEMBLAGE.name(), rdv.getAssemblageNid() + "", Store.NO));
				}
				else if (indexSpec.getKey() == INDEXABLE.COLUMN_DATA && indexSpec.getValue() != null)
				{
					for (int col : indexSpec.getValue())
					{
						RefexDynamicDataBI dataCol = rdv.getData(col);

						//add an entry for the column ID, to support very specific searches
						doc.add(new StringField(COLUMN_FIELD_ID, col + "", Store.NO));

						if (dataCol instanceof RefexDynamicBooleanBI)
						{
							doc.add(new StringField(COLUMN_FIELD_DATA, ((RefexDynamicBooleanBI) dataCol).getDataBoolean() + "", Store.NO));
						}
						else if (dataCol instanceof RefexDynamicByteArrayBI)
						{
							logger.warning("Dynamic Refex Indexer configured to index a field that isn't indexable");
						}
						else if (dataCol instanceof RefexDynamicDoubleBI)
						{
							doc.add(new DoubleField(COLUMN_FIELD_DATA, ((RefexDynamicDoubleBI) dataCol).getDataDouble(), Store.NO));
						}
						else if (dataCol instanceof RefexDynamicFloatBI)
						{
							doc.add(new FloatField(COLUMN_FIELD_DATA, ((RefexDynamicFloatBI) dataCol).getDataFloat(), Store.NO));
						}
						else if (dataCol instanceof RefexDynamicIntegerBI)
						{
							doc.add(new IntField(COLUMN_FIELD_DATA, ((RefexDynamicIntegerBI) dataCol).getDataInteger(), Store.NO));
						}
						else if (dataCol instanceof RefexDynamicLongBI)
						{
							doc.add(new LongField(COLUMN_FIELD_DATA, ((RefexDynamicLongBI) dataCol).getDataLong(), Store.NO));
						}
						else if (dataCol instanceof RefexDynamicNidBI)
						{
							//No need for ranges on a nid
							doc.add(new StringField(COLUMN_FIELD_DATA, ((RefexDynamicNidBI) dataCol).getDataNid() + "", Store.NO));
						}
						else if (dataCol instanceof RefexDynamicPolymorphicBI)
						{
							logger.log(Level.SEVERE, "This should have been impossible (polymorphic?)");
						}
						else if (dataCol instanceof RefexDynamicStringBI)
						{
							doc.add(new TextField(COLUMN_FIELD_DATA, ((RefexDynamicStringBI) dataCol).getDataString(), Store.NO));
						}
						else if (dataCol instanceof RefexDynamicUUIDBI)
						{
							doc.add(new StringField(COLUMN_FIELD_DATA, ((RefexDynamicUUIDBI) dataCol).getDataUUID().toString(), Store.NO));
						}
						else
						{
							logger.log(Level.SEVERE, "This should have been impossible (no match on col type)");
						}
					}
				}
			}
		}
	}

	/**
	 * @param queryDataLower
	 * @param queryDataLowerInclusive
	 * @param queryDataUpper
	 * @param queryDataUpperInclusive
	 * @param assemblageNid (optional) limit the search to the specified assemblage
	 * @param searchColumns (optional) limit the search to the specified columns of attached data
	 * @param sizeLimit
	 * @param targetGeneration (optional) wait for an index to build, or null to not wait
	 */
	public final List<SearchResult> queryNumericRange(RefexDynamicDataBI queryDataLower, boolean queryDataLowerInclusive, RefexDynamicDataBI queryDataUpper,
			boolean queryDataUpperInclusive, Integer assemblageNid, Integer[] searchColumns, int sizeLimit, Long targetGeneration) throws IOException, ParseException
	{
		BooleanQuery bq = new BooleanQuery();

		if (assemblageNid != null)
		{
			bq.add(new TermQuery(new Term(INDEXABLE.ASSEMBLAGE.name(), assemblageNid + "")), Occur.MUST);
		}

		addColumnConstraint(bq, searchColumns);

		if (queryDataLower.getRefexDataType() != queryDataUpper.getRefexDataType())
		{
			throw new ParseException("Lower and Upper values must be ov the same type");
		}

		bq.add(buildNumericQuery(queryDataLower, queryDataLowerInclusive, queryDataUpper, queryDataUpperInclusive), Occur.MUST);
		return search(bq, sizeLimit, targetGeneration);
	}

	/**
	 * A convenience method.
	 * 
	 * Search RefexDynamicData columns, treating them as text - and handling the search in the same mechanism as if this were a
	 * call to the method {@link LuceneIndexer#query(String, boolean, ComponentProperty, int, long)} using a ComponentProperty type
	 * of {@link ComponentProperty#DESCRIPTION_TEXT}
	 * 
	 * Calls the method {@link #query(RefexDynamicDataBI, Integer, boolean, Integer[], int, long) with a null parameter for
	 * the searchColumns, and wraps the queryString into a RefexDynamicString.
	 */
	public final List<SearchResult> query(String queryString, Integer assemblageNid, boolean prefixSearch, int sizeLimit, long targetGeneration) throws IOException,
			ParseException
	{
		try
		{
			return query(new RefexDynamicString(queryString), assemblageNid, prefixSearch, null, sizeLimit, targetGeneration);
		}
		catch (PropertyVetoException e)
		{
			throw new ParseException(e.getMessage());
		}
	}

	/**
	 * 
	 * @param queryData - The query data object (string, int, etc)
	 * @param assemblageNid (optional) limit the search to the specified assemblage
	 * @param prefixSearch see {@link LuceneIndexer#query(String, boolean, ComponentProperty, int, Long)} for a description.
	 * @param searchColumns (optional) limit the search to the specified columns of attached data
	 * @param sizeLimit
	 * @param targetGeneration (optional) wait for an index to build, or null to not wait
	 */
	public final List<SearchResult> query(RefexDynamicDataBI queryData, Integer assemblageNid, boolean prefixSearch, Integer[] searchColumns, int sizeLimit,
			Long targetGeneration) throws IOException, ParseException
	{
		BooleanQuery bq = new BooleanQuery();
		if (assemblageNid != null)
		{
			bq.add(new TermQuery(new Term(INDEXABLE.ASSEMBLAGE.name(), assemblageNid + "")), Occur.MUST);
		}

		addColumnConstraint(bq, searchColumns);

		if (queryData instanceof RefexDynamicStringBI)
		{
			//This is the only query type that needs tokenizing, etc.
			return runTokenizedStringSearch(bq, ((RefexDynamicStringBI) queryData).getDataString(), COLUMN_FIELD_DATA, prefixSearch, sizeLimit, targetGeneration);
		}
		else
		{
			if (queryData instanceof RefexDynamicBooleanBI || queryData instanceof RefexDynamicNidBI || queryData instanceof RefexDynamicUUIDBI)
			{
				bq.add(new TermQuery(new Term(COLUMN_FIELD_DATA, queryData.getDataObject().toString())), Occur.MUST);
			}
			else if (queryData instanceof RefexDynamicDoubleBI || queryData instanceof RefexDynamicFloatBI || queryData instanceof RefexDynamicIntegerBI
					|| queryData instanceof RefexDynamicLongBI)
			{
				bq.add(buildNumericQuery(queryData, true, queryData, true), Occur.MUST);
			}
			else if (queryData instanceof RefexDynamicByteArrayBI)
			{
				throw new ParseException("RefexDynamicByteArray isn't indexed");
			}
			else if (queryData instanceof RefexDynamicPolymorphicBI)
			{
				throw new ParseException("This should have been impossible (polymorphic?)");
			}
			else
			{
				logger.log(Level.SEVERE, "This should have been impossible (no match on col type)");
				throw new ParseException("unexpected error, see logs");
			}
			return search(bq, sizeLimit, targetGeneration);
		}
	}

	/**
	 * A convenience method that calls:
	 * 
	 * {@link LuceneIndexer#query(String, boolean, ComponentProperty, int, Long) with:
	 * "boolean prefexSearch" set to false "ComponentProperty field" set to {@link ComponentProperty#ASSEMBLAGE_ID}
	 */
	public final List<SearchResult> queryAssemblageUsage(int assemblageNid, int sizeLimit, Long targetGeneration) throws IOException, ParseException
	{
		return query(assemblageNid + "", false, ComponentProperty.ASSEMBLAGE_ID, sizeLimit, targetGeneration);
	}

	private void addColumnConstraint(BooleanQuery bq, Integer[] columns)
	{
		if (columns != null && columns.length > 0)
		{
			BooleanQuery nested = new BooleanQuery();
			for (int i : columns)
			{
				nested.add(new TermQuery(new Term(COLUMN_FIELD_ID, i + "")), Occur.SHOULD);
			}
			bq.add(nested, Occur.MUST);
		}
	}

	private Query buildNumericQuery(RefexDynamicDataBI queryDataLower, boolean queryDataLowerInclusive, RefexDynamicDataBI queryDataUpper, boolean queryDataUpperInclusive)
			throws ParseException
	{
		if (queryDataLower instanceof RefexDynamicDoubleBI)
		{
			return NumericRangeQuery.newDoubleRange(COLUMN_FIELD_DATA, ((RefexDynamicDoubleBI) queryDataLower).getDataDouble(),
					((RefexDynamicDoubleBI) queryDataUpper).getDataDouble(), queryDataLowerInclusive, queryDataUpperInclusive);
		}
		else if (queryDataLower instanceof RefexDynamicFloatBI)
		{
			return NumericRangeQuery.newFloatRange(COLUMN_FIELD_DATA, ((RefexDynamicFloatBI) queryDataLower).getDataFloat(),
					((RefexDynamicFloatBI) queryDataUpper).getDataFloat(), queryDataLowerInclusive, queryDataUpperInclusive);
		}
		else if (queryDataLower instanceof RefexDynamicIntegerBI)
		{
			return NumericRangeQuery.newIntRange(COLUMN_FIELD_DATA, ((RefexDynamicIntegerBI) queryDataLower).getDataInteger(),
					((RefexDynamicIntegerBI) queryDataUpper).getDataInteger(), queryDataLowerInclusive, queryDataUpperInclusive);
		}
		else if (queryDataLower instanceof RefexDynamicLongBI)
		{
			return NumericRangeQuery.newLongRange(COLUMN_FIELD_DATA, ((RefexDynamicLongBI) queryDataLower).getDataLong(),
					((RefexDynamicLongBI) queryDataUpper).getDataLong(), queryDataLowerInclusive, queryDataUpperInclusive);
		}
		else
		{
			throw new ParseException("Not a numeric data type - can't perform a range query");
		}
	}
}
