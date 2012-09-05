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
package org.olat.util;

import org.olat.util.FunctionalUtil.OlatSite;
import org.olat.util.FunctionalUtil.WaitLimitAttribute;

import com.thoughtworks.selenium.Selenium;

/**
 * Description: <br>
 * 
 * @author jkraehemann, joel.kraehemann@frentix.com, frentix.com
 */
public class FunctionalCourseUtil {
	public final static String COURSE_RUN_CSS = "o_course_run";
	public final static String COURSE_OPEN_EDITOR_CSS = "o_sel_course_open_editor";
	
	public final static String COURSE_EDITOR_PUBLISH_CSS = "b_toolbox_publish";
	public final static String COURSE_EDITOR_PUBLISH_WIZARD_SELECT_ALL_CSS = null; //FIXME:JK: needs a css class
	public final static String COURSE_EDITOR_PUBLISH_WIZARD_ACCESS_ID = "o_fioaccessBox_SELBOX";
	public final static String COURSE_EDITOR_PUBLISH_WIZARD_CATALOG_ID = "o_fiocatalogBox_SELBOX";
	public final static String ADD_TO_CATALOG_YES_VALUE = "yes";
	public final static String ADD_TO_CATALOG_NO_VALUE = "no";
	
	public final static String COURSE_EDITOR_INSERT_CONTENT_CSS = "b_toolbox_content";
	public final static String CREATE_COURSE_NODE_TARGET_POSITION_ITEM_CSS = "b_selectiontree_item";
	
	public final static String EPORTFOLIO_ADD_CSS = "b_eportfolio_add";
	
	public final static String FORUM_ICON_CSS = "o_fo_icon";
	public final static String BLOG_ICON_CSS = "o_blog_icon";
	
	public final static String FORUM_TOOLBAR_CSS = "o_forum_toolbar";
	public final static String FORUM_THREAD_NEW_CSS = "o_sel_forum_thread_new";
	public final static String FORUM_ARCHIVE_CSS = "o_sel_forum_archive";
	public final static String FORUM_FILTER_CSS = "o_sel_forum_filter";
	
	public final static String WIKI_CREATE_ARTICLE_CSS = "o_sel_wiki_search";
	public final static String WIKI_ARTICLE_BOX_CSS = "o_wikimod-article-box";
	public final static String WIKI_EDIT_FORM_WRAPPER_CSS = "o_wikimod_editform_wrapper";
	
	public final static String BLOG_CREATE_ENTRY_CSS = "o_sel_feed_item_new";
	public final static String BLOG_FORM_CSS = "o_sel_blog_form";
	
	public final static String TEST_CHOOSE_REPOSITORY_FILE_CSS = "o_sel_test_choose_repofile";
	public final static String TEST_CREATE_RESOURCE_CSS = "o_sel_repo_popup_create_resource";
	
	public enum CourseNodeTab {
		TITLE_AND_DESCRIPTION,
		VISIBILITY,
		ACCESS,
		CONTENT;
	};
	
	public enum VisibilityOption {
		BLOCKED_FOR_LEARNERS,
		DEPENDING_ON_DATE,
		DEPENDING_ON_GROUP,
		DEPENDING_ON_ASSESSMENT,
		APPLY_TO_OWNERS_AND_TUTORS(DEPENDING_ON_ASSESSMENT);
		
		private VisibilityOption requires;
		
		VisibilityOption(){
			this(null);
		}
		
		VisibilityOption(VisibilityOption requires){
			setRequires(requires);
		}

		public VisibilityOption getRequires() {
			return requires;
		}

		public void setRequires(VisibilityOption requires) {
			this.requires = requires;
		}
	};
	
	public enum AccessOption {
		BLOCKED_FOR_LEARNERS,
		DEPENDING_ON_DATE,
		DEPENDING_ON_GROUP,
		DEPENDING_ON_ASSESSMENT,
		APPLY_TO_OWNERS_AND_TUTORS(DEPENDING_ON_ASSESSMENT);
		
		private AccessOption requires;
		
		AccessOption(){
			this(null);
		}
		
		AccessOption(AccessOption requires){
			setRequires(requires);
		}

		public AccessOption getRequires() {
			return requires;
		}

		public void setRequires(AccessOption requires) {
			this.requires = requires;
		}
	}
	
	public enum CourseNodeAlias {
		IQ_TEST("o_iqtest_icon"),
		IQ_SELFTEST("o_iqself_icon"),
		IQ_QUESTIONAIRE("o_iqsurv_icon");
		
