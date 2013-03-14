/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.qpool.manager;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.StringHelper;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.model.QItemDocument;
import org.olat.resource.OLATResource;
import org.olat.search.model.OlatDocument;
import org.olat.search.service.SearchResourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 28.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service("questionItemDocumentFactory")
public class QuestionItemDocumentFactory {

	@Autowired
	private PoolDAO poolDao;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private QuestionItemDAO questionItemDao;
	@Autowired
	private QPoolService qpoolService;


	public Document createDocument(SearchResourceContext searchResourceContext, Long itemKey) {
		QuestionItem item = questionItemDao.loadById(itemKey);
		if(item != null) {
			return createDocument(searchResourceContext, item);
		}
		return null;
	}

	public Document createDocument(SearchResourceContext searchResourceContext, QuestionItem item) {
		OlatDocument oDocument = new OlatDocument();
		oDocument.setId(item.getKey());
		oDocument.setCreatedDate(item.getCreationDate());
		oDocument.setLastChange(item.getLastModified());
		oDocument.setTitle(item.getTitle());
		oDocument.setDescription(item.getDescription());
		oDocument.setResourceUrl("[QuestionItem:" + item.getKey() + "]");
		oDocument.setDocumentType(QItemDocument.TYPE);
		oDocument.setCssIcon("o_qitem_icon");
		oDocument.setParentContextType(searchResourceContext.getParentContextType());
		oDocument.setParentContextName(searchResourceContext.getParentContextName());
		oDocument.setContent(item.getDescription());
		//author
		StringBuilder authorSb = new StringBuilder();
		List<Identity> owners = qpoolService.getAuthors(item);
		for(Identity owner:owners) {
			User user = owner.getUser();
			authorSb.append(user.getProperty(UserConstants.FIRSTNAME, null))
			  .append(" ")
			  .append(user.getProperty(UserConstants.LASTNAME, null))
			  .append(" ");
		}
		oDocument.setAuthor(authorSb.toString());
		
		//add specific fields
		Document document = oDocument.getLuceneDocument();
		
		//general fields
		addStringField(document, QItemDocument.IDENTIFIER_FIELD, item.getIdentifier(), 1.0f);
		addStringField(document, QItemDocument.MASTER_IDENTIFIER_FIELD,  item.getMasterIdentifier(), 1.0f);
		addTextField(document, QItemDocument.KEYWORDS_FIELD, item.getKeywords(), 2.0f);
		addTextField(document, QItemDocument.COVERAGE_FIELD, item.getCoverage(), 2.0f);
		addTextField(document, QItemDocument.ADD_INFOS_FIELD, item.getAdditionalInformations(), 2.0f);
		addStringField(document, QItemDocument.LANGUAGE_FIELD,  item.getLanguage(), 1.0f);
		
		//educational
		addStringField(document, QItemDocument.EDU_CONTEXT_FIELD,  item.getEducationalContext(), 1.0f);
		
		//question
		if(item.getQuestionType() != null) {
			addStringField(document, QItemDocument.ITEM_TYPE_FIELD,  item.getQuestionType().name(), 1.0f);
		}
		addStringField(document, QItemDocument.ASSESSMENT_TYPE_FIELD, item.getAssessmentType(), 1.0f);
		
		//lifecycle
		addStringField(document, QItemDocument.ITEM_VERSION_FIELD, item.getItemVersion(), 1.0f);
		if(item.getQuestionStatus() != null) {
			addStringField(document, QItemDocument.ITEM_STATUS_FIELD, item.getQuestionStatus().name(), 1.0f);
		}
		
		//rights
		addTextField(document, QItemDocument.COPYRIGHT_FIELD, item.getCopyright(), 2.0f);

		//technical
		addTextField(document, QItemDocument.EDITOR_FIELD, item.getEditor(), 2.0f);
		addStringField(document, QItemDocument.EDITOR_VERSION_FIELD, item.getEditorVersion(), 1.0f);
		addStringField(document, QItemDocument.FORMAT_FIELD, item.getFormat(), 1.0f);

		//save owners key
		for(Identity owner:owners) {
			document.add(new StringField(QItemDocument.OWNER_FIELD, owner.getKey().toString(), Field.Store.NO));
		}
		
		//link resources
		List<OLATResource> resources = questionItemDao.getSharedResources(item);
		for(OLATResource resource:resources) {
			document.add(new StringField(QItemDocument.SHARE_FIELD, resource.getKey().toString(), Field.Store.NO));
		}
		
		//need pools
		List<Pool> pools = poolDao.getPools(item);
		for(Pool pool:pools) {
			document.add(new StringField(QItemDocument.POOL_FIELD, pool.getKey().toString(), Field.Store.NO));
		}

		//need path
		String path = item.getTaxonomicPath();
		if(StringHelper.containsNonWhitespace(path)) {
			for(StringTokenizer tokenizer = new StringTokenizer(path, "/"); tokenizer.hasMoreTokens(); ) {
				String nextToken = tokenizer.nextToken();
				document.add(new TextField(QItemDocument.STUDY_FIELD, nextToken, Field.Store.NO));
			}
		}
		return document;
	}
	
	private void addStringField(Document doc, String fieldName, String content, float boost) {
		if(StringHelper.containsNonWhitespace(content)) {
			TextField field = new TextField(fieldName, content, Field.Store.YES);
			field.setBoost(boost);
			doc.add(field);
		}
	}
	
	/**
	 * indexed and tokenized
	 * @param fieldName
	 * @param content
	 * @param boost
	 * @return
	 */
	private void addTextField(Document doc, String fieldName, String content, float boost) {
		if(StringHelper.containsNonWhitespace(content)) {
			TextField field = new TextField(fieldName, content, Field.Store.YES);
			field.setBoost(boost);
			doc.add(field);
		}
	}
}
