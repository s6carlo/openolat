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

package org.olat.core.util.i18n.ui;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;

/**
 * <h3>Description:</h3> This controller offers a workflow to configure the
 * system languages:
 * <ul>
 * <li>System default language</li>
 * <li>Enabled languages</li>
 * <li>Create new language</li>
 * <li>Delete languages</li>
 * <li>Import language packs</li>
 * <li>Export language packs</li>
 * <li>Delete language packs</li>
 * </ul>
 * The configuration is activated right away and saved through the I8nModule to
 * the system configuration properties files <h3>Events thrown by this
 * controller:</h3> none
 * <p>
 * Initial Date: 24.11.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

class I18nConfigController extends FormBasicController {
	private SingleSelection defaultLangSelection;
	private MultipleSelectionElement enabledLangSelection;
	private FormLink createLanguageLink, deleteLanguageLink, importPackageLink, exportPackageLink, deletePackageLink;
	private CloseableModalController cmc;
	private Controller subCtr;

	/**
	 * Constructor for the language configuration workflow
	 * 
	 * @param ureq
	 * @param control
	 */
	public I18nConfigController(UserRequest ureq, WindowControl control) {
		super(ureq, control, "i18nConfiguration");
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		I18nManager i18nMgr = I18nManager.getInstance();
		//
		// Add default languages pulldown
		Set<String> availableKeys = I18nModule.getAvailableLanguageKeys();
		String[] defaultlangKeys = ArrayHelper.toArray(availableKeys);
		String[] defaultLangValues = new String[defaultlangKeys.length];
		for (int i = 0; i < defaultlangKeys.length; i++) {
			String key = defaultlangKeys[i];
			String explLang = i18nMgr.getLanguageInEnglish(key, I18nModule.isOverlayEnabled());
			String all = explLang;
			if (explLang != null && !explLang.equals(key)) all += " (" + key + ")";
			defaultLangValues[i] = all;
		}
		ArrayHelper.sort(defaultlangKeys, defaultLangValues, false, true, false);
		String[] defaultLangCssClasses = i18nMgr.createLanguageFlagsCssClasses(defaultlangKeys, "o_flag");
		defaultLangSelection = uifactory.addDropdownSingleselect("configuration.defaultLangSelection", formLayout, defaultlangKeys,
				defaultLangValues, defaultLangCssClasses);
		defaultLangSelection.addActionListener(FormEvent.ONCHANGE);
		// Enable the current default language
		Locale defaultLocale = I18nModule.getDefaultLocale();
		flc.contextPut("defaultLangKey", defaultLocale.toString());
		defaultLangSelection.select(defaultLocale.toString(), true);
		//
		// Add enabled languages checkboxes
		String[] availablelangKeys = ArrayHelper.toArray(availableKeys);
		String[] availableValues = new String[availablelangKeys.length];
		int referenceKeyCount = i18nMgr.countI18nItems(I18nModule.getFallbackLocale(), null, true);
		for (int i = 0; i < availablelangKeys.length; i++) {
			String key = availablelangKeys[i];
			String explLang = i18nMgr.getLanguageInEnglish(key, I18nModule.isOverlayEnabled());
			String all = explLang;
			if (explLang != null && !explLang.equals(key)) all += " (" + key + ")";
			// count translation status
			int keyCount = i18nMgr.countI18nItems(i18nMgr.getLocaleOrNull(key), null, true);
			if(keyCount > 0) {
				all += "   <span class='b_translation_status'>" + (keyCount * 100 / referenceKeyCount) + "%</span>";
			} else {
				all += "   <span class='b_translation_status'>0%</span>";
			}
			availableValues[i] = all;
		}
		ArrayHelper.sort(availablelangKeys, availableValues, false, true, false);
		String[] availableLangIconCss = i18nMgr.createLanguageFlagsCssClasses(availablelangKeys, "o_flag");
		enabledLangSelection = uifactory.addCheckboxesVertical("configuration.enabledLangSelection", null, formLayout, availablelangKeys,
				availableValues, null, availableLangIconCss, 3);
		enabledLangSelection.setEscapeHtml(false);
		enabledLangSelection.addActionListener(FormEvent.ONCLICK); // Radios/Checkboxes need onclick because of IE bug OLAT-5753
		// Enable current enabled languages
		for (String langKey : I18nModule.getEnabledLanguageKeys()) {
			enabledLangSelection.select(langKey, true);
		}
		//
		// Add create / delete links, but only when translation tool is configured
		if (I18nModule.isTransToolEnabled()) {
			createLanguageLink = uifactory.addFormLink("configuration.management.create", formLayout, Link.BUTTON);
			deleteLanguageLink = uifactory.addFormLink("configuration.management.delete", formLayout, Link.BUTTON);
		}

		importPackageLink = uifactory.addFormLink("configuration.management.package.import", formLayout, Link.BUTTON);
		exportPackageLink = uifactory.addFormLink("configuration.management.package.export", formLayout, Link.BUTTON);

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		// save happens on inner form events after each click, no explicit form submit
	}

	/**
	 * 
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem,
	 *      org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == defaultLangSelection) {
			// Get new default language and update I18nModule accordingly
			String langKey = defaultLangSelection.getSelectedKey();
			Locale defaultLocale = I18nManager.getInstance().getLocaleOrNull(langKey);
			this.flc.contextPut("defaultLangKey", defaultLocale.toString());
			I18nModule.setDefaultLocale(defaultLocale);
			// Make sure this language is in the list of enabled languages
			enabledLangSelection.select(langKey, true);
			I18nModule.getEnabledLanguageKeys().add(langKey);

		} else if (source == enabledLangSelection) {
			// Get enabled values, make sure the default language is enabled and
			// update the I18nModule
			Collection<String> enabledLangKeys = enabledLangSelection.getSelectedKeys();
			Locale defaultLocale = I18nModule.getDefaultLocale();
			// Check if default language is still enabled
			if (!enabledLangKeys.contains(defaultLocale.toString())) {
				enabledLangSelection.select(defaultLocale.toString(), true);
				enabledLangKeys.add(defaultLocale.toString());
				showWarning("configuration.default.lang.must.be.enabed", defaultLocale.toString());
			}
			
// fxdiff FXOLAT-40 don't force fallback language to be enabled in the GUI, 
// conflict with languages with country/variant information
//			// Check if fallback language is still enabled
//			String fallbackLangKey = I18nModule.getFallbackLocale().toString();
//			if (!enabledLangKeys.contains(fallbackLangKey)) {
//				enabledLangSelection.select(fallbackLangKey, true);
//				enabledLangKeys.add(fallbackLangKey);
//				showWarning("configuration.fallback.lang.must.be.enabed", fallbackLangKey);
//			}

			I18nModule.setEnabledLanguageKeys(enabledLangKeys);

		} else if (source == createLanguageLink) {
			// Show new language sub form in an overlay window
			subCtr = new I18nConfigSubNewLangController(ureq, getWindowControl());
			listenTo(subCtr);
			cmc = new CloseableModalController(getWindowControl(), "close", subCtr.getInitialComponent());
			cmc.activate();
			listenTo(cmc);

		} else if (source == deleteLanguageLink) {
			// Show delete language sub form in an overlay window
			subCtr = new I18nConfigSubDeleteLangController(ureq, getWindowControl());
			listenTo(subCtr);
			cmc = new CloseableModalController(getWindowControl(), "close", subCtr.getInitialComponent());
			cmc.activate();
			listenTo(cmc);

		} else if (source == importPackageLink) {
			// Show import languages sub form in an overlay window
			subCtr = new I18nConfigSubImportLangController(ureq, getWindowControl());
			listenTo(subCtr);
			cmc = new CloseableModalController(getWindowControl(), "close", subCtr.getInitialComponent());
			cmc.activate();
			listenTo(cmc);

		} else if (source == exportPackageLink) {
			// Show export languages sub form in an overlay window
			subCtr = new I18nConfigSubExportLangController(ureq, getWindowControl());
			listenTo(subCtr);
			cmc = new CloseableModalController(getWindowControl(), "close", subCtr.getInitialComponent());
			cmc.activate();
			listenTo(cmc);

		} else if (source == deletePackageLink) {
			// Show export languages sub form in an overlay window
			subCtr = new I18nConfigSubDeletePackageController(ureq, getWindowControl());
			listenTo(subCtr);
			cmc = new CloseableModalController(getWindowControl(), "close", subCtr.getInitialComponent());
			cmc.activate();
			listenTo(cmc);
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == cmc) {
			// must be close event
			removeAsListenerAndDispose(cmc);
			removeAsListenerAndDispose(subCtr);
		} else if (source == subCtr) {
			// just reinitialize everything regardless of what happened (simple way)
			// and remove controllers
			initForm(this.flc, this, ureq);
			cmc.deactivate();
			removeAsListenerAndDispose(cmc);
			removeAsListenerAndDispose(subCtr);
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
	// nothing to dispose, sub controller autodisposed by basic controller
	}
}