		private String iconCss;
		
		CourseNodeAlias(String iconCss){
			setIconCss(iconCss);
		}

		public String getIconCss() {
			return iconCss;
		}

		public void setIconCss(String iconCss) {
			this.iconCss = iconCss;
		}
	}
	
	public enum AccessSettings {
		OWNERS("1"),
		OWNERS_AND_AUTHORS("2"),
		ALL_REGISTERED_USERS("3"),
		REGISTERED_USERS_AND_GUESTS("4"),
		MEMBERS_ONLY("membersonly");
		
		private String accessValue;
		
		AccessSettings(String accessValue){
			setAccessValue(accessValue);
		}

		public String getAccessValue() {
			return accessValue;
		}

		public void setAccessValue(String accessValue) {
			this.accessValue = accessValue;
		}
	}
	
	public enum CourseEditorIQTestTab {
		TITLE_AND_DESCRIPTION,
		VISIBILITY,
		ACCESS,
		TEST_CONFIGURATION;
	}
	
	private String courseRunCss;
	private String courseOpenEditorCss;
	
	private String courseEditorPublishCss;
	private String courseEditorPublishWizardSelectAllCss;
	private String courseEditorPublishWizardAccessId;
	private String courseEditorPublishWizardCatalogId;
	
	private String courseEditorInsertContentCss;
	private String createCourseNodeTargetPositionItemCss;
	
	private String eportfolioAddCss;
	
	private String forumIconCss;
	private String blogIconCss;
	
	private String forumToolbarCss;
	private String forumThreadNewCss;
	private String forumArchiveCss;
	private String forumFilterCss;
	
	private String wikiCreateArticleCss;
	private String wikiArticleBoxCss;
	private String wikiEditFormWrapperCss;
	
	private String blogCreateEntryCss;
	private String blogFormCss;
	
	private String testChooseRepositoryFileCss;
	private String testCreateResourceCss;
	
	private FunctionalUtil functionalUtil;
	private FunctionalRepositorySiteUtil functionalRepositorySiteUtil;
	
	public FunctionalCourseUtil(FunctionalUtil functionalUtil, FunctionalRepositorySiteUtil functionalRepositorySiteUtil){
		this.functionalUtil = functionalUtil;
		this.functionalRepositorySiteUtil = functionalRepositorySiteUtil;
		
		setCourseRunCss(COURSE_RUN_CSS);
		setCourseOpenEditorCss(COURSE_OPEN_EDITOR_CSS);
		
		setCourseEditorPublishCss(COURSE_EDITOR_PUBLISH_CSS);
		setCourseEditorPublishWizardSelectAllCss(COURSE_EDITOR_PUBLISH_WIZARD_SELECT_ALL_CSS);
		setCourseEditorPublishWizardAccessId(COURSE_EDITOR_PUBLISH_WIZARD_ACCESS_ID);
		setCourseEditorPublishWizardCatalogId(COURSE_EDITOR_PUBLISH_WIZARD_CATALOG_ID);
		
		setCourseEditorInsertContentCss(COURSE_EDITOR_INSERT_CONTENT_CSS);
		setCreateCourseNodeTargetPositionItemCss(CREATE_COURSE_NODE_TARGET_POSITION_ITEM_CSS);
		
		setEportfolioAddCss(EPORTFOLIO_ADD_CSS);
		
		setForumIconCss(FORUM_ICON_CSS);
		setBlogIconCss(BLOG_ICON_CSS);
		
		setForumToolbarCss(FORUM_TOOLBAR_CSS);
		setForumThreadNewCss(FORUM_THREAD_NEW_CSS);
		setForumArchiveCss(FORUM_ARCHIVE_CSS);
		setForumFilterCss(FORUM_FILTER_CSS);
		
		setWikiCreateArticleCss(WIKI_CREATE_ARTICLE_CSS);
		setWikiArticleBoxCss(WIKI_ARTICLE_BOX_CSS);
		setWikiEditFormWrapperCss(WIKI_EDIT_FORM_WRAPPER_CSS);
		
		setBlogCreateEntryCss(BLOG_CREATE_ENTRY_CSS);
		setBlogFormCss(BLOG_FORM_CSS);
		
		setTestChooseRepositoryFileCss(TEST_CHOOSE_REPOSITORY_FILE_CSS);
		setTestCreateResourceCss(TEST_CREATE_RESOURCE_CSS);
	}
	
