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
* <p>
*/ 

package org.olat.group;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.StaleObjectStateException;
import org.jfree.util.Log;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.SecurityGroup;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.commons.lifecycle.LifeCycleManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.KnownIssueException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ActionType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.delete.service.GroupDeletionManager;
import org.olat.group.properties.BusinessGroupPropertyManager;
import org.olat.group.right.BGRightManager;
import org.olat.group.right.BGRightManagerImpl;
import org.olat.group.ui.BGConfigFlags;
import org.olat.group.ui.BGMailHelper;
import org.olat.group.ui.edit.BusinessGroupModifiedEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.syncservice.SyncSingleUserTask;
import org.olat.notifications.NotificationsManagerImpl;
import org.olat.repository.RepoJumpInHandlerFactory;
import org.olat.repository.RepositoryEntry;
import org.olat.testutils.codepoints.server.Codepoint;
import org.olat.user.UserDataDeletable;
import org.olat.util.logging.activity.LoggingResourceable;

import com.anthonyeden.lib.config.Configuration;
import com.anthonyeden.lib.config.ConfigurationException;
import com.anthonyeden.lib.config.Dom4jConfiguration;
import com.anthonyeden.lib.config.MutableConfiguration;
import com.anthonyeden.lib.config.XMLConfiguration;

/**
 * Description:<br>
 * Persisting implementation of the business group manager. Persists the data in
 * the database.
 * <P>
 * Initial Date: Jul 28, 2004 <br>
 * 
 * @author patrick
 */
public class BusinessGroupManagerImpl extends BasicManager implements BusinessGroupManager, UserDataDeletable {

	private static BusinessGroupManager INSTANCE;

	private static final String EXPORT_ATTR_NAME = "name";
	private static final String EXPORT_ATTR_MAX_PARTICIPATS = "maxParticipants";
	private static final String EXPORT_ATTR_MIN_PARTICIPATS = "minParticipants";
	private static final String EXPORT_ATTR_WAITING_LIST = "waitingList";
	private static final String EXPORT_ATTR_AUTO_CLOSE_RANKS = "autoCloseRanks";
	private static final String EXPORT_KEY_AREA_RELATION = "AreaRelation";
	private static final String EXPORT_KEY_GROUP = "Group";
	private static final String EXPORT_KEY_GROUP_COLLECTION = "GroupCollection";
	private static final String EXPORT_KEY_AREA = "Area";
	private static final String EXPORT_KEY_AREA_COLLECTION = "AreaCollection";
	private static final String EXPORT_KEY_ROOT = "OLATGroupExport";
	private static final String EXPORT_KEY_DESCRIPTION = "Description";
	private static final String EXPORT_KEY_COLLABTOOLS = "CollabTools";
	private static final String EXPORT_KEY_SHOW_OWNERS = "showOwners";
	private static final String EXPORT_KEY_SHOW_PARTICIPANTS = "showParticipants";
	private static final String EXPORT_KEY_SHOW_WAITING_LIST = "showWaitingList";
	private static final String EXPORT_KEY_CALENDAR_ACCESS = "calendarAccess";
	private static final String EXPORT_KEY_FOLDER_ACCESS = "folderAccess";
	private static final String EXPORT_KEY_NEWS = "info";

	private BaseSecurity securityManager;
	private List<DeletableGroupData> deleteListeners;
	
	/**
	 * @return singleton instance
	 */
	public static BusinessGroupManager getInstance() {
		return INSTANCE;
	}

	/**
	 * [used by spring]
	 */
	private BusinessGroupManagerImpl(BaseSecurity securityManager, UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		this.securityManager = securityManager;
		deleteListeners = new ArrayList<DeletableGroupData>();
		INSTANCE = this;
	}

  /** 
   * @see org.olat.group.BusinessGroupManager#createAndPersistBusinessGroup(java.lang.String, org.olat.core.id.Identity, java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Boolean, java.lang.Boolean, org.olat.group.context.BGContext)
   */
	public BusinessGroup createAndPersistBusinessGroup(String type, Identity identity, String name, String description,
			Integer minParticipants, Integer maxParticipants, Boolean enableWaitinglist, Boolean enableAutoCloseRanks, BGContext groupContext) {
		BusinessGroup grp = BusinessGroupFactory.createAndPersistBusinessGroup(type, identity, name, description, minParticipants,
				maxParticipants, enableWaitinglist, enableAutoCloseRanks, groupContext);
		if (grp != null) {
			Tracing.logAudit("Created Business Group", grp.toString(), this.getClass());
		}
		// else no group created
		return grp;
	}

	/**
	 * check if all given names in context exists.
	 * @param names
	 * @param groupContext
	 * @return
	 */
	protected boolean checkIfOneOrMoreNameExistsInContext(Set names, BGContext groupContext){
		return BusinessGroupFactory.checkIfOneOrMoreNameExistsInContext(names, groupContext);
	}
	
	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsOwnedBy(java.lang.String,
	 *      org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsOwnedBy(String type, Identity identityP, BGContext bgContext) {
		// attach group context to session - maybe a proxy...
		String query = "select bgi from " + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi,"
				+ " org.olat.group.BusinessGroupImpl as bgi" + " where bgi.ownerGroup = sgmi.securityGroup and sgmi.identity = :identId";
		if (bgContext != null) query = query + " and bgi.groupContext = :context";
		if (type != null) query = query + " and bgi.type = :type";

		DB db = DBFactory.getInstance();
		DBQuery dbq = db.createQuery(query);
		/*
		 * query.append("select distinct v from" + "
		 * org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + "
		 * org.olat.repository.RepositoryEntry v" + " inner join fetch v.ownerGroup
		 * as secGroup" + " inner join fetch v.olatResource as res where" + "
		 * sgmsi.securityGroup = secGroup and sgmsi.identity.key=");
		 */

		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) dbq.setEntity("context", bgContext);
		if (type != null) dbq.setString("type", type);

		List res = dbq.list();
		return res;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsAttendedBy(java.lang.String,
	 *      org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsAttendedBy(String type, Identity identityP, BGContext bgContext) {
		String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi "
				+ ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ " where bgi.partipiciantGroup = sgmi.securityGroup";
		query = query + " and sgmi.identity = :identId";
		if (bgContext != null) query = query + " and bgi.groupContext = :context";
		if (type != null) query = query + " and bgi.type = :type";

		DB db = DBFactory.getInstance();
		DBQuery dbq = db.createQuery(query);
		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) dbq.setEntity("context", bgContext);
		if (type != null) dbq.setString("type", type);

		List res = dbq.list();
		return res;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#getAllBusinessGroups()
	 */
	public List getAllBusinessGroups() {
		DBQuery dbq = DBFactory.getInstance().createQuery("select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi ");
		return dbq.list();
	}
	
	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsAttendedBy(java.lang.String,
	 *      org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsWithWaitingListAttendedBy(String type, Identity identityP, BGContext bgContext) {
		String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi "
				+ ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ " where bgi.waitingGroup = sgmi.securityGroup and sgmi.identity = :identId";
		if (bgContext != null) query = query + " and bgi.groupContext = :context";
		if (type != null) query = query + " and bgi.type = :type";

		DB db = DBFactory.getInstance();
		DBQuery dbq = db.createQuery(query);
		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) dbq.setEntity("context", bgContext);
		if (type != null) dbq.setString("type", type);

		List res = dbq.list();
		return res;
	}
	
	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroup(org.olat.basesecurity.SecurityGroup)
	 */
	@Override
	public BusinessGroup findBusinessGroup(SecurityGroup secGroup) {
		StringBuilder sb = new StringBuilder(); 
		sb.append("select bgi from ").append(BusinessGroupImpl.class.getName()).append(" as bgi where ")
			.append("(bgi.partipiciantGroup=:secGroup or bgi.ownerGroup=:secGroup or bgi.waitingGroup=:secGroup)");

		DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setEntity("secGroup", secGroup);
		List<BusinessGroup> res = query.list();
		if(res.isEmpty()) return null;
		return res.get(0);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#updateBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public void updateBusinessGroup(BusinessGroup updatedBusinessGroup) {
		updatedBusinessGroup.setLastModified(new Date());
		DBFactory.getInstance().updateObject(updatedBusinessGroup);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public void deleteBusinessGroup(BusinessGroup businessGroupTodelete) {
		try{
			OLATResourceableJustBeforeDeletedEvent delEv = new OLATResourceableJustBeforeDeletedEvent(businessGroupTodelete);
			// notify all (currently running) BusinessGroupXXXcontrollers
			// about the deletion which will occur.
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(delEv, businessGroupTodelete);
	
			String type = businessGroupTodelete.getType();
			// refresh object to avoid stale object exceptions
			businessGroupTodelete = loadBusinessGroup(businessGroupTodelete);
			// 0) Loop over all deletableGroupData
			for (DeletableGroupData deleteListener : deleteListeners) {
				Log.debug("deleteBusinessGroup: call deleteListener=" + deleteListener);
				deleteListener.deleteGroupDataFor(businessGroupTodelete);
			} 
			ProjectBrokerManagerFactory.getProjectBrokerManager().deleteGroupDataFor(businessGroupTodelete);
			// 1) Delete all group properties
			CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroupTodelete);
			ct.deleteTools(businessGroupTodelete);// deletes everything concerning properties&collabTools
			// 1.b)delete display member property
			BusinessGroupPropertyManager bgpm = new BusinessGroupPropertyManager(businessGroupTodelete);
			bgpm.deleteDisplayMembers();
			// 2) Delete the group areas
			if (BusinessGroup.TYPE_LEARNINGROUP.equals(type)) {
				BGAreaManagerImpl.getInstance().deleteBGtoAreaRelations(businessGroupTodelete);
			}
			// 3) Delete the group object itself on the database
			DBFactory.getInstance().deleteObject(businessGroupTodelete);
			// 4) Delete the associated security groups
			if (BusinessGroup.TYPE_BUDDYGROUP.equals(type) || BusinessGroup.TYPE_LEARNINGROUP.equals(type)) {
				SecurityGroup owners = businessGroupTodelete.getOwnerGroup();
				securityManager.deleteSecurityGroup(owners);
			}
			// in all cases the participant groups
			SecurityGroup partips = businessGroupTodelete.getPartipiciantGroup();
			securityManager.deleteSecurityGroup(partips);
			// Delete waiting-group when one exists
			if (businessGroupTodelete.getWaitingGroup() != null) {
				securityManager.deleteSecurityGroup(businessGroupTodelete.getWaitingGroup());
			}
	
			// delete the publisher attached to this group (e.g. the forum and folder
			// publisher)
			NotificationsManagerImpl.getInstance().deletePublishersOf(businessGroupTodelete);
	
			// delete potential jabber group roster
			if (InstantMessagingModule.isEnabled()) {
				String groupID = InstantMessagingModule.getAdapter().createChatRoomString(businessGroupTodelete);
				InstantMessagingModule.getAdapter().deleteRosterGroup(groupID);
			}
			Tracing.logAudit("Deleted Business Group", businessGroupTodelete.toString(), this.getClass());
		} catch(DBRuntimeException dbre) {
			Throwable th = dbre.getCause();
			if ((th instanceof StaleObjectStateException) &&
					(th.getMessage().startsWith("Row was updated or deleted by another transaction"))) {
				// known issue OLAT-3654
				Tracing.logInfo("Group was deleted by another user in the meantime. Known issue OLAT-3654", this.getClass());
				throw new KnownIssueException("Group was deleted by another user in the meantime", 3654);
			} else {
				throw dbre;
			}
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroupWithMail(org.olat.group.BusinessGroup,
	 *      org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, java.util.List)
	 */
	public void deleteBusinessGroupWithMail(BusinessGroup businessGroupTodelete, WindowControl wControl, UserRequest ureq, Translator trans,
			List contactLists) {
		Codepoint.codepoint(this.getClass(), "deleteBusinessGroupWithMail");
		
		// collect data for mail
		BaseSecurity secMgr = BaseSecurityManager.getInstance();
		List users = new ArrayList();
		SecurityGroup ownerGroup = businessGroupTodelete.getOwnerGroup();
		if (ownerGroup != null) {
			List owner = secMgr.getIdentitiesOfSecurityGroup(ownerGroup);
			users.addAll(owner);
		}
		SecurityGroup partGroup = businessGroupTodelete.getPartipiciantGroup();
		if (partGroup != null) {
			List participants = secMgr.getIdentitiesOfSecurityGroup(partGroup);
			users.addAll(participants);
		}
		SecurityGroup watiGroup = businessGroupTodelete.getWaitingGroup();
		if (watiGroup != null) {
			List waiting = secMgr.getIdentitiesOfSecurityGroup(watiGroup);
			users.addAll(waiting);
		}
		// now delete the group first
		deleteBusinessGroup(businessGroupTodelete);
		// finally send email
		MailerWithTemplate mailer = MailerWithTemplate.getInstance();
		MailTemplate mailTemplate = BGMailHelper.createDeleteGroupMailTemplate(businessGroupTodelete, ureq.getIdentity());
		if (mailTemplate != null) {
			MailerResult mailerResult = mailer.sendMailAsSeparateMails(users, null, null, mailTemplate, null);
			MailHelper.printErrorsAndWarnings(mailerResult, wControl, ureq.getLocale());
		}
		
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroups(java.util.List)
	 */
	public void deleteBusinessGroups(List businessGroups) {
		Iterator iterator = businessGroups.iterator();
		while (iterator.hasNext()) {
			BusinessGroup group = (BusinessGroup) iterator.next();
			deleteBusinessGroup(group);
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#isIdentityInBusinessGroup(org.olat.core.id.Identity,
	 *      java.lang.String, org.olat.group.context.BGContext)
	 */
	public boolean isIdentityInBusinessGroup(Identity identity, String groupName, BGContext groupContext) {
		DB db = DBFactory.getInstance();
		StringBuilder q = new StringBuilder();
		q.append(" select count(grp) from").append(" org.olat.group.BusinessGroupImpl as grp,").append(
				" org.olat.basesecurity.SecurityGroupMembershipImpl as secgmemb").append(" where");
		if (groupContext != null) {
			q.append(" grp.groupContext = :context and");
		}
		q.append(" grp.name = :name").append(" and ((grp.partipiciantGroup = secgmemb.securityGroup").append(" and secgmemb.identity = :id) ")
				.append(" or (grp.ownerGroup = secgmemb.securityGroup").append(" and secgmemb.identity = :id)) ");
		DBQuery query = db.createQuery(q.toString());
		query.setEntity("id", identity);
		if (groupContext != null) {
			query.setEntity("context", groupContext);
		}
		query.setString("name", groupName);
		query.setCacheable(true);
		List result = query.list();
		if (result.size() == 0) return false;
		return ( ((Long) result.get(0)).intValue() > 0);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#isIdentityInBusinessGroup(org.olat.core.id.Identity,
	 *      org.olat.group.BusinessGroup)
	 */
	public boolean isIdentityInBusinessGroup(Identity identity, BusinessGroup businessGroup) {
		SecurityGroup participants = businessGroup.getPartipiciantGroup();
		SecurityGroup owners = businessGroup.getOwnerGroup();
		if (participants != null) {
			if (securityManager.isIdentityInSecurityGroup(identity, participants)) return true;
		}
		if (owners != null) {
			if (securityManager.isIdentityInSecurityGroup(identity, owners)) return true;
		}
		return false;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#loadBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public BusinessGroup loadBusinessGroup(BusinessGroup currBusinessGroup) {
		return (BusinessGroup) DBFactory.getInstance().loadObject(currBusinessGroup);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#loadBusinessGroup(java.lang.Long,
	 *      boolean)
	 */
	public BusinessGroup loadBusinessGroup(Long groupKey, boolean strict) {
		if (strict) return (BusinessGroup) DBFactory.getInstance().loadObject(BusinessGroupImpl.class, groupKey);
		return (BusinessGroup) DBFactory.getInstance().findObject(BusinessGroupImpl.class, groupKey);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#copyBusinessGroup(org.olat.group.BusinessGroup,
	 *      java.lang.String, java.lang.String, java.lang.Integer,
	 *      java.lang.Integer, org.olat.group.context.BGContext, java.util.Map,
	 *      boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public BusinessGroup copyBusinessGroup(BusinessGroup sourceBusinessGroup, String targetName, String targetDescription, Integer targetMin,
			Integer targetMax, BGContext targetBgContext, Map areaLookupMap, boolean copyAreas, boolean copyCollabToolConfig, boolean copyRights,
			boolean copyOwners, boolean copyParticipants, boolean copyMemberVisibility, boolean copyWaitingList) {

		// 1. create group
		String bgType = sourceBusinessGroup.getType();
		// create group, set waitingListEnabled, enableAutoCloseRanks like source business-group
		BusinessGroup newGroup = createAndPersistBusinessGroup(bgType, null, targetName, targetDescription, targetMin, targetMax, 
				sourceBusinessGroup.getWaitingListEnabled(), sourceBusinessGroup.getAutoCloseRanksEnabled(), targetBgContext);
		// return immediately with null value to indicate an already take groupname
		if (newGroup == null) { return null; }
		// 2. copy tools
		if (copyCollabToolConfig) {
			CollaborationToolsFactory toolsF = CollaborationToolsFactory.getInstance();
			// get collab tools from original group and the new group
			CollaborationTools oldTools = toolsF.getOrCreateCollaborationTools(sourceBusinessGroup);
			CollaborationTools newTools = toolsF.getOrCreateCollaborationTools(newGroup);
			// copy the collab tools settings
			for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
				String tool = CollaborationTools.TOOLS[i];
				newTools.setToolEnabled(tool, oldTools.isToolEnabled(tool));
			}			
			String oldNews = oldTools.lookupNews();
			newTools.saveNews(oldNews);
		}
		// 3. copy member visibility
		if (copyMemberVisibility) {
			BusinessGroupPropertyManager bgpm = new BusinessGroupPropertyManager(newGroup);
			bgpm.copyConfigurationFromGroup(sourceBusinessGroup);
		}
		// 4. copy areas
		if (copyAreas) {
			BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
			List areas = areaManager.findBGAreasOfBusinessGroup(sourceBusinessGroup);
			Iterator iterator = areas.iterator();
			while (iterator.hasNext()) {
				BGArea area = (BGArea) iterator.next();
				if (areaLookupMap == null) {
					// reference target group to source groups areas
					areaManager.addBGToBGArea(newGroup, area);
				} else {
					// reference target group to mapped group areas
					BGArea mappedArea = (BGArea) areaLookupMap.get(area);
					areaManager.addBGToBGArea(newGroup, mappedArea);
				}
			}
		}
		// 5. copy owners
		if (copyOwners) {
			List owners = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getOwnerGroup());
			Iterator iter = owners.iterator();
			while (iter.hasNext()) {
				Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getOwnerGroup());
			}
		}
		// 6. copy participants
		if (copyParticipants) {
			List participants = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getPartipiciantGroup());
			Iterator iter = participants.iterator();
			while (iter.hasNext()) {
				Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getPartipiciantGroup());
			}
		}
		// 7. copy rights
		if (copyRights) {
			BGRightManager rightManager = BGRightManagerImpl.getInstance();
			List sourceRights = rightManager.findBGRights(sourceBusinessGroup);
			Iterator iterator = sourceRights.iterator();
			while (iterator.hasNext()) {
				String sourceRight = (String) iterator.next();
				rightManager.addBGRight(sourceRight, newGroup);
			}
		}
		// 8. copy waiting-lisz
		if (copyWaitingList) {
			List waitingList = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getWaitingGroup());
			Iterator iter = waitingList.iterator();
			while (iter.hasNext()) {
				Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getWaitingGroup());
			}
		}
		return newGroup;

	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addParticipant(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void addParticipantAndFireEvent(Identity ureqIdentity, final Identity identity, final BusinessGroup group, BGConfigFlags flags,
			boolean doOnlyPostAddingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getPartipiciantGroup());
		}
		// add user to buddies rosters
		addToRoster(ureqIdentity, identity, group, flags);
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_PARTICIPANT_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addOwner(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void addOwnerAndFireEvent(Identity ureqIdentity, Identity identity, BusinessGroup group, BGConfigFlags flags, 
			boolean doOnlyPostAddingStuff) {
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getOwnerGroup());
		}
		// add user to buddies rosters
		addToRoster(ureqIdentity, identity, group, flags);
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OWNER_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeOwner(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void removeOwnerAndFireEvent(Identity ureqIdentity, Identity identity, BusinessGroup group, BGConfigFlags flags,
			boolean doOnlyPostRemovingStuff) {
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getOwnerGroup());
		}
		// remove user from buddies rosters
		removeFromRoster(identity, group, flags);
		
		//remove subsciptions if user gets removed
		removeSubscriptions(identity, group);
		
		// notify currently active users of this business group
		if (identity.getKey().equals(ureqIdentity.getKey()) ) {
			BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.MYSELF_ASOWNER_REMOVED_EVENT, group, identity);
		} else {
  		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		}
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OWNER_REMOVED, getClass(),
				LoggingResourceable.wrap(group), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeParticipant(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void removeParticipantAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags,
			final boolean doOnlyPostRemovingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getPartipiciantGroup());
		}
		// remove user from buddies rosters
		removeFromRoster(identity, group, flags);
		
		//remove subsciptions if user gets removed
		removeSubscriptions(identity, group);
		
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_PARTICIPANT_REMOVED, getClass(), LoggingResourceable.wrap(identity), LoggingResourceable.wrap(group));
		// Check if a waiting-list with auto-close-ranks is configurated
		if ( group.getWaitingListEnabled().booleanValue() && group.getAutoCloseRanksEnabled().booleanValue() ) {
			// even when doOnlyPostRemovingStuff is set to true we really transfer the first Identity here
			transferFirstIdentityFromWaitingToParticipant(ureqIdentity, group, flags);
		}	
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addParticipant(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void addToWaitingListAndFireEvent(Identity ureqIdentity, final Identity identity, final BusinessGroup group,
			boolean doOnlyPostAddingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getWaitingGroup());
		}
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_TO_WAITING_LIST_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}
	
	/**
	 * @see org.olat.group.BusinessGroupManager#removeFromWaitingListAndFireEvent(org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger,
	 *      boolean)
	 */
	public void removeFromWaitingListAndFireEvent(Identity userRequestIdentity, final Identity identity, final BusinessGroup group, 
			boolean doOnlyPostRemovingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getWaitingGroup());
		}
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_FROM_WAITING_LIST_REMOVED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}
	
	/**
	 * @see org.olat.group.BusinessGroupManager#exportGroups(org.olat.group.context.BGContext,
	 *      java.io.File)
	 */
	public void exportGroups(BGContext context, File fExportFile) {
		if (context == null) return; // nothing to do... says Florian.
		Dom4jConfiguration root = new Dom4jConfiguration(EXPORT_KEY_ROOT);

		// export areas
		MutableConfiguration confAreas = root.addChild(EXPORT_KEY_AREA_COLLECTION);
		BGAreaManager am = BGAreaManagerImpl.getInstance();
		List areas = am.findBGAreasOfBGContext(context);
		for (Iterator iter = areas.iterator(); iter.hasNext();) {
			BGArea area = (BGArea) iter.next();
			MutableConfiguration newArea = confAreas.addChild(EXPORT_KEY_AREA);
			newArea.addAttribute(EXPORT_ATTR_NAME, area.getName());
			newArea.addChild(EXPORT_KEY_DESCRIPTION, area.getDescription());
		}

		// TODO fg: export group rights

		// export groups
		MutableConfiguration confGroups = root.addChild(EXPORT_KEY_GROUP_COLLECTION);
		BGContextManager cm = BGContextManagerImpl.getInstance();
		List groups = cm.getGroupsOfBGContext(context);
		for (Iterator iter = groups.iterator(); iter.hasNext();) {
			BusinessGroup group = (BusinessGroup) iter.next();
			exportGroup(fExportFile, confGroups, group);
		}

		saveGroupConfiguration(fExportFile, root);
	}

	public void exportGroup(BusinessGroup group, File fExportFile) {
		Dom4jConfiguration root = new Dom4jConfiguration(EXPORT_KEY_ROOT);
		MutableConfiguration confGroups = root.addChild(EXPORT_KEY_GROUP_COLLECTION);
		exportGroup(fExportFile, confGroups, group);
		saveGroupConfiguration(fExportFile, root);
	}

	private void exportGroup(File fExportFile, MutableConfiguration confGroups, BusinessGroup group) {
		MutableConfiguration newGroup = confGroups.addChild(EXPORT_KEY_GROUP);
		newGroup.addAttribute(EXPORT_ATTR_NAME, group.getName());
		if (group.getMinParticipants() != null) newGroup.addAttribute(EXPORT_ATTR_MIN_PARTICIPATS, group.getMinParticipants());
		if (group.getMaxParticipants() != null) newGroup.addAttribute(EXPORT_ATTR_MAX_PARTICIPATS, group.getMaxParticipants());			
		if (group.getWaitingListEnabled() != null) newGroup.addAttribute(EXPORT_ATTR_WAITING_LIST, group.getWaitingListEnabled());
		if (group.getAutoCloseRanksEnabled() != null) newGroup.addAttribute(EXPORT_ATTR_AUTO_CLOSE_RANKS, group.getAutoCloseRanksEnabled());
		newGroup.addChild(EXPORT_KEY_DESCRIPTION, group.getDescription());
		// collab tools
		MutableConfiguration toolsConfig = newGroup.addChild(EXPORT_KEY_COLLABTOOLS);
		CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(group);
		for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
			toolsConfig.addAttribute(CollaborationTools.TOOLS[i], ct.isToolEnabled(CollaborationTools.TOOLS[i]) ? "true" : "false");
		}
		Long calendarAccess = ct.lookupCalendarAccess();
		if (calendarAccess != null) {
			newGroup.addAttribute(EXPORT_KEY_CALENDAR_ACCESS,calendarAccess);			
		}
		Long folderAccess = ct.lookupFolderAccess();
		if(folderAccess != null) {
			newGroup.addAttribute(EXPORT_KEY_FOLDER_ACCESS,folderAccess);
		}
		
		String info = ct.lookupNews();
		if(info!=null && !info.trim().equals("")) {
			newGroup.addAttribute(EXPORT_KEY_NEWS,info.trim());			
		}		
		
		Tracing.logDebug("fExportFile.getParent()=" + fExportFile.getParent(), this.getClass());
		ct.archive(fExportFile.getParent());
		// export membership
		List bgAreas = BGAreaManagerImpl.getInstance().findBGAreasOfBusinessGroup(group);
		for (Iterator iterator = bgAreas.iterator(); iterator.hasNext();) {
			BGArea areaRelation = (BGArea) iterator.next();
			MutableConfiguration newGroupAreaRel = newGroup.addChild(EXPORT_KEY_AREA_RELATION);
			newGroupAreaRel.setValue(areaRelation.getName());
		}
		//export properties
		BusinessGroupPropertyManager bgPropertyManager = new BusinessGroupPropertyManager(group);
		boolean showOwners = bgPropertyManager.showOwners();
		boolean showParticipants = bgPropertyManager.showPartips();
		boolean showWaitingList = bgPropertyManager.showWaitingList();
		
		newGroup.addAttribute(EXPORT_KEY_SHOW_OWNERS, showOwners);		
		newGroup.addAttribute(EXPORT_KEY_SHOW_PARTICIPANTS, showParticipants);		
		newGroup.addAttribute(EXPORT_KEY_SHOW_WAITING_LIST, showWaitingList);		
	}

	private void saveGroupConfiguration(File fExportFile, Dom4jConfiguration root) {
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(fExportFile);
			BufferedOutputStream bos = FileUtils.getBos(fOut);
			root.save(bos);
			bos.flush();
			bos.close();
		} catch (IOException ioe) {
			throw new OLATRuntimeException("Error writing group configuration during group export.", ioe);
		} catch (ConfigurationException cfe) {
			throw new OLATRuntimeException("Error writing group configuration during group export.", cfe);
		} finally {
			FileUtils.closeSafely(fOut);
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#importGroups(org.olat.group.context.BGContext,
	 *      java.io.File)
	 */
	public void importGroups(BGContext context, File fGroupExportXML) {
		if (!fGroupExportXML.exists()) return;

		Configuration groupConfig = null;
		try {
			groupConfig = new XMLConfiguration(fGroupExportXML);
		} catch (ConfigurationException ce) {
			throw new OLATRuntimeException("Error importing group config.", ce);
		}
		if (!groupConfig.getName().equals(EXPORT_KEY_ROOT)) throw new AssertException("Invalid group export file. Root does not match.");

		// get areas
		BGAreaManager am = BGAreaManagerImpl.getInstance();
		Configuration confAreas = groupConfig.getChild(EXPORT_KEY_AREA_COLLECTION);
		if (confAreas != null) {
			List areas = confAreas.getChildren(EXPORT_KEY_AREA);
			for (Iterator iter = areas.iterator(); iter.hasNext();) {
				Configuration area = (Configuration) iter.next();
				String areaName = area.getAttribute(EXPORT_ATTR_NAME);
				String areaDesc = area.getChildValue(EXPORT_KEY_DESCRIPTION);
				am.createAndPersistBGAreaIfNotExists(areaName, areaDesc, context);
			}
		}

		// TODO fg: import group rights

		// get groups
		Configuration confGroups = groupConfig.getChild(EXPORT_KEY_GROUP_COLLECTION);
		if (confGroups != null) {
			BusinessGroupManager gm = BusinessGroupManagerImpl.getInstance();
			List groups = confGroups.getChildren(EXPORT_KEY_GROUP);
			for (Iterator iter = groups.iterator(); iter.hasNext();) {
				// create group
				Configuration group = (Configuration) iter.next();
				String groupName = group.getAttribute(EXPORT_ATTR_NAME);
				String groupDesc = group.getChildValue(EXPORT_KEY_DESCRIPTION);

				// get min/max participants
				Integer groupMinParticipants = null;
				String sMinParticipants = group.getAttribute(EXPORT_ATTR_MIN_PARTICIPATS);
				if (sMinParticipants != null) groupMinParticipants = new Integer(sMinParticipants);
				Integer groupMaxParticipants = null;
				String sMaxParticipants = group.getAttribute(EXPORT_ATTR_MAX_PARTICIPATS);
				if (sMaxParticipants != null) groupMaxParticipants = new Integer(sMaxParticipants);

				// waiting list configuration
				String waitingListConfig = group.getAttribute(EXPORT_ATTR_WAITING_LIST);
				Boolean waitingList = null;
				if (waitingListConfig == null) {
					waitingList = Boolean.FALSE;
				} else {
					waitingList = Boolean.valueOf(waitingListConfig);
				}
				String enableAutoCloseRanksConfig = group.getAttribute(EXPORT_ATTR_AUTO_CLOSE_RANKS);
				Boolean enableAutoCloseRanks = null;
				if (enableAutoCloseRanksConfig == null) {
					enableAutoCloseRanks = Boolean.FALSE;
				} else {
					enableAutoCloseRanks = Boolean.valueOf(enableAutoCloseRanksConfig);
				}
				
				BusinessGroup newGroup = gm.createAndPersistBusinessGroup(context.getGroupType(), null, groupName, groupDesc, groupMinParticipants,
						groupMaxParticipants, waitingList, enableAutoCloseRanks, context);

				// get tools config
				Configuration toolsConfig = group.getChild(EXPORT_KEY_COLLABTOOLS);
				CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(newGroup);
				for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
					String sTool = toolsConfig.getAttribute(CollaborationTools.TOOLS[i]);
					if (sTool != null) ct.setToolEnabled(CollaborationTools.TOOLS[i], sTool.equals("true") ? true : false);
				}
				if(group.getAttribute(EXPORT_KEY_CALENDAR_ACCESS)!=null) {
				  Long calendarAccess = Long.valueOf(group.getAttribute(EXPORT_KEY_CALENDAR_ACCESS));
				  ct.saveCalendarAccess(calendarAccess);				  
				}
				if(group.getAttribute(EXPORT_KEY_FOLDER_ACCESS)!=null) {
				  Long folderAccess = Long.valueOf(group.getAttribute(EXPORT_KEY_FOLDER_ACCESS));
				  ct.saveFolderAccess(folderAccess);				  
				}
				if(group.getAttribute(EXPORT_KEY_NEWS)!=null) {
				  String info = group.getAttribute(EXPORT_KEY_NEWS);
				  ct.saveNews(info);				 
				}

				// get memberships
				List memberships = group.getChildren(EXPORT_KEY_AREA_RELATION);
				for (Iterator iterator = memberships.iterator(); iterator.hasNext();) {
					Configuration areaRelation = (Configuration) iterator.next();
					BGArea area = am.findBGArea(areaRelation.getValue(), context);
					if (area == null) throw new AssertException("Group-Area-Relationship in export, but area was not created during import.");
					am.addBGToBGArea(newGroup, area);
				}
				
				//get properties
				boolean showOwners = true;
				boolean showParticipants = true;
				boolean showWaitingList = true;				
				if(group.getAttribute(EXPORT_KEY_SHOW_OWNERS)!=null) {
					showOwners = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_OWNERS));
				}
				if(group.getAttribute(EXPORT_KEY_SHOW_PARTICIPANTS)!=null) {
					showParticipants = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_PARTICIPANTS));
				}
				if(group.getAttribute(EXPORT_KEY_SHOW_WAITING_LIST)!=null) {
					showWaitingList = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_WAITING_LIST));
				}
				BusinessGroupPropertyManager bgPropertyManager = new BusinessGroupPropertyManager(newGroup);
				bgPropertyManager.updateDisplayMembers(showOwners, showParticipants, showWaitingList);
			}
		}
	}
  
  /**
   * 
   * @see org.olat.group.BusinessGroupManager#moveIdenitFromWaitingListToParticipant(org.olat.core.id.Identity, org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
   */
	public BusinessGroupAddResponse moveIdenityFromWaitingListToParticipant(final List<Identity> choosenIdentities, final Identity ureqIdentity, final BusinessGroup currBusinessGroup, final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
			CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup,new SyncerExecutor(){
				public void execute() {
					for (final Identity identity : choosenIdentities) {
						// check if idenity is allready in participant
						if (!securityManager.isIdentityInSecurityGroup(identity,currBusinessGroup.getPartipiciantGroup()) ) {
							// Idenity is not in participant-list => move idenity from waiting-list to participant-list
							BusinessGroupManagerImpl.this.addParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
							BusinessGroupManagerImpl.this.removeFromWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
							response.getAddedIdentities().add(identity);
							// notification mail is handled in controller
						} else {
							response.getIdentitiesAlreadyInGroup().add(identity);
						}
					}
				}});
		return response;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#getPositionInWaitingListFor(org.olat.core.id.Identity, org.olat.group.BusinessGroup)
	 */
	public int getPositionInWaitingListFor(Identity identity, BusinessGroup businessGroup) {
		// get position in waiting-list
		List identities = securityManager.getIdentitiesAndDateOfSecurityGroup(businessGroup.getWaitingGroup(),true);
		int pos = 0;
		for (int i = 0; i<identities.size(); i++) {
		  Object[] co = (Object[])identities.get(i);
		  Identity waitingListIdentity = (Identity) co[0];
		  if ( waitingListIdentity.getName().equals(identity.getName()) ) {
		  	pos = i+1;// '+1' because list begins with 0 
		  }
		}
		return pos;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#addOwnersAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addOwnersAndFireEvent(Identity ureqIdentity, List<Identity> addIdentities, BusinessGroup currBusinessGroup, BGConfigFlags flags) {
		BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		for (Identity identity : addIdentities) {
			currBusinessGroup = loadBusinessGroup(currBusinessGroup); // reload business group
			if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
				response.getIdentitiesWithoutPermission().add(identity);
			}
			// Check if identity is already in group. make a db query in case
			// someone in another workflow already added this user to this group. if
			// found, add user to model
			else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getOwnerGroup())) {
				response.getIdentitiesAlreadyInGroup().add(identity);
			} else {
	      // identity has permission and is not already in group => add it
				addOwnerAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
				response.getAddedIdentities().add(identity);
				Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(), this.getClass());
			}
		}
		return response;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#addParticipantsAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addParticipantsAndFireEvent(final Identity ureqIdentity, final List<Identity> addIdentities, BusinessGroup acurrBusinessGroup, final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		final BusinessGroup currBusinessGroup = loadBusinessGroup(acurrBusinessGroup); // reload business group
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor(){
			public void execute() {
				for (final Identity identity : addIdentities) {
					if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
						response.getIdentitiesWithoutPermission().add(identity);
					}
					// Check if identity is already in group. make a db query in case
					// someone in another workflow already added this user to this group. if
					// found, add user to model
					else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getPartipiciantGroup())) {
						response.getIdentitiesAlreadyInGroup().add(identity);
					} else {
						// identity has permission and is not already in group => add it
						addParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
						response.getAddedIdentities().add(identity);
						Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(), this.getClass());
					}
				}
			}});
		return response;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#addToWaitingListAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addToWaitingListAndFireEvent(final Identity ureqIdentity, final List<Identity> addIdentities, BusinessGroup acurrBusinessGroup, BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		final BusinessGroup currBusinessGroup = loadBusinessGroup(acurrBusinessGroup); // reload business group
			CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor(){
				public void execute() {
					for (final Identity identity : addIdentities) {	
						if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
							response.getIdentitiesWithoutPermission().add(identity);
						}
						// Check if identity is already in group. make a db query in case
						// someone in another workflow already added this user to this group. if
						// found, add user to model
						else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getWaitingGroup())) {
							response.getIdentitiesAlreadyInGroup().add(identity);
						} else {
							// identity has permission and is not already in group => add it
							BusinessGroupManagerImpl.this.addToWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
							response.getAddedIdentities().add(identity);
							Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(), this.getClass());
						}
					}
				}});
		return response;
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#removeOwnersAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public void removeOwnersAndFireEvent(Identity ureqIdentity, List<Identity> identities, BusinessGroup currBusinessGroup, BGConfigFlags flags) {
		for (Identity identity : identities) {
		  removeOwnerAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
		  Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(), this.getClass());
		}
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#removeParticipantsAndFireEvent(org.olat.core.gui.control.WindowControl, org.olat.core.id.Identity, org.olat.core.gui.translator.Translator, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public void removeParticipantsAndFireEvent(final Identity ureqIdentity, final List<Identity> identities, final BusinessGroup currBusinessGroup, final BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor(){
			public void execute() {
				for (Identity identity : identities) {
				  removeParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
				  Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(), this.getClass());
				}
			}
		});
	}

	/**
	 * 
	 * @see org.olat.group.BusinessGroupManager#removeFromWaitingListAndFireEvent(org.olat.core.id.Identity, org.olat.core.gui.translator.Translator, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public void removeFromWaitingListAndFireEvent(final Identity ureqIdentity, final List<Identity> identities, final BusinessGroup currBusinessGroup, final BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor(){
			public void execute() {
				for (Identity identity : identities) {
				  removeFromWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
				  Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(), this.getClass());
				}
			}
		});
	}

	//////////////////
	// Private Methods
	//////////////////
	/**
	 * Get all learning resources associated with the context from the given group
	 * and buid a direct jump url to this resources
	 * @param group
	 * @return String with direct-jumpin-urls
	 */
	private String getAllLearningResourcesFor(BusinessGroup group) {
		// g
		StringBuilder learningResources = new StringBuilder();
		if (group.getGroupContext() != null) {
			BGContextManager contextManager = BGContextManagerImpl.getInstance();
			List repoEntries = contextManager.findRepositoryEntriesForBGContext(group.getGroupContext());
			Iterator iter = repoEntries.iterator();
			while (iter.hasNext()) {
				RepositoryEntry entry = (RepositoryEntry) iter.next();
				String title = entry.getDisplayname();
				String url = RepoJumpInHandlerFactory.buildRepositoryDispatchURI(entry);
				learningResources.append(title);
				learningResources.append("\n");
				learningResources.append(url);
				learningResources.append("\n\n");
			}
		}
		return learningResources.toString();
  }

	private void addToRoster(Identity ureqIdentity, Identity identity, BusinessGroup group, BGConfigFlags flags) {
		if (flags.isEnabled(BGConfigFlags.BUDDYLIST)) {
			if (InstantMessagingModule.isEnabled()) {
				//evaluate whether to sync or not
				boolean syncBuddy = InstantMessagingModule.getAdapter().getConfig().isSyncPersonalGroups();
				boolean isBuddy = group.getType().equals(BusinessGroup.TYPE_BUDDYGROUP);
				
				boolean syncLearn = InstantMessagingModule.getAdapter().getConfig().isSyncLearningGroups();
				boolean isLearn = group.getType().equals(BusinessGroup.TYPE_LEARNINGROUP);
				
				//only sync when a group is a certain type and this type is configured that you want to sync it
				if ((syncBuddy && isBuddy) || (syncLearn && isLearn)) { 
					String groupID = InstantMessagingModule.getAdapter().createChatRoomString(group);
					String groupDisplayName = group.getName();
					//course group enrolment is time critial so we move this in an separate thread and catch all failures 
					TaskExecutorManager.getInstance().runTask(new SyncSingleUserTask(ureqIdentity, groupID, groupDisplayName, identity));
				}
			}
		}
	}

	private void removeFromRoster(Identity identity, BusinessGroup group, BGConfigFlags flags) {
		if (flags.isEnabled(BGConfigFlags.BUDDYLIST)) {
			if (InstantMessagingModule.isEnabled()) {
				// only remove user from roster if not in other security group
				if (!isIdentityInBusinessGroup(identity, group)) {
					String groupID = InstantMessagingModule.getAdapter().createChatRoomString(group);
					InstantMessagingModule.getAdapter().removeUserFromFriendsRoster(groupID, identity.getName());
				}
			}
		}
	}

	/**
	 * Transfer first identity of waiting.list (if there is one) to the participant-list.
	 * Not thread-safe! Do call this method only from a synchronized block!
	 * @param wControl
	 * @param ureq
	 * @param trans
	 * @param identity
	 * @param group
	 * @param flags
	 * @param logger
	 * @param secMgr
	 */
	//o_clusterOK by:cg call this method only from synchronized code-block (removeParticipantAndFireEvent( ).
	private void transferFirstIdentityFromWaitingToParticipant(Identity ureqIdentity, BusinessGroup group, BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		// Check if waiting-list is enabled and auto-rank-up
		if (group.getWaitingListEnabled().booleanValue() && group.getAutoCloseRanksEnabled().booleanValue()) {
			// Check if participant is not full
			Integer maxSize = group.getMaxParticipants();
			int waitingPartipiciantSize = securityManager.countIdentitiesOfSecurityGroup(group.getPartipiciantGroup());
			if ( (maxSize != null) && (waitingPartipiciantSize < maxSize.intValue()) ) {
				// ok it has free places => get first idenity from Waitinglist
				List identities = securityManager.getIdentitiesAndDateOfSecurityGroup(group.getWaitingGroup(), true/*sortedByAddedDate*/);
				int i = 0;
				boolean transferNotDone = true;
			  while (i<identities.size() && transferNotDone) {
			  	// It has an identity and transfer from waiting-list to participant-group is not done
					Object[] co = (Object[])identities.get(i++);
					Identity firstWaitingListIdentity = (Identity) co[0];
					//reload group
					group = (BusinessGroup)DBFactory.getInstance().loadObject(group, true);
					// Check if firstWaitingListIdentity is not allready in participant-group
					if (!securityManager.isIdentityInSecurityGroup(firstWaitingListIdentity,group.getPartipiciantGroup())) {
						// move the identity from the waitinglist to the participant group
						
						ActionType formerStickyActionType = ThreadLocalUserActivityLogger.getStickyActionType();
						try{
							// OLAT-4955: force add-participant and remove-from-waitinglist logging actions 
							//            that get triggered in the next two methods to be of ActionType admin
							//            This is needed to make sure the targetIdentity ends up in the o_loggingtable
							ThreadLocalUserActivityLogger.setStickyActionType(ActionType.admin);
							this.addParticipantAndFireEvent(ureqIdentity, firstWaitingListIdentity, group, flags, false);
							this.removeFromWaitingListAndFireEvent(ureqIdentity, firstWaitingListIdentity, group, false);
						} finally {
							ThreadLocalUserActivityLogger.setStickyActionType(formerStickyActionType);
						}
						// send a notification mail if available
						MailTemplate mailTemplate = BGMailHelper.createWaitinglistTransferMailTemplate(group, ureqIdentity);
						if (mailTemplate != null) {
							MailerWithTemplate mailer = MailerWithTemplate.getInstance();
							MailerResult mailerResult = mailer.sendMail(firstWaitingListIdentity, null, null, mailTemplate, null);
							// Does not report errors to current screen because this is the identity who triggered the transfer
							Tracing.logWarn("Could not send WaitinglistTransferMail for identity=" + firstWaitingListIdentity.getName() , BusinessGroupManagerImpl.class);
						}						
						transferNotDone = false;
				  }
				}
			}
		} else {
			Tracing.logWarn("Called method transferFirstIdentityFromWaitingToParticipant but waiting-list or autoCloseRanks is disabled.", BusinessGroupManagerImpl.class);
		}
	}

	/**
	 * Delete all entries as participant, owner and waiting-list for certain identity.
	 * If there is no other owner for a group, the olat-administrator (defined in spring config) will be added as owner.
	 *   
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		// remove as Participant 
		List attendedGroups = findAllBusinessGroupsAttendedBy(identity);
		for (Iterator iter = attendedGroups.iterator(); iter.hasNext();) {
			securityManager.removeIdentityFromSecurityGroup(identity, ((BusinessGroup)iter.next()).getPartipiciantGroup());
		}
		Tracing.logDebug("Remove partipiciant identity=" + identity + " from " + attendedGroups.size() + " groups", this.getClass());
		// remove from waitinglist 
		List waitingGroups = findBusinessGroupsWithWaitingListAttendedBy(identity);
		for (Iterator iter = waitingGroups.iterator(); iter.hasNext();) {
			securityManager.removeIdentityFromSecurityGroup(identity, ((BusinessGroup)iter.next()).getWaitingGroup());
		}
		Tracing.logDebug("Remove from waiting-list identity=" + identity + " in " + waitingGroups.size() + " groups", this.getClass());

		// remove as owner
		List ownerGroups = findAllBusinessGroupsOwnedBy(identity);
		for (Iterator iter = ownerGroups.iterator(); iter.hasNext();) {
			BusinessGroup businessGroup = (BusinessGroup) iter.next();
			securityManager.removeIdentityFromSecurityGroup(identity, businessGroup.getOwnerGroup());
			if (businessGroup.getType().equals(BusinessGroup.TYPE_BUDDYGROUP) && securityManager.countIdentitiesOfSecurityGroup(businessGroup.getOwnerGroup()) == 0 ) {
				// Buddygroup has no owner anymore => add OLAT-Admin as owner
				securityManager.addIdentityToSecurityGroup(UserDeletionManager.getInstance().getAdminIdentity(), businessGroup.getOwnerGroup());
				Tracing.logInfo("Delete user-data, add Administrator-identity as owner of businessGroup=" + businessGroup.getName(), this.getClass());
			}
		}
		Tracing.logDebug("Remove owner identity=" + identity + " from " + ownerGroups.size() + " groups", this.getClass());
		Tracing.logDebug("All entries in groups deleted for identity=" + identity, this.getClass());
	}

	private List findAllBusinessGroupsOwnedBy(Identity identity) {
		return findBusinessGroupsOwnedBy(null, identity, null);
	}

	private List findAllBusinessGroupsAttendedBy(Identity identity) {
		return findBusinessGroupsAttendedBy(null, identity, null);
	}
	
	private List findBusinessGroupsWithWaitingListAttendedBy(Identity identity) {
		return findBusinessGroupsWithWaitingListAttendedBy(null, identity, null);
	}

	public void archiveGroups(BGContext context, File exportFile) {
		BusinessGroupArchiver.getInstance().archiveBGContext(context, exportFile);		
	}
	
	private void removeSubscriptions(Identity identity, BusinessGroup group) {
		NotificationsManager notiMgr = NotificationsManager.getInstance();
		List<Subscriber> l = notiMgr.getSubscribers(identity);
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			Subscriber subscriber = (Subscriber) iterator.next();
			Long resId = subscriber.getPublisher().getResId();
			Long groupKey = group.getKey();
			if (resId != null && groupKey != null && resId.equals(groupKey)) {
				notiMgr.unsubscribe(subscriber);
			}
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#setLastUsageFor(org.olat.group.BusinessGroup)
	 */
	public void setLastUsageFor(final BusinessGroup currBusinessGroup) {
		//o_clusterOK by:cg
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor(){
			public void execute() {
				// force a reload from db loadObject(..., true) by evicting it from
				// hibernates session
				// cache to catch up on a different thread having commited the update of
				// the launchcounter
				BusinessGroup reloadedBusinessGroup = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(currBusinessGroup);
				reloadedBusinessGroup.setLastUsage(new Date());
				LifeCycleManager.createInstanceFor(reloadedBusinessGroup).deleteTimestampFor(GroupDeletionManager.SEND_DELETE_EMAIL_ACTION);
				BusinessGroupManagerImpl.getInstance().updateBusinessGroup(reloadedBusinessGroup);
			}
		});
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#createUniqueBusinessGroupsFor(java.util.Set, org.olat.group.context.BGContext, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Boolean, java.lang.Boolean)
	 */
	public Set<BusinessGroup> createUniqueBusinessGroupsFor(final Set<String> allNames, final BGContext bgContext, final String bgDesc, final Integer bgMin, 
			final Integer bgMax, final Boolean enableWaitinglist, final Boolean enableAutoCloseRanks) {
	   //o_clusterOK by:cg
		Set<BusinessGroup> createdGroups = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(bgContext, new SyncerCallback<Set<BusinessGroup>>(){
	      public Set<BusinessGroup> execute() {
					if(checkIfOneOrMoreNameExistsInContext(allNames, bgContext)){
						// set error of non existing name
						return null;
					} else {
						// create bulkgroups only if there is no name which already exists.
						Set<BusinessGroup> newGroups = new HashSet<BusinessGroup>();
						for (Iterator<String> iter = allNames.iterator(); iter.hasNext();) {
							String bgName = iter.next();
							BusinessGroup newGroup = createAndPersistBusinessGroup(bgContext.getGroupType(), null, bgName, bgDesc, bgMin, bgMax,
									enableWaitinglist, enableAutoCloseRanks, bgContext);
							newGroups.add(newGroup);
						}
						return newGroups;
					}
	      }
		});
		return createdGroups;
	}
	
	public void registerDeletableGroupDataListener(DeletableGroupData listener) {
		this.deleteListeners.add(listener);
	}

	@Override
	public List<String> getDependingDeletablableListFor(BusinessGroup currentGroup, Locale locale) {
		List<String> deletableList = new ArrayList<String>();
		for (DeletableGroupData deleteListener : deleteListeners) {
			DeletableReference deletableReference = deleteListener.checkIfReferenced(currentGroup, locale);
			if (deletableReference.isReferenced()) {
				deletableList.add(deletableReference.getName());
			}
		}
		return deletableList;
	}

}
