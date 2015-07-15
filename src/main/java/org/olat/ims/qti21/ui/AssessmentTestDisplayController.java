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

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.MultipartFileInfos;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.fileresource.types.ImsQTI21Resource.PathResourceLocator;
import org.olat.ims.qti21.OutcomesListener;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.UserTestSession;
import org.olat.ims.qti21.model.CandidateItemEventType;
import org.olat.ims.qti21.model.CandidateTestEventType;
import org.olat.ims.qti21.model.jpa.CandidateEvent;
import org.olat.ims.qti21.ui.components.AssessmentTestFormItem;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.JqtiExtensionManager;
import uk.ac.ed.ph.jqtiplus.JqtiPlus;
import uk.ac.ed.ph.jqtiplus.exception.QtiCandidateStateException;
import uk.ac.ed.ph.jqtiplus.node.result.AbstractResult;
import uk.ac.ed.ph.jqtiplus.node.result.AssessmentResult;
import uk.ac.ed.ph.jqtiplus.node.result.ItemVariable;
import uk.ac.ed.ph.jqtiplus.node.result.OutcomeVariable;
import uk.ac.ed.ph.jqtiplus.node.test.SubmissionMode;
import uk.ac.ed.ph.jqtiplus.notification.NotificationLevel;
import uk.ac.ed.ph.jqtiplus.notification.NotificationRecorder;
import uk.ac.ed.ph.jqtiplus.provision.BadResourceException;
import uk.ac.ed.ph.jqtiplus.reading.QtiModelBuildingError;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlInterpretationException;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentTest;
import uk.ac.ed.ph.jqtiplus.running.TestPlanner;
import uk.ac.ed.ph.jqtiplus.running.TestProcessingInitializer;
import uk.ac.ed.ph.jqtiplus.running.TestSessionController;
import uk.ac.ed.ph.jqtiplus.running.TestSessionControllerSettings;
import uk.ac.ed.ph.jqtiplus.state.TestPlan;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNode;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNodeKey;
import uk.ac.ed.ph.jqtiplus.state.TestProcessingMap;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;
import uk.ac.ed.ph.jqtiplus.types.FileResponseData;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.ResponseData;
import uk.ac.ed.ph.jqtiplus.types.StringResponseData;
import uk.ac.ed.ph.jqtiplus.value.BooleanValue;
import uk.ac.ed.ph.jqtiplus.value.NumberValue;
import uk.ac.ed.ph.jqtiplus.value.Value;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

