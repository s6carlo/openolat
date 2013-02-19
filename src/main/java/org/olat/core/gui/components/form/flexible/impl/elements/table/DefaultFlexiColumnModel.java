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

package org.olat.core.gui.components.form.flexible.impl.elements.table;


/**
 * 
 * @author Christian Guretzki
 */
public class DefaultFlexiColumnModel implements FlexiColumnModel {

	private String headerKey;

	private boolean sortable;
	private String sortedKey;
	
	private int alignment;
	private FlexiCellRenderer cellRenderer;

	public DefaultFlexiColumnModel(String headerKey) {
		this(headerKey, false, null);
	}
	
	public DefaultFlexiColumnModel(String headerKey, boolean sortable, String sortKey) {
		this(headerKey, sortable, sortKey, FlexiColumnModel.ALIGNMENT_LEFT,  new TextFlexiCellRenderer());
	}
	
	public DefaultFlexiColumnModel(String headerKey, boolean sortable, String sortKey, int alignment, FlexiCellRenderer cellRenderer) {
		this.sortable = sortable;
		this.sortedKey = sortKey;
		this.headerKey = headerKey;
		this.alignment = alignment;
		this.cellRenderer = cellRenderer;
	}

	@Override
	public String getAction() {
		return null;
	}

	public String getHeaderKey() {
		return headerKey;
	}

	@Override
	public boolean isSortable() {
		return sortable;
	}

	@Override
	public void setSortable(boolean enable) {
		sortable = enable;
	}

	@Override
	public String getSortKey() {
		return sortedKey;
	}

	@Override
	public void setSortKey(String sortedKey) {
		this.sortedKey = sortedKey;
	}

	public int getAlignment() {
		return alignment;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}

	public void setCellRenderer(FlexiCellRenderer cellRenderer) {
		this.cellRenderer = cellRenderer;
	}

	public FlexiCellRenderer getCellRenderer() {
		return cellRenderer;
	}

}