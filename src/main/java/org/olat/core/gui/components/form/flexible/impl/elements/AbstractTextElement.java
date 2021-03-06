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
package org.olat.core.gui.components.form.flexible.impl.elements;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormItemImpl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.ValidationStatusImpl;
import org.olat.core.util.filter.Filter;

/**
 * Description:<br>
 * TODO: patrickb Class Description for AbstractTextElement
 * <P>
 * Initial Date: 27.11.2006 <br>
 * 
 * @author patrickb
 */
public abstract class AbstractTextElement extends FormItemImpl implements TextElement {

	public AbstractTextElement(String name) {
		this(name, false);
	}
	
	protected AbstractTextElement(String name, boolean asInlineEditingElement) {
		this(name, name, asInlineEditingElement);
	}

	/**
	 * @param id A fix identifier for state-less behavior, must be unique
	 */
	protected AbstractTextElement(String id, String name, boolean asInlineEditingElement) {
		super(id, name, asInlineEditingElement);
		displaySize = 35;
	}

	protected String original;
	protected String value;
	private boolean checkForNotEmpty = false;
	private boolean checkForLength = false;
	private boolean checkForEquals = false; 
	private boolean checkForMatchRegexp = false;
	private boolean checkForCustomItemValidator = false;
	private boolean preventTrim = false; //OO-31
	private String notEmptyErrorKey;
	private int notLongerLength;
	protected int displaySize;
	protected int maxlength = -1; //default no maxlength restriction
	protected boolean checkVisibleLength = false;
	private String notLongerThanErrorKey;
	private String checkForOtherValue;
	private String otherValueErrorKey;
	private String checkRegexp;
	private String checkRegexpErrorKey;
	private String placeholder;
	private ItemValidatorProvider itemValidatorProvider;
	protected boolean originalInitialised=false;
	
	
	@Override
	public void validate(List<ValidationStatus> validationResults) {
		if(checkForNotEmpty && !notEmpty()){
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if(checkForLength && !notLongerThan()){
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if(checkForEquals && !checkForIsEqual()){
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if(checkForMatchRegexp && !checkRegexMatch()){
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if (checkForCustomItemValidator && !checkItemValidatorIsValid()){
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		//else no error
		clearError();
	}
	
	@Override
	public void reset() {
		//reset to original value and clear error msg's
		setValue(original);
		clearError();
	}
	
	@Override
	protected void rootFormAvailable(){
		//
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#getValue()
	 */
	@Override
	public String getValue() {
		return value;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#getValue(org.olat.core.util.filter.Filter)
	 */
	@Override
	public String getValue(Filter filter) {
		return filter.filter(value);
	}
	
	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#preventValueTrim()
	 */
	public void preventValueTrim(boolean preventTrim){
		this.preventTrim = preventTrim;
	}

	/**
	 * Sets the value. if null is given, an empty string is assumed.
	 * 
	 * @param value The value to set
	 */
	public void setValue(String value) {
		if (value == null) {
			value = "";
		} else {
			if(!preventTrim) // OO-31
				value = value.trim();
			
			// Remember original value for dirty evaluation.
			// null value is not regarded as initial value. only
			// real values are used inital values
			if (!originalInitialised){
				original = new String(value);
				originalInitialised = true;
			}
		}

		this.value = StringHelper.cleanUTF8ForXml(value);
		Component c = getComponent();
		if (c != null) {
			// c may be null since it is only created when this formelement is added to a FormItemContainer
			c.setDirty(true);
		}
	}

	/**
	 * Set a new value as the original value that is used when resetting the
	 * form. This can be used when a form is saved and in a later form should be
	 * resetted to the intermediate save state.
	 * <p>
	 * Does not change the value of the element, just the reset-value!
	 * 
	 * @param value The new original value
	 */
	public void setNewOriginalValue(String value) {
		if (value == null) value = "";
		original = new String(value);
		originalInitialised = true;
		if (getValue() != null && !getValue().equals(value)) {
			getComponent().setDirty(true);
		}
	}

	/**
	 * 
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#setDisplaySize(int)
	 */
	public void setDisplaySize(int displaySize){
		this.displaySize = displaySize;
	}
	
	@Override
	public int getMaxLength() {
		return maxlength;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength){
		this.maxlength = maxLength;
	}

	public void setCheckVisibleLength(boolean checkVisibleLength) {
		this.checkVisibleLength = checkVisibleLength;
	}

	/**
	 * @param errorKey
	 * @return
	 */
	public void setNotEmptyCheck(String errorKey) {
		checkForNotEmpty = true;
		notEmptyErrorKey = errorKey;
	}

	private boolean notEmpty(){
		if (value == null || value.equals("")) {
			setErrorKey(notEmptyErrorKey, null);
			return false;
		} else {
			clearError();
			return true;
		}
	}
	

	/**
	 * @param maxLength if value is -1 maxlength will not be checked 
	 * @param errorKey 
	 * @return
	 * @see org.olat.core.gui.components.form.flexible.elements.TextElement#setNotLongerThanCheck(int,
	 *      java.lang.String)
	 */
	public void setNotLongerThanCheck(int maxLength, String errorKey) {
		if (maxLength == -1) {
			checkForLength = false;
			return;
		}
		checkForLength = true;
		notLongerThanErrorKey = errorKey;
		notLongerLength = maxLength;
	}

	private boolean notLongerThan(){
		boolean lengthError = false;
		try {
			if(checkVisibleLength) {
				if (value.length() > notLongerLength) {
					lengthError = true;
				}
			} else if (value.length() > notLongerLength || value.getBytes("UTF-8").length > notLongerLength) {
				lengthError = true;
			} 
		} catch (UnsupportedEncodingException e) {
			if (value.length() > notLongerLength){
				lengthError = true; 
			}
		}		
		if (lengthError) {
			setErrorKey(notLongerThanErrorKey, new String[]{notLongerLength + ""});
		} else {
			clearError();
		}		
		return !lengthError;
	}
	/**
	 * compares a text value with another value
	 * 
	 * @param otherValue
	 * @param errorKey
	 * @return true if they are equal
	 */
	public void setIsEqualCheck(String otherValue, String errorKey) {
		checkForEquals = true;
		checkForOtherValue = otherValue;
		otherValueErrorKey = errorKey;
	}
	
	private boolean checkForIsEqual(){
		if (value == null || !value.equals(checkForOtherValue)) {
			setErrorKey(otherValueErrorKey, null);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Check if the text element is empty
	 * 
	 * @return boolean true if is empty, false otherwhise
	 */
	public boolean isEmpty() {
		return value.equals("");
	}

	/**
	 * Check if the text element is empty
	 * 
	 * @param errorKey
	 * @return boolean true if is empty, false otherwise
	 */
	public boolean isEmpty(String errorKey) {
		if (isEmpty()) {
			setErrorKey(errorKey, null);
			return true;
		}
		return false;
	}

	@Override
	public void setPlaceholderText(String placeholderText) {
		if (StringHelper.containsNonWhitespace(placeholderText)) {
			placeholder = StringEscapeUtils.escapeHtml(placeholderText);
		} else {
			placeholder = null;
		}
	}
	
	@Override
	public void setPlaceholderKey(String i18nKey, String[] args) {
		if (StringHelper.containsNonWhitespace(i18nKey) && translator != null) {
			setPlaceholderText(translator.translate(i18nKey, args));
		} else {
			placeholder = null;
		}
	}

	@Override
	public String getPlaceholder() {
		return placeholder;
	}

	@Override
	public boolean hasPlaceholder() {
		return (placeholder != null);
	}
	

	
	/**
	 * @param regExp
	 * @param errorKey
	 * @return
	 */
	public void setRegexMatchCheck(String regExp, String errorKey) {
		checkForMatchRegexp = true;
		checkRegexp = regExp;
		checkRegexpErrorKey = errorKey;
	}
	
	private boolean checkRegexMatch(){
		if (value == null || !value.matches(checkRegexp)) {
			setErrorKey(checkRegexpErrorKey, null);
			return false;
		} else {
			return true;
		}
	}
	
	public void setItemValidatorProvider(ItemValidatorProvider itemValidatorProvider){
		checkForCustomItemValidator = (itemValidatorProvider != null);
		this.itemValidatorProvider = itemValidatorProvider;
	}
	
	private boolean checkItemValidatorIsValid (){ 
		Locale locale = getTranslator().getLocale();
		ValidationError validationErrorCallback = new ValidationError();
		boolean isValid = itemValidatorProvider.isValidValue(value, validationErrorCallback, locale);
		if (isValid) {
			return true;
		} else {
			setErrorKey(validationErrorCallback.getErrorKey(), validationErrorCallback.getArgs());
			return false; 
		}
	}
}