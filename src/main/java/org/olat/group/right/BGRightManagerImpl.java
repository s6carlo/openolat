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
*/

package org.olat.group.right;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.Grant;
import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.core.commons.persistence.DB;
import org.olat.core.manager.BasicManager;
import org.olat.group.BusinessGroup;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Description:<BR>
 * 
 * Initial Date: Aug 24, 2004
 * 
 * @author gnaegi
 */
@Service("rightManager")
public class BGRightManagerImpl extends BasicManager implements BGRightManager {

	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;

	/**
	 * @see org.olat.group.right.BGRightManager#addBGRight(java.lang.String,
	 *      org.olat.group.BusinessGroup)
	 */
	@Override
	public void addBGRight(String bgRight, BusinessGroup rightGroup, BGRightsRole roles) {
		Group baseGroup = rightGroup.getBaseGroup();
		List<OLATResource> resources = findResources(baseGroup);
		for(OLATResource resource:resources) {
			if(roles == BGRightsRole.participant) {
				groupDao.addGrant(baseGroup, GroupRoles.participant.name(), bgRight, resource);
			} else if(roles == BGRightsRole.tutor) {
				groupDao.addGrant(baseGroup, GroupRoles.coach.name(), bgRight, resource);
			}
		}
	}
	
	@Override
	public void addBGRight(String bgRight, BusinessGroup rightGroup, OLATResource resource, BGRightsRole roles) {
		if(roles == BGRightsRole.participant) {
			groupDao.addGrant(rightGroup.getBaseGroup(), GroupRoles.participant.name(), bgRight, resource);
		} else if(roles == BGRightsRole.tutor) {
			groupDao.addGrant(rightGroup.getBaseGroup(), GroupRoles.coach.name(), bgRight, resource);
		}
	}

	/**
	 * @see org.olat.group.right.BGRightManager#removeBGRight(java.lang.String,
	 *      org.olat.group.BusinessGroup)
	 */
	@Override
	public void removeBGRight(String bgRight, BusinessGroup rightGroup, OLATResource resource, BGRightsRole roles) {
		if(roles == BGRightsRole.participant) {
			groupDao.removeGrant(rightGroup.getBaseGroup(), GroupRoles.participant.name(), bgRight, resource);
		} else if (roles == BGRightsRole.tutor) {
			groupDao.removeGrant(rightGroup.getBaseGroup(), GroupRoles.coach.name(), bgRight, resource);
		}
	}
	
	@Override
	public void removeBGRights(BusinessGroup rightGroup, OLATResource resource, BGRightsRole role) {
		if(role == BGRightsRole.tutor) {
			groupDao.removeGrants(rightGroup.getBaseGroup(), GroupRoles.coach.name(), resource);
		} else if(role == BGRightsRole.participant) {
			groupDao.removeGrants(rightGroup.getBaseGroup(), GroupRoles.participant.name(), resource);
		}
	}

	@Override
	public void removeBGRights(Collection<BusinessGroup> groups, OLATResource resource) {
		for(BusinessGroup group:groups) {
			groupDao.removeGrants(group.getBaseGroup(), GroupRoles.coach.name(), resource);
			groupDao.removeGrants(group.getBaseGroup(), GroupRoles.participant.name(), resource);
		}
	}

	@Override
	public boolean hasBGRight(String bgRight, IdentityRef identity, OLATResource resource) {
		return groupDao.hasGrant(identity, bgRight, resource);
	}
	
	@Override
	public List<String> getBGRights(IdentityRef identity, OLATResource resource) {
		return groupDao.getPermissions(identity, resource);
	}

	/**
	 * @see org.olat.group.right.BGRightManager#findBGRights(org.olat.group.BusinessGroup)
	 */
	@Override
	public List<String> findBGRights(BusinessGroup group, BGRightsRole role) {
		GroupRoles groupRole = null;
		if(role == BGRightsRole.tutor) {
			groupRole = GroupRoles.coach;
		} else if(role == BGRightsRole.participant) {
			groupRole = GroupRoles.participant;
		} else {
			return Collections.emptyList();
		}
		
		List<Grant> grants = groupDao.getGrants(group.getBaseGroup(), groupRole.name());
		// filter all business group rights permissions. group right permissions
		// start with bgr.
		List<String> rights = new ArrayList<String>();
		for (Grant grant:grants) {
			String right = grant.getPermission();
			if (right.indexOf(BG_RIGHT_PREFIX) == 0) {
				rights.add(right);
			}
		}
		return rights;
	}
	
	@Override
	public List<BGRights> findBGRights(List<BusinessGroup> groups, OLATResource resource) {
		Map<Group,BusinessGroup> secToGroupMap = new HashMap<Group,BusinessGroup>();
		List<Group> baseGroups = new ArrayList<>(groups.size());
		for(BusinessGroup group:groups) {
			baseGroups.add(group.getBaseGroup());
			secToGroupMap.put(group.getBaseGroup(), group);
		}
		List<BGRights> rights = new ArrayList<BGRights>();
		List<Grant> grants = groupDao.getGrants(baseGroups, resource);
		rights.addAll(findBGRights(grants, secToGroupMap, resource));
		return rights;
	}
	
	private List<OLATResource> findResources(Group group) {
		StringBuilder sb = new StringBuilder();
		sb.append(" select resource from repoentrytogroup as rel")
		  .append(" inner join rel.entry as entry")
		  .append(" inner join entry.olatResource as resource")
		  .append(" where rel.group.key=:groupKey");
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), OLATResource.class)
			.setParameter("groupKey", group.getKey())
			.getResultList();
	}
	
	private List<BGRights> findBGRights(List<Grant> grants, Map<Group,BusinessGroup> secToGroupMap, OLATResource resource) {
		List<BGRights> rights = new ArrayList<BGRights>();
		Map<BGRights,BGRights> rightsMap = new HashMap<BGRights,BGRights>();
		for (Grant grant:grants) {
			String right = grant.getPermission();
			if (right.indexOf(BG_RIGHT_PREFIX) == 0 && grant.getResource().equals(resource)) {
				BusinessGroup group = secToGroupMap.get(grant.getGroup());
				
				BGRightsRole role = null;
				if(GroupRoles.participant.name().equals(grant.getRole())) {
					role = BGRightsRole.participant;
				} else if(GroupRoles.coach.name().equals(grant.getRole())) {
					role = BGRightsRole.tutor;
				}
				BGRights wrapper = new BGRightsImpl(group.getKey(), role);
				if(rightsMap.containsKey(wrapper)) {
					wrapper = rightsMap.get(wrapper);
				} else {
					rightsMap.put(wrapper, wrapper);
				}
				wrapper.getRights().add(right);
				rights.add(wrapper);
			}
		}
		
		return rights;
	}

	@Override
	public boolean hasBGRight(List<BusinessGroup> groups) {
		if(groups == null || groups.isEmpty()) return false;
		
		List<Group> secGroups = new ArrayList<>(groups.size());
		for(BusinessGroup group:groups) {
			secGroups.add(group.getBaseGroup());
		}
		
		List<Grant> grants = groupDao.getGrants(secGroups);
		// filter all business group rights permissions. group right permissions
		// start with bgr.
		for (Grant grant:grants) {
			String right = grant.getPermission();
			if (right.indexOf(BG_RIGHT_PREFIX) == 0) {
				return true;
			}
		}
		return false;
	}
}
