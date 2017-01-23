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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.khc.KhcNotFoundException;

@Component
public class CommandTransactionDao extends BaseDao {

	@Transactional
	public Boolean acceptTransactionKnownToServer(
			CommandTransactionEntity transaction) {
		Assert.notNull(transaction);
		try {
			findSingle(CommandTransactionEntity.class, new String[] { "hash" },
					new Object[] { transaction.getHash() });
			return false;
		} catch (KhcNotFoundException e) {
			super.save(transaction);
			transaction.setTimestamp(System.currentTimeMillis());
			return true;
		}
	}

	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	public Integer cleanOldTransactions(Long timeToLive, Integer maxResult) {
		return em
				.createNamedQuery(
						CommandTransactionEntity.NQ_NAME_DELETE_OLD_TRANSACTIONS)
				.setParameter(CommandTransactionEntity.NQ_PARAM_TIMESTAMP,
						System.currentTimeMillis() - timeToLive)
				.setMaxResults(maxResult).executeUpdate();
	}
}
