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
package org.olat.course.assessment.manager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.activity.StringResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentChangedEvent;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.AssessmentLoggingAction;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.model.AssessmentNodeData;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.certificate.CertificateTemplate;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.model.CertificateInfos;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreAccounting;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.AssessmentService;
import org.olat.repository.RepositoryEntry;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Initial date: 20.07.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CourseAssessmentManagerImpl implements AssessmentManager {
	
	private static final Float FLOAT_ZERO = new Float(0);
	private static final Integer INTEGER_ZERO = new Integer(0);
	
	private final CourseGroupManager cgm;
	private final AssessmentService assessmentService;
	private final CertificatesManager certificatesManager;
	private final EfficiencyStatementManager efficiencyStatementManager;
	
	public CourseAssessmentManagerImpl(CourseGroupManager cgm) {
		this.cgm = cgm;
		assessmentService = CoreSpringFactory.getImpl(AssessmentService.class);
		certificatesManager = CoreSpringFactory.getImpl(CertificatesManager.class);
		efficiencyStatementManager = CoreSpringFactory.getImpl(EfficiencyStatementManager.class);
	}

	private AssessmentEntry getOrCreate(Identity assessedIdentity, CourseNode courseNode) {
		return assessmentService.getOrCreateAssessmentEntry(assessedIdentity, null, cgm.getCourseEntry(), courseNode.getIdent(), courseNode.getReferencedRepositoryEntry());
	}
	
	private AssessmentEntry getOrCreate(Identity assessedIdentity, String subIdent, RepositoryEntry referenceEntry) {
		return assessmentService.getOrCreateAssessmentEntry(assessedIdentity, null, cgm.getCourseEntry(), subIdent, referenceEntry);
	}

	@Override
	public List<AssessmentEntry> getAssessmentEntries(CourseNode courseNode) {
		return assessmentService.loadAssessmentEntriesBySubIdent(cgm.getCourseEntry(), courseNode.getIdent());
	}

	@Override
	public AssessmentEntry getAssessmentEntry(CourseNode courseNode, Identity assessedIdentity) {
		return assessmentService.loadAssessmentEntry(assessedIdentity, cgm.getCourseEntry(), courseNode.getIdent());
	}

	@Override
	public List<AssessmentEntry> getAssessmentEntries(Identity assessedIdentity) {
		return assessmentService.loadAssessmentEntriesByAssessedIdentity(assessedIdentity, cgm.getCourseEntry());
	}

	@Override
	public List<AssessmentEntry> getAssessmentEntries(BusinessGroup assessedGoup, CourseNode courseNode) {
		return assessmentService.loadAssessmentEntries(assessedGoup, cgm.getCourseEntry(), courseNode.getIdent());
	}

	@Override
	public AssessmentEntry createAssessmentEntry(CourseNode courseNode, Identity assessedIdentity, ScoreEvaluation scoreEvaluation) {
		RepositoryEntry referenceEntry = null;
		if(courseNode.needsReferenceToARepositoryEntry()) {
			referenceEntry = courseNode.getReferencedRepositoryEntry();
		}
		Float score = null;
		Boolean passed = null;
		if(scoreEvaluation != null) {
			score = scoreEvaluation.getScore();
			passed = scoreEvaluation.getPassed();
		}
		return assessmentService
				.createAssessmentEntry(assessedIdentity, null, cgm.getCourseEntry(), courseNode.getIdent(), referenceEntry, score, passed);
	}

	@Override
	public AssessmentEntry updateAssessmentEntry(AssessmentEntry assessmentEntry) {
		return assessmentService.updateAssessmentEntry(assessmentEntry);
	}

	@Override
	public void saveNodeAttempts(CourseNode courseNode, Identity identity, Identity assessedIdentity, Integer attempts) {
		ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setAttempts(attempts);
		assessmentService.updateAssessmentEntry(nodeAssessment);

		//node log
		UserNodeAuditManager am = course.getCourseEnvironment().getAuditManager();
		am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "ATTEMPTS set to: " + String.valueOf(attempts));

		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_ATTEMPTS_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiAttempts, "", String.valueOf(attempts)));	
	}

	@Override
	public void saveNodeComment(CourseNode courseNode, Identity identity, Identity assessedIdentity, String comment) {
		ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setComment(comment);
		assessmentService.updateAssessmentEntry(nodeAssessment);
		
		// node log
		UserNodeAuditManager am = course.getCourseEnvironment().getAuditManager();
		am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "COMMENT set to: " + comment);

		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_USER_COMMENT_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_USERCOMMENT_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiUserComment, "", StringHelper.stripLineBreaks(comment)));	
	}

	@Override
	public void saveNodeCoachComment(CourseNode courseNode, Identity assessedIdentity, String comment) {
		ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		nodeAssessment.setCoachComment(comment);
		assessmentService.updateAssessmentEntry(nodeAssessment);
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_COACH_COMMENT_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);

		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_COACHCOMMENT_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiCoachComment, "", StringHelper.stripLineBreaks(comment)));	
	}

	@Override
	public void incrementNodeAttempts(CourseNode courseNode, Identity assessedIdentity, UserCourseEnvironment userCourseEnv) {
		ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		int attempts = nodeAssessment.getAttempts() == null ? 1 :nodeAssessment.getAttempts().intValue() + 1;
		nodeAssessment.setAttempts(attempts);
		assessmentService.updateAssessmentEntry(nodeAssessment);
		if(courseNode instanceof AssessableCourseNode) {
			// Update users efficiency statement
			efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		}
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);
		
		// user activity logging
		ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_ATTEMPTS_UPDATED, 
				getClass(), 
				LoggingResourceable.wrap(assessedIdentity), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiAttempts, "", String.valueOf(attempts)));
	}

	@Override
	public void incrementNodeAttemptsInBackground(CourseNode courseNode, Identity assessedIdentity, UserCourseEnvironment userCourseEnv) {
		ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		
		AssessmentEntry nodeAssessment = getOrCreate(assessedIdentity, courseNode);
		int attempts = nodeAssessment.getAttempts() == null ? 1 :nodeAssessment.getAttempts().intValue() + 1;
		nodeAssessment.setAttempts(attempts);
		assessmentService.updateAssessmentEntry(nodeAssessment);
		if(courseNode instanceof AssessableCourseNode) {
			// Update users efficiency statement
			efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		}
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_ATTEMPTS_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);
	}
	
	@Override
	public void saveScoreEvaluation(AssessableCourseNode courseNode, Identity identity, Identity assessedIdentity,
			ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnv,
			boolean incrementUserAttempts) {
		final ICourse course = CourseFactory.loadCourse(cgm.getCourseEntry());
		final CourseEnvironment courseEnv = userCourseEnv.getCourseEnvironment();
		
		Float score = scoreEvaluation.getScore();
		Boolean passed = scoreEvaluation.getPassed();
		Long assessmentId = scoreEvaluation.getAssessmentID();
		
		String subIdent = courseNode.getIdent();
		RepositoryEntry referenceEntry = courseNode.getReferencedRepositoryEntry();
		AssessmentEntry assessmentEntry = getOrCreate(assessedIdentity, subIdent, referenceEntry);
		if(referenceEntry != null && !referenceEntry.equals(assessmentEntry.getReferenceEntry())) {
			assessmentEntry.setReferenceEntry(referenceEntry);
		}
		if(score == null) {
			assessmentEntry.setScore(null);
		} else {
			assessmentEntry.setScore(new BigDecimal(Float.toString(score)));
		}
		assessmentEntry.setPassed(passed);
		assessmentEntry.setFullyAssessed(scoreEvaluation.getFullyAssessed());
		assessmentEntry.setAssessmentId(assessmentId);
		if(scoreEvaluation.getAssessmentStatus() != null) {
			assessmentEntry.setAssessmentStatus(scoreEvaluation.getAssessmentStatus());
		}
		Integer attempts = null;
		if(incrementUserAttempts) {
			attempts = assessmentEntry.getAttempts() == null ? 1 :assessmentEntry.getAttempts().intValue() + 1;
			assessmentEntry.setAttempts(attempts);
		}
		assessmentEntry = assessmentService.updateAssessmentEntry(assessmentEntry);
		DBFactory.getInstance().commit();//commit before sending events
		//reevalute the tree
		ScoreAccounting scoreAccounting = userCourseEnv.getScoreAccounting();
		scoreAccounting.evaluateAll(true);
		DBFactory.getInstance().commit();//commit before sending events
		
		// node log
		UserNodeAuditManager am = courseEnv.getAuditManager();
		am.appendToUserNodeLog(courseNode, identity, assessedIdentity,  "score set to: " + String.valueOf(scoreEvaluation.getScore()));
		if(scoreEvaluation.getPassed()!=null) {
			am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "passed set to: " + scoreEvaluation.getPassed().toString());
		} else {
			am.appendToUserNodeLog(courseNode, identity, assessedIdentity, "passed set to \"undefined\"");
		}
		if(scoreEvaluation.getAssessmentID()!=null) {
			am.appendToUserNodeLog(courseNode, assessedIdentity, assessedIdentity, "assessmentId set to: " + scoreEvaluation.getAssessmentID().toString());
		}
		
		// notify about changes
		AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_SCORE_EVAL_CHANGED, assessedIdentity);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, course);
		
		// user activity logging
		if (scoreEvaluation.getScore()!=null) {
			ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_SCORE_UPDATED, 
					getClass(), 
					LoggingResourceable.wrap(assessedIdentity), 
					LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiScore, "", String.valueOf(scoreEvaluation.getScore())));
		}

		if (scoreEvaluation.getPassed()!=null) {
			ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_PASSED_UPDATED, 
					getClass(), 
					LoggingResourceable.wrap(assessedIdentity), 
					LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiPassed, "", String.valueOf(scoreEvaluation.getPassed())));
		} else {
			ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_PASSED_UPDATED, 
					getClass(), 
					LoggingResourceable.wrap(assessedIdentity), 
					LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiPassed, "", "undefined"));
		}

		if (incrementUserAttempts && attempts!=null) {
			ThreadLocalUserActivityLogger.log(AssessmentLoggingAction.ASSESSMENT_ATTEMPTS_UPDATED, 
					getClass(), 
					LoggingResourceable.wrap(identity), 
					LoggingResourceable.wrapNonOlatResource(StringResourceableType.qtiAttempts, "", String.valueOf(attempts)));	
		}
		
		// write only when enabled for this course
		if (courseEnv.getCourseConfig().isEfficencyStatementEnabled()) {
			List<AssessmentNodeData> data = new ArrayList<AssessmentNodeData>(50);
			AssessmentHelper.getAssessmentNodeDataList(0, courseEnv.getRunStructure().getRootNode(),
					scoreAccounting, userCourseEnv, true, true, data);
			efficiencyStatementManager.updateUserEfficiencyStatement(assessedIdentity, courseEnv, data, cgm.getCourseEntry());
		}

		if(course.getCourseConfig().isAutomaticCertificationEnabled()) {
			CourseNode rootNode = courseEnv.getRunStructure().getRootNode();
			ScoreEvaluation rootEval = scoreAccounting.evalCourseNode((AssessableCourseNode)rootNode);
			if(rootEval != null && rootEval.getPassed() != null && rootEval.getPassed().booleanValue()
					&& certificatesManager.isCertificationAllowed(assessedIdentity, cgm.getCourseEntry())) {
				CertificateTemplate template = null;
				Long templateId = course.getCourseConfig().getCertificateTemplate();
				if(templateId != null) {
					template = certificatesManager.getTemplateById(templateId);
				}
				CertificateInfos certificateInfos = new CertificateInfos(assessedIdentity, rootEval.getScore(), rootEval.getPassed());
				certificatesManager.generateCertificate(certificateInfos, cgm.getCourseEntry(), template, true);
			}
		}
	}

	@Override
	public Float getNodeScore(CourseNode courseNode, Identity identity) {
		if (courseNode == null) {
			return FLOAT_ZERO; // return default value
		}
		
		AssessmentEntry entry = assessmentService.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		if(entry != null && entry.getScore() != null) {
			return entry.getScore().floatValue();
		}
		return FLOAT_ZERO;
	}

	@Override
	public String getNodeComment(CourseNode courseNode, Identity identity) {
		AssessmentEntry entry = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return entry == null ? null : entry.getComment();
	}

	@Override
	public String getNodeCoachComment(CourseNode courseNode, Identity identity) {
		AssessmentEntry entry = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return entry == null ? null : entry.getCoachComment();
	}

	@Override
	public Boolean getNodePassed(CourseNode courseNode, Identity identity) {
		if (courseNode == null) {
			return Boolean.FALSE; // return default value
		}
		
		AssessmentEntry nodeAssessment = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getPassed();
	}

	@Override
	public Integer getNodeAttempts(CourseNode courseNode, Identity identity) {
		if(courseNode == null) return INTEGER_ZERO;
		
		AssessmentEntry nodeAssessment = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return nodeAssessment == null || nodeAssessment.getAttempts() == null  ? INTEGER_ZERO : nodeAssessment.getAttempts();
	}

	@Override
	public Long getAssessmentID(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getAssessmentId();
	}

	@Override
	public Date getScoreLastModifiedDate(CourseNode courseNode, Identity identity) {
		if(courseNode == null) return null;
		AssessmentEntry nodeAssessment = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());
		return nodeAssessment == null ? null : nodeAssessment.getLastModified();
	}

	@Override
	public Boolean getNodeFullyAssessed(CourseNode courseNode, Identity identity) {
		AssessmentEntry nodeAssessment = assessmentService
				.loadAssessmentEntry(identity, cgm.getCourseEntry(), courseNode.getIdent());	
		return nodeAssessment == null ? null : nodeAssessment.getFullyAssessed();
	}
	
	@Override
	public OLATResourceable createOLATResourceableForLocking(Identity assessedIdentity) {
		return OresHelper.createOLATResourceableInstance("AssessmentManager::Identity", assessedIdentity.getKey());
	}
	
	@Override
	public void registerForAssessmentChangeEvents(GenericEventListener gel, Identity identity) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(gel, identity, cgm.getCourseEntry().getOlatResource());
	}

	@Override
	public void deregisterFromAssessmentChangeEvents(GenericEventListener gel) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(gel, cgm.getCourseEntry().getOlatResource());
	}
}