/**
 * 
 * Initial date: 08.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentTestDisplayController extends BasicController implements CandidateSessionContext {
	
	private final File fUnzippedDirRoot;
	private final String mapperUri;
	
	private VelocityContainer mainVC;
	private QtiWorksController qtiWorksCtrl;
	private TestSessionController testSessionController;
	
	private CandidateEvent lastEvent;
	private Date currentRequestTimestamp;
	private UserTestSession candidateSession;
	
	private OutcomesListener outcomesListener;


	@Autowired
	private QTI21Service qtiService;
	@Autowired
	private JqtiExtensionManager jqtiExtensionManager;
	
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param listener
	 * @param entry
	 * @param courseRe Course repository entry (optional)
	 * @param subIdent The course node identifier (mandatory only if in a course is used)
	 */
	public AssessmentTestDisplayController(UserRequest ureq, WindowControl wControl, OutcomesListener listener,
			RepositoryEntry entry, RepositoryEntry courseRe, String courseSubIdent) {
		super(ureq, wControl);
		
		this.outcomesListener = listener;
		
		FileResourceManager frm = FileResourceManager.getInstance();
		fUnzippedDirRoot = frm.unzipFileResource(entry.getOlatResource());
		mapperUri = registerCacheableMapper(null, "QTI21Resources::" + entry.getKey(), new ResourcesMapper());
		
		currentRequestTimestamp = ureq.getRequestTimestamp();
		
		UserTestSession lastSession = qtiService.getResumableTestSession(entry, courseRe, courseSubIdent, getIdentity());
		if(lastSession == null) {
			candidateSession = qtiService.createTestSession(entry, courseRe, courseSubIdent, getIdentity());
			testSessionController = enterSession(ureq);
		} else {
			candidateSession = lastSession;
			lastEvent = new CandidateEvent();
			lastEvent.setCandidateSession(candidateSession);
			lastEvent.setTestEventType(CandidateTestEventType.ITEM_EVENT);
			
			testSessionController = resumeSession();
		}

		/* Handle immediate end of test session */
        if (testSessionController.getTestSessionState().isEnded()) {
        	AssessmentResult assessmentResult = null;
            qtiService.finishTestSession(candidateSession, assessmentResult, ureq.getRequestTimestamp());
        	mainVC = createVelocityContainer("end");
        } else {
        	mainVC = createVelocityContainer("run");
        	initQtiWorks(ureq);
        }
        
        putInitialPanel(mainVC);
	}
	
	private void initQtiWorks(UserRequest ureq) {
		qtiWorksCtrl = new QtiWorksController(ureq, getWindowControl());
    	listenTo(qtiWorksCtrl);
    	mainVC.put("qtirun", qtiWorksCtrl.getInitialComponent());
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public boolean isTerminated() {
		return candidateSession.getTerminationTime() != null;
	}

	@Override
	public UserTestSession getCandidateSession() {
		return candidateSession;
	}
	
	@Override
	public CandidateEvent getLastEvent() {
		return lastEvent;
	}

	@Override
	public Date getCurrentRequestTimestamp() {
		return currentRequestTimestamp;
	}

	protected CandidateEvent assertSessionEntered(UserTestSession candidateSession) {
		return lastEvent;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}
	
	private void doExitTest(UserRequest ureq) {
		fireEvent(ureq, new QTI21Event(QTI21Event.EXIT));
	}

	private void processQTIEvent(UserRequest ureq, QTIWorksAssessmentTestEvent qe) {
		currentRequestTimestamp = ureq.getRequestTimestamp();
		
		switch(qe.getEvent()) {
			case selectItem:
				processSelectItem(ureq, qe.getSubCommand());
				break;
			case finishItem:
				processFinish(ureq);
				break;
			case reviewItem:
				processReviewItem(ureq, qe.getSubCommand());
				break;
			case itemSolution:
				processItemSolution(qe.getSubCommand());
				break;
			case testPartNavigation:
				processTestPartNavigation(ureq);
				break;
			case response:
				handleResponse(ureq, qe.getStringResponseMap(), qe.getFileResponseMap());
				break;
			case endTestPart:
				processEndTestPart(ureq);
				break;
			case advanceTestPart:
				processAdvanceTestPart(ureq);
				break;
			case reviewTestPart:
				processReviewTestPart();
				break;
			case exitTest:
				processExitTest(ureq);
				break;
			case source:
				logError("QtiWorks event source not implemented", null);
				break;
			case state:
				logError("QtiWorks event state not implemented", null);
				break;
			case result:
				logError("QtiWorks event result not implemented", null);
				break;
			case validation:
				logError("QtiWorks event validation not implemented", null);
				break;
			case authorview:
				logError("QtiWorks event authorview not implemented", null);
				break;
		}
	}
	
	private void processSelectItem(UserRequest ureq, String key) {
		TestPlanNodeKey nodeKey = TestPlanNodeKey.fromString(key);
		Date requestTimestamp = ureq.getRequestTimestamp();
        testSessionController.selectItemNonlinear(requestTimestamp, nodeKey);
	}
	
	private void processReviewItem(UserRequest ureq, String key) {
		TestPlanNodeKey itemKey = TestPlanNodeKey.fromString(key);
		Date requestTimestamp = ureq.getRequestTimestamp();
		
        //Assert.notNull(itemKey, "itemKey");

        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();

        /* Make sure caller may do this */
        //assertSessionNotTerminated(candidateSession);
        try {
            if (!testSessionController.mayReviewItem(itemKey)) {
            	logError("CANNOT_REVIEW_TEST_ITEM", null);
               //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_REVIEW_TEST_ITEM);
                return;
            }
        } catch (final QtiCandidateStateException e) {
        	logError("CANNOT_REVIEW_TEST_ITEM", e);
           // candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_REVIEW_TEST_ITEM);
            return;
        }  catch (final RuntimeException e) {
        	logError("CANNOT_REVIEW_TEST_ITEM", e);
            return;// handleExplosion(e, candidateSession);
        }

        /* Record current result state */
        computeAndRecordTestAssessmentResult(candidateSession, testSessionController, false);

        /* Record and log event */
        final CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.REVIEW_ITEM, null, itemKey, testSessionState, notificationRecorder);
        this.lastEvent = candidateTestEvent;
        //candidateAuditLogger.logCandidateEvent(candidateTestEvent);
	}

	private void processItemSolution(String key) {
		TestPlanNodeKey itemKey = TestPlanNodeKey.fromString(key);

        /* Get current JQTI state and create JQTI controller */
        NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        TestSessionState testSessionState = testSessionController.getTestSessionState();

        /* Make sure caller may do this */
        //assertSessionNotTerminated(candidateSession);
        try {
            if (!testSessionController.mayAccessItemSolution(itemKey)) {
                //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_SOLUTION_TEST_ITEM);
            	logError("CANNOT_SOLUTION_TEST_ITEM", null);
                return;
            }
        }
        catch (final QtiCandidateStateException e) {
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_SOLUTION_TEST_ITEM);
            logError("CANNOT_SOLUTION_TEST_ITEM", e);
        	return;
        } catch (final RuntimeException e) {
        	logError("Exploded", e);
            return;// handleExplosion(e, candidateSession);
        }

        /* Record current result state */
        computeAndRecordTestAssessmentResult(candidateSession, testSessionController, false);

        /* Record and log event */
        CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.SOLUTION_ITEM, null, itemKey, testSessionState, notificationRecorder);
        this.lastEvent = candidateTestEvent;
        //candidateAuditLogger.logCandidateEvent(candidateTestEvent); 
	}
	
	//public CandidateSession finishLinearItem(final CandidateSessionContext candidateSessionContext)
    // throws CandidateException {
	private void processFinish(UserRequest ureq) {
		
        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();
		
		try {
			if (!testSessionController.mayAdvanceItemLinear()) {
				logError("CANNOT_FINISH_LINEAR_TEST_ITEM", null);
                return;
            }
		} catch (QtiCandidateStateException e) {
         	logError("CANNOT_FINISH_LINEAR_TEST_ITEM", e);
         	return;
		} catch (RuntimeException e) {
         	logError("CANNOT_FINISH_LINEAR_TEST_ITEM", e);
			 //return handleExplosion(e, candidateSession);
		}
		 
		// Update state
		final Date requestTimestamp = ureq.getRequestTimestamp();
	    final TestPlanNode nextItemNode = testSessionController.advanceItemLinear(requestTimestamp);
	    
	    boolean terminated = nextItemNode == null && testSessionController.findNextEnterableTestPart() == null; 

	    // Record current result state
	    final AssessmentResult assessmentResult = computeAndRecordTestAssessmentResult(candidateSession, testSessionController, terminated);

	    /* If we ended the testPart and there are now no more available testParts, then finish the session now */
	    if (nextItemNode==null && testSessionController.findNextEnterableTestPart()==null) {
	    	candidateSession = qtiService.finishTestSession(candidateSession, assessmentResult, requestTimestamp);
	    }

	    // Record and log event 
	    final CandidateTestEventType eventType = nextItemNode!=null ? CandidateTestEventType.FINISH_ITEM : CandidateTestEventType.FINISH_FINAL_ITEM;
	   	final CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
	                eventType, null, testSessionState, notificationRecorder);
	   	this.lastEvent = candidateTestEvent;
	}
	
	private void processTestPartNavigation(UserRequest ureq) {
		final Date requestTimestamp = ureq.getRequestTimestamp();
        testSessionController.selectItemNonlinear(requestTimestamp, null);
	}
	
	//public CandidateSession handleResponses(final CandidateSessionContext candidateSessionContext,
    //        final Map<Identifier, StringResponseData> stringResponseMap,
    //        final Map<Identifier, MultipartFile> fileResponseMap,
    //        final String candidateComment)
            
	private void handleResponse(UserRequest ureq,
			Map<Identifier, StringResponseData> stringResponseMap,
			Map<Identifier,MultipartFileInfos> fileResponseMap) {
		String candidateComment = null;
		
		//Assert.notNull(candidateSessionContext, "candidateSessionContext");
        //assertSessionType(candidateSessionContext, AssessmentObjectType.ASSESSMENT_TEST);
        //final CandidateSession candidateSession = candidateSessionContext.getCandidateSession();
        //assertSessionNotTerminated(candidateSession);

        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();
		
		final Map<Identifier, ResponseData> responseDataMap = new HashMap<Identifier, ResponseData>();
        if (stringResponseMap != null) {
            for (final Entry<Identifier, StringResponseData> stringResponseEntry : stringResponseMap.entrySet()) {
                final Identifier identifier = stringResponseEntry.getKey();
                final StringResponseData stringResponseData = stringResponseEntry.getValue();
                responseDataMap.put(identifier, stringResponseData);
            }
        }
        
       // final Map<Identifier, CandidateFileSubmission> fileSubmissionMap = new HashMap<Identifier, CandidateFileSubmission>();
        if (fileResponseMap!=null) {
            for (final Entry<Identifier, MultipartFileInfos> fileResponseEntry : fileResponseMap.entrySet()) {
                final Identifier identifier = fileResponseEntry.getKey();
                final MultipartFileInfos multipartFile = fileResponseEntry.getValue();
                if (!multipartFile.isEmpty()) {
                    //final CandidateFileSubmission fileSubmission = candidateUploadService.importFileSubmission(candidateSession, multipartFile);
                	String storedFilePath = qtiService.importFileSubmission(candidateSession, multipartFile);
                	File storedFile = new File(storedFilePath);
                	final FileResponseData fileResponseData = new FileResponseData(storedFile, multipartFile.getContentType(), multipartFile.getFileName());
                    responseDataMap.put(identifier, fileResponseData);
                    //fileSubmissionMap.put(identifier, fileSubmission);
                }
            }
        }
        
        boolean allResponsesValid = true;
        boolean allResponsesBound = true;
		
		//TODO files upload
		final Date timestamp = ureq.getRequestTimestamp();
        if (candidateComment != null) {
            testSessionController.setCandidateCommentForCurrentItem(timestamp, candidateComment);
        }

        /* Attempt to bind responses (and maybe perform RP & OP) */
        testSessionController.handleResponsesToCurrentItem(timestamp, responseDataMap);
        
        /* Classify this event */
        final SubmissionMode submissionMode = testSessionController.getCurrentTestPart().getSubmissionMode();
        final CandidateItemEventType candidateItemEventType;
        if (allResponsesValid) {
            candidateItemEventType = submissionMode == SubmissionMode.INDIVIDUAL
            		? CandidateItemEventType.ATTEMPT_VALID : CandidateItemEventType.RESPONSE_VALID;
        }  else {
            candidateItemEventType = allResponsesBound
            		? CandidateItemEventType.RESPONSE_INVALID : CandidateItemEventType.RESPONSE_BAD;
        }

        /* Record resulting event */
        final CandidateEvent candidateEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.ITEM_EVENT, candidateItemEventType, testSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateEvent);
        this.lastEvent = candidateEvent;

        /* Persist CandidateResponse entities */
        /*for (final CandidateResponse candidateResponse : candidateResponseMap.values()) {
            candidateResponse.setCandidateEvent(candidateEvent);
            candidateResponseDao.persist(candidateResponse);
        }*/
        
        
        /* Record current result state */
        computeAndRecordTestAssessmentResult(candidateSession, testSessionController, false);

        /* Save any change to session state */
        candidateSession = qtiService.updateTestSession(candidateSession);
	}

	//public CandidateSession endCurrentTestPart(final CandidateSessionContext candidateSessionContext)
	private void processEndTestPart(UserRequest ureq) {
		 /* Update state */
        final Date requestTimestamp = ureq.getRequestTimestamp();
        testSessionController.endCurrentTestPart(requestTimestamp);
	}
	
	private void processAdvanceTestPart(UserRequest ureq) {
		
		//final CandidateSessionContext candidateSessionContext = getCandidateSessionContext();
		
        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();

        /* Perform action */
        final TestPlanNode nextTestPart;
        final Date currentTimestamp = ureq.getRequestTimestamp();
        try {
            nextTestPart = testSessionController.enterNextAvailableTestPart(currentTimestamp);
        } catch (final QtiCandidateStateException e) {
            logError("CANNOT_ADVANCE_TEST_PART", e);
            return;
        } catch (final RuntimeException e) {
            logError("RuntimeException", e);
            return;// handleExplosion(e, candidateSession);
        }

        CandidateTestEventType eventType;
        if (nextTestPart!=null) {
            /* Moved into next test part */
            eventType = CandidateTestEventType.ADVANCE_TEST_PART;
        }
        else {
            /* No more test parts.
             *
             * For single part tests, we terminate the test completely now as the test feedback was shown with the testPart feedback.
             * For multi-part tests, we shall keep the test open so that the test feedback can be viewed.
             */
            if (testSessionState.getTestPlan().getTestPartNodes().size()==1) {
                eventType = CandidateTestEventType.EXIT_TEST;
                testSessionController.exitTest(currentTimestamp);
                candidateSession.setTerminationTime(currentTimestamp);
                candidateSession = qtiService.updateTestSession(candidateSession);
            }
            else {
                eventType = CandidateTestEventType.ADVANCE_TEST_PART;
            }
        }
        
        boolean terminated = isTerminated();

        /* Record current result state */
        computeAndRecordTestAssessmentResult(candidateSession, testSessionController, terminated);

        /* Record and log event */
        final CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
               eventType, testSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateTestEvent);
        this.lastEvent = candidateTestEvent;

        if (terminated) {
        	doExitTest(ureq);
        }
	}
	
	private void processReviewTestPart() {
		
        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();

        /* Make sure caller may do this */
        //assertSessionNotTerminated(candidateSession);
        if (testSessionState.getCurrentTestPartKey()==null || !testSessionState.getCurrentTestPartSessionState().isEnded()) {
        	
            // candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_REVIEW_TEST_PART);
            logError("CANNOT_REVIEW_TEST_PART", null);
        	return;
        }

        /* Record and log event */
        final CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.REVIEW_TEST_PART, null, null, testSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateTestEvent);
        this.lastEvent = candidateTestEvent;
	}
	
	/**
	 * Exit multi-part tests
	 */
	private void processExitTest(UserRequest ureq) {

        /* Get current JQTI state and create JQTI controller */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        final CandidateEvent mostRecentEvent = assertSessionEntered(candidateSession);
        //final TestSessionController testSessionController = candidateDataService.createTestSessionController(mostRecentEvent, notificationRecorder);
        final TestSessionState testSessionState = testSessionController.getTestSessionState();

        /* Perform action */
        final Date currentTimestamp = ureq.getRequestTimestamp();
        try {
            testSessionController.exitTest(currentTimestamp);
        } catch (final QtiCandidateStateException e) {
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANNOT_EXIT_TEST);
        	logError("CANNOT_EXIT_TEST", null);
            return;
        } catch (final RuntimeException e) {
        	logError("Exploded", null);
            return;// handleExplosion(e, candidateSession);
        }

        /* Update CandidateSession as appropriate */
        candidateSession.setTerminationTime(currentTimestamp);
        candidateSession = qtiService.updateTestSession(candidateSession);

        /* Record current result state (final) */
        computeAndRecordTestAssessmentResult(candidateSession, testSessionController, true);

        /* Record and log event */
        final CandidateEvent candidateTestEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.EXIT_TEST, testSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateTestEvent);
        this.lastEvent = candidateTestEvent;
        
        doExitTest(ureq);
	}
	
	//private CandidateSession enterCandidateSession(final CandidateSession candidateSession)
	private TestSessionController enterSession(UserRequest ureq) {
		/* Set up listener to record any notifications */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);

        /* Create fresh JQTI+ state & controller for it */
        TestSessionController testSessionController = createNewTestSessionStateAndController(notificationRecorder);
        if (testSessionController == null) {
            return null;
        }
        
        /* Initialise test state and enter test */
        final TestSessionState testSessionState = testSessionController.getTestSessionState();
        final Date timestamp = ureq.getRequestTimestamp();
        try {
            testSessionController.initialize(timestamp);
            final int testPartCount = testSessionController.enterTest(timestamp);
            if (testPartCount==1) {
                /* If there is only testPart, then enter this (if possible).
                 * (Note that this may cause the test to exit immediately if there is a failed
                 * preCondition on this part.)
                 */
                testSessionController.enterNextAvailableTestPart(timestamp);
            }
            else {
                /* Don't enter first testPart yet - we shall tell candidate that
                 * there are multiple parts and let them enter manually.
                 */
            }
        }
        catch (final RuntimeException e) {
        	logError("", e);
            return null;
        }
        
        /* Record and log event */
        final CandidateEvent candidateEvent = qtiService.recordCandidateTestEvent(candidateSession,
                CandidateTestEventType.ENTER_TEST, testSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateEvent);
        this.lastEvent = candidateEvent;
        
        boolean ended = testSessionState.isEnded();

        /* Record current result state */
        final AssessmentResult assessmentResult = computeAndRecordTestAssessmentResult(candidateSession, testSessionController, ended);

        /* Handle immediate end of test session */
        if (ended) {
            qtiService.finishTestSession(candidateSession, assessmentResult, timestamp);
        }
        
        return testSessionController;
	}
	
	private TestSessionController createNewTestSessionStateAndController(NotificationRecorder notificationRecorder) {
		TestProcessingMap testProcessingMap = getTestProcessingMap();
		/* Generate a test plan for this session */
        final TestPlanner testPlanner = new TestPlanner(testProcessingMap);
        if (notificationRecorder!=null) {
            testPlanner.addNotificationListener(notificationRecorder);
        }
        final TestPlan testPlan = testPlanner.generateTestPlan();

        final TestSessionState testSessionState = new TestSessionState(testPlan);
        
        final TestSessionControllerSettings testSessionControllerSettings = new TestSessionControllerSettings();
        testSessionControllerSettings.setTemplateProcessingLimit(computeTemplateProcessingLimit());

        /* Create controller and wire up notification recorder */
        final TestSessionController result = new TestSessionController(jqtiExtensionManager,
                testSessionControllerSettings, testProcessingMap, testSessionState);
        if (notificationRecorder!=null) {
            result.addNotificationListener(notificationRecorder);
        }
		return result;
	}
	
	private TestSessionController resumeSession() {
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
		return createTestSessionController(notificationRecorder);
	}
	
	private TestSessionController createTestSessionController(NotificationRecorder notificationRecorder) {
        final TestSessionState testSessionState = qtiService.loadTestSessionState(candidateSession);
        return createTestSessionController(testSessionState, notificationRecorder);
    }
	
    public TestSessionController createTestSessionController(TestSessionState testSessionState,  NotificationRecorder notificationRecorder) {
        /* Try to resolve the underlying JQTI+ object */
        final TestProcessingMap testProcessingMap = getTestProcessingMap();
        if (testProcessingMap == null) {
            return null;
        }

        /* Create config for TestSessionController */
        final TestSessionControllerSettings testSessionControllerSettings = new TestSessionControllerSettings();
        testSessionControllerSettings.setTemplateProcessingLimit(computeTemplateProcessingLimit());

        /* Create controller and wire up notification recorder (if passed) */
        final TestSessionController result = new TestSessionController(jqtiExtensionManager,
                testSessionControllerSettings, testProcessingMap, testSessionState);
        if (notificationRecorder!=null) {
            result.addNotificationListener(notificationRecorder);
        }

        return result;
    }
	
	private AssessmentResult computeAndRecordTestAssessmentResult(UserTestSession candidateSession,
			TestSessionController testSessionController, boolean submit) {
		AssessmentResult assessmentResult = computeTestAssessmentResult(candidateSession, testSessionController);
		qtiService.recordTestAssessmentResult(candidateSession, assessmentResult);
		processOutcomeVariables(assessmentResult.getTestResult(), submit);
		return assessmentResult;
	}
	
	private void processOutcomeVariables(AbstractResult resultNode, boolean submit) {
		Float score = null;
		Boolean pass = null;
		
        for (final ItemVariable itemVariable : resultNode.getItemVariables()) {
            if (itemVariable instanceof OutcomeVariable) {
            	OutcomeVariable outcomeVariable = (OutcomeVariable)itemVariable;
            	Identifier identifier = outcomeVariable.getIdentifier();
            	if(QTI21Constants.SCORE_IDENTIFIER.equals(identifier)) {
            		Value value = itemVariable.getComputedValue();
            		if(value instanceof NumberValue) {
            			score = (float) ((NumberValue)value).doubleValue();
            		}
            	} else if(QTI21Constants.PASS_IDENTIFIER.equals(identifier)) {
            		Value value = itemVariable.getComputedValue();
            		if(value instanceof BooleanValue) {
            			pass = ((BooleanValue)value).booleanValue();
            		}
            	}
            }
        }
        
        if(score != null) {
        	if(submit) {
        		outcomesListener.updateOutcomes(score, pass);
        	} else {
        		outcomesListener.submit(score, pass);
        	}
        }
    }
	
    private AssessmentResult computeTestAssessmentResult(final UserTestSession candidateSession, final TestSessionController testSessionController) {
    	String baseUrl = "http://localhost:8080/olat";
        final URI sessionIdentifierSourceId = URI.create(baseUrl);
        final String sessionIdentifier = "testsession/" + candidateSession.getKey();
        
        Date timestamp = new Date();//requestTimestampContext.getCurrentRequestTimestamp();
        return testSessionController.computeAssessmentResult(timestamp, sessionIdentifier, sessionIdentifierSourceId);
    }
	
	private TestProcessingMap getTestProcessingMap() {
		boolean assessmentPackageIsValid = true;

		final ResolvedAssessmentTest resolvedAssessmentTest = qtiService.loadAndResolveAssessmentObject(fUnzippedDirRoot);
		BadResourceException ex = resolvedAssessmentTest.getTestLookup().getBadResourceException();
		if(ex instanceof QtiXmlInterpretationException) {
			QtiXmlInterpretationException exml = (QtiXmlInterpretationException)ex;
			System.out.println(exml.getInterpretationFailureReason());
			for(QtiModelBuildingError err :exml.getQtiModelBuildingErrors()) {
				System.out.println(err);
			}
		}
		
		TestProcessingInitializer initializer = new TestProcessingInitializer(resolvedAssessmentTest, assessmentPackageIsValid);
		TestProcessingMap result = initializer.initialize();
		return result;
	}
	
	/**
	 * Request limit configured outer of the QTI 2.1 file.
	 * @return
	 */
	public int computeTemplateProcessingLimit() {
		final Integer requestedLimit = null;// deliverySettings.getTemplateProcessingLimit();
		if (requestedLimit == null) {
			/* Not specified, so use default */
			return JqtiPlus.DEFAULT_TEMPLATE_PROCESSING_LIMIT;
		}
		final int requestedLimitIntValue = requestedLimit.intValue();
		return requestedLimitIntValue > 0 ? requestedLimitIntValue : JqtiPlus.DEFAULT_TEMPLATE_PROCESSING_LIMIT;
	}
	
	/**
	 * QtiWorks manage the form tag itself.
	 * 
	 * Initial date: 20.05.2015<br>
	 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
	 *
	 */
	private class QtiWorksController extends FormBasicController {
		
		private AssessmentTestFormItem qtiEl;
		
		public QtiWorksController(UserRequest ureq, WindowControl wControl) {
			super(ureq, wControl, LAYOUT_BAREBONE);
			initForm(ureq);
		}

		@Override
		protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
			mainForm.setStandaloneRendering(true);
			mainForm.setMultipartEnabled(true, Integer.MAX_VALUE);
			
			qtiEl = new AssessmentTestFormItem("qtirun");
			formLayout.add("qtirun", qtiEl);

			ResourceLocator fileResourceLocator = new PathResourceLocator(fUnzippedDirRoot.toPath());
			final ResourceLocator inputResourceLocator = 
	        		ImsQTI21Resource.createResolvingResourceLocator(fileResourceLocator);
			qtiEl.setResourceLocator(inputResourceLocator);
			qtiEl.setTestSessionController(testSessionController);
			qtiEl.setAssessmentObjectUri(qtiService.createAssessmentObjectUri(fUnzippedDirRoot));
			qtiEl.setCandidateSessionContext(AssessmentTestDisplayController.this);
			qtiEl.setMapperUri(mapperUri);
		}
		
		@Override
		protected void doDispose() {
			//
		}

		@Override
		protected void formOK(UserRequest ureq) {
			//
		}

		@Override
		protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
			if(source == qtiEl) {
				if(event instanceof QTIWorksAssessmentTestEvent) {
					QTIWorksAssessmentTestEvent qe = (QTIWorksAssessmentTestEvent)event;
					processQTIEvent(ureq, qe);
				}
			}
			super.formInnerEvent(ureq, source, event);
		}
	}
}