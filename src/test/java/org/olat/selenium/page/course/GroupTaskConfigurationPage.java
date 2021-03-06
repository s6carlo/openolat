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

import java.io.File;
import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Drive the configuration of the course element of type group task.
 * 
 * Initial date: 03.06.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GroupTaskConfigurationPage {

	@Drone
	private WebDriver browser;
	
	public GroupTaskConfigurationPage() {
		//
	}
	
	public GroupTaskConfigurationPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public GroupTaskConfigurationPage selectWorkflow() {
		By configBy = By.className("o_sel_course_gta_steps");
		return selectTab(configBy);
	}
	
	public GroupTaskConfigurationPage saveWorkflow() {
		By saveBy = By.cssSelector(".o_sel_course_gta_save_workflow button.btn-primary");
		browser.findElement(saveBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage openBusinessGroupChooser() {
		By chooseGroupBy = By.cssSelector("a.o_form_groupchooser");
		browser.findElement(chooseGroupBy).click();
		OOGraphene.waitBusy(browser);
		OOGraphene.waitModalDialog(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage createBusinessGroup(String name) {
		By createGroupBy = By.cssSelector("div.o_button_group_right a");
		browser.findElement(createGroupBy).click();
		OOGraphene.waitBusy(browser);
		OOGraphene.waitModalDialog(browser);
		
		//fill the form
		By nameBy = By.cssSelector(".o_sel_group_edit_title input[type='text']");
		browser.findElement(nameBy).sendKeys(name);
		OOGraphene.tinymce("-", browser);
		
		//save the group
		By submitBy = By.cssSelector(".o_sel_group_edit_group_form button.btn-primary");
		WebElement submitButton = browser.findElement(submitBy);
		submitButton.click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage confirmBusinessGroupsSelection() {
		By saveBy = By.cssSelector(".o_sel_group_selection_groups button.btn-primary");
		WebElement saveButton = browser.findElement(saveBy);
		saveButton.click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage selectAssessment() {
		By configBy = By.className("o_sel_course_ms_form");
		return selectTab(configBy);
	}
	
	public GroupTaskConfigurationPage selectAssignment() {
		By configBy = By.className("o_sel_course_gta_tasks");
		return selectTab(configBy);
	}
	
	public GroupTaskConfigurationPage selectSolution() {
		By configBy = By.className("o_sel_course_gta_solutions");
		return selectTab(configBy);
	}
	
	public GroupTaskConfigurationPage uploadTask(String title, File file) {
		By addTaskBy = By.className("o_sel_course_gta_add_task");
		browser.findElement(addTaskBy).click();
		OOGraphene.waitBusy(browser);
		
		By titleBy = By.cssSelector(".o_sel_course_gta_upload_task_title input[type='text']");
		browser.findElement(titleBy).sendKeys(title);
		
		By inputBy = By.cssSelector(".o_fileinput input[type='file']");
		OOGraphene.uploadFile(inputBy, file, browser);
		OOGraphene.waitBusy(browser);
		
		//save
		By saveBy = By.cssSelector(".o_sel_course_gta_upload_task_form button.btn-primary");
		browser.findElement(saveBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage saveTasks() {
		By saveBy = By.cssSelector(".o_sel_course_gta_task_config_buttons button.btn-primary");
		List<WebElement> saveEls = browser.findElements(saveBy);
		Assert.assertEquals(1, saveEls.size());
		saveEls.get(0).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage uploadSolution(String title, File file) {
		By addTaskBy = By.className("o_sel_course_gta_add_solution");
		browser.findElement(addTaskBy).click();
		OOGraphene.waitBusy(browser);
		
		By titleBy = By.cssSelector(".o_sel_course_gta_upload_solution_title input[type='text']");
		browser.findElement(titleBy).sendKeys(title);
		
		By inputBy = By.cssSelector(".o_fileinput input[type='file']");
		OOGraphene.uploadFile(inputBy, file, browser);
		OOGraphene.waitBusy(browser);
		
		//save
		By saveBy = By.cssSelector(".o_sel_course_gta_upload_solution_form button.btn-primary");
		List<WebElement> saveEls = browser.findElements(saveBy);
		Assert.assertEquals(1, saveEls.size());
		saveEls.get(0).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public GroupTaskConfigurationPage setAssessmentOptions(Float minVal, Float maxVal, Float cutVal) {
		new AssessmentCEConfigurationPage(browser).setScoreAuto(minVal, maxVal, cutVal);
		return this;
	}
	
	public GroupTaskConfigurationPage saveAssessmentOptions() {
		By saveBy = By.cssSelector(".o_sel_course_ms_form button.btn-primary");
		browser.findElement(saveBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	private GroupTaskConfigurationPage selectTab(By tabBy) {
		List<WebElement> tabLinks = browser.findElements(CourseEditorPageFragment.navBarNodeConfiguration);

		boolean found = false;
		a_a:
		for(WebElement tabLink:tabLinks) {
			tabLink.click();
			OOGraphene.waitBusy(browser);
			List<WebElement> elements = browser.findElements(tabBy);
			if(elements.size() > 0) {
				found = true;
				break a_a;
			}
		}

		Assert.assertTrue("Found the tab", found);
		return this;
	}
}
