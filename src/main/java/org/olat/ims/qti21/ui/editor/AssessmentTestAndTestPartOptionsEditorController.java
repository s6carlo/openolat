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
package org.olat.ims.qti21.ui.editor;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.ims.qti21.model.xml.AssessmentTestBuilder;
import org.olat.ims.qti21.ui.editor.events.AssessmentTestEvent;

import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.node.test.NavigationMode;
import uk.ac.ed.ph.jqtiplus.node.test.TestPart;

/**
 * 
 * Initial date: 22.05.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentTestAndTestPartOptionsEditorController extends ItemSessionControlController {

	private static final String[] navigationKeys = new String[]{
			NavigationMode.LINEAR.name(), NavigationMode.NONLINEAR.name()
	};
	
	private TextElement titleEl, maxScoreEl, cutValueEl;
	private SingleSelection navigationModeEl;

	private final TestPart testPart;
	private final AssessmentTest assessmentTest;
	private final AssessmentTestBuilder testBuilder;
	
	public AssessmentTestAndTestPartOptionsEditorController(UserRequest ureq, WindowControl wControl,
			AssessmentTest assessmentTest, TestPart testPart, AssessmentTestBuilder testBuilder, boolean restrictedEdit) {
		super(ureq, wControl, testPart, restrictedEdit, testBuilder.isEditable());
		this.assessmentTest = assessmentTest;
		this.testBuilder = testBuilder;
		this.testPart = testPart;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormContextHelp("Test editor QTI 2.1 in detail#details_testeditor_test");
		if(!editable) {
			setFormWarning("warning.alien.assessment.test");
		}
		
		String title = assessmentTest.getTitle();
		titleEl = uifactory.addTextElement("title", "form.metadata.title", 255, title, formLayout);
		titleEl.setEnabled(testBuilder.isEditable());
		titleEl.setMandatory(true);
		
		//score
		String maxScore = testBuilder.getMaxScore() == null ? "" : AssessmentHelper.getRoundedScore(testBuilder.getMaxScore());
		maxScoreEl = uifactory.addTextElement("max.score", "max.score", 8, maxScore, formLayout);
		maxScoreEl.setEnabled(false);
		
		Double cutValue = testBuilder.getCutValue();
		String cutValueStr = cutValue == null ? "" : cutValue.toString();
		cutValueEl = uifactory.addTextElement("cut.value", "cut.value", 8, cutValueStr, formLayout);
		cutValueEl.setEnabled(!restrictedEdit && testBuilder.isEditable());
		
		uifactory.addSpacerElement("space-test-part", formLayout, false);
		
		String[] navigationValues = new String[] {
				translate("form.testPart.navigationMode.linear"), translate("form.testPart.navigationMode.nonlinear")
		};
		String mode = testPart.getNavigationMode() == null ? NavigationMode.LINEAR.name() : testPart.getNavigationMode().name();
		navigationModeEl = uifactory.addRadiosHorizontal("navigationMode", "form.testPart.navigationMode", formLayout, navigationKeys, navigationValues);
		navigationModeEl.select(mode, true);
		navigationModeEl.setEnabled(!restrictedEdit && testBuilder.isEditable());
		navigationModeEl.setHelpText(translate("form.testPart.navigationMode.hint"));
		navigationModeEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_test");
		
		super.initForm(formLayout, listener, ureq);
		allowSkippingEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_test");
		allowCommentEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_test");
		allowReviewEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_test");
		showSolutionEl.setHelpUrlForManualPage("Test editor QTI 2.1 in detail#details_testeditor_test");
		
		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonsCont);
		FormSubmit submit = uifactory.addFormSubmitButton("save", "save", buttonsCont);
		submit.setEnabled(testBuilder.isEditable());
	}
	
	@Override
	protected void doDispose() {
		//
	}

	public String getTitle() {
		return titleEl.getValue();
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;

		titleEl.clearError();
		if(!StringHelper.containsNonWhitespace(titleEl.getValue())) {
			titleEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		}
		
		cutValueEl.clearError();
		if(StringHelper.containsNonWhitespace(cutValueEl.getValue())) {
			String cutValue = cutValueEl.getValue();
			try {
				double val = Double.parseDouble(cutValue);
				if(val < 0.0) {
					cutValueEl.setErrorKey("form.error.nointeger", null);
					allOk &= false;
				}
			} catch (NumberFormatException e) {
				cutValueEl.setErrorKey("form.error.nointeger", null);
				allOk &= false;
			}
		}
		
		navigationModeEl.clearError();
		if(!navigationModeEl.isOneSelected()) {
			navigationModeEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		super.formOK(ureq);
		
		String title = titleEl.getValue();
		assessmentTest.setTitle(title);
		
		String cutValue = cutValueEl.getValue();
		if(StringHelper.containsNonWhitespace(cutValue)) {
			testBuilder.setCutValue(new Double(cutValue));
		} else {
			testBuilder.setCutValue(null);
		}
		
		// navigation mode
		if(navigationModeEl.isOneSelected() && navigationModeEl.isSelected(0)) {
			testPart.setNavigationMode(NavigationMode.LINEAR);
		} else {
			testPart.setNavigationMode(NavigationMode.NONLINEAR);
		}
		
		fireEvent(ureq, AssessmentTestEvent.ASSESSMENT_TEST_CHANGED_EVENT);
	}
}
