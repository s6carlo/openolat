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
package org.olat.course.nodes.gta.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.GTAManager;
import org.olat.course.nodes.gta.GTAType;
import org.olat.course.nodes.gta.Task;
import org.olat.course.nodes.gta.TaskHelper;
import org.olat.course.nodes.gta.TaskProcess;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.modules.ModuleConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 02.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GTAParticipantRevisionAndCorrectionsController extends BasicController {
	
	private Link submitRevisionButton;
	private final VelocityContainer mainVC;
	
	private DirectoryController correctionsCtrl, revisionsCtrl;
	private SubmitDocumentsController uploadRevisionsCtrl;
	
	private Task assignedTask;
	private final boolean businessGroupTask;
	private final GTACourseNode gtaNode;
	private final BusinessGroup assessedGroup;
	private final CourseEnvironment courseEnv;
	private final UserCourseEnvironment assessedUserCourseEnv;
	
	@Autowired
	private GTAManager gtaManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	
	public GTAParticipantRevisionAndCorrectionsController(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment assessedUserCourseEnv,Task assignedTask,
			GTACourseNode gtaNode, BusinessGroup assessedGroup) {
		super(ureq, wControl);
		this.gtaNode = gtaNode;
		courseEnv = assessedUserCourseEnv.getCourseEnvironment();
		this.assessedUserCourseEnv = assessedUserCourseEnv;
		this.assignedTask = assignedTask;
		this.assessedGroup = assessedGroup;
		this.businessGroupTask = GTAType.group.name().equals(gtaNode.getModuleConfiguration().getStringValue(GTACourseNode.GTASK_TYPE));
		
		mainVC = createVelocityContainer("participant_revisions");
		putInitialPanel(mainVC);
		initRevisionProcess(ureq);
	}
	
	public Task getAssignedTask() {
		return assignedTask;
	}

	@Override
	protected void doDispose() {
		//
	}
	
	private void initRevisionProcess(UserRequest ureq) {
		List<String> revisionStepNames = new ArrayList<>();
		mainVC.contextPut("previousRevisions", revisionStepNames);
		
		if(assignedTask.getRevisionLoop() > 1) {
			for(int i=1 ; i<assignedTask.getRevisionLoop(); i++ ) {
				//revisions
				setRevisionIteration(ureq, i, revisionStepNames);
			}
		}
		
		TaskProcess status = assignedTask.getTaskStatus();
		if(status == TaskProcess.revision) {
			//assessed user can return some revised documents
			setUploadRevision(ureq, assignedTask);
		} else if(status == TaskProcess.correction) {
			//coach can return some corrections
			setRevision(ureq, "revisions", assignedTask.getRevisionLoop());
			setCorrections(ureq, "corrections", assignedTask.getRevisionLoop());
		} else {
			int lastRevision = assignedTask.getRevisionLoop();
			setRevisionIteration(ureq, lastRevision, revisionStepNames);
		}
	}
	
	private void setRevisionIteration(UserRequest ureq, int iteration, List<String> revisionStepNames) {
		String revCmpName = "revisions-" + iteration;
		if(setRevision(ureq, revCmpName, iteration)) {
			revisionStepNames.add(revCmpName);
		}
		//corrections;
		String correctionCmpName = "corrections-" + iteration;
		if(setCorrections(ureq, correctionCmpName, iteration)) {
			revisionStepNames.add(correctionCmpName);
		}
	}
	
	private void setUploadRevision(UserRequest ureq, Task task) {
		File documentsDir;
		VFSContainer documentsContainer;
		int iteration = assignedTask.getRevisionLoop();
		ModuleConfiguration config = gtaNode.getModuleConfiguration();
		if(businessGroupTask) {
			documentsDir = gtaManager.getRevisedDocumentsDirectory(courseEnv, gtaNode, iteration, assessedGroup);
			documentsContainer = gtaManager.getRevisedDocumentsContainer(courseEnv, gtaNode, iteration, assessedGroup);
		} else {
			documentsDir = gtaManager.getRevisedDocumentsDirectory(courseEnv, gtaNode, iteration, getIdentity());
			documentsContainer = gtaManager.getRevisedDocumentsContainer(courseEnv, gtaNode, iteration, getIdentity());
		}
		uploadRevisionsCtrl = new SubmitDocumentsController(ureq, getWindowControl(), task, documentsDir, documentsContainer, -1, config);
		listenTo(uploadRevisionsCtrl);
		mainVC.put("uploadRevisions", uploadRevisionsCtrl.getInitialComponent());
		
		submitRevisionButton  = LinkFactory.createCustomLink("run.submit.revision.button", "submit", "run.submit.revision.button", Link.BUTTON, mainVC, this);
		submitRevisionButton.setCustomEnabledLinkCSS("btn btn-primary");
		submitRevisionButton.setIconLeftCSS("o_icon o_icon o_icon_submit");
	}
	
	private boolean setRevision(UserRequest ureq, String cmpName, int iteration) {
		File documentsDir;
		if(businessGroupTask) {
			documentsDir = gtaManager.getRevisedDocumentsDirectory(courseEnv, gtaNode, iteration, assessedGroup);
		} else {
			documentsDir = gtaManager.getRevisedDocumentsDirectory(courseEnv, gtaNode, iteration, getIdentity());
		}

		boolean hasDocument = TaskHelper.hasDocuments(documentsDir);
		if(hasDocument) {
			revisionsCtrl = new DirectoryController(ureq, getWindowControl(), documentsDir, "run.revised.description");
			listenTo(revisionsCtrl);
			mainVC.put(cmpName, revisionsCtrl.getInitialComponent());
		}
		return hasDocument;
	}
	
	private boolean setCorrections(UserRequest ureq, String cmpName, int iteration) {
		File documentsDir;
		if(businessGroupTask) {
			documentsDir = gtaManager.getRevisedDocumentsCorrectionsDirectory(courseEnv, gtaNode, iteration, assessedGroup);
		} else {
			documentsDir = gtaManager.getRevisedDocumentsCorrectionsDirectory(courseEnv, gtaNode, iteration, getIdentity());
		}
		
		boolean hasDocument = TaskHelper.hasDocuments(documentsDir);
		if(hasDocument) {
			correctionsCtrl = new DirectoryController(ureq, getWindowControl(), documentsDir, "run.corrections.description", "bulk.review", "review");
			listenTo(correctionsCtrl);
			mainVC.put(cmpName, correctionsCtrl.getInitialComponent());
		}
		return hasDocument;
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(uploadRevisionsCtrl == source) {
			if(event instanceof SubmitEvent) {
				Task aTask = uploadRevisionsCtrl.getAssignedTask();
				gtaManager.log("Revision", (SubmitEvent)event, aTask, getIdentity(), getIdentity(), assessedGroup, courseEnv, gtaNode);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(submitRevisionButton == source) {
			doSubmitRevisions();
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}
	
	private void doSubmitRevisions() {
		assignedTask = gtaManager.updateTask(assignedTask, TaskProcess.correction);
		gtaManager.log("Revision", "revision submitted", assignedTask, getIdentity(), getIdentity(), assessedGroup, courseEnv, gtaNode);

		if(businessGroupTask) {
			List<Identity> identities = businessGroupService.getMembers(assessedGroup, GroupRoles.participant.name());
			AssessmentManager assessmentManager = courseEnv.getAssessmentManager();
			assessmentManager.preloadCache(identities);
			ICourse course = CourseFactory.loadCourse(courseEnv.getCourseResourceableId());

			for(Identity identity:identities) {
				UserCourseEnvironment userCourseEnv = AssessmentHelper.createAndInitUserCourseEnvironment(identity, course);
				gtaNode.incrementUserAttempts(userCourseEnv);
			}
		} else {
			gtaNode.incrementUserAttempts(assessedUserCourseEnv);
		}
	}
}