// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.kurento.khc.datamodel;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.utils.FileRepository;

@Component("groupDao")
public class GroupDao extends BaseDao {

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private ConversationDao conversationDao;

	@Autowired
	private ContentDao contentDao;

	@Autowired
	private TimelineDao timelineDao;

	@Autowired
	private FileRepository repository;

	@Transactional
	public GroupEntity createAccountGroup(GroupEntity group,
			AccountEntity account) {

		// Create group entity
		group.setAutomanaged(true);
		super.save(group);

		// Attach account
		AccountEntity dbAccount = accountDao.attach(account);
		group.setAccount(dbAccount);

		// Create conversation
		ConversationEntity conversation = conversationDao
				.createConversation(new ConversationEntity());
		group.setConversation(conversation);

		em.flush(); // Group will be searched while adding admin
		return group;
	}

	@Transactional
	public GroupEntity createAutomanagedGroup(GroupEntity group,
			AccountEntity account, UserEntity owner) {

		createAccountGroup(group, account);
		UserEntity dbOwner = userDao.attach(owner);
		addGroupAdmin(group, dbOwner);
		em.flush();

		return group;
	}


	@Transactional
	public GroupEntity updateGroup(GroupEntity group) {

		em.detach(group);
		GroupEntity dbGroup = attach(group);

		if (group.getName() != null) {
			dbGroup.setName(group.getName());
		}
		if (group.getPhone() != null) {
			dbGroup.setPhone(group.getPhone());
		}
		if (group.getPhoneRegion() != null) {
			dbGroup.setPhoneRegion(group.getPhoneRegion());
		}

		return dbGroup;
	}

	@Transactional
	public void deleteGroup(GroupEntity group) {

		GroupEntity dbGroup = attach(group);

		// Cascaded content deletion

		// Delete Timelines and conversation
		dbGroup.setConversation(null); // Unlink so conversation can be deleted
										// when last timeline is deleted
		em.flush();
		List<TimelineEntity> timelines = timelineDao.getGroupTimelines(dbGroup);
		for (TimelineEntity timeline : timelines) {
			timelineDao.deleteTimeline(timeline);
		}

		// Clear relationships
		dbGroup.setAccount(null);
		dbGroup.getMembers().clear();
		dbGroup.getAdmins().clear();

		super.delete(dbGroup);

	}

	@Transactional(noRollbackFor = { KhcNotFoundException.class,
			FileNotFoundException.class })
	public ContentEntity getPicture(GroupEntity group)
			throws FileNotFoundException {
		final GroupEntity dbGroup = attach(group);
		ContentEntity picture;
		if ((picture = dbGroup.getPicture()) != null) {
			repository.getMediaFile(picture.getContentUrl());
			return picture;
		} else {
			throw new KhcNotFoundException("Unable to find group avatar",
					KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
					ContentEntity.class.getSimpleName(),
					new HashMap<String, String>() {
						private static final long serialVersionUID = 1L;
						{
							put("uuid", "" + dbGroup.getUUID());
						}
					});
		}
	}

	@Transactional
	public void setPicture(GroupEntity group, ContentEntity picture) {

		GroupEntity dbGroup = attach(group);
		ContentEntity currentPic = dbGroup.getPicture();
		ContentEntity dbPicture = null;

		if (picture != null) {
			dbPicture = contentDao.attach(picture);
		}
		dbGroup.setPicture(dbPicture);

		if (currentPic != null) {
			contentDao.deleteContent(currentPic);
		}
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public GroupEntity findGroupById(Long id) {
		return findSingle(GroupEntity.class, new String[] { "id" },
				new Object[] { id });

	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public GroupEntity findGroupByUUID(Long uuid) {
		return findSingle(GroupEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public GroupEntity findGroupByName(String name) {
		return findSingle(GroupEntity.class, new String[] { "name" },
				new Object[] { name });
	}

	public AccountEntity getGroupAccount(GroupEntity group) {
		GroupEntity dbGroup = attach(group);
		return dbGroup.getAccount();
	}

	@Transactional
	public void addGroupMember(GroupEntity group, UserEntity user) {
		GroupEntity dbGroup = attach(group);
		UserEntity dbUser = userDao.attach(user);

		// Add user to member list
		dbGroup.getMembers().put(dbUser.getId(), dbUser);

		// Add User's timeline for this group's conversation
		// Timeline will remain IDLE until a message is received
		timelineDao.createTimeline(dbUser, dbGroup);

		em.flush();
	}

	@Transactional
	public void removeGroupMember(GroupEntity group, UserEntity user) {

		// Lock for writin. This is added as deadlock has been found
		GroupEntity dbGroup = attach(group);
		em.lock(dbGroup, LockModeType.PESSIMISTIC_WRITE);
		UserEntity dbUser = userDao.attach(user);

		// Delete timeline when leaving the group
		TimelineEntity timeline = dbUser.getTimelines().remove(
				dbGroup.getUUID());
		if (timeline != null) {
			timelineDao.deleteTimeline(timeline);
		}

		// Remove user from admin list
		dbGroup.getAdmins().remove(dbUser.getId());
		// Remove user from member list
		dbGroup.getMembers().remove(dbUser.getId());
		// Interaction below isl be automatically fixed by JPA and
		// it is actually useless from JPA point of view. It is performed
		// in order to have the actual list of groups for member if they
		// are checked in this same transaction
		dbUser.getGroups().remove(dbGroup.getId());

		em.flush();
	}

	@Transactional
	public List<UserEntity> getGroupMembers(GroupEntity group) {
		GroupEntity dbGroup = attach(group);
		return new ArrayList<UserEntity>(dbGroup.getMembers().values());
	}

	@Transactional
	public void addGroupAdmin(GroupEntity group, UserEntity user) {
		GroupEntity dbGroup = attach(group);
		UserEntity dbUser = userDao.attach(user);

		// All admins are required to be members
		addGroupMember(dbGroup, dbUser);
		dbGroup.getAdmins().put(dbUser.getId(), dbUser);
		em.flush();
	}

	@Transactional
	public void removeGroupAdmin(GroupEntity group, UserEntity user) {
		GroupEntity dbGroup = attach(group);
		UserEntity dbUser = userDao.attach(user);

		dbGroup.getAdmins().remove(dbUser.getId());
	}

	@Transactional
	public List<UserEntity> getGroupAdmins(GroupEntity group) {
		GroupEntity dbGroup = attach(group);
		return new ArrayList<UserEntity>(dbGroup.getAdmins().values());
	}

	@Transactional
	public List<UserEntity> getGroupMembersNotAdmins(GroupEntity group) {
		GroupEntity dbGroup = attach(group);
		List<UserEntity> nonAdmins = new ArrayList<UserEntity>();
		for (UserEntity member : dbGroup.getMembers().values()) {
			if (!dbGroup.getAdmins().containsKey(member.getId())) {
				nonAdmins.add(member);
			}
		}
		return nonAdmins;
	}

	// /////////////////////////////
	// Security verifications
	// ////////////////////////////

	@Transactional
	public Boolean isGroupMember(GroupEntity group, UserEntity member) {
		try {
			GroupEntity dbGroup = attach(group);
			UserEntity dbMember = userDao.attach(member);
			if (dbMember.getGroups().containsKey(dbGroup.getId())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean isGroupAdmin(GroupEntity group, UserEntity admin) {
		try {
			GroupEntity dbGroup = attach(group);
			UserEntity dbUser = userDao.attach(admin);
			AccountEntity acc;
			if (admin.isRoot()) {
				return true;
			} else if (dbGroup.getAdmins().containsKey(dbUser.getId())) {
				return true;
			} else if ((acc = dbGroup.getAccount()) != null) {
				return accountDao.isAccountAdmin(acc, admin);
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}



	@Transactional
	public Boolean hasSameAccount(GroupEntity group, UserEntity user) {
		UserEntity dbUser;
		GroupEntity dbGroup;
		try {
			dbUser = userDao.attach(user);
			dbGroup = attach(group);
		} catch (Exception e) {
			return false;
		}
		// Automanaged groups must comply account membership
		if (!dbGroup.getAccount().getId().equals(dbUser.getAccount().getId())) {
			return false;
		}
		return true;
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	protected GroupEntity attach(final GroupEntity group) {

		Assert.notNull(group);
		if (em.contains(group)) {
			return group;
		} else {
			GroupEntity dbGroup;
			if ((dbGroup = em.find(GroupEntity.class, group.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown group to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						GroupEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + group.getUUID());
							}
						});
			} else {
				return dbGroup;
			}
		}
	}
}
