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
* <p>
*/ 
package org.olat.core.util.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.Preferences;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Description:<br>
 * Some mail helpers
 * <P>
 * Initial Date: 21.11.2006 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH<br>
 *         http://www.frentix.com
 */
public class MailHelper {
	private static Map<String, Translator> translators = new HashMap<String, Translator>();
	
	
	public static String getMailFooter(Locale locale) {
		Translator trans = getTranslator(locale);
		return trans.translate("footer.no.userdata", new String[] { Settings.getServerContextPathURI() });
	}
	
	public static String getMailFooter(Identity sender) {
		Preferences prefs = sender.getUser().getPreferences();
		Locale locale = I18nManager.getInstance().getLocaleOrDefault(prefs.getLanguage());
		return getMailFooter(locale, sender);
	}
	
	public static String getMailFooter(MailBundle bundle) {
		if(bundle.getFromId() != null) {
			return getMailFooter(bundle.getFromId());
		}
		return getMailFooter(I18nModule.getDefaultLocale());
	}
	

	/**
	 * Create a mail footer for the given locale and sender.
	 * 
	 * @param locale Defines language of footer text. If null, the systems default
	 *          locale is used
	 * @param sender Details about sender embedded in mail footer. If null no such
	 *          details are attached to the footer
	 * @return The mail footer as string
	 */
	public static String getMailFooter(Locale locale, Identity sender) {
		if (locale == null) {
			locale = I18nModule.getDefaultLocale();
		}
		Translator trans = getTranslator(locale);
		if (sender == null) {
			// mail sent by plattform configured sender address
			return trans.translate("footer.no.userdata", new String[] { Settings.getServerContextPathURI() });
		}
		// mail sent by a system user
		User user = sender.getUser();

		// FXOLAT-356: separate context for mail footer
		// username / server-url are always first [0], [1].		
		UserManager um = UserManager.getInstance();
		List<UserPropertyHandler> userPropertyHandlers = um.getUserPropertyHandlersFor(MailHelper.class.getCanonicalName(), false);
		List<String> userPropList = new ArrayList<String>(userPropertyHandlers.size()+2);
		userPropList.add(sender.getUser().getProperty(UserConstants.EMAIL, null));
		userPropList.add(Settings.getServerContextPathURI());
		for (Iterator<UserPropertyHandler> iterator = userPropertyHandlers.iterator(); iterator.hasNext();) {
			userPropList.add(iterator.next().getUserProperty(user, locale));
		}
		// add empty strings to prevent non-replaced wildcards like "{5}" etc. in emails.
		while (userPropList.size() < 15){
			userPropList.add("");
		}
		
		String[] userPropArr = userPropList.toArray(new String[userPropList.size()]);
		for(int i=userPropArr.length; i-->0; ) {
			if(userPropArr[i] == null) {
				userPropArr[i] = "";
			}
		}
		return trans.translate("footer.with.userdata", userPropArr);
	}
	
	public static String getTitleForFailedUsersError(Locale locale) {
		return getTranslator(locale).translate("mailhelper.error.failedusers.title");
	}
	
	public static String getMessageForFailedUsersError(Locale locale, List<Identity> disabledIdentities) { 
		String message = getTranslator(locale).translate("mailhelper.error.failedusers");
		message += "\n<ul>\n";
		for (Identity identity : disabledIdentities) {
			message += "<li>\n";
			message += identity.getUser().getProperty(UserConstants.FIRSTNAME, null);
			message += " ";
			message += identity.getUser().getProperty(UserConstants.LASTNAME, null);
			message += "\n</li>\n";
		}
		message += "</ul>\n";
		return message;
	}

	/**
	 * Helper method to reuse translators. It makes no sense to build a new
	 * translator over and over again. We keep one for each language and reuse
	 * this one during the whole lifetime
	 * 
	 * @param locale
	 * @return a translator for the given locale
	 */
	private static Translator getTranslator(Locale locale) {
		String ident = locale.toString();
		synchronized (translators) {  //o_clusterok   brasato:::: nice idea, but move to translatorfactory and kick out translator.setLocale() (move it to LocaleChangableTranslator)
			Translator trans = translators.get(ident);
			if (trans == null) {
				trans = Util.createPackageTranslator(MailHelper.class, locale);
				translators.put(ident, trans);
			}
			return trans;
		}
	}

