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
package org.olat.modules.qpool.ui;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionPoolService;

/**
 * 
 * Initial date: 12.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PooledItemsSource implements QuestionItemsSource {
	
	private final Pool pool;
	private final QuestionPoolService qpoolService;
	
	public PooledItemsSource(Pool pool) {
		this.pool = pool;
		qpoolService = CoreSpringFactory.getImpl(QuestionPoolService.class);
	}

	@Override
	public int getNumOfItems() {
		return qpoolService.getNumOfItemsInPool(pool);
	}

	@Override
	public List<QuestionItem> getItems(int firstResult, int maxResults) {
		return qpoolService.getItemsOfPool(pool, firstResult, maxResults);
	}
}
