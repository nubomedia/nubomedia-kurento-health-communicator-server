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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcNotFoundException;

public abstract class BaseDao {

	protected static Logger log = LoggerFactory.getLogger(BaseDao.class
			.getName());

	@PersistenceContext
	protected EntityManager em;

	// /////////////////
	//
	// COMMON DDBB PROCEDURES
	//
	// /////////////////

	protected void save(BaseEntity entity) {
		Assert.notNull(entity);
		Assert.isNull(entity.getId());
		log.trace("Object save request:\n" + toString());
		/*
		 * We should use em.find to find out if entity exists, but Spring 3.1.4
		 * fails with @MappedSuperclass. See
		 * https://jira.springsource.org/browse/ROO-757
		 */
		em.persist(entity);
		em.flush();
		log.trace("----> Object inserted into database ==> PERSIT");

	}

	protected void delete(BaseEntity entity) {
		Assert.notNull(entity);
		Assert.notNull(entity.getId());
		/*
		 * We should use em.find to find out if entity exists, but Spring 3.1.4
		 * fails with @MappedSuperclass. See
		 * https://jira.springsource.org/browse/ROO-757
		 */
		BaseEntity dbEntity = em.merge(entity);
		em.remove(dbEntity);
		em.flush();

	}

	// /////////////////
	//
	// COMMON QUERIES
	//
	// /////////////////

	protected <T extends BaseEntity> T findSingle(Class<T> clazz,
			String[] fields, Object[] values) {

		List<T> obs = getQuery(clazz, 1, fields, values);
		if (!obs.isEmpty()) {
			return obs.get(0);
		} else {
			Map<String, String> filter = new HashMap<String, String>();
			for (int i = 0, end = fields.length; i < end; ++i) {
				filter.put(fields[i] != null ? fields[i] : "null",
						values[i] != null ? values[i].toString() : "null");
			}

			final String exceptionMessage = "Object [" + clazz.getSimpleName()
					+ "] " + filter.toString() + " not found in DB";

			throw new KhcNotFoundException(exceptionMessage,
					KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
					clazz.getSimpleName(), filter);
		}
	}

	protected <T> List<T> findAll(Class<T> clazz) {
		return getQuery(clazz, 0, null, null);
	}

	protected <T> List<T> findAll(final Class<T> clazz, final int maxResult,
			final String[] filterFields, final Object[] filterValues) {
		return getQuery(clazz, maxResult, filterFields, filterValues);
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getQuery(Class<T> clazz, int maxResult,
			String[] filterFields, Object[] filterValues) {

		// Build string query
		String queryString = "select table from " + clazz.getName() + " table";

		int i;
		if (filterFields != null) {
			String prefix = " where ";
			for (i = 0; i < filterFields.length; i++) {
				// If any filter is null then reject the query as it will less
				// restrictive than required and will yield undesired results
				if (filterFields[i] == null || filterValues[i] == null) {
					return new ArrayList<T>();
				}
				if (!filterFields[i].equals("")) {
					if (filterValues[i] instanceof Collection<?>) {
						queryString += prefix + "table." + filterFields[i]
								+ " in (:value_" + i + ")";
					} else
						queryString += prefix + "table." + filterFields[i]
							+ "=:value_" + i;
					prefix = " AND ";
				}
			}
		}

		// Build query
		Query q = em.createQuery(queryString);

		// Add filter values
		if (filterFields != null && filterValues != null) {
			for (i = 0; i < filterValues.length; i++) {
				if (filterValues[i] != null) {
					q.setParameter("value_" + i, filterValues[i]);
				}
			}
		}

		if (maxResult > 0)
			q.setMaxResults(maxResult);
		return q.getResultList();

	}

	@SuppressWarnings("unchecked")
	public static <T> T unProxy(T entity) {
		Assert.notNull(entity);

		Hibernate.initialize(entity);
		if (entity instanceof HibernateProxy) {
			entity = (T) ((HibernateProxy) entity)
					.getHibernateLazyInitializer().getImplementation();
		}
		return entity;
	}

}
