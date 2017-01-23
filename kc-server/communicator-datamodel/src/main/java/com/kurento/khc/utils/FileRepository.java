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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("khcFileRespository")
public class FileRepository {

	private static final Logger log = LoggerFactory
			.getLogger(FileRepository.class);

	@Value("${kurento.content.repository:#{null}}")
	private String CONTENT_REPOSITORY_PATH = "tmp";
	private static SecureRandom random = new SecureRandom();

	@PostConstruct
	public void init() throws IOException {
		// Initialize repository path
		if (!CONTENT_REPOSITORY_PATH.endsWith("/")) {
			CONTENT_REPOSITORY_PATH += File.separator;
		}

		// Verify there are write permissions for repository
		File repository = new File(CONTENT_REPOSITORY_PATH);
		File pCheck;
		if (repository.exists() && repository.isDirectory()) {
			do {
				String pCheckName = String.valueOf(random
						.nextInt(Integer.MAX_VALUE));
				pCheck = new File(repository + File.separator + pCheckName);
			} while (!pCheck.createNewFile());
			pCheck.delete();
		}
	}

	public String uploadMedia(InputStream media) throws IOException {
		File mediaFile = buildMediaFile();
		FileOutputStream output = new FileOutputStream(mediaFile);

		IOUtils.copy(media, output);
		output.close();
		media.close();
		String absoluteMediaUrl = mediaFile.getAbsolutePath();
		log.debug("File uploaded to repository path: {}, size: {}",
				absoluteMediaUrl, mediaFile.length());
		return absoluteMediaUrl.substring(CONTENT_REPOSITORY_PATH.length());
	}

	public String uploadMedia(String fileUrl, String media) throws IOException {
		File mediaFile = new File(CONTENT_REPOSITORY_PATH + fileUrl);
		mediaFile.createNewFile();
		FileUtils.writeStringToFile(
				new File(CONTENT_REPOSITORY_PATH + fileUrl), media);
		return fileUrl;
	}

	public String createMedia() throws IOException {
		File mediaFile = buildMediaFile();
		String absoluteMediaUrl = mediaFile.getAbsolutePath();
		return absoluteMediaUrl.substring(CONTENT_REPOSITORY_PATH.length());
	}

	public InputStream downloadMedia(String mediaUrl)
			throws FileNotFoundException {
		String[] pathsToCheck = new String[] { mediaUrl,
				CONTENT_REPOSITORY_PATH + mediaUrl };

		for (int i = 0; i < pathsToCheck.length; i++) {
			File file = new File(pathsToCheck[i]);
			if (file.exists()) {
				return new FileInputStream(file);
			}
		}

		throw new FileNotFoundException();
	}

	public File getMediaFile(String mediaUrl) throws FileNotFoundException {
		return findFile(mediaUrl);
	}

	public String getMediaUrl(String mediaUrl) throws FileNotFoundException {
		File file = findFile(mediaUrl);
		return file.getAbsolutePath();
	}

	public void deleteMedia(String mediaUrl) throws FileNotFoundException {

		File file = findFile(mediaUrl);
		File parentDir = file.getParentFile();
		file.delete();

		// Directory clean
		while (parentDir != null) {
			File currentDir = parentDir;
			parentDir = currentDir.getParentFile();
			if (!currentDir.delete()) {
				// Directory not empty ==> give up
				break;
			}
		}
	}

	public Long getMediaSize(String mediaUrl) throws FileNotFoundException {
		File file = findFile(mediaUrl);
		return file.length();
	}

	// //////////////
	//
	// HELPERS
	//
	// //////////////

	private File buildMediaFile() throws IOException {
		Calendar calendar = Calendar.getInstance();
		String mediaUrl = CONTENT_REPOSITORY_PATH;

		// Build directory
		mediaUrl += calendar.get(Calendar.YEAR) + File.separator;
		mediaUrl += (calendar.get(Calendar.MONTH) + 1) + File.separator;
		mediaUrl += calendar.get(Calendar.DAY_OF_MONTH) + File.separator;
		mediaUrl += calendar.get(Calendar.HOUR_OF_DAY) + File.separator;
		mediaUrl += calendar.get(Calendar.MINUTE) + File.separator;
		mediaUrl += calendar.get(Calendar.SECOND) + File.separator;

		File mediaDir = new File(mediaUrl);
		if (!mediaDir.mkdirs() && !mediaDir.exists()) {
			throw new IOException("Unable to build media file:" + mediaUrl);
		}
		// Build file name
		String fileUrl;
		File mediaFile;
		do {
			fileUrl = "" + random.nextInt(Integer.MAX_VALUE);
			mediaFile = new File(mediaUrl + fileUrl);
		} while (!mediaFile.createNewFile());
		return mediaFile;
	}

	private File findFile(String mediaUrl) throws FileNotFoundException {
		String[] pathsToCheck = new String[] { mediaUrl,
				CONTENT_REPOSITORY_PATH + mediaUrl };

		for (int i = 0; i < pathsToCheck.length; i++) {
			if (pathsToCheck[i] != null) {
				File file = new File(pathsToCheck[i]);
				if (file.exists()) {
					return file;
				}
			}
		}
		throw new FileNotFoundException();
	}
}
