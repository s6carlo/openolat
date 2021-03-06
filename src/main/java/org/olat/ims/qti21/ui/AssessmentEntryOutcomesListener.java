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
package org.olat.ims.qti21.ui;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.ims.qti21.OutcomesListener;
import org.olat.ims.qti21.QTI21LoggingAction;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.AssessmentService;
import org.olat.modules.assessment.model.AssessmentEntryStatus;

/**
 * 
 * Initial date: 24.05.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentEntryOutcomesListener implements OutcomesListener {
	
	private AssessmentEntry assessmentEntry;
	private final AssessmentService assessmentService;
	
	private final boolean authorMode;
	private final boolean needManualCorrection;

	private AtomicBoolean start = new AtomicBoolean(true);
	private AtomicBoolean close = new AtomicBoolean(true);
	
	public AssessmentEntryOutcomesListener(AssessmentEntry assessmentEntry, boolean needManualCorrection,
			AssessmentService assessmentService, boolean authorMode) {
		this.assessmentEntry = assessmentEntry;
		this.assessmentService = assessmentService;
		this.authorMode = authorMode;
		this.needManualCorrection = needManualCorrection;
	}
	
	@Override
	public void updateOutcomes(Float updatedScore, Boolean updatedPassed) {
		AssessmentEntryStatus assessmentStatus = AssessmentEntryStatus.inProgress;
		assessmentEntry.setAssessmentStatus(assessmentStatus);
		assessmentEntry = assessmentService.updateAssessmentEntry(assessmentEntry);
		
		boolean firstStart = start.getAndSet(false);
		if(firstStart && !authorMode) {
			ThreadLocalUserActivityLogger.log(QTI21LoggingAction.QTI_START_AS_RESOURCE, getClass());
		}
	}

	@Override
	public void submit(Float submittedScore, Boolean submittedPass, Long assessmentId) {
		AssessmentEntryStatus assessmentStatus;
		if(needManualCorrection) {
			assessmentStatus = AssessmentEntryStatus.inReview;
		} else {
			assessmentStatus = AssessmentEntryStatus.done;
		}
		assessmentEntry.setAssessmentStatus(assessmentStatus);
		if(submittedScore == null) {
			assessmentEntry.setScore(null);
		} else {
			assessmentEntry.setScore(new BigDecimal(Float.toString(submittedScore)));
		}
		assessmentEntry.setPassed(submittedPass);
		assessmentEntry.setAssessmentId(assessmentId);
		assessmentEntry = assessmentService.updateAssessmentEntry(assessmentEntry);
		
		boolean firstClose = close.getAndSet(false);
		if(firstClose && !authorMode) {
			ThreadLocalUserActivityLogger.log(QTI21LoggingAction.QTI_CLOSE_AS_RESOURCE, getClass());
		}
	}
}
