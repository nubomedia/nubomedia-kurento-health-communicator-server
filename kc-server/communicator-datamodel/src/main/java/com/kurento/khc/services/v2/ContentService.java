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

import java.io.InputStream;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.khc.datamodel.ContentEntity;

public interface ContentService {

	/**
	 * Saves content into repository.
	 * 
	 * @param content
	 *            InputStream where from where content is delivered
	 * @param contentType
	 *            Content's MIME type
	 * @return POJO with all information relative to the new content, including
	 *         location URL and type
	 */
	Content saveContent(InputStream content, String contentType)
;

	/**
	 * Deletes content files associated to a Content. No exception is thrown if
	 * failure is detected while deleting associated files.
	 * 
	 * @param contentEntity
	 *            Files pointed by the entity will be deleted
	 */
	void deleteContent(Content content);

	// //////////////////////////
	// Format converters
	// //////////////////////////

	Content buildContentPojo(ContentEntity contentEntity);

	ContentEntity buildContentEntity(Content content);

}
