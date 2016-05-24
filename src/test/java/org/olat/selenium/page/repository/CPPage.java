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
package org.olat.selenium.page.repository;

import java.util.List;

import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 04.12.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CPPage {
	
	private final By toolsMenu = By.cssSelector("ul.o_sel_repository_tools");
	private static final By settingsMenu = By.cssSelector("ul.o_sel_course_settings");
	
	private WebDriver browser;
	
	public CPPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public CPPage assertInIFrame(By by) {
		By iframeBy = By.xpath("//div[contains(@class,'o_iframedisplay')]//iframe");
		OOGraphene.waitElement(iframeBy, 2, browser);
		List<WebElement> iframes = browser.findElements(iframeBy);
		browser = browser.switchTo().frame(iframes.get(0));
		
		OOGraphene.waitElement(by, 5, browser);
		List<WebElement> elements = browser.findElements(by);
		Assert.assertFalse(elements.isEmpty());
		
		browser = browser.switchTo().defaultContent();
		return this;
	}
	
	public CPPage assertPageDeleted(String title) {
		By pageBy = By.xpath("//a[@title='" + title + "']");
		List<WebElement> pageEls = browser.findElements(pageBy);
		Assert.assertTrue(pageEls.isEmpty());
		return this;
	}
	
	public CPPage selectPage(String title) {
		By pageBy = By.xpath("//a[@title='" + title + "']/i");
		browser.findElement(pageBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public CPEditorPage openEditor() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}

		By editBy = By.xpath("//ul[contains(@class,'o_sel_repository_tools')]//a[contains(@onclick,'edit.cmd')]");
		browser.findElement(editBy).click();
		OOGraphene.waitBusy(browser);
		return new CPEditorPage(browser);
	}

	/**
	 * Open the access configuration
	 * 
	 * @return
	 */
	public RepositoryAccessPage accessConfiguration() {
		if(!browser.findElement(settingsMenu).isDisplayed()) {
			openSettingsMenu();
		}
		By accessConfigBy = By.cssSelector("a.o_sel_course_access");
		browser.findElement(accessConfigBy).click();
		OOGraphene.waitBusy(browser);

		By mainId = By.id("o_main_container");
		OOGraphene.waitElement(mainId, 5, browser);
		return new RepositoryAccessPage(browser);
	}
	
	/**
	 * Open the tools menu
	 * @return
	 */
	public CPPage openToolsMenu() {
		By toolsMenuCaret = By.cssSelector("a.o_sel_repository_tools");
		browser.findElement(toolsMenuCaret).click();
		OOGraphene.waitElement(toolsMenu, browser);
		return this;
	}
	
	/**
	 * Open the settings menu.
	 * @return
	 */
	public CPPage openSettingsMenu() {
		By toolsMenuCaret = By.cssSelector("a.o_sel_course_settings");
		browser.findElement(toolsMenuCaret).click();
		OOGraphene.waitElement(settingsMenu, browser);
		return this;
	}

}
