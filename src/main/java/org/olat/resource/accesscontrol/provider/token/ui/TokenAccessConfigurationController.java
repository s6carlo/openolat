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

package org.olat.resource.accesscontrol.provider.token.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Util;
import org.olat.resource.accesscontrol.Offer;
import org.olat.resource.accesscontrol.OfferAccess;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.OfferImpl;
import org.olat.resource.accesscontrol.ui.AbstractConfigurationMethodController;
import org.olat.resource.accesscontrol.ui.AccessConfigurationController;

/**
 * 
 * Description:<br>
 * Configuration for a token
 * 
 * <P>
 * Initial Date:  15 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class TokenAccessConfigurationController extends AbstractConfigurationMethodController {

	private TextElement descEl, tokenEl;
	private DateChooser dateFrom, dateTo;
	private final OfferAccess link;
	
	public TokenAccessConfigurationController(UserRequest ureq, WindowControl wControl, OfferAccess link, boolean edit) {
		super(ureq, wControl, edit);
		this.link = link;
		setTranslator(Util.createPackageTranslator(AccessConfigurationController.class, getLocale(), getTranslator()));
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		formLayout.setElementCssClass("o_sel_accesscontrol_token_form");
		
		String desc = null;
		if(link.getOffer() != null) {
			desc = link.getOffer().getDescription();
		}
		descEl = uifactory.addTextAreaElement("offer-desc", "offer.description", 2000, 6, 80, false, desc, formLayout);
		descEl.setElementCssClass("o_sel_accesscontrol_description");
		
		String token = "";
		if(link.getOffer() instanceof OfferImpl) {
			token = ((OfferImpl)link.getOffer()).getToken();
		}
		tokenEl = uifactory.addTextElement("token", "accesscontrol.token", 255, token, formLayout);
		tokenEl.setElementCssClass("o_sel_accesscontrol_token");
		
		dateFrom = uifactory.addDateChooser("from_" + link.getKey(), "from", link.getValidFrom(), formLayout);
		dateTo = uifactory.addDateChooser("to_" + link.getKey(), "to", link.getValidTo(), formLayout);

		super.initForm(formLayout, listener, ureq);
	}
	
	@Override
	public AccessMethod getMethod() {
		return link.getMethod();
	}

	@Override
	public OfferAccess commitChanges() {
		Offer offer = link.getOffer();
		if(offer instanceof OfferImpl) {
			((OfferImpl)offer).setToken(tokenEl.getValue());
		}
		offer.setDescription(descEl.getValue());
		offer.setValidFrom(dateFrom.getDate());
		offer.setValidTo(dateTo.getDate());
		link.setValidFrom(dateFrom.getDate());
		link.setValidTo(dateTo.getDate());
		return link;
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		String token = tokenEl.getValue();
		tokenEl.clearError();
		if(token == null || token.length() < 2) {
			tokenEl.setErrorKey("invalid.token.format", null);
			allOk = false;
		}
		
		return allOk && super.validateFormLogic(ureq);
	}
}
