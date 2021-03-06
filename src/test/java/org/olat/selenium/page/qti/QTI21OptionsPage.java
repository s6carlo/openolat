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
package org.olat.selenium.page.qti;

import org.olat.ims.qti21.QTI21DeliveryOptions.ShowResultsOnFinish;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 17 nov. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI21OptionsPage {
	
	private final WebDriver browser;
	
	public QTI21OptionsPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public QTI21OptionsPage showResults(Boolean show, ShowResultsOnFinish level) {
		By showResultsBy = By.cssSelector("div.o_sel_qti_show_results input[type='checkbox']");
		WebElement showResultsEl = browser.findElement(showResultsBy);
		OOGraphene.check(showResultsEl, show);
		OOGraphene.waitBusy(browser);
		
		By resultsLevelBy = By.cssSelector("div.o_sel_qti_show_results_options input[type='radio'][value='" + level + "']");
		OOGraphene.waitElement(resultsLevelBy, 5, browser);
		browser.findElement(resultsLevelBy).click();
		return this;
	}
	
	public QTI21OptionsPage save() {
		By saveBy = By.cssSelector("fieldset.o_sel_qti_resource_options button");
		browser.findElement(saveBy).click();
		OOGraphene.waitBusy(browser);		
		return this;
	}
}
