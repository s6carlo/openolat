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
package org.olat.modules.qpool.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.modules.qpool.QuestionPoolService;
import org.olat.modules.qpool.StudyField;

/**
 * 
 * Initial date: 28.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class StudyFieldTreeModel extends GenericTreeModel {

	private static final long serialVersionUID = 3032222581990406868L;
	private QuestionPoolService qpoolService;
	
	public StudyFieldTreeModel() {
		qpoolService = CoreSpringFactory.getImpl(QuestionPoolService.class);
		
		buildTree();
	}
	
	private void buildTree() {
		GenericTreeNode rootNode = new GenericTreeNode("root", "root");
		setRootNode(rootNode);

		List<StudyField> fields = qpoolService.getStudyFields();
		Map<Long,GenericTreeNode> fieldKeyToNode = new HashMap<Long, GenericTreeNode>();
		for(StudyField field:fields) {
			Long key = field.getKey();
			GenericTreeNode node = fieldKeyToNode.get(key);
			if(node == null) {
				node = new GenericTreeNode(field.getField(), field);
				fieldKeyToNode.put(key, node);
			}

			StudyField parentField = field.getParentField();
			if(parentField == null) {
				//this is a root
				rootNode.addChild(node);
			} else {
				Long parentKey = parentField.getKey();
				GenericTreeNode parentNode = fieldKeyToNode.get(parentKey);
				if(parentNode == null) {
					parentNode = new GenericTreeNode(parentField.getField(), parentField);
					fieldKeyToNode.put(key, parentNode);
				}
				parentNode.addChild(node);
			}
		}
	}
}
