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
package org.olat.ims.qti21.ui.components;

import static org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent.Event.close;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent.Event.exit;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent.Event.resethard;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent.Event.resetsoft;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent.Event.solution;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.util.StringHelper;
import org.olat.ims.qti21.ui.QTIWorksAssessmentItemEvent;

import uk.ac.ed.ph.jqtiplus.node.item.interaction.Interaction;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;

/**
 * 
 * Initial date: 11.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentItemFormItem extends AssessmentObjectFormItem {
	
	private final AssessmentItemComponent component;
	
	public AssessmentItemFormItem(String name, FormSubmit submitButton) {
		super(name, submitButton);
		component = new AssessmentItemComponent(name + "_cmp", this);
	}

	@Override
	public AssessmentItemComponent getComponent() {
		return component;
	}
	
	public ResolvedAssessmentItem getResolvedAssessmentItem() {
		return component.getResolvedAssessmentItem();
	}
	
	public void setResolvedAssessmentItem(ResolvedAssessmentItem resolvedAssessmentItem) {
		component.setResolvedAssessmentItem(resolvedAssessmentItem);
	}

	public ItemSessionController getItemSessionController() {
		return component.getItemSessionController();
	}

	public void setItemSessionController(ItemSessionController itemSessionController) {
		component.setItemSessionController(itemSessionController);
	}
	
	public Interaction getInteractionOfResponseUniqueIdentifier(String responseUniqueId) {
		return component.getInteractionOfResponseUniqueIdentifier(responseUniqueId);
	}

	@Override
	protected Component getFormItemComponent() {
		return component;
	}

	@Override
	protected void rootFormAvailable() {
		//
	}

	@Override
	public void evalFormRequest(UserRequest ureq) {
		String uri = ureq.getModuleURI();
		if(uri == null) {
			QTIWorksAssessmentItemEvent event = null;
			String cmd = ureq.getParameter("cid");
			if(StringHelper.containsNonWhitespace(cmd)) {
				switch(QTIWorksAssessmentItemEvent.Event.valueOf(cmd)) {
					case solution:
						event = new QTIWorksAssessmentItemEvent(solution, this);
						break;
					case resethard:
						event = new QTIWorksAssessmentItemEvent(resethard, this);
						break;
					case resetsoft:
						event = new QTIWorksAssessmentItemEvent(resetsoft, this);
						break;
					case close:
						event = new QTIWorksAssessmentItemEvent(close, this);
						break;
					case exit:
						event = new QTIWorksAssessmentItemEvent(exit, this);
						break;
					default:
						event = null;	
				}
			}
			
			if(event != null) {
				getRootForm().fireFormEvent(ureq, event);
			}
		}
	}

	@Override
	public void reset() {
		//
	}
}