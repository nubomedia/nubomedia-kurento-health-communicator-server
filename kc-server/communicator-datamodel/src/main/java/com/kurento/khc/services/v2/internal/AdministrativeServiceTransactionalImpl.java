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

package com.kurento.khc.services.v2.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.khc.datamodel.AccountDao;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.AccountSecureDao;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.datamodel.UserSecureDao;
import com.kurento.khc.services.v2.ContentService;

@Component("khcAdministrativeServiceTransactionalImpl")
public class AdministrativeServiceTransactionalImpl {

	@Autowired
	private AccountDao accountDao;
	@Autowired
	private AccountSecureDao accountSecureDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private UserSecureDao userSecureDao;
	@Autowired
	private ContentDao contentDao;
	@Autowired
	private ContentService contentService;

	@Transactional
	public AccountEntity createAccount(AccountEntity account) {
		return accountSecureDao.createAccount(account);
	}
	
	@Transactional
	public UserEntity createUserInAccount(UserEntity user, AccountEntity account) {
		return userSecureDao.createAccountUser(user, account);
	}

	@Transactional
	public UserEntity createUserInAutoregister(UserEntity user,
			AccountEntity account, Content content) {

		UserEntity userEntity = userSecureDao.createAutoregisterUser(user,
				account);

		// Create contentEntity if provided
		if (content != null) {
			ContentEntity contentEntity = contentService
					.buildContentEntity(content);
			contentEntity = contentDao.createContent(contentEntity);
			userDao.setPicture(userEntity, contentEntity);
		}

		return userEntity;
	}

}
