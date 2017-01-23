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

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kurento.khc.datamodel.UserEntity;

public class KhcUserDetails implements UserDetails {

	private static final long serialVersionUID = -7597589259774805863L;

	private final UserEntity user;
	private final boolean expired;
	private final boolean locked;
	private final boolean enabled;
	private final boolean credentialExpired;
	private final Collection<GrantedAuthority> authorities;

	public KhcUserDetails() {
		this.user = null;
		this.expired = true;
		this.locked = true;
		this.enabled = false;
		this.credentialExpired = true;
		this.authorities = null;
	}

	public KhcUserDetails(UserEntity user, boolean expired, boolean locked,
			boolean enabled, boolean credentialExpired,
			Collection<? extends GrantedAuthority> authorities) {
		this.user = user;
		this.expired = expired;
		this.locked = locked;
		this.enabled = enabled;
		this.credentialExpired = credentialExpired;
		if (authorities != null)
			this.authorities = Collections.unmodifiableCollection(authorities);
		else
			this.authorities = null;
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		if (user == null)
			return null;

		return user.getPassword();
	}

	@Override
	public String getUsername() {
		if (user == null)
			return null;

		return user.getLogin();
	}

	@Override
	public boolean isAccountNonExpired() {
		return !expired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return !credentialExpired;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public UserEntity getUser() {
		return user;
	}

	@Override
	public String toString() {
		return "" + user;
	}
}
