/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.ims.qti.repository.handlers;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.ims.qti.QTIRuntimeController;
import org.olat.ims.qti.editor.QTIEditorMainController;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.ImsRepositoryResolver;
import org.olat.ims.qti.process.Resolver;
import org.olat.modules.iq.IQManager;
import org.olat.modules.iq.IQPreviewSecurityCallback;
import org.olat.modules.iq.IQSecurityCallback;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.handlers.EditionSupport;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.repository.ui.RepositoryEntryRuntimeController.RuntimeControllerCreator;
import org.olat.resource.OLATResource;
import org.olat.resource.references.Reference;
import org.olat.resource.references.ReferenceManager;

import de.bps.onyx.plugin.OnyxModule;
import de.bps.onyx.plugin.run.OnyxRunController;


/**
 * Initial Date:  Apr 6, 2004
 *
 * @author Mike Stock
 * 
 * Comment:  
 * 
 */
public class QTISurveyHandler extends QTIHandler {
	
	@Override
	public boolean isCreate() {
		return true;
	}

	@Override
	public String getCreateLabelI18nKey() {
		return "new.survey";
	}

	@Override
	public RepositoryEntry createResource(Identity initialAuthor, String displayname, String description, Object createObject, Locale locale) {
		SurveyFileResource ores = new SurveyFileResource();
		return super.createResource(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY, ores, initialAuthor, displayname, description, createObject, locale);
	}

	@Override
	public ResourceEvaluation acceptImport(File file, String filename) {
		ResourceEvaluation eval = SurveyFileResource.evaluate(file, filename);
		if(!eval.isValid() && CoreSpringFactory.getImpl(OnyxModule.class).isEnabled()) {
			eval = OnyxModule.isOnyxTest(file, filename);
		}
		return eval;
	}

	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname, String description,
			boolean withReferences, Locale locale, File file, String filename) {
		return super.importResource(initialAuthor, displayname, description, new SurveyFileResource(), file, filename);
	}

	@Override
	public MediaResource getAsMediaResource(OLATResourceable res, boolean backwardsCompatible) {
		return FileResourceManager.getInstance().getAsDownloadeableMediaResource(res);
	}

	@Override
	public String getSupportedType() {
		return SurveyFileResource.TYPE_NAME;
	}

	@Override
	public boolean supportsDownload() {
		return true;
	}

	@Override
	public EditionSupport supportsEdit(OLATResourceable resource) {
		if(resource != null && OnyxModule.isOnyxTest(resource)) {
			return EditionSupport.no;
		}
		return EditionSupport.yes;
	}

	@Override
	public StepsMainRunController createWizardController(OLATResourceable res, UserRequest ureq, WindowControl wControl) {
		throw new AssertException("Trying to get wizard where no creation wizard is provided for this type.");
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param res
	 * @return Controller
	 */
	@Override
	public MainLayoutController createLaunchController(RepositoryEntry re, RepositoryEntrySecurity reSecurity, UserRequest ureq, WindowControl wControl) {
		return new QTIRuntimeController(ureq, wControl, re, reSecurity,
			new RuntimeControllerCreator() {
				@Override
				public Controller create(UserRequest uureq, WindowControl wwControl, TooledStackedPanel toolbarPanel,
						RepositoryEntry entry, RepositoryEntrySecurity security, AssessmentMode assessmentMode) {
					Controller runController;
					OLATResource res = entry.getOlatResource();
					CoreSpringFactory.getImpl(UserCourseInformationsManager.class)
						.updateUserCourseInformations(entry.getOlatResource(), uureq.getIdentity());
					if (OnyxModule.isOnyxTest(res)) {
						runController = new OnyxRunController(uureq, wwControl, entry, false);
					} else {
						Resolver resolver = new ImsRepositoryResolver(entry);
						IQSecurityCallback secCallback = new IQPreviewSecurityCallback();
						runController = CoreSpringFactory.getImpl(IQManager.class)
								.createIQDisplayController(res, resolver, AssessmentInstance.QMD_ENTRY_TYPE_SURVEY, secCallback, uureq, wwControl);
					}
					return runController;
				}
		});
	}

	@Override
	public Controller createEditorController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar) {
		OLATResource res = re.getOlatResource();
		if (OnyxModule.isOnyxTest(res)) {
			return null;
		}
		
		SurveyFileResource fr = new SurveyFileResource();
		fr.overrideResourceableId(res.getResourceableId());
		
		//check if we can edit in restricted mode -> only typos 
		List<Reference> referencees = CoreSpringFactory.getImpl(ReferenceManager.class).getReferencesTo(res);
		QTIEditorMainController editor =  new QTIEditorMainController(ureq, wControl, re, referencees, fr);
		if (editor.isLockedSuccessfully()) {
			return editor;
		} else {
			return null;
		}
	}

	@Override
	protected String getDeletedFilePrefix() {
		return "del_qtisurvey_"; 
	}
}