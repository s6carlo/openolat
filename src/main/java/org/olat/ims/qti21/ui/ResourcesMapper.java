/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.ui;

import java.io.File;
import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.media.FileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.NotFoundMediaResource;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 11.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ResourcesMapper implements Mapper {
	
	private static final OLog log = Tracing.createLoggerFor(ResourcesMapper.class);
	
	private final URI assessmentObjectUri;
	private final File submissionDirectory;
	private final Map<Long,File> submissionDirectoryMaps;
	
	public ResourcesMapper(URI assessmentObjectUri, File submissionDirectory) {
		this.assessmentObjectUri = assessmentObjectUri;
		this.submissionDirectory = submissionDirectory;
		submissionDirectoryMaps = null;
	}
	
	public ResourcesMapper(URI assessmentObjectUri, Map<Long,File> submissionDirectoryMaps) {
		this.assessmentObjectUri = assessmentObjectUri;
		this.submissionDirectoryMaps = submissionDirectoryMaps;
		submissionDirectory = null;
	}

	@Override
	public MediaResource handle(String relPath, HttpServletRequest request) {
		String filename = null;
		MediaResource resource = null;
		try {
			File root = new File(assessmentObjectUri.getPath());
			String href = request.getParameter("href");
			if(StringHelper.containsNonWhitespace(href)) {
				filename = href;	
			} else if(StringHelper.containsNonWhitespace(relPath)) {
				filename = relPath;
				if(filename.startsWith("/")) {
					filename = filename.substring(1, filename.length());
				}
			}
			
			File file = new File(root.getParentFile(), filename);
			if(file.exists()) {
				resource = new FileMediaResource(file);
			} else {
				
				String submissionName = null;
				File storage = null;
				if(filename.startsWith("submissions/")) {
					String submission = filename.substring("submissions/".length());
					int candidateSessionIndex = submission.indexOf('/');
					if(candidateSessionIndex > 0) {
						submissionName = submission.substring(candidateSessionIndex + 1);
						if(submissionDirectory != null) {
							storage = submissionDirectory;
						} else if(submissionDirectoryMaps != null) {
							String sessionKey = submission.substring(0, candidateSessionIndex);
							if(StringHelper.isLong(sessionKey)) {
								try {
									storage = submissionDirectoryMaps.get(new Long(sessionKey));
								} catch (Exception e) {
									log.error("", e);
								}
							}
						}
					}
				}
				
				if(storage != null && StringHelper.containsNonWhitespace(submissionName)) {
					File submissionFile = new File(storage, submissionName);
					if(submissionFile.exists()) {
						resource = new FileMediaResource(submissionFile);
					} else {
						resource = new NotFoundMediaResource(href);
					}
				} else {
					resource = new NotFoundMediaResource(href);
				}
			}
		} catch (Exception e) {
			log.error("", e);
			resource = new NotFoundMediaResource(filename);
		}
		return resource;
	}
}