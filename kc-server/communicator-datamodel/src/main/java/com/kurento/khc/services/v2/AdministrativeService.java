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

package com.kurento.khc.services.v2;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

import com.kurento.agenda.datamodel.pojo.Account;
import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.datamodel.pojo.PasswordRecovery;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;

public interface AdministrativeService {

	// ///////////////////////////////////////////////////////////
	// Account services
	// ///////////////////////////////////////////////////////////

	/**
	 * Creates a new Account and returns its public ID.
	 *
	 * @return result Account's
	 */
	Account createAccount(Account account);

	/**
	 * Deletes the account identified by the public ID. Datamodel under this
	 * account is deleted and not recoverable
	 *
	 * @param accountId
	 *            Public ID of the account to be deleted
	 */
	void deleteAccount(Long accountId);

	/**
	 * Updates the account record with provided data. Relationships with other
	 * entities can not be modified with this method
	 *
	 * @param account
	 *            Account POJO with new values to modify account
	 */
	void updateAccount(Account account);

	/**
	 * Updates the account avatars record with provided data. Relationships with other
	 * entities can not be modified with this method
	 *
	 * @param account
	 *            Account POJO with new values to modify account
	 * @param content
	 *            content is a picture of avatar to record
	 */
	void updateAccountAvatar(Account account, Content content);

	/**
	 * Returns the account entity associated to given name
	 *
	 * @param accountName
	 *            Name of requested account
	 * @return Account record
	 */
	Account getAccount(String accountName);

	/**
	 * Returns the account entity associated to public ID
	 *
	 * @param accountId
	 *            Public ID of requested account
	 * @return Account record
	 */
	Account getAccount(Long accountId);

	/**
	 * Returns the list of system accounts
	 *
	 * @return list of account POJOs
	 */
	List<Account> getAccounts();

	/**
	 * Returns public account info
	 *
	 * @param accountId
	 *            Public account ID
	 * @return Account record
	 */
	Account getAccountInfo(Long accountId);

	Account getAccountInfo(String accountName);

	/**
	 * Returns the list of users with administration permissions on given
	 * account
	 *
	 * @param accountId
	 *            Public account ID
	 * @return List of users with administration permission on given account
	 */
	List<User> getAccountAdmins(Long accountId);

	/**
	 * Returns the list of users belonging to given account
	 *
	 * @param accountId
	 *            Public account ID
	 * @param firstResult
	 *            How many records are skiped
	 * @param maxResult
	 *            how mny records are returned as maximum
	 * @return List of users belongin to given account
	 */
	List<User> getAccountUsers(Long accountId, Integer firstResult,
			Integer maxResult);

	/**
	 * Returns the list of groups belonging to given account
	 *
	 * @param accountId
	 *            Public account ID
	 * @param firstResult
	 *            How many records are skipped
	 * @param maxResult
	 *            How many records are returned as maximum
	 * @return List of groups belonging to given account
	 */
	List<Group> getAccountGroups(Long accountId, Integer firstResult,
			Integer maxResult);

	/**
	 * Returns the list of groups belonging to given account whose name contains
	 * pattern
	 *
	 * @param accountId
	 *            Public account ID
	 * @param pattern
	 *            String that will be searched in fields name
	 * @param firstResult
	 *            How many records are skiped
	 * @param maxResult
	 *            how mny records are returned as maximum
	 * @return List of groups belongin to given account
	 */
	List<Group> searchAccountGroupsByFilter(Long accountId, String pattern,
			Integer firstResult, Integer maxResult);

	/**
	 * Returns the list of calls by range of dates between firstResult and maxResult
	 *
	 * @param accountId
	 *            Public account ID
	 * @param startDate
	 *            Initial date to get calls
	 * @param endDate
	 *            End date to get calls
	 * @param firstResult
	 *            How many records are skiped
	 * @param maxResult
	 *            how mny records are returned as maximum
	 * @return List of calls and theirs localId
	 */
	List<Call> searchAccountCallsByDate(Long accountId, Date startDate,
			Date endDate, Integer firstResult, Integer maxResult);

	/**
	 * Returns the list of calls by range of dates
	 *
	 * @param accountId
	 *            Public account ID
	 * @param startDate
	 *            Initial date to get calls
	 * @param endDate
	 *            End date to get calls
	 * @return List of calls and theirs localId
	 */
	List<Call> searchAccountAllCallsByDate(Long accountId, Date startDate, Date endDate);

	/**
	 * Returns the list of users without administration permission for given
	 * account
	 *
	 * @param accountId
	 *            Public account ID
	 * @return List of users without administration permission
	 */
	List<User> getAccountUsersNotAdmins(Long accountId);

	/**
	 * Promotes an user as account administrator
	 *
	 * @param accountId
	 *            Public account ID where becomes administrator
	 * @param userId
	 *            Public user ID promoted to account administrator
	 */
	void addAccountAdmin(Long accountId, Long userId);

	/**
	 * Removes administration permission from user
	 *
	 * @param accountId
	 *            Public account ID from where administration permission are
	 *            removed
	 * @param userId
	 *            Public user ID to whom administration permission are removed
	 */
	void removeAccountAdmin(Long accountId, Long userId);

	/**
	 * Returns true if logged user has permission to administer given account
	 *
	 * @param userId
	 * @return
	 */
	Boolean hasPermissionAdminAccount(Long accountId);

	/**
	 * Returns the list of users belonging to given account whose name, surname,
	 * email or phone contains pattern
	 *
	 * @param accountId
	 *            Public account ID
	 * @param pattern
	 *            String that will be searched in fields name, surname, email
	 *            and phone
	 * @param firstResult
	 *            How many records are skiped
	 * @param maxResult
	 *            how mny records are returned as maximum
	 * @return List of users belongin to given account
	 */
	List<User> searchAccountUsersByFilter(Long accountId, String pattern,
			Integer firstResult, Integer maxResult);

	/**
	 * Returns the list of users belonging to given account that do not belong
	 * to the given group
	 *
	 * @param accountId
	 *            Public account ID
	 * @param groupId
	 *            Public group ID
	 * @return List of users belongin to given account that are not members of
	 *         group
	 */
	List<User> getAccountUsersNotInGroup(Long accountId, Long groupId,
			Integer firstResult, Integer maxResult);

	// ///////////////////////////////////////////////////////////
	// Group services
	// ///////////////////////////////////////////////////////////

	/**
	 * Creates a group directly associated to one account. It can be called by
	 * any user with administration permissions
	 *
	 * @param group
	 *            Data of group to be created
	 * @param accountId
	 *            Public account ID where group is to be created
	 * @return Newly created group
	 */
	Group createAccountGroup(Group group, Long accountId);

	/**
	 * Returns group POJO identified by groupId
	 *
	 * @param groupId
	 *            Public ID of group to be retrieved
	 * @return group POJO
	 */
	Group getGroup(Long groupId);

	/**
	 * Return group's avatar
	 *
	 * @param groupId
	 *            Public ID of group from where avatar is requested
	 * @return Content POJO with media URL and MIME type
	 *
	 * @throws FileNotFoundException
	 *             Thrown if avatar file is not found in repository
	 */
	Content getGroupAvatar(Long groupId) throws FileNotFoundException;

	/**
	 * Returns the list of members of group identified by groupId
	 *
	 * @param groupId
	 *            Public ID of group from where list of members is requested
	 * @return List of user POJOs members of group
	 */
	List<User> getGroupMembers(Long groupId);

	/**
	 * Returns the group's list of members with administration permission
	 *
	 * @param groupId
	 *            Public ID of group from where list of administrators is
	 *            requested
	 * @return List of user POJOs admins of group
	 */
	List<User> getGroupAdmins(Long groupId);

	/**
	 * Returns the group's list of members without administration permission
	 *
	 * @param groupId
	 *            Public ID of group from where list of members not admins is
	 *            requested
	 * @return List of user POJOs admins of group
	 */
	List<User> getGroupMembersNotAdmins(Long groupId);

	// ///////////////////////////////////////////////////////////
	// User services
	// ///////////////////////////////////////////////////////////

	/**
	 * Creates an user record and adds it to a given account. Only authenticated
	 * users can access this service and require permission CREATE_USER for the
	 * account
	 *
	 * @param user
	 *            POJO with user data
	 * @param accountId
	 *            Public account ID where user is inserted
	 * @return Public ID of new user record
	 */
	User createUserInAccount(User user, Long accountId);

	/**
	 * Allows user creation in a given account, without associating it to any
	 * organization. Anonymous requests are accepted, but requires the account
	 * to have active flag <code>userAutoregister</code>, otherwise access is
	 * denied
	 *
	 * @param user
	 *            User's data to be inserted in database
	 * @param accountId
	 *            Public account ID where user has to be inserted
	 * @return Public ID of newly created user
	 */
	User createUserInAutoregister(User user, Long accountId);

	/**
	 * Allows user creation in a given account, including user's picture.
	 * Anonymous requests are accepted, but requires the account to have active
	 * flag <code>userAutoregister</code>, otherwise access is denied
	 *
	 * @param user
	 *            User's data to be inserted in database
	 * @param accountId
	 *            Public account ID where user has to be inserted
	 * @param content
	 *            Reference to the user's avatar content stored in repository
	 * @return Public ID of newly created user
	 */
	User createUserInAutoregister(User user, Long accountId, Content content);

	/**
	 * Delete a user from the system. The record is removed from its parent
	 * account, organizations and groups where has memberships. This service
	 * requires an authenticated user with permission DELETE_USER for the record
	 * itself. This normally will be only the account administrator
	 *
	 * @param userId
	 *            Public ID of user to delete
	 */
	void deleteUser(Long userId);

	/**
	 * Gets the user record for the given public ID
	 *
	 * @param userId
	 *            public ID of requested user
	 * @return User record associated to given ID
	 */
	User getUser(Long userId);

	/**
	 * Returns user's avatar
	 *
	 * @param userId
	 *            public Id of user whose avatar is requested
	 * @return Content reference to picture avatar
	 * @throws FileNotFoundException
	 */
	Content getUserAvatar(Long userId) throws FileNotFoundException;

	/**
	 * Gets the user record for authenticated user
	 *
	 * @param username
	 *            Username of user to be finded
	 * @return User record associated to given username
	 */
	User getMe();

	/**
	 * Returns a reference to the picture avatar for user associated to given
	 * username
	 *
	 * @param username
	 *            Username whose avatar is requested
	 * @return Content reference to picture avatar
	 * @throws FileNotFoundException
	 */
	Content getMeAvatar() throws FileNotFoundException;

	/**
	 * Returns the user identified by given public ID if requester has read
	 * permission on any of contact roles
	 *
	 * @param userId
	 *            public ID of requested user
	 * @return User record associated to given ID
	 */
	User getUserContact(long userId);

	/**
	 * Returns a reference to the user's avatar identified by given public ID if
	 * requester has read permission on any of contact roles
	 *
	 * @param userId
	 *            public Id of user whose avatar is requested
	 * @return Content reference to picture avatar
	 * @throws FileNotFoundException
	 */
	Content getUserContactAvatar(Long userId) throws FileNotFoundException;

	/**
	 * Returns a reference to the account's avatar identified by given public ID if
	 * requester has read permission on any of contact roles
	 *
	 * @param accountId
	 *            public Id of user whose avatar is requested
	 * @return Content reference to picture avatar
	 * @throws FileNotFoundException
	 */
	Content getAccountAvatar(Long accountId) throws FileNotFoundException;

	/**
	 * Provides the list of root users
	 *
	 * @return List with a list of POJOs representing ROOT users
	 */
	List<User> getRootUsers();

	/**
	 * Returns the list of groups where user is member. The list is constrained
	 * to groups that can administer the authenticated user
	 *
	 * @param userId
	 *            Public ID of the user whose groups are requested
	 *
	 * @return List of groups the user is member of
	 */
	List<Group> getGroupsWhereUserIsMember(Long userId);

	/**
	 * Returns the list of groups requester user is administrator
	 *
	 * @param userId
	 *            Public ID of user to request
	 * @return List of groups where user is administrator
	 */
	List<Group> getGroupsWhereUserIsAdmin(Long userId);

	/**
	 * Gets the administered account for authenticated user
	 *
	 * @return Account record administered by given username
	 */
	Account getAdministeredAccount();

	/**
	 * Returns true if logged user has root permission
	 *
	 * @return true if principal is root
	 */
	public Boolean hasPermissionRoot();

	/**
	 * Returns true is given phone is not already assigned to other user in
	 * database
	 *
	 * @param phone
	 *            User's phone
	 * @param userId
	 * 			  userId to check new phone is correct
	 * @param defaulRegion
	 *            Country code assigned when no cc prefix is provided
	 * @return True if phone can be used
	 */
	public Boolean isPhoneAvailable(String phone, Long userId, String defaultRegion);

	/**
	 * Returns true if given email is not already assigned to other user in
	 * database
	 *
	 * @param email
	 *            User's email
	 * @return True if phone can be used
	 */
	public Boolean isEmailAvailable(String email);

	// ///////////////////////////////////////////////////////////
	// Password recovery services
	// ///////////////////////////////////////////////////////////

	/**
	 * Generates a secure password recovery code intended to allow users to
	 * recover their passwords. The code allows a single use and expires after
	 * 300 seconds
	 *
	 * @param username
	 *            Username of User entity requesting password change
	 * @return Password recovery info, including
	 */
	PasswordRecovery getSecurityCode(String username);

	/**
	 * Sends a security code to the email registered for the user who requested
	 * the code generation. Allows setting up subject and body of the message
	 *
	 * @param pwdRecovery
	 *            Security code info to be send by email
	 * @param subject
	 *            Message subject
	 * @param message
	 *            Message body
	 */
	void sendSecurityCode(PasswordRecovery pwdRecovery, String subject,
			String message);

	/**
	 * get property icon_head_web to show on web
	 *
	 */
	String getIconHeadWeb();

	/**
	 * get property title_head_web to show on web
	 *
	 */
	String getTitleHeadWeb();

	/**
	 * Changes password to user owning security code. If code is invalid or
	 * expired no operation is performed
	 *
	 * @param securityCode
	 *            Security code
	 * @param newPassword
	 *            Clear password
	 * @throws DatamodelException
	 */
	void changePassword(String securityCode, String newPassword);

	// //////////////////////////
	// Format converters
	// //////////////////////////

	UserEntity buildUserEntity(User user);

	GroupEntity buildGroupEntity(Group group);

	Account buildAccountPojo(AccountEntity accountEntity);

	Group buildGroupPojo(GroupEntity groupEntity);

	User buildUserPojo(UserEntity userEntity);

	// //////////////////////////
	// QoS control
	// //////////////////////////

	void clearQosChannels();

	void reloadQosChannels();
}
