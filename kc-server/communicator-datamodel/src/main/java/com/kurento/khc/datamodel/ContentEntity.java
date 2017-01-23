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

import javax.persistence.Entity;

@Entity
public class ContentEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	// Attributes

	private String contentUrl;

	private String contentType;

	private Long contentSize;

	private String smallThumbnailUrl;

	private String mediumThumbnailUrl;

	private String largeThumbnailUrl;

	private String thumbnailType;

	// Getters & Setters

	public String getContentUrl() {
		return contentUrl;
	}

	public void setContentUrl(String contentUrl) {
		this.contentUrl = contentUrl;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getContentSize() {
		return contentSize;
	}

	public void setContentSize(Long contentSize) {
		this.contentSize = contentSize;
	}

	public String getSmallThumbnailUrl() {
		return smallThumbnailUrl;
	}

	public void setSmallThumbnailUrl(String smallThumbnailUrl) {
		this.smallThumbnailUrl = smallThumbnailUrl;
	}

	public String getMediumThumbnailUrl() {
		return mediumThumbnailUrl;
	}

	public void setMediumThumbnailUrl(String mediumThumbnailUrl) {
		this.mediumThumbnailUrl = mediumThumbnailUrl;
	}

	public String getLargeThumbnailUrl() {
		return largeThumbnailUrl;
	}

	public void setLargeThumbnailUrl(String largeThumbnailUrl) {
		this.largeThumbnailUrl = largeThumbnailUrl;
	}

	public String getThumbnailType() {
		return thumbnailType;
	}

	public void setThumbnailType(String thumbnailType) {
		this.thumbnailType = thumbnailType;
	}

}
