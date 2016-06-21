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
package org.olat.modules.fo.portfolio;

import java.io.File;

import org.olat.core.commons.services.image.Size;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.util.FileUtils;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.Message;
import org.olat.modules.fo.MessageLight;
import org.olat.modules.fo.manager.ForumManager;
import org.olat.modules.portfolio.Media;
import org.olat.modules.portfolio.handler.AbstractMediaHandler;
import org.olat.modules.portfolio.manager.MediaDAO;
import org.olat.modules.portfolio.manager.PortfolioFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 20.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class ForumMediaHandler extends AbstractMediaHandler {
	
	public static final String FORUM_HANDLER = OresHelper.calculateTypeName(Forum.class);
	
	@Autowired
	private MediaDAO mediaDao;
	@Autowired
	private ForumManager forumManager;
	@Autowired
	private PortfolioFileStorage fileStorage;
	
	public ForumMediaHandler() {
		super(FORUM_HANDLER);
	}

	@Override
	public String getIconCssClass(Media media) {
		return "o_fo_icon";
	}

	@Override
	public VFSLeaf getThumbnail(Media media, Size size) {
		return null;
	}

	@Override
	public Media createMedia(String title, String description, Object mediaObject, String businessPath, Identity author) {
		Message message = null;
		if(mediaObject instanceof Message) {
			message = (Message)mediaObject;
		} else if(mediaObject instanceof MessageLight) {
			MessageLight messageLight = (MessageLight)mediaObject;
			message = forumManager.loadMessage(messageLight.getKey());
		}
		
		String content = message.getBody();
		Media media = mediaDao.createMedia(title, description, content, FORUM_HANDLER, businessPath, 70, author);
		
		File messageDir = forumManager.getMessageDirectory(message.getForum().getKey(), message.getKey(), false);
		if (messageDir != null && messageDir.exists()) {
			File[] attachments = messageDir.listFiles();
			if (attachments.length > 0) {
				File mediaDir = fileStorage.generateMediaSubDirectory(media);
				for(File attachment:attachments) {
					FileUtils.copyFileToDir(attachment, mediaDir, "Forum media");
				}
				String storagePath = fileStorage.getRelativePath(mediaDir);
				media = mediaDao.updateStoragePath(media, storagePath);
			}
		}

		return media;
	}

	@Override
	public Controller getMediaController(UserRequest ureq, WindowControl wControl, Media media) {
		return new ForumMessageMediaController(ureq, wControl, media);
	}
	
	
}
