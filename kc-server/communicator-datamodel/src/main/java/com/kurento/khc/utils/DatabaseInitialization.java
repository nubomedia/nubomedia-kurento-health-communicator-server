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

package com.kurento.khc.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountDao;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;

@Component
public class DatabaseInitialization implements
		ApplicationListener<ContextRefreshedEvent> {

	private static final String PREFIX_ACCOUNT = "kurento.account";
	private static final String PREFIX_USER = "kurento.user";

	private static Logger log = LoggerFactory
			.getLogger(DatabaseInitialization.class);

	@Autowired
	private Environment environment;
	@Autowired
	private AccountDao accountDao;
	@Autowired
	private GroupDao groupDao;
	@Autowired
	private UserDao userDao;

	@Override
	// @Transactional(rollbackFor = Exception.class)
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		try {
			loadAccounts();
		} catch (Exception e) {
			log.warn(
					"Unable to update datamodel according to configuration. Rollback all configuration",
					e);
		}
	}

	private void loadAccounts() {
		List<String[]> keys = getPropertiesMatching(PREFIX_ACCOUNT);
		// Load all accounts in configuration file
		for (String[] tokens : keys) {
			if (tokens.length < 4) {
				throw new KhcInvalidDataException(
						"Database intialization fail. Wrong key format loading account:"
								+ stringValueOf(tokens, "."),
						Code.CONFIGURATION_ERROR);
			}
			String accName = tokens[2];

			AccountEntity account = loadAccount(accName);
			if (account == null) {
				throw new KhcInvalidDataException(
						"Database intialization fail. Account data not found:"
								+ accName, Code.CONFIGURATION_ERROR);
			} else if (account.getId() == null) {
				// Get configuration details
				Boolean autoregister = environment.getProperty(
						"kurento.account." + account.getName()
								+ ".autoregister", Boolean.class, false);
				Boolean pub = environment.getProperty("kurento.account."
						+ account.getName() + ".public", Boolean.class, false);
				account.setUserAutoregister(autoregister);
				account.setGroupAutoregister(autoregister);
				account.setPub(pub);
				log.debug("Persist account: " + accName);
				accountDao.createAccount(account);
				loadAccountData(account);
			}

		}
	}

	private AccountEntity loadAccount(String accName) {

		AccountEntity account;
		try {
			account = accountDao.findAccountByName(accName);
		} catch (KhcNotFoundException e) {
			account = new AccountEntity();
			account.setName(accName);
			account.setActive(true);
		}
		return account;
	}

	private void loadAccountData(AccountEntity account) {

		// Get data structure
		List<String[]> properties = getPropertiesMatching("kurento.account"
				+ "." + account.getName());

		// Load member users
		List<UserEntity> users = loadUsers(
				PREFIX_ACCOUNT + "." + account.getName() + ".users", properties);
		for (UserEntity user : users) {
			log.debug("Persist user: " + user.getEmail());
			userDao.createAccountUser(user, account);
		}

		// Load admin users
		List<UserEntity> admins = loadUsers(
				PREFIX_ACCOUNT + "." + account.getName() + ".admins",
				properties);
		for (UserEntity admin : admins) {
			try {
				userDao.findUserById(admin.getId());
			} catch (KhcNotFoundException e) {
				log.debug("Persist user: " + admin.getEmail());
				userDao.createAccountUser(admin, account);
			}
			accountDao.addAccountAdmin(account, admin);

		}

	}

	private List<UserEntity> loadUsers(String prefix, List<String[]> properties) {
		List<UserEntity> users = new ArrayList<UserEntity>();
		for (String[] tokens : properties) {
			if (stringValueOf(tokens, ".").startsWith(prefix)) {
				String[] usernames = environment.getProperty(
						stringValueOf(tokens, ".")).split(",");
				for (String username : usernames) {
					String name = username.trim();
					if (name.isEmpty())
						continue;
					UserEntity user = loadUser(name);
					if (user == null) {
						throw new KhcInvalidDataException(
								"Database intialization fail. Data not found for user: "
										+ username, Code.CONFIGURATION_ERROR);
					} else {
						users.add(user);
					}
				}
			}
		}
		return users;
	}

	private UserEntity loadUser(String email) {

		UserEntity user;
		try {
			return userDao.findUserByEmail(email);
		} catch (KhcNotFoundException e) {
			user = new UserEntity();
			user.setEmail(email);
			user.setPassword("");
		}

		// Load user data
		List<String[]> properties = getPropertiesMatching(PREFIX_USER + "."
				+ email);
		for (String[] tokens : properties) {
			if (tokens.length < 4) {
				throw new KhcInvalidDataException(
						"Database intialization fail. Wrong key loading user:"
								+ stringValueOf(tokens, "."),
						Code.CONFIGURATION_ERROR);
			}
			String prop = tokens[3];
			if ("PASSWORD".equalsIgnoreCase(prop)) {
				// Modify password if not set
				log.debug("Add password to user: " + user.getEmail());
				user.setEmail(email);
				user.setPassword(String.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
			} else if ("NAME".equalsIgnoreCase(prop) && user.getName() == null) {
				user.setName(String.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
				log.debug("Add name to user: " + user.getEmail() + " ==> "
						+ user.getName());
			} else if ("SURNAME".equalsIgnoreCase(prop)
					&& user.getSurname() == null) {
				user.setSurname(String.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
				log.debug("Add Surname to user: " + user.getEmail() + " ==> "
						+ user.getSurname());
			} else if ("URI".equalsIgnoreCase(prop) && user.getUri() == null) {
				user.setUri(String.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
				log.debug("Add uri to user: " + user.getEmail() + " ==> "
						+ user.getUri());
			} else if ("ACTIVE".equalsIgnoreCase(prop)
					&& user.isActive() == null) {
				user.setActive(Boolean.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
				log.debug("Set active state to user: " + user.getEmail()
						+ " ==> " + user.isActive());
			} else if ("ISROOT".equalsIgnoreCase(prop) && user.isRoot() == null) {
				user.setIsRoot(Boolean.valueOf(environment
						.getProperty(stringValueOf(tokens, "."))));
				log.debug("Set root flag to user:" + user.getEmail() + " ==> "
						+ user.isRoot());
			}

		}
		return user;
	}

	private List<String[]> getPropertiesMatching(String pattern) {

		List<String> propertyNames = new ArrayList<String>();
		for (Iterator<PropertySource<?>> it = ((AbstractEnvironment) environment)
				.getPropertySources().iterator(); it.hasNext();) {
			PropertySource<?> propertySource = (PropertySource<?>) it.next();
			if (propertySource instanceof MapPropertySource) {
				propertyNames.addAll(((MapPropertySource) propertySource)
						.getSource().keySet());
			}

		}
		List<String[]> propertyTokens = new ArrayList<String[]>();

		for (String property : propertyNames) {
			String k = (String) property;
			if (k.startsWith(pattern)) {
				String[] tokens = k.split("\\.");
				propertyTokens.add(tokens);
			}
		}
		return propertyTokens;
	}

	private String stringValueOf(String[] tokens, String concatenator) {
		String property = "";
		String prefix = "";
		for (String token : tokens) {
			property += prefix + token;
			prefix = concatenator;
		}
		return property;
	}

}
