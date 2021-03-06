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
package org.olat.selenium.page.course;

import java.net.URL;
import java.util.List;

import org.jboss.arquillian.graphene.Graphene;
import org.junit.Assert;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.selenium.page.core.BookingPage;
import org.olat.selenium.page.core.MenuTreePageFragment;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.repository.RepositoryAccessPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CoursePageFragment {
	
	public static final By courseRun = By.className("o_course_run");
	public static final By toolsMenu = By.cssSelector("ul.o_sel_course_tools");
	public static final By toolsMenuCaret = By.cssSelector("a.o_sel_course_tools");

	public static final By settingsMenu = By.cssSelector("ul.o_sel_course_settings");
	public static final By settingsMenuCaret = By.cssSelector("a.o_sel_course_settings");
	
	public static final By editCourseBy = By.className("o_sel_course_editor");
	public static final By accessConfigBy = By.className("o_sel_course_access");
	public static final By bookingBy = By.className("o_sel_course_ac_tool");
	public static final By assessmentToolBy = By.className("o_sel_course_assessment_tool");
	public static final By assessmentModeBy = By.className("o_sel_course_assessment_mode");
	public static final By membersCourseBy = By.className("o_sel_course_members");
	public static final By treeContainerBy = By.id("o_main_left_content");
	public static final By efficiencyStatementsBy = By.className(" o_sel_course_options_certificates");
	
	private WebDriver browser;
	
	public CoursePageFragment(WebDriver browser) {
		this.browser = browser;
	}
	
	public static CoursePageFragment getCourse(WebDriver browser, URL deploymentUrl, CourseVO course) {
		browser.navigate().to(deploymentUrl.toExternalForm() + "url/RepositoryEntry/" + course.getRepoEntryKey());
		OOGraphene.waitElement(courseRun, browser);
		return new CoursePageFragment(browser);
	}
	
	public static CoursePageFragment getCourse(WebDriver browser) {
		OOGraphene.waitElement(courseRun, browser);
		return new CoursePageFragment(browser);
	}
	
	public CoursePageFragment assertOnCoursePage() {
		WebElement treeContainer = browser.findElement(treeContainerBy);
		Assert.assertTrue(treeContainer.isDisplayed());
		return this;
	}
	
	public CoursePageFragment assertOnTitle(String displayName) {
		By titleBy = By.xpath("//h2[text()[contains(.,'" + displayName + "')]]");
		OOGraphene.waitElement(titleBy, 5, browser);
		
		WebElement titleEl = browser.findElement(titleBy);
		Assert.assertNotNull(titleEl);
		Assert.assertTrue(titleEl.isDisplayed());
		return this;
	}
	
	/**
	 * Assert if the password field is displayed.
	 * @return
	 */
	public CoursePageFragment assertOnPassword() {
		By passwordBy = By.cssSelector(".o_sel_course_password_form input[type='password']");
		List<WebElement> passwordEls = browser.findElements(passwordBy);
		Assert.assertEquals(1, passwordEls.size());
		return this;
	}
	
	public CoursePageFragment enterPassword(String password) {
		By passwordBy = By.cssSelector(".o_sel_course_password_form .o_sel_course_password input[type='password']");
		browser.findElement(passwordBy).sendKeys(password);
		
		By enterBy = By.cssSelector(".o_sel_course_password_form button.btn-primary");
		browser.findElement(enterBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	/**
	 * Wait until the restart button appears or make an error.
	 * 
	 * @return
	 */
	public CoursePageFragment assertOnRestart() {
		By restartBy = By.cssSelector("a.btn.o_sel_course_restart");
		OOGraphene.waitElement(restartBy, 10, browser);
		return this;
	}
	
	public CoursePageFragment clickRestart() {
		By restartBy = By.cssSelector("a.btn.o_sel_course_restart");
		browser.findElement(restartBy).click();
		OOGraphene.waitBusy(browser);
		OOGraphene.waitElement(courseRun, 5, browser);
		return this;
	}
	
	/**
	 * Click the first element of the menu tree
	 * @return
	 */
	public MenuTreePageFragment clickTree() {
		OOGraphene.waitElement(MenuTreePageFragment.treeBy, 2, browser);
		MenuTreePageFragment menuTree = new MenuTreePageFragment(browser);
		menuTree.selectRoot();
		return menuTree;
	}
	
	/**
	 * Open the tools drop-down
	 * @return
	 */
	public CoursePageFragment openToolsMenu() {
		browser.findElement(toolsMenuCaret).click();
		OOGraphene.waitElement(toolsMenu, 5, browser);
		return this;
	}
	
	/**
	 * Open the settings drop-down
	 * @return
	 */
	public CoursePageFragment openSettingsMenu() {
		browser.findElement(settingsMenuCaret).click();
		OOGraphene.waitElement(settingsMenu, browser);
		return this;
	}
	
	public RemindersPage reminders() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		By reminderBy = By.cssSelector("a.o_sel_course_reminders");
		browser.findElement(reminderBy).click();
		OOGraphene.waitBusy(browser);
		return new RemindersPage(browser);
	}
	
	public CourseOptionsPage options() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		
		By reminderBy = By.cssSelector("a.o_sel_course_options");
		browser.findElement(reminderBy).click();
		OOGraphene.waitBusy(browser);
		return new CourseOptionsPage(browser);
	}
	
	/**
	 * Click the editor link in the tools drop-down
	 * @return
	 */
	public CourseEditorPageFragment edit() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		browser.findElement(editCourseBy).click();
		OOGraphene.waitBusy(browser);
		OOGraphene.closeBlueMessageWindow(browser);
		return new CourseEditorPageFragment(browser);
	}
	
	/**
	 * Click the members link in the tools drop-down
	 * @return
	 */
	public MembersPage members() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		browser.findElement(membersCourseBy).click();
		OOGraphene.waitBusy(browser);

		By mainId = By.id("o_main");
		OOGraphene.waitElement(mainId, 5, browser);
		return new MembersPage(browser);
	}
	
	public AssessmentToolPage assessmentTool() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		browser.findElement(assessmentToolBy).click();
		OOGraphene.waitBusy(browser);

		WebElement main = browser.findElement(By.id("o_assessment_tool_main"));
		Assert.assertTrue(main.isDisplayed());
		return new AssessmentToolPage(browser);
	}
	
	public AssessmentModePage assessmentConfiguration() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		browser.findElement(assessmentModeBy).click();
		OOGraphene.waitBusy(browser);

		WebElement main = browser.findElement(By.id("o_main_container"));
		return Graphene.createPageFragment(AssessmentModePage.class, main);
	}
	
	public RepositoryAccessPage accessConfiguration() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		browser.findElement(accessConfigBy).click();
		OOGraphene.waitBusy(browser);

		By mainId = By.id("o_main_container");
		OOGraphene.waitElement(mainId, 5, browser);
		return new RepositoryAccessPage(browser);
	}
	
	public EfficiencyStatementConfigurationPage efficiencyStatementConfiguration() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		browser.findElement(efficiencyStatementsBy).click();
		OOGraphene.waitBusy(browser);

		WebElement main = browser.findElement(By.id("o_main_container"));
		return Graphene.createPageFragment(EfficiencyStatementConfigurationPage.class, main);
	}
	
	public BookingPage bookingTool() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		browser.findElement(bookingBy).click();
		OOGraphene.waitBusy(browser);
		return new BookingPage(browser);
	}
}
