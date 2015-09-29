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

import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.advanceTestPart;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.endTestPart;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.exitTest;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.finishItem;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.itemSolution;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.reviewItem;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.reviewTestPart;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.selectItem;
import static org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent.Event.testPartNavigation;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.util.StringHelper;
import org.olat.ims.qti21.ui.QTIWorksAssessmentTestEvent;

import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentTest;
import uk.ac.ed.ph.jqtiplus.running.TestSessionController;

/**
 * 
 * Initial date: 11.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentTestFormItem extends AssessmentObjectFormItem {
	
	private final AssessmentTestComponent component;
	
	public AssessmentTestFormItem(String name, FormSubmit submitButton) {
		super(name, submitButton);
		component = new AssessmentTestComponent(name + "_cmp", this);
	}

	@Override
	public AssessmentTestComponent getComponent() {
		return component;
	}

	public ResolvedAssessmentTest getResolvedAssessmentTest() {
		return component.getResolvedAssessmentTest();
	}

	public void setResolvedAssessmentTest(ResolvedAssessmentTest resolvedAssessmentTest) {
		component.setResolvedAssessmentTest(resolvedAssessmentTest);
	}

	public TestSessionController getTestSessionController() {
		return component.getTestSessionController();
	}

	public void setTestSessionController(TestSessionController testSessionController) {
		component.setTestSessionController(testSessionController);
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
			QTIWorksAssessmentTestEvent event = null;
			String cmd = ureq.getParameter("cid");
			if(StringHelper.containsNonWhitespace(cmd)) {
				switch(QTIWorksAssessmentTestEvent.Event.valueOf(cmd)) {
					case selectItem: {
						String selectedItem = ureq.getParameter("item");
						event = new QTIWorksAssessmentTestEvent(selectItem, selectedItem, this);
						break;
					}
					case finishItem: {
						event = new QTIWorksAssessmentTestEvent(finishItem, this);
						break;
					}
					case endTestPart: {
						event = new QTIWorksAssessmentTestEvent(endTestPart, this);
						break;
					}
					case advanceTestPart: {
						event = new QTIWorksAssessmentTestEvent(advanceTestPart, this);
						break;
					}
					case testPartNavigation: {
						event = new QTIWorksAssessmentTestEvent(testPartNavigation, this);
						break;
					}
					case reviewItem: {
						String selectedItem = ureq.getParameter("item");
						event = new QTIWorksAssessmentTestEvent(reviewItem, selectedItem, this);
						break;
					}
					case itemSolution: {
						String selectedItem = ureq.getParameter("item");
						event = new QTIWorksAssessmentTestEvent(itemSolution, selectedItem, this);
						break;
					}
					case reviewTestPart: {
						event = new QTIWorksAssessmentTestEvent(reviewTestPart, this);
						break;
					}
					case exitTest: {
						event = new QTIWorksAssessmentTestEvent(exitTest, this);
						break;
					}
					default: {
						event = null;
					}
				}
			} 
			if(event != null) {
				getRootForm().fireFormEvent(ureq, event);
			}
			component.setDirty(true);
		}
	}

	@Override
	public void reset() {
		//
	}
}