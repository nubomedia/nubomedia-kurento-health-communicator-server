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
import java.io.StringWriter;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kurento.khc.KhcMediaException;


@Component("khcThumbnailer")
public class Thumbnailer {

	private static Logger log = LoggerFactory.getLogger(Thumbnailer.class);

	// Command query
	private final static String FIND_AVCONV = "which avconv";
	private final static String FIND_FFMPEG = "which ffmpeg";
	private final static String FIND_MEDIAINFO = "which mediainfo";
	private final static String FIND_IM = "which convert";

	// Commands
	private static String AVCONV_CMD = "avconv ";
	private static String MEDIAINFO_CMD = "mediainfo ";
	private static String CONVERT_CMD = "convert ";

	// Media query strings
	private static String QUERY_IMAGE_FORMAT;
	private static String QUERY_VIDEO_FORMAT;
	private static String QUERY_VIDEO_ROTATION;
	private static String QUERY_VIDEO_DURATION;

	// ffmpeg options
	private static String P_FRAMES = " -frames ";

	// ImageMagick options
	private final static String P_THUMBNAIL = " -thumbnail ";
	private final static String P_AUTO_ORIENT = " -auto-orient ";
	private final static String P_ROTATE = " -rotate ";

	private Boolean avconvActive = false;
	private Double avconvVersion = 0.0;

	private Boolean mediainfoActive = false;

	private Boolean imageMagickActive = false;

	private Boolean thumbnailerActive = false;

	private static final String khcPath = "/bin:/usr/bin:/sbin:/user/sbin:/usr/local/bin:/opt/X11/bin:/opt/local/bin:/opt/local/sbin";
	private String[] envp;

	@Autowired
	FileRepository repository;

	@PostConstruct
	public void init() {

		buildEnv();

		if (!findAvconv()) {
			findFfmpeg();
		}
		if (avconvActive) {
			findAvconvVersion();
		}

		findMediainfo();

		findImageMagick();

		thumbnailerActive = avconvActive & mediainfoActive & imageMagickActive;

		if (!thumbnailerActive) {
			log.warn("Unable to activate thumbnailer due to missing tools: AVCON: "
					+ avconvActive
					+ "; MEDIAINFO: "
					+ mediainfoActive
					+ "; IMAGEMAGICK:" + imageMagickActive);
		}
	}

	private Boolean findAvconv() {
		// Verify ffmpeg is found
		avconvActive = false;
		try {
			Command cmd = new Command(FIND_AVCONV);
			CommandResult result = cmd.getResult();
			if (result.isSuccess()) {
				avconvActive = true;
				AVCONV_CMD = result.getOutput();
				log.info("AVCONV tool found in PATH");
			}
		} catch (Exception e) {
			log.warn("Problem while searching avconv", e);
		}
		return avconvActive;
	}

	private Boolean findFfmpeg() {
		avconvActive = false;
		try {
			// Verify ffmpeg is found
			Command cmd = new Command(FIND_FFMPEG);
			CommandResult result = cmd.getResult();
			if (result.isSuccess()) {
				avconvActive = true;
				AVCONV_CMD = result.getOutput();
				log.info("FFMPEG tool found in PATH");
			} else {
				log.info("Unable to find FFMPEG tool in PATH. Install package ffmpeg");
			}

		} catch (Exception e) {
			log.warn("Problem while searching ffmpeg", e);
		}
		return false;
	}

	private void findAvconvVersion() {

		try {
			String cmdString = AVCONV_CMD + " -version 2>/dev/null";
			Command cmd = new Command(cmdString);
			CommandResult result = cmd.getResult();

			String pattern = "(?ms)^.*(ffmpeg|avconv)\\s+(version\\s*)*(\\d+\\.\\d+).*\\z";
			try {
				avconvVersion = Double.valueOf(result.getOutput().replaceAll(
						pattern, "$3"));
			} catch (NumberFormatException e) {
				log.warn("Unable to detect ffmpeg version "
						+ result.getOutput());
				avconvActive = false;
				return;
			}
			if (avconvVersion < 1.0) {
				P_FRAMES = " -vframes ";
			} else {
				P_FRAMES = " -frames ";
			}
		} catch (Exception e) {
			log.warn("Problem while searching ffmpeg version", e);
		}
	}

	private Boolean findMediainfo() {
		mediainfoActive = false;
		try {
			// Verify mediainfo is found
			Command cmd = new Command(FIND_MEDIAINFO);
			CommandResult result = cmd.getResult();
			if (result.isSuccess()) {
				mediainfoActive = true;
				MEDIAINFO_CMD = result.getOutput();
				log.info("MEDIAINFO tool found in PATH");

				// Create media queries
				QUERY_IMAGE_FORMAT = MEDIAINFO_CMD
						+ " --Inform='Image;%Format%' ";
				QUERY_VIDEO_FORMAT = MEDIAINFO_CMD
						+ " --Inform='Video;%Format%' ";
				QUERY_VIDEO_ROTATION = MEDIAINFO_CMD
						+ " --Inform='Video;%Rotation%' ";
				QUERY_VIDEO_DURATION = MEDIAINFO_CMD
						+ " --Inform='Video;%Duration%' ";
			} else {
				log.info("Unable to find MEDIAINFO tool in PATH. Install package mediainfo");
			}
		} catch (Exception e) {
			log.warn("Problem while searching mediainfo", e);
		}
		return mediainfoActive;

	}

	private Boolean findImageMagick() {
		imageMagickActive = false;
		try {
			Command cmd = new Command(FIND_IM);
			CommandResult result = cmd.getResult();
			if (result.isSuccess()) {
				imageMagickActive = true;
				CONVERT_CMD = result.getOutput();
				log.info("IMAGEMAGICK tool found in PATH");
			} else {
				log.warn("Unable to find IMAGEMAGICK tool in PATH");
			}
		} catch (Exception e) {
			log.warn(
					"Problem while searching ImageMagick. Install imagemagick",
					e);
		}
		return imageMagickActive;
	}

	// ////////////
	//
	// PUBLIC API
	//
	// ////////////

	public void createThumbnail(Integer width, String mediaPath,
			String thumbnailPath) throws FileNotFoundException, IOException,
			InterruptedException {
		if (!thumbnailerActive) {
			throw new KhcMediaException("Thumbnailer is not active");
		}
		int retries = 0;
		String absMediaPath = repository.getMediaUrl(mediaPath);
		String absThumbnailPath = repository.getMediaUrl(thumbnailPath);
		do {
			if (isVideo(absMediaPath)) {
				createVideoThumbnail(width, absMediaPath, absThumbnailPath);
				return;
			} else if (isImage(absMediaPath)) {
				createImageThumbnail(width, absMediaPath, absThumbnailPath);
				return;
			} else {
				retries++;
				log.warn("Unable to indentify media file {} after {} retries",
						absMediaPath, retries);
			}
		} while (retries < 3);

		File media = new File(absMediaPath);
		log.warn("Unable to create thumbnail of unknown media type: "
				+ absMediaPath);
		if (!media.exists()) {
			log.warn("Media {} does not exists", absMediaPath);
		} else {
			log.warn("Media file size is: " + media.length());
			log.warn("Total space available in partition: "
					+ media.getUsableSpace());
			log.warn("Output of mediainfo:"
					+ new Command(MEDIAINFO_CMD + absMediaPath).getResult()
							.getOutput());
			log.warn("Output of isImage:"
					+ new Command(QUERY_IMAGE_FORMAT + absMediaPath)
							.getResult().getOutput());

			IOUtils.copy(new FileInputStream(absMediaPath),
					new FileOutputStream(absMediaPath + "-failed"));

			throw new KhcMediaException(
					"Unable to create thumbnail of unknown media type: "
							+ absMediaPath);
		}
	}

	// //////////////////
	//
	// MEDIA PROCESSORS
	//
	// //////////////////

	private void createVideoThumbnail(Integer width, String mediaPath,
			String thumbnailPath) throws IOException, InterruptedException {
		Integer rotation = getVideoRotation(mediaPath);
		Double duration = getVideoDuration(mediaPath);
		Double thTs = Math.min(duration / 10, 5);
		String mediaCmd = AVCONV_CMD + " -ss " + thTs + " -i " + mediaPath
				+ P_FRAMES + " 1 " + " -f image2 pipe:1 | " + CONVERT_CMD
				+ " - " + P_ROTATE + rotation + P_THUMBNAIL + width + "x"
				+ width + " " + thumbnailPath;
		Command cmd = new Command(mediaCmd);
		CommandResult result = cmd.getResult();
		if (!result.isSuccess()) {
			throw new KhcMediaException(
					"Unable to create video thumbnail due to error: "
							+ result.getOutput());
		}

	}

	private void createImageThumbnail(Integer width, String mediaPath,
			String thumbnailPath) throws IOException, InterruptedException {
		// convert mediaPath -thumbnail widthxwidth thumbnailPath
		String mediaCmd = CONVERT_CMD + " " + mediaPath + " " + P_AUTO_ORIENT
				+ " " + P_THUMBNAIL + " " + width + "x" + width + " "
				+ thumbnailPath;
		Command cmd = new Command(mediaCmd);
		CommandResult result = cmd.getResult();
		if (!result.isSuccess()) {
			throw new KhcMediaException(
					"Unable to craete image thumbnail due to error: "
							+ result.getOutput());
		}
	}

	// ///////////////
	//
	// MEDIA QUERIES
	//
	// ///////////////

	private Boolean isVideo(String mediaPath) throws IOException,
			InterruptedException {
		String queryString = QUERY_VIDEO_FORMAT + mediaPath;
		Command cmd = new Command(queryString);
		CommandResult result = cmd.getResult();
		if (result.getOutput().isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	private Boolean isImage(String mediaPath) throws IOException,
			InterruptedException {
		String queryString = QUERY_IMAGE_FORMAT + mediaPath;
		Command cmd = new Command(queryString);
		CommandResult result = cmd.getResult();
		if (result.isSuccess() && !result.getOutput().isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	private Integer getVideoRotation(String mediaPath) throws IOException,
			InterruptedException {
		String queryString = QUERY_VIDEO_ROTATION + mediaPath;
		Command cmd = new Command(queryString);
		CommandResult result = cmd.getResult();
		if (result.getOutput().isEmpty()) {
			return 0;
		} else {
			return (int) Math.round(Double.valueOf(result.getOutput()));
		}

	}

	private Double getVideoDuration(String mediaPath) throws IOException,
			InterruptedException {
		String queryString = QUERY_VIDEO_DURATION + mediaPath;
		Command cmd = new Command(queryString);
		CommandResult result = cmd.getResult();
		if (result.getOutput().isEmpty()) {
			return 0.0;
		} else {
			return Double.valueOf(result.getOutput()) / 1000.0;
		}

	}

	// /////////
	//
	// HELPERS
	//
	// /////////

	private void buildEnv() {
		Map<String, String> env = System.getenv();
		Integer env_ix = env.size();
		if (!env.containsKey("PATH")) {
			env_ix++;
		}
		envp = new String[env_ix];
		String path = env.get("PATH");
		if (path != null && !path.isEmpty()) {
			path += ":";
		}

		int i = 0;
		for (String var : env.keySet()) {
			if (var.equalsIgnoreCase("PATH")) {
				envp[i++] = var + "=" + path + khcPath;
			} else {
				envp[i++] = var + "=" + env.get(var);
			}
		}
	}

	private class Command {

		private CommandResult cmdResult;

		private Command(String cmdString) throws IOException,
				InterruptedException {

				Process process = Runtime.getRuntime().exec(
						new String[] { "/bin/sh", "-c", cmdString }, envp);
				String out = readCmdOutput(process);
				process.waitFor();
				Boolean code = process.exitValue() == 0 ? true : false;
				cmdResult = new CommandResult(code, out);

		}

		private CommandResult getResult() {
			return cmdResult;
		}

		private String readCmdOutput(Process process) throws IOException {
			StringWriter writer = new StringWriter();
			IOUtils.copy(process.getInputStream(), writer);
			String output = writer.toString().trim();
			if (output.isEmpty()) {
				IOUtils.copy(process.getErrorStream(), writer);
				output = writer.toString().trim();
			}
			return output;
		}

	}

	private class CommandResult {
		private String cmdOutput;
		private Boolean success;

		private CommandResult(Boolean success, String cmdOutput) {
			this.cmdOutput = cmdOutput;
			this.success = success;
		}

		private Boolean isSuccess() {
			return success;
		}

		private String getOutput() {
			return cmdOutput;
		}
	}
}
