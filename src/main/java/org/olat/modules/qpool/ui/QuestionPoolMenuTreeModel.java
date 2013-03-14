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

import org.olat.core.gui.components.tree.DnDTreeModel;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.group.BusinessGroup;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QuestionItemCollection;

/**
 * 
 * Initial date: 15.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QuestionPoolMenuTreeModel extends GenericTreeModel implements DnDTreeModel{

	private static final long serialVersionUID = -665560407090871912L;

	@Override
	public boolean canDrop(TreeNode droppedNode, TreeNode targetNode,	boolean sibling) {
		if(droppedNode == null && targetNode == null) {
			return false;
		} else if(droppedNode == null) {
			Object uObject = targetNode.getUserObject();
			if("menu.database.my".equals(uObject)
					|| "menu.database.favorit".equals(uObject)) {
				return !sibling;
			} else if(uObject instanceof BusinessGroup
					|| uObject instanceof QuestionItemCollection
					|| uObject instanceof Pool) {
				return !sibling;
			}
			return false;
		}
		return false;
	}
}
