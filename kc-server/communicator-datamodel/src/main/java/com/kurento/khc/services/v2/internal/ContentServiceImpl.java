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

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.ContentSecureDao;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.utils.FileRepository;
import com.kurento.khc.utils.Thumbnailer;

@Service("khcContentServicev2")
public class ContentServiceImpl implements ContentService {

	private static Logger log = LoggerFactory
			.getLogger(ContentServiceImpl.class);

	@Autowired
	private ContentSecureDao contentSecureDao;

	@Autowired
	private ContentDao contentDao;

	@Autowired
	private FileRepository repository;

	@Autowired
	private Thumbnailer thumbnailer;

	@Autowired
	private Environment environment;

	@Value("${kurento.thumbnail.small.width:#{null}}")
	private Integer smallThWidth = 128;
	@Value("${kurento.thumbnail.medium.width:#{null}}")
	private Integer mediumThWidth = 320;
	@Value("${kurento.thumbnail.large.width:#{null}}")
	private Integer largeThWidth = 720;

	public Content saveContent(InputStream media, String mediaType) {
		Content content = null;
		if (media != null) {
			try {
				// Save media into repository
				String contentUrl = repository.uploadMedia(media);
				Long mediaSize = repository.getMediaSize(contentUrl);
				log.debug("Media Uploaded to repository: {} with size {}",
						contentUrl, mediaSize);

				// Create ContentEntity
				content = new Content();
				content.setContentUrl(contentUrl);
				content.setContentType(mediaType);
				content.setContentSize(mediaSize);

				// Create content Thumbnails
				// LARGE
				String largeThumbUrl = repository.createMedia();
				log.debug("Create large thumbnail in {}", largeThumbUrl);
				thumbnailer.createThumbnail(largeThWidth, contentUrl,
						largeThumbUrl);
				content.setLargeThumbnailUrl(largeThumbUrl);

				// MEDIUM (from large)
				String mediumThumbUrl = repository.createMedia();
				log.debug("Create medium thumbnail in {}", mediumThumbUrl);
				thumbnailer.createThumbnail(mediumThWidth, contentUrl,
						mediumThumbUrl);
				content.setMediumThumbnailUrl(mediumThumbUrl);

				// SMALL (from medium)
				String smallThumbUrl = repository.createMedia();
				log.debug("Create small thumbnail in {}", smallThumbUrl);
				thumbnailer.createThumbnail(smallThWidth, contentUrl,
						smallThumbUrl);
				content.setSmallThumbnailUrl(smallThumbUrl);

				content.setThumbnailType("image/jpeg");

			} catch (Exception e) {
				deleteContent(content);
				throw new KhcInternalServerException("Unable to save content",
						e);
			}
		}
		return content;
	}

	public void deleteContent(Content content) {
		if (content != null) {
			try {
				if (content.getContentUrl() != null) {
					repository.deleteMedia(content.getContentUrl());
				}
				if (content.getSmallThumbnailUrl() != null) {
					repository.deleteMedia(content.getSmallThumbnailUrl());
				}
				if (content.getMediumThumbnailUrl() != null) {
					repository.deleteMedia(content.getMediumThumbnailUrl());
				}
				if (content.getLargeThumbnailUrl() != null) {
					repository.deleteMedia(content.getLargeThumbnailUrl());
				}
			} catch (FileNotFoundException e1) {
				log.warn(
						"Unable to delete attachment content of failed message",
						e1);
			}
		}
	}

	// ///////////
	//
	// HELPERS
	//
	// ///////////

	public Content buildContentPojo(ContentEntity contentEntity) {
		Content content = new Content();
		content.setId(contentEntity.getUUID());
		content.setContentUrl(contentEntity.getContentUrl());
		content.setContentType(contentEntity.getContentType());
		content.setContentSize(contentEntity.getContentSize());
		content.setSmallThumbnailUrl(contentEntity.getSmallThumbnailUrl());
		content.setMediumThumbnailUrl(contentEntity.getMediumThumbnailUrl());
		content.setLargeThumbnailUrl(contentEntity.getLargeThumbnailUrl());
		content.setThumbnailType(contentEntity.getThumbnailType());

		return content;
	}

	public ContentEntity buildContentEntity(Content content) {
		log.debug("go to build contentEntity from content:: " + content);
		Assert.notNull(content);
		log.debug("content is not null");
		ContentEntity contentEntity;
		if (content.getId() != null) {
			contentEntity = contentDao.findContentByUUID(content.getId());
		} else {
			contentEntity = new ContentEntity();
		}
		contentEntity.setContentUrl(content.getContentUrl() != null ? content
				.getContentUrl() : contentEntity.getContentUrl());
		contentEntity.setContentType(content.getContentType() != null ? content
				.getContentType() : contentEntity.getContentType());
		contentEntity.setContentSize(content.getContentSize() != null ? content
				.getContentSize() : contentEntity.getContentSize());
		contentEntity
				.setLargeThumbnailUrl(content.getLargeThumbnailUrl() != null ? content
						.getLargeThumbnailUrl() : contentEntity
						.getLargeThumbnailUrl());
		contentEntity
				.setMediumThumbnailUrl(content.getMediumThumbnailUrl() != null ? content
						.getMediumThumbnailUrl() : contentEntity
						.getMediumThumbnailUrl());
		contentEntity
				.setSmallThumbnailUrl(content.getSmallThumbnailUrl() != null ? content
						.getSmallThumbnailUrl() : contentEntity
						.getSmallThumbnailUrl());
		contentEntity
				.setThumbnailType(content.getThumbnailType() != null ? content
						.getThumbnailType() : contentEntity.getThumbnailType());

		return contentEntity;
	}
}