	/**
	 * Method to evaluate the mailer result and disply general error and warning
	 * messages. If you want to display other messages instead you have to
	 * evaluate the mailer result yourself and print messages accordingly.
	 * 
	 * @param mailerResult The mailer result to be evaluated
	 * @param wControl The current window controller
	 * @param locale The users local
	 */
	public static void printErrorsAndWarnings(MailerResult mailerResult, WindowControl wControl, Locale locale) {
		StringBuilder errors = new StringBuilder();
		StringBuilder warnings = new StringBuilder();
		appendErrorsAndWarnings(mailerResult, errors, warnings, locale);
		// now print a warning to the users screen
		if (errors.length() > 0) {
			wControl.setError(errors.toString());
		}
		if (warnings.length() > 0) {
			wControl.setWarning(warnings.toString());
		}
	}

	/**
	 * Method to evaluate the mailer result. The errors and warnings will be
	 * attached to the given string buffers. If you want to display other messages
	 * instead you have to evaluate the mailer result yourself and print messages
	 * accordingly.
	 * 
	 * @param mailerResult The mailer result to be evaluated
	 * @param errors StringBuilder for the error messages
	 * @param warnings StringBuilder for the warnings
	 * @param locale The users local
	 */
	public static void appendErrorsAndWarnings(MailerResult mailerResult, StringBuilder errors, StringBuilder warnings, Locale locale) {
		Translator trans = Util.createPackageTranslator(MailerResult.class, locale);
		int returnCode = mailerResult.getReturnCode();
		List<Identity> failedIdentites = mailerResult.getFailedIdentites();

		// first the severe errors
		if (returnCode == MailerResult.SEND_GENERAL_ERROR) {
			errors.append("<p>").append(trans.translate("mailhelper.error.send.general")).append("</p>");
		} else if (returnCode == MailerResult.SENDER_ADDRESS_ERROR) {
			errors.append("<p>").append(trans.translate("mailhelper.error.sender.address")).append("</p>");
		} else if (returnCode == MailerResult.RECIPIENT_ADDRESS_ERROR) {
			errors.append("<p>").append(trans.translate("mailhelper.error.recipient.address")).append("</p>");
		} else if (returnCode == MailerResult.TEMPLATE_GENERAL_ERROR) {
			errors.append("<p>").append(trans.translate("mailhelper.error.template.general")).append("</p>");
		} else if (returnCode == MailerResult.TEMPLATE_PARSE_ERROR) {
			errors.append("<p>").append(trans.translate("mailhelper.error.template.parse")).append("</p>");
		} else if (returnCode == MailerResult.ATTACHMENT_INVALID) {
			errors.append("<p>").append(trans.translate("mailhelper.error.attachment")).append("</p>");
		} else {
			// mail could be send, but maybe not to all the users (e.g. invalid mail
			// adresses or a temporary problem)
			if (failedIdentites != null && failedIdentites.size() > 0) {
				warnings.append("<p>").append(trans.translate("mailhelper.error.failedusers"));
				warnings.append("<ul>");
				for (Identity identity : failedIdentites) {
					User user = identity.getUser();
					warnings.append("<li>");
					String fullname = UserManager.getInstance().getUserDisplayName(identity);
					warnings.append(trans.translate("mailhelper.error.failedusers.user", new String[] {
							user.getProperty(UserConstants.FIRSTNAME, null),
							user.getProperty(UserConstants.LASTNAME, null),
							user.getProperty(UserConstants.EMAIL, null),
							fullname
						}));
					warnings.append("</li>");
				}
				warnings.append("</ul></p>");
			}
		}
	}

	/**
	 * Checks if the given mail address is potentially a valid email address that
	 * can be used to send emails. It does NOT check if the mail address exists,
	 * it checks only for syntactical validity.
	 * 
	 * @param mailAddress
	 * @return 
	 */
	public static boolean isValidEmailAddress(String mailAddress) {
		return EmailAddressValidator.isValidEmailAddress(mailAddress);
	}
	
	public static boolean isDisabledMailAddress(Identity identity, MailerResult result) {
		String value = identity.getUser().getProperty("emailDisabled", null);
		if (value != null && value.equals("true")) {
			if(result != null) {
				result.addFailedIdentites(identity);
				if(result.getReturnCode() != MailerResult.RECIPIENT_ADDRESS_ERROR) {
					result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
				}
			}
			return true;
		}
		return false;
	}
	
	public static List<File> checkAttachments(File[] attachments, MailerResult result) {
		List<File> attachmentList = new ArrayList<File>();
		if(attachments != null) {
			for(File attachment:attachments) {
				if(attachment == null || !attachment.exists()) {
					result.setReturnCode(MailerResult.ATTACHMENT_INVALID);
				} else {
					attachmentList.add(attachment);
				}
			}
		}
		return attachmentList;
	}
}



