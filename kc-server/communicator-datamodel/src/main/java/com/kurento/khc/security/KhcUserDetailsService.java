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

package com.kurento.khc.security;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;

@Component
public class KhcUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory
			.getLogger(KhcUserDetailsService.class);

	@Autowired
	private UserDao userDao;

	@Override
	public UserDetails loadUserByUsername(String login)
			throws UsernameNotFoundException {

		// email or phone is accepted as login
		UserEntity user;
		try {
			user = userDao.findUserByEmail(login);
			user.setLogin(login);
		} catch (Exception e) {
			try {
				// Complete phone(including CC) must be provided by client
				user = userDao.findUserByPhone(login, "ES");
				user.setLogin(login);
			} catch (Exception e1) {
				throw new UsernameNotFoundException(
						"User not found in database");
			}
		}
		// build authorities
		Collection<GrantedAuthority> authorities = buildAuthorities(user);

		return new KhcUserDetails(user, false, false, user.isActive(), false,
				authorities);
	}

	private Collection<GrantedAuthority> buildAuthorities(UserEntity user) {
		Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

		// Check if ROLE_ROOT has to be added
		if (user.isRoot()) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ROOT"));
		}

		// Check if ROLE_ADMIN_ACCOUNT has to be added
		try {
			AccountEntity administeredAccount = userDao
					.getAccountWhereUserIsAdmin(user);
			if (administeredAccount != null) {
				authorities.add(new SimpleGrantedAuthority(administeredAccount
						.getAdminAccountRolename()));
			}
		} catch (Exception e) {
			log.warn("Unable to activate account administration authority", e);
		}

		// Add ROLE_ADMIN if at least one authority was added
		if (authorities.size() > 0) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		return authorities;
	}

}
