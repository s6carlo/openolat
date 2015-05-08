/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 
package org.olat.course.nodes.en;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.WindowBackOffice;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.info.WindowControlInfo;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.ENCourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.repository.RepositoryEntry;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description: <BR/>
 * Test the enrollment
 * <P/> Initial Date: Jul 28, 2004
 * 
 * @author patrick
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EnrollmentManagerTest extends OlatTestCase implements WindowControl {
	//
	private static OLog log = Tracing.createLoggerFor(EnrollmentManagerTest.class);
	/*
	 * ::Test Setup::
	 */
	private static Identity id1;
	// For WaitingGroup tests
	private static Identity wg1, wg2,wg3;
	private static Roles wg1Roles, wg2Roles, wg3Roles;
	
	
		// For WaitingGroup tests
	private static Translator testTranslator = null;
	private static BusinessGroup bgWithWaitingList = null;
	
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private EnrollmentManager enrollmentManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private DB dbInstance;
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before public void setup() throws Exception {
			// Identities
			id1 =  JunitTestHelper.createAndPersistIdentityAsUser("id1");
			DBFactory.getInstance().closeSession();				
			// create business-group with waiting-list
			String bgWithWaitingListName = "Group with WaitingList";
			String bgWithWaitingListDesc = "some short description for Group with WaitingList";
			Boolean enableWaitinglist = new Boolean(true);
			Boolean enableAutoCloseRanks = new Boolean(true);
			RepositoryEntry resource =  JunitTestHelper.createAndPersistRepositoryEntry();
			log.info("testAddToWaitingListAndFireEvent: resource=" + resource);
			bgWithWaitingList = businessGroupService.createBusinessGroup(id1, bgWithWaitingListName,
					bgWithWaitingListDesc, -1, -1, enableWaitinglist, enableAutoCloseRanks, resource);
			bgWithWaitingList.setMaxParticipants(new Integer(2));
			log.info("TEST bgWithWaitingList=" + bgWithWaitingList);
			log.info("TEST bgWithWaitingList.getMaxParticipants()=" + bgWithWaitingList.getMaxParticipants() );
			log.info("TEST bgWithWaitingList.getWaitingListEnabled()=" + bgWithWaitingList.getWaitingListEnabled() );
			// create mock objects
			testTranslator = Util.createPackageTranslator(EnrollmentManagerTest.class, new Locale("de"));
			// Identities
			wg1 = JunitTestHelper.createAndPersistIdentityAsUser("wg1");
			wg1Roles = securityManager.getRoles(wg1);
			wg2 = JunitTestHelper.createAndPersistIdentityAsUser("wg2");
			wg2Roles = securityManager.getRoles(wg2);
			wg3 = JunitTestHelper.createAndPersistIdentityAsUser("wg3");
			wg3Roles = securityManager.getRoles(wg3);
			DBFactory.getInstance().closeSession();	
			
	}


	// Test for WaitingList
	///////////////////////
	/**
	 * Enroll 3 identities (group with max-size=2 and waiting-list).
	 * Cancel enrollment. Check size after each step.
	 */
	@Test public void testEnroll() throws Exception {
		log.info("testEnroll: start...");
		ENCourseNode enNode = new ENCourseNode();
		OLATResourceable ores = OresHelper.createOLATResourceableTypeWithoutCheck("TestCourse");
		CourseEnvironment cenv = CourseFactory.createEmptyCourse(ores, "Test", "Test", "learningObjectives").getCourseEnvironment();
		// 1. enroll wg1 user
		IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(wg1);
		UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		CoursePropertyManager coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		log.info("enrollmentManager=" + enrollmentManager);
		log.info("bgWithWaitingList=" + bgWithWaitingList);
		assertTrue("bgWithWaitingList is null",bgWithWaitingList != null);
		log.info("userCourseEnv=" + userCourseEnv);
		log.info("userCourseEnv.getCourseEnvironment()=" + userCourseEnv.getCourseEnvironment());
		enrollmentManager.doEnroll(wg1, wg1Roles, bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator,
				new ArrayList<Long>()/*enrollableGroupNames*/, new ArrayList<Long>()/*enrollableAreaNames*/, userCourseEnv.getCourseEnvironment().getCourseGroupManager());	
		assertTrue("Enrollment failed, user='wg1'", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		int participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants," + participantsCounter , participantsCounter == 1);
		// 2. enroll wg2 user
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg2);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doEnroll(wg2, wg2Roles, bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator,
				new ArrayList<Long>()/*enrollableGroupNames*/, new ArrayList<Long>()/*enrollableAreaNames*/, userCourseEnv.getCourseEnvironment().getCourseGroupManager());	
		assertTrue("Enrollment failed, user='wg2'", businessGroupService.isIdentityInBusinessGroup(wg2,bgWithWaitingList));	
		assertTrue("Enrollment failed, user='wg1'", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants," + participantsCounter , participantsCounter == 2);
		// 3. enroll wg3 user => list is full => waiting-list
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg3);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doEnroll(wg3, wg3Roles, bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator,
				new ArrayList<Long>()/*enrollableGroupNames*/, new ArrayList<Long>()/*enrollableAreaNames*/, userCourseEnv.getCourseEnvironment().getCourseGroupManager());		
		assertFalse("Wrong enrollment, user='wg3' is in PartipiciantGroup, must be on waiting-list", businessGroupService.isIdentityInBusinessGroup(wg3,bgWithWaitingList));	
		assertFalse("Wrong enrollment, user='wg3' is in PartipiciantGroup, must be on waiting-list", businessGroupService.hasRoles(wg3, bgWithWaitingList, GroupRoles.participant.name()));
		assertTrue("Wrong enrollment, user='wg3' must be on waiting-list", businessGroupService.hasRoles(wg3, bgWithWaitingList, GroupRoles.waiting.name()));
		assertTrue("Enrollment failed, user='wg2'", businessGroupService.isIdentityInBusinessGroup(wg2,bgWithWaitingList));	
		assertTrue("Enrollment failed, user='wg1'", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants," + participantsCounter , participantsCounter == 2);
		int waitingListCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.waiting.name());
		assertTrue("Wrong number of waiting-list, must be 1, is " + waitingListCounter , waitingListCounter == 1);
		// cancel enrollment for wg2 => transfer wg3 from waiting-list to participants
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg2);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg2,bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator);		
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg2,bgWithWaitingList));	
		assertTrue("Enrollment failed, user='wg3'", businessGroupService.isIdentityInBusinessGroup(wg3,bgWithWaitingList));	
		assertTrue("Enrollment failed, user='wg1'", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants, must be 2, is " + participantsCounter , participantsCounter == 2);
		waitingListCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.waiting.name());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter , waitingListCounter == 0);
		// cancel enrollment for wg1 
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg1);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg1,bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator);		
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg2,bgWithWaitingList));	
		assertFalse("Cancel enrollment failed, user='wg1' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		assertTrue("Enrollment failed, user='wg3'", businessGroupService.isIdentityInBusinessGroup(wg3,bgWithWaitingList));	
		participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants, must be 1, is " + participantsCounter , participantsCounter == 1);
		waitingListCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.waiting.name());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter , waitingListCounter == 0);
		// cancel enrollment for wg3 
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg3);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg3,bgWithWaitingList, enNode, coursePropertyManager,this /*WindowControl mock*/,testTranslator);		
		assertFalse("Cancel enrollment failed, user='wg3' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg3,bgWithWaitingList));	
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg2,bgWithWaitingList));	
		assertFalse("Cancel enrollment failed, user='wg1' is still participants.", businessGroupService.isIdentityInBusinessGroup(wg1,bgWithWaitingList));	
		participantsCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.participant.name());
		assertTrue("Wrong number of participants, must be 0, is " + participantsCounter , participantsCounter == 0);
		waitingListCounter = businessGroupService.countMembers(bgWithWaitingList, GroupRoles.waiting.name());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter , waitingListCounter == 0);

		log.info("testEnroll: done...");
	}
	
	@Test
	public void testConcurrentEnrollmentWithWaitingList() {
		List<Identity> ids = new ArrayList<Identity>(30);	
		for(int i=0; i<30; i++) {
			Identity id = JunitTestHelper.createAndPersistIdentityAsUser("enroll-a-" + i + "-" + UUID.randomUUID().toString());
			ids.add(id);
		}
		
		ENCourseNode enNode = new ENCourseNode();
		OLATResourceable ores = OresHelper.createOLATResourceableTypeWithoutCheck("TestEnrollmentCourse");
		CourseEnvironment cenv = CourseFactory.createEmptyCourse(ores, "Test-Enroll", "Test", "Test enrollment with concurrent users").getCourseEnvironment();
		BusinessGroup group = businessGroupService.createBusinessGroup(id1, "Enrollment", "Enroll", new Integer(1), new Integer(10), true, false, null);
		Assert.assertNotNull(group);
		dbInstance.commitAndCloseSession();

		final CountDownLatch doneSignal = new CountDownLatch(ids.size());
		for(Identity id:ids) {
			EnrollThread thread = new EnrollThread(id, group, enNode, cenv, doneSignal);
			thread.start();
		}
		
		try {
			boolean interrupt = doneSignal.await(360, TimeUnit.SECONDS);
			assertTrue("Test takes too long (more than 10s)", interrupt);
		} catch (InterruptedException e) {
			fail("" + e.getMessage());
		}

		List<Identity> enrolledIds = businessGroupService.getMembers(group, GroupRoles.participant.name());
		Assert.assertNotNull(enrolledIds);
		Assert.assertEquals(10, enrolledIds.size());
		
		List<Identity> waitingIds = businessGroupService.getMembers(group, GroupRoles.waiting.name());
		Assert.assertNotNull(waitingIds);
		Assert.assertEquals(ids.size() - 10, waitingIds.size());
	}

	private class EnrollThread extends Thread {
		private final ENCourseNode enNode;
		private final Identity identity;
		private final CourseEnvironment cenv;
		private final BusinessGroup group;
		private final CountDownLatch doneSignal;
		
		public EnrollThread(Identity identity, BusinessGroup group, ENCourseNode enNode, CourseEnvironment cenv, CountDownLatch doneSignal) {
			this.enNode = enNode;
			this.group = group;
			this.identity = identity;
			this.cenv = cenv;
			this.doneSignal = doneSignal;
		}

		@Override
		public void run() {
			try {
				sleep(10);
				IdentityEnvironment ienv = new IdentityEnvironment();
				ienv.setIdentity(identity);
				UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
				CoursePropertyManager coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
				CourseGroupManager courseGroupManager = userCourseEnv.getCourseEnvironment().getCourseGroupManager();
				
				enrollmentManager.doEnroll(identity, JunitTestHelper.getUserRoles(), group, enNode, coursePropertyManager, EnrollmentManagerTest.this /*WindowControl mock*/, testTranslator,
						new ArrayList<Long>()/*enrollableGroupNames*/, new ArrayList<Long>()/*enrollableAreaNames*/, courseGroupManager);
				DBFactory.getInstance().commitAndCloseSession();
			} catch (Exception e) {
				log.error("", e);
			}	finally {
				doneSignal.countDown();
			}
		}
	}

	
	// Implements interface WindowControl
  /////////////////////////////////////
	public void pushToMainArea(Component comp){}
	public void pushAsModalDialog(Component comp){}
	@Override
	public void pushAsCallout(Component comp, String targetId){}
	public void pop(){}
	public void setInfo(String string){}
	public void setError(String string){}
	public void setWarning(String string){}
	public DTabs getDTabs(){return null;}
	public WindowControlInfo getWindowControlInfo(){return null;}
	public void makeFlat(){}
	public BusinessControl getBusinessControl() {
		
		BusinessControl control = new BusinessControl() {

			@Override
			public String getAsString() {
				return null;
			}

			@Override
			public List<ContextEntry> getEntries() {
				return Collections.<ContextEntry>emptyList();
			}
			
			@Override
			public List<ContextEntry> getEntriesDownTheControls() {
				return Collections.<ContextEntry>emptyList();
			}

			@Override
			public ContextEntry popLauncherContextEntry() {
				return null;
			}

			@Override
			public ContextEntry getCurrentContextEntry() {
				return null;
			}

			@Override
			public void setCurrentContextEntry(ContextEntry cw) {
				//
			}

			@Override
			public void dropLauncherEntries() {
				//
			}

			@Override
			public boolean hasContextEntry() {
				return false;
			}
			
		};
		
		return control;
		
	}

	public WindowBackOffice getWindowBackOffice() {
		return null;
	};
}