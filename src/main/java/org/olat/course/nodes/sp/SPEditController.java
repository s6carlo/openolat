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

package org.olat.course.nodes.sp;


import org.olat.core.commons.controllers.filechooser.LinkFileCombiCalloutController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.iframe.DeliveryOptions;
import org.olat.core.gui.control.generic.iframe.DeliveryOptionsConfigurationController;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSManager;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.CourseEditorHelper;
import org.olat.course.editor.NodeEditController;
import org.olat.course.nodes.SPCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<BR/>
 * Edit controller for single page course nodes
 * <P/>
 * Initial Date:  Oct 12, 2004
 *
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class SPEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_SPCONFIG = "pane.tab.spconfig";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final  String PANE_TAB_DELIVERYOPTIONS = "pane.tab.layout";
	/** configuration key for the filename */
	public static final String CONFIG_KEY_FILE = "file";
	public static final String CONFIG_KEY_DELIVERYOPTIONS = "deliveryOptions";
	/** configuration key: should relative links like ../otherfolder/my.css be allowed? **/
	public static final String CONFIG_KEY_ALLOW_RELATIVE_LINKS = "allowRelativeLinks";
	/** configuration key: should the students be allowed to edit the page? */
	public static final String CONFIG_KEY_ALLOW_COACH_EDIT = "allowCoachEdit";

	private static final String[] paneKeys = {PANE_TAB_SPCONFIG, PANE_TAB_ACCESSIBILITY};
	
	// NLS support:
	
	private static final String NLS_CONDITION_ACCESSIBILITY_TITLE = "condition.accessibility.title";
	
	private ModuleConfiguration moduleConfiguration;
	private VelocityContainer myContent;
		
	private SPCourseNode courseNode;
	private VFSContainer courseFolderBaseContainer;
	private ConditionEditController accessibilityCondContr;
	private DeliveryOptionsConfigurationController deliveryOptionsCtrl;
	private TabbedPane myTabbedPane;
	private LinkFileCombiCalloutController combiLinkCtr;
	private SecuritySettingsForm securitySettingForm;
	
	/**
	 * Constructor for single page editor controller
	 * @param config The node module configuration
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param spCourseNode The current single page course node
	 * @param course
	 * @param euce
	 */
	public SPEditController(ModuleConfiguration config, UserRequest ureq,
			WindowControl wControl, SPCourseNode spCourseNode, ICourse course, UserCourseEnvironment euce) {
		super(ureq, wControl);
		moduleConfiguration = config;
		courseNode = spCourseNode;				
		courseFolderBaseContainer = course.getCourseFolderContainer();

		myContent = createVelocityContainer("edit");
		myContent.contextPut("fieldSetLegend", getTranslator().translate("fieldSetLegend"));
		
		moduleConfiguration.remove("iniframe");//on the fly remove deprecated stuff
		moduleConfiguration.remove("statefulMicroWeb");

		// Read configuration
		String relFilePath = (String) moduleConfiguration.get(CONFIG_KEY_FILE);		
		boolean relFilPathIsProposal = false;
		boolean allowRelativeLinks = moduleConfiguration.getBooleanSafe(CONFIG_KEY_ALLOW_RELATIVE_LINKS, false);
		boolean allowCoachEdit = moduleConfiguration.getBooleanSafe(CONFIG_KEY_ALLOW_COACH_EDIT, false);

		if(relFilePath == null){
			// Use calculated file and folder name as default when not yet configured
			relFilePath = CourseEditorHelper.createUniqueRelFilePathFromShortTitle(courseNode, this.courseFolderBaseContainer);
			relFilPathIsProposal = true;
		}
		// File create/select controller
		combiLinkCtr = new LinkFileCombiCalloutController(ureq, wControl, courseFolderBaseContainer, relFilePath, relFilPathIsProposal, allowRelativeLinks, new CourseInternalLinkTreeModel(course.getEditorTreeModel()));
		listenTo(combiLinkCtr);
		myContent.put("combiCtr", combiLinkCtr.getInitialComponent());		
		myContent.contextPut("editorEnabled", combiLinkCtr.isEditorEnabled());
		
		// Security configuration form
		securitySettingForm = new SecuritySettingsForm(ureq, wControl, allowRelativeLinks, allowCoachEdit);
		listenTo(securitySettingForm);
		myContent.put("allowRelativeLinksForm", securitySettingForm.getInitialComponent());
		
		// Access conditions
		CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		Condition accessCondition = courseNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), euce, accessCondition,
				AssessmentHelper.getAssessableNodes(editorModel, spCourseNode));		
		listenTo(accessibilityCondContr);

		// Delivery options form
		DeliveryOptions deliveryOptions = (DeliveryOptions)moduleConfiguration.get(CONFIG_KEY_DELIVERYOPTIONS);
		deliveryOptionsCtrl = new DeliveryOptionsConfigurationController(ureq, getWindowControl(), deliveryOptions, "Knowledge Transfer#_splayout");
		listenTo(deliveryOptionsCtrl);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest urequest, Controller source, Event event) {
		if (source instanceof NodeEditController) {
			if(combiLinkCtr != null && combiLinkCtr.isDoProposal()){
				combiLinkCtr.setRelFilePath(CourseEditorHelper.createUniqueRelFilePathFromShortTitle(courseNode, courseFolderBaseContainer));
			}
		} else if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				Condition cond = accessibilityCondContr.getCondition();
				courseNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if(source == deliveryOptionsCtrl) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				DeliveryOptions config = deliveryOptionsCtrl.getDeliveryOptions();
				if (config != null) {
					moduleConfiguration.set(CONFIG_KEY_DELIVERYOPTIONS, config);
					fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				}
			}
		} else if(source == combiLinkCtr){
			if(event == Event.DONE_EVENT){
				String relPath = VFSManager.getRelativeItemPath(combiLinkCtr.getFile(), courseFolderBaseContainer, null);
				moduleConfiguration.set(CONFIG_KEY_FILE, relPath);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				if(!myTabbedPane.containsTab(deliveryOptionsCtrl.getInitialComponent())) {
					myTabbedPane.addTab(translate(PANE_TAB_DELIVERYOPTIONS), deliveryOptionsCtrl.getInitialComponent());
				}
				myContent.contextPut("editorEnabled", combiLinkCtr.isEditorEnabled());
			}
		}else if(source == securitySettingForm){
			if(event == Event.DONE_EVENT){
				boolean allowRelativeLinks = securitySettingForm.getAllowRelativeLinksConfig();
				moduleConfiguration.set(CONFIG_KEY_ALLOW_RELATIVE_LINKS, allowRelativeLinks);
				moduleConfiguration.set(CONFIG_KEY_ALLOW_COACH_EDIT, securitySettingForm.getAllowCoachEditConfig());
				combiLinkCtr.setAllowEditorRelativeLinks(allowRelativeLinks);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	public void addTabs(TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate(NLS_CONDITION_ACCESSIBILITY_TITLE)));
		tabbedPane.addTab(translate(PANE_TAB_SPCONFIG), myContent);
		if(combiLinkCtr != null && combiLinkCtr.isEditorEnabled()) {
			tabbedPane.addTab(translate(PANE_TAB_DELIVERYOPTIONS), deliveryOptionsCtrl.getInitialComponent());
		}
	}
	
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//child controllers registered with listenTo() get disposed in BasicController
	}

	public String[] getPaneKeys() {
		return paneKeys;
	}

	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}
}