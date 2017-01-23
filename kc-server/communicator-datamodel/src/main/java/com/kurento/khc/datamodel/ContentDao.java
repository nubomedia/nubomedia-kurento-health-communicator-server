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
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.utils.FileRepository;

@Component("khcContentDao")
public class ContentDao extends BaseDao {

	@Autowired
	private FileRepository fileRepository;

	@Transactional
	public ContentEntity createContent(ContentEntity content) {

		super.save(content);
		return content;
	}

	@Transactional
	public void deleteContent(ContentEntity content) {
		// Make sure references to this entity are removed before calling

		ContentEntity dbContent = attach(content);
		// delete files
		try {
			fileRepository.deleteMedia(dbContent.getContentUrl());
		} catch (FileNotFoundException e) {
			log.warn("Unable to delete file: " + dbContent.getContentUrl());
		}

		super.delete(content);
		em.flush();
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public ContentEntity findContentByUUID(Long uuid) {
		return findSingle(ContentEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	protected ContentEntity attach(final ContentEntity content) {

		Assert.notNull(content);
		if (em.contains(content)) {
			return content;
		} else {
			ContentEntity dbContent;
			if ((dbContent = em.find(ContentEntity.class, content.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown content to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						ContentEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + content.getUUID());
							}
						});
			} else {
				return dbContent;
			}
		}

	}

}