	/**
	 * @param browser
	 * @param courseId
	 * @param nth
	 * @return true on success otherwise false
	 * 
	 * Opens the nth course element within the specified course.
	 */
	public boolean open(Selenium browser, long courseId, int nth){
		if(!functionalRepositorySiteUtil.openCourse(browser, courseId))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();

		selectorBuffer.append("xpath=//ul[contains(@class, 'b_tree_l1')]//li[")
		.append(nth + 1)
		.append("]//a");
		
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param courseId
	 * @param nth
	 * @return true on success otherwise false
	 * 
	 * Opens the nth course element within the specified course
	 * without using business paths.
	 */
	public boolean openWithoutBusinessPath(Selenium browser, long courseId, int nth){
		if(!functionalRepositorySiteUtil.openCourseWithoutBusinessPath(browser, courseId))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();

		selectorBuffer.append("xpath=//ul[contains(@class, 'b_tree_l1')]//li[")
		.append(nth + 1)
		.append("]//a");
		
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @return true on success
	 * 
	 * Opens the course editor but the course must be opened.
	 */
	public boolean openCourseEditor(Selenium browser){
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getCourseOpenEditorCss())
		.append("')]");
		
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param option
	 * @param nthForm
	 * @return true on success
	 * 
	 * Disables the specified access option, the course editor should be open.
	 */
	public boolean disableAccessOption(Selenium browser, AccessOption option, int nthForm){
		//TODO:JK: implement me
		
		return(false);
	}
	
	/**
	 * @param browser
	 * @param option
	 * @param nthForm
	 * @return true on success
	 * 
	 * Enables the specified access option, the course editor should be open.
	 */
	public boolean enableAccessOption(Selenium browser, AccessOption option, int nthForm){
		//TODO:JK: implement me
		
		return(false);
	}
	
	private String[] createCatalogSelectors(String path){
		//TODO:JK: implement me
		
		return(null);
	}
	
	public boolean publishEntireCourse(Selenium browser, AccessSettings access, String catalog){
		/* click publish */
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getCourseEditorPublishCss());
		
		browser.click(selectorBuffer.toString());
		
		/* select all course nodes */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getCourseEditorPublishWizardSelectAllCss())
		.append("')]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		functionalUtil.clickWizardNext(browser);
		
		/* access options */
		functionalUtil.waitForPageToLoadElement(browser, "id=" + getCourseEditorPublishWizardAccessId());
		
		if(access != null){
			functionalUtil.selectOption(browser, getCourseEditorPublishWizardAccessId(), access.getAccessValue());
		}
		
		functionalUtil.clickWizardNext(browser);
		
		/* add to catalog or not */
		functionalUtil.waitForPageToLoadElement(browser, "id=" + getCourseEditorPublishWizardCatalogId());
		
		if(catalog != null){
			functionalUtil.selectOption(browser, getCourseEditorPublishWizardCatalogId(), ADD_TO_CATALOG_YES_VALUE);
			
			String[] catalogSelectors = createCatalogSelectors(catalog);
			
			for(String catalogSelector: catalogSelectors){
				functionalUtil.waitForPageToLoadElement(browser, catalogSelector);
				browser.click(catalogSelector);
			}
		}else{
			functionalUtil.selectOption(browser, getCourseEditorPublishWizardCatalogId(), ADD_TO_CATALOG_NO_VALUE);
		}
		
		functionalUtil.clickWizardFinish(browser);
		
		return(false);
	}

	/**
	 * @param browser
	 * @param node
	 * @param title
	 * @param description
	 * @param position
	 * @return true on success otherwise false
	 * 
	 * Creates the specified course node in a opened course editor.
	 */
	public boolean createCourseNode(Selenium browser, CourseNodeAlias node, String shortTitle, String longTitle, String description, int position){
		/* click on the appropriate link to create node */
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getCourseEditorInsertContentCss())
		.append("')]")
		.append("//a[contains(@class, '")
		.append(node.getIconCss())
		.append("')]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		/* choose insertion point */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=(//form//div[contains(@class, '")
		.append(getCreateCourseNodeTargetPositionItemCss())
		.append("')]//a)[")
		.append(position + 2)
		.append("]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form//button[0]");
		browser.click(selectorBuffer.toString());
		
		/* fill in short title */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=(//form//input[@type='text'])[1]");
		
		browser.type(selectorBuffer.toString(), shortTitle);
		
		/* fill in long title */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=(//form//input[@type='text'])[2]");
		
		browser.type(selectorBuffer.toString(), longTitle);
		
		/* fill in description */
		functionalUtil.typeMCE(browser, description);
		
		/* click save */
		selectorBuffer.append("xpath=//form//button[0]");
		browser.click(selectorBuffer.toString());
		
		return(false);
	}
	
	/**
	 * @param browser
	 * @return true on success
	 * 
	 * Adds an artefact to eportfolio by clicking the appropriate
	 * button.
	 */
	public boolean addToEportfolio(Selenium browser, String binder, String page, String structure,
			String title, String description, String[] tags,
			FunctionalEPortfolioUtil functionalEPortfolioUtil){
		
		/* open wizard */
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getEportfolioAddCss())
		.append("')]");
		
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		if(binder != null){
			/* fill in wizard - title & description */
			functionalEPortfolioUtil.fillInTitleAndDescription(browser, title, description);
			
			/* fill in wizard - tags */
			functionalEPortfolioUtil.fillInTags(browser, tags);
			
			/* fill in wizard - destination */
			String selector = functionalEPortfolioUtil.createSelector(binder, page, structure);
			
			functionalUtil.waitForPageToLoadElement(browser, selector);
			
			browser.click(selector);
			
			/* click finish */
			functionalUtil.clickWizardFinish(browser);
			functionalUtil.waitForPageToUnloadElement(browser, selector);
		}

		return(true);
	}
	
	/**
	 * @param browser
	 * @param courseId
	 * @param nth forum in the course
	 * @return true on success, otherwise false
	 * 
	 * Opens the course with courseId and nth forum within the specified
	 * course.
	 */
	public boolean openForum(Selenium browser, long courseId, int nth){
		if(!functionalRepositorySiteUtil.openCourse(browser, courseId))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();

		selectorBuffer.append("xpath=(//ul//li//a[contains(@class, '")
		.append(getForumIconCss())
		.append("')])[")
		.append(nth + 1)
		.append("]")
		.append("");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param courseId
	 * @param nthForum
	 * @param title
	 * @param message
	 * @return true on success, otherwise false
	 * 
	 * Opens the specified forum in the course and posts a new topic.
	 */
	public boolean postForumMessage(Selenium browser, long courseId, int nthForum, String title, String message){
		if(!openForum(browser, courseId, nthForum))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		/* click open new topic */
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getForumToolbarCss())
		.append("')]//a[contains(@class, '")
		.append(getForumThreadNewCss())
		.append("')]");
		
		browser.click(selectorBuffer.toString());
		functionalUtil.waitForPageToLoad(browser);
		
		/* fill in form - title */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getCourseRunCss())
		.append("')]//form//input[@type='text']");
		
		browser.type(selectorBuffer.toString(), title);
		
//		functionalUtil.waitForPageToLoad(browser);
		
		/* fill in form - post */
		functionalUtil.typeMCE(browser, message);
		
		/* save form */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getCourseRunCss())
		.append("')]//form//button[last()]");
		
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param id
	 * @return
	 * 
	 * Opens the wiki specified by id.
	 */
	public boolean openWiki(Selenium browser, long id){
		browser.open(functionalUtil.getDeploymentPath() + "/url/RepositoryEntry/" + id);
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param wikiId
	 * @param pagename
	 * @param content
	 * @return true on success, otherwise false
	 * 
	 * Creates a new wiki article.
	 */
	public boolean createWikiArticle(Selenium browser, long wikiId, String pagename, String content){
		if(!openWiki(browser, wikiId))
			return(false);
		
		/* type pagename */
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form//div[contains(@class, '")
		.append(getWikiCreateArticleCss())
		.append("')]/..//input[@type='text']");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		browser.type(selectorBuffer.toString(), pagename);
		
		/* click create */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form//div[contains(@class, '")
		.append(getWikiCreateArticleCss())
		.append("')]//button");
		
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		/* edit content */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getWikiArticleBoxCss())
		.append("')]//a");
		
		browser.click(selectorBuffer.toString());

		functionalUtil.waitForPageToLoad(browser);
		
		
		/* fill in text area */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getWikiEditFormWrapperCss())
		.append("')]//textarea");
		
		browser.type(selectorBuffer.toString(), content);
		
		/* click save */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//div[contains(@class, '")
		.append(getWikiEditFormWrapperCss())
		.append("')]//button[last()]");
		
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param id
	 * @return true on success, otherwise false
	 * 
	 * Opens the blog specified by id.
	 */
	public boolean openBlog(Selenium browser, long id){
		browser.open(functionalUtil.getDeploymentPath() + "/url/RepositoryEntry/" + id);
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param courseId
	 * @param nth
	 * @return
	 * 
	 * Opens the course with courseId and nth blog within the specified
	 * course.
	 */
	public boolean openBlogWithoutBusinessPath(Selenium browser, long courseId, int nth){
		if(!functionalRepositorySiteUtil.openCourse(browser, courseId))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();

		selectorBuffer.append("xpath=(//ul//li//a[contains(@class, '")
		.append(getBlogIconCss())
		.append("')])[")
		.append(nth + 1)
		.append("]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}

	/**
	 * @param browser
	 * @param blogId
	 * @param title
	 * @param description
	 * @param content
	 * @return true on success, otherwise false
	 * 
	 * Create a new blog entry.
	 */
	public boolean createBlogEntry(Selenium browser, long courseId, int nth,
			String title, String description, String content){
		if(!openBlogWithoutBusinessPath(browser, courseId, nth))
			return(false);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getBlogCreateEntryCss())
		.append("')]");
		
		browser.click(selectorBuffer.toString());
		
		/* fill in form - title */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=(//form//div[contains(@class, '")
		.append(getBlogFormCss())
		.append("')]//input[@type='text'])[1]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		browser.type(selectorBuffer.toString(), title);
		
		/* fill in form - description */
		functionalUtil.typeMCE(browser, getBlogFormCss(), description);
		
		/* fill in form - content */
		functionalUtil.typeMCE(browser, getBlogFormCss(), content);
		
		/* save form */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form//div[contains(@class, '")
		.append(getBlogFormCss())
		.append("')]//button[last()]");
		
		browser.click(selectorBuffer.toString());
		functionalUtil.waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * @param browser
	 * @param tab
	 * @return true on success
	 * 
	 * Opens the test configurations appropriate tab.
	 */
	public boolean openCourseEditorIQTestTab(Selenium browser, CourseEditorIQTestTab tab){
		return(functionalUtil.openContentTab(browser, tab.ordinal()));
	}
	
	/**
	 * @param browser
	 * @param title
	 * @param description
	 * @return true on success
	 * 
	 * Creates a new test.
	 */
	public boolean createQTITest(Selenium browser, String title, String description){
		if(!openCourseEditorIQTestTab(browser, CourseEditorIQTestTab.TEST_CONFIGURATION))
			return(false);
		
		/* click on "choose, create or import file" button */
		StringBuffer selectorBuffer = new StringBuffer();

		selectorBuffer.append("xpath=//a[contains(@class '")
		.append(getTestChooseRepositoryFileCss())
		.append("']");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		/* click create button */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//a[contains(@class, '")
		.append(getTestCreateResourceCss())
		.append("')]");
		
		functionalUtil.waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		functionalUtil.waitForPageToLoad(browser);
		
		/* fill in title */
		selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form//input[@type='text']");
		
		browser.type(selectorBuffer.toString(), title);
		
		/* fill in description */
		functionalUtil.typeMCE(browser, description);
		
		/* click save */
		selectorBuffer.append("xpath=");
		
		browser.click(selectorBuffer.toString());
		
		/* click next */
		functionalUtil.clickWizardNext(browser);
		
		return(false);
	}
	
	public FunctionalUtil getFunctionalUtil() {
		return functionalUtil;
	}

	public void setFunctionalUtil(FunctionalUtil functionalUtil) {
		this.functionalUtil = functionalUtil;
	}

	public FunctionalRepositorySiteUtil getFunctionalRepositorySiteUtil() {
		return functionalRepositorySiteUtil;
	}

	public void setFunctionalRepositorySiteUtil(
			FunctionalRepositorySiteUtil functionalRepositorySiteUtil) {
		this.functionalRepositorySiteUtil = functionalRepositorySiteUtil;
	}

	public String getCourseRunCss() {
		return courseRunCss;
	}

	public void setCourseRunCss(String courseRunCss) {
		this.courseRunCss = courseRunCss;
	}

	public String getCourseOpenEditorCss() {
		return courseOpenEditorCss;
	}

	public void setCourseOpenEditorCss(String courseOpenEditorCss) {
		this.courseOpenEditorCss = courseOpenEditorCss;
	}

	public String getCourseEditorPublishCss() {
		return courseEditorPublishCss;
	}

	public void setCourseEditorPublishCss(String courseEditorPublishCss) {
		this.courseEditorPublishCss = courseEditorPublishCss;
	}

	public String getCourseEditorPublishWizardSelectAllCss() {
		return courseEditorPublishWizardSelectAllCss;
	}

	public void setCourseEditorPublishWizardSelectAllCss(
			String courseEditorPublishWizardSelectAllCss) {
		this.courseEditorPublishWizardSelectAllCss = courseEditorPublishWizardSelectAllCss;
	}

	public String getCourseEditorPublishWizardAccessId() {
		return courseEditorPublishWizardAccessId;
	}

	public void setCourseEditorPublishWizardAccessId(
			String courseEditorPublishWizardAccessId) {
		this.courseEditorPublishWizardAccessId = courseEditorPublishWizardAccessId;
	}

	public String getCourseEditorPublishWizardCatalogId() {
		return courseEditorPublishWizardCatalogId;
	}

	public void setCourseEditorPublishWizardCatalogId(
			String courseEditorPublishWizardCatalogId) {
		this.courseEditorPublishWizardCatalogId = courseEditorPublishWizardCatalogId;
	}

	public String getCourseEditorInsertContentCss() {
		return courseEditorInsertContentCss;
	}

	public void setCourseEditorInsertContentCss(String courseEditorInsertContentCss) {
		this.courseEditorInsertContentCss = courseEditorInsertContentCss;
	}

	public String getCreateCourseNodeTargetPositionItemCss() {
		return createCourseNodeTargetPositionItemCss;
	}

	public void setCreateCourseNodeTargetPositionItemCss(
			String createCourseNodeTargetPositionItemCss) {
		this.createCourseNodeTargetPositionItemCss = createCourseNodeTargetPositionItemCss;
	}

	public String getEportfolioAddCss() {
		return eportfolioAddCss;
	}

	public void setEportfolioAddCss(String eportfolioAddCss) {
		this.eportfolioAddCss = eportfolioAddCss;
	}

	public String getForumIconCss() {
		return forumIconCss;
	}

	public void setForumIconCss(String forumIconCss) {
		this.forumIconCss = forumIconCss;
	}

	public String getBlogIconCss() {
		return blogIconCss;
	}

	public void setBlogIconCss(String blogIconCss) {
		this.blogIconCss = blogIconCss;
	}

	public String getForumToolbarCss() {
		return forumToolbarCss;
	}

	public void setForumToolbarCss(String forumToolbarCss) {
		this.forumToolbarCss = forumToolbarCss;
	}

	public String getForumThreadNewCss() {
		return forumThreadNewCss;
	}

	public void setForumThreadNewCss(String forumThreadNewCss) {
		this.forumThreadNewCss = forumThreadNewCss;
	}

	public String getForumArchiveCss() {
		return forumArchiveCss;
	}

	public void setForumArchiveCss(String forumArchiveCss) {
		this.forumArchiveCss = forumArchiveCss;
	}

	public String getForumFilterCss() {
		return forumFilterCss;
	}

	public void setForumFilterCss(String forumFilterCss) {
		this.forumFilterCss = forumFilterCss;
	}

	public String getWikiCreateArticleCss() {
		return wikiCreateArticleCss;
	}

	public void setWikiCreateArticleCss(String wikiCreateArticleCss) {
		this.wikiCreateArticleCss = wikiCreateArticleCss;
	}

	public String getWikiArticleBoxCss() {
		return wikiArticleBoxCss;
	}

	public void setWikiArticleBoxCss(String wikiArticleBoxCss) {
		this.wikiArticleBoxCss = wikiArticleBoxCss;
	}

	public String getWikiEditFormWrapperCss() {
		return wikiEditFormWrapperCss;
	}

	public void setWikiEditFormWrapperCss(String wikiEditFormWrapperCss) {
		this.wikiEditFormWrapperCss = wikiEditFormWrapperCss;
	}

	public String getBlogCreateEntryCss() {
		return blogCreateEntryCss;
	}

	public void setBlogCreateEntryCss(String blogCreateEntryCss) {
		this.blogCreateEntryCss = blogCreateEntryCss;
	}

	public String getBlogFormCss() {
		return blogFormCss;
	}

	public void setBlogFormCss(String blogFormCss) {
		this.blogFormCss = blogFormCss;
	}

	public String getTestChooseRepositoryFileCss() {
		return testChooseRepositoryFileCss;
	}

	public void setTestChooseRepositoryFileCss(String testChooseRepositoryFileCss) {
		this.testChooseRepositoryFileCss = testChooseRepositoryFileCss;
	}

	public String getTestCreateResourceCss() {
		return testCreateResourceCss;
	}

	public void setTestCreateResourceCss(String testCreateResourceCss) {
		this.testCreateResourceCss = testCreateResourceCss;
	}
	
}
