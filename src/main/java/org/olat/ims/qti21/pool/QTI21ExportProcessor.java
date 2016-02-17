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
package org.olat.ims.qti21.pool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.io.ShieldOutputStream;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.model.xml.AssessmentTestFactory;
import org.olat.ims.qti21.model.xml.ManifestPackage;
import org.olat.imscp.xml.manifest.ManifestType;
import org.olat.modules.qpool.QuestionItemFull;
import org.olat.modules.qpool.manager.QPoolFileStorage;

import uk.ac.ed.ph.jqtiplus.node.test.AssessmentSection;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.serialization.QtiSerializer;

/**
 * 
 * Initial date: 05.02.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI21ExportProcessor {
	
	private static final OLog log = Tracing.createLoggerFor(QTI21ExportProcessor.class);
	
	private final QTI21Service qtiService;
	private final QPoolFileStorage qpoolFileStorage;

	public QTI21ExportProcessor(QTI21Service qtiService, QPoolFileStorage qpoolFileStorage) {
		this.qtiService = qtiService;
		this.qpoolFileStorage = qpoolFileStorage;
	}

	public void process(QuestionItemFull qitem, ZipOutputStream zout, Set<String> names) {
		String dir = qitem.getDirectory();
		File rootDirectory = qpoolFileStorage.getDirectory(dir);

		String rootDir = "qitem_" + qitem.getKey();
		File[] items = rootDirectory.listFiles();
		addMetadata(qitem, rootDirectory, zout);
		for(File item:items) {
			ZipUtil.addFileToZip(rootDir + "/" + item.getName(), item, zout);
		}
	}
	
	public ResolvedAssessmentItem exportToQTIEditor(QuestionItemFull fullItem, File editorContainer)
	throws IOException {
		AssessmentItemsAndResources itemAndMaterials = new AssessmentItemsAndResources();
		collectMaterials(fullItem, itemAndMaterials);
		if(itemAndMaterials.getAssessmentItems().isEmpty()) {
			return null;//nothing found
		}
		
		ResolvedAssessmentItem assessmentItem = itemAndMaterials.getAssessmentItems().get(0);
		//write materials
		for(ItemMaterial material:itemAndMaterials.getMaterials()) {
			String exportPath = material.getExportUri();
			File leaf = new File(editorContainer, exportPath);
			FileUtils.bcopy(leaf, editorContainer, "Export to QTI 2.1 editor");
		}
		return assessmentItem;
	}
	
	protected void collectMaterials(QuestionItemFull fullItem, AssessmentItemsAndResources materials) {
		String dir = fullItem.getDirectory();
		String rootFilename = fullItem.getRootFilename();
		File resourceDirectory = qpoolFileStorage.getDirectory(dir);
		File itemFile = new File(resourceDirectory, rootFilename);

		if(itemFile.exists()) {
			ResolvedAssessmentItem assessmentItem = qtiService.loadAndResolveAssessmentItem(itemFile.toURI(), resourceDirectory);
			//enrichScore(itemEl);
			//enrichWithMetadata(fullItem, itemEl);
			//collectResources(itemEl, container, materials);
			materials.addItemEl(assessmentItem);
		}
	}
	
	public void addMetadata(QuestionItemFull item, File rootDirectory, ZipOutputStream zout) {
		
	}
	
	public void assembleTest(List<QuestionItemFull> fullItems, File directory) {
		try {
			QtiSerializer qtiSerializer = qtiService.qtiSerializer();
			//imsmanifest
			ManifestType manifest = ManifestPackage.createEmptyManifest();
			
			//assessment test
			AssessmentTest assessmentTest = AssessmentTestFactory.createAssessmentTest("Assessment test from pool");
			String assessmentTestFilename = assessmentTest.getIdentifier() + ".xml";
			ManifestPackage.appendAssessmentTest(assessmentTestFilename, manifest);

			//make a section
			AssessmentSection section = assessmentTest.getTestParts().get(0).getAssessmentSections().get(0);

			//assessment items
			for(QuestionItemFull qitem:fullItems) {
				String rootFilename = qitem.getRootFilename();
				File resourceDirectory = qpoolFileStorage.getDirectory(qitem.getDirectory());
				File itemFile = new File(resourceDirectory, rootFilename);
				String itemFilename = itemFile.getName();
				
				//enrichScore(itemEl);
				//enrichWithMetadata(fullItem, itemEl);
				//collectResources(itemEl, container, materials);
				FileUtils.bcopy(itemFile, new File(directory, rootFilename), "");
				AssessmentTestFactory.appendAssessmentItem(section, itemFilename);
				ManifestPackage.appendAssessmentItem(itemFilename, manifest);
			}

			try(FileOutputStream out = new FileOutputStream(new File(directory, assessmentTestFilename))) {
				qtiSerializer.serializeJqtiObject(assessmentTest, out);	
			} catch(Exception e) {
				log.error("", e);
			}
			
	        try(FileOutputStream out = new FileOutputStream(new File(directory, "imsmanifest.xml"))) {
	        	ManifestPackage.write(manifest, out);
	        } catch(Exception e) {
	        	log.error("", e);
	        }
		} catch (IOException | URISyntaxException e) {
			log.error("", e);
		}
	}
	
	public void assembleTest(List<QuestionItemFull> fullItems, ZipOutputStream zout) {
		try {
			QtiSerializer qtiSerializer = qtiService.qtiSerializer();
			//imsmanifest
			ManifestType manifest = ManifestPackage.createEmptyManifest();
			
			//assessment test
			AssessmentTest assessmentTest = AssessmentTestFactory.createAssessmentTest("Assessment test from pool");
			String assessmentTestFilename = assessmentTest.getIdentifier() + ".xml";
			ManifestPackage.appendAssessmentTest(assessmentTestFilename, manifest);

			//make a section
			AssessmentSection section = assessmentTest.getTestParts().get(0).getAssessmentSections().get(0);

			//assessment items
			for(QuestionItemFull qitem:fullItems) {
				String rootFilename = qitem.getRootFilename();
				File resourceDirectory = qpoolFileStorage.getDirectory(qitem.getDirectory());
				File itemFile = new File(resourceDirectory, rootFilename);
				String itemFilename = itemFile.getName();
				
				//enrichScore(itemEl);
				//enrichWithMetadata(fullItem, itemEl);
				//collectResources(itemEl, container, materials);

				ZipUtil.addFileToZip(itemFilename, itemFile, zout);
				AssessmentTestFactory.appendAssessmentItem(section, itemFilename);
				ManifestPackage.appendAssessmentItem(itemFilename, manifest);
			}

			zout.putNextEntry(new ZipEntry(assessmentTestFilename));
			qtiSerializer.serializeJqtiObject(assessmentTest, new ShieldOutputStream(zout));
			zout.closeEntry();

			zout.putNextEntry(new ZipEntry("imsmanifest.xml"));
			ManifestPackage.write(manifest, zout);
			zout.closeEntry();
		} catch (IOException | URISyntaxException e) {
			log.error("", e);
		}
	}

	private static final class AssessmentItemsAndResources {
		private final Set<String> paths = new HashSet<String>();
		private final List<ResolvedAssessmentItem> itemEls = new ArrayList<ResolvedAssessmentItem>();
		private final List<ItemMaterial> materials = new ArrayList<ItemMaterial>();
		
		public Set<String> getPaths() {
			return paths;
		}
		
		public List<ResolvedAssessmentItem> getAssessmentItems() {
			return itemEls;
		}
		
		public void addItemEl(ResolvedAssessmentItem el) {
			itemEls.add(el);
		}
		
		public List<ItemMaterial> getMaterials() {
			return materials;
		}
		
		public void addMaterial(ItemMaterial material) {
			materials.add(material);
		}
	}
	
	private static final class ItemMaterial {
		private final VFSLeaf leaf;
		private final String exportUri;
		
		public ItemMaterial(VFSLeaf leaf, String exportUri) {
			this.leaf = leaf;
			this.exportUri = exportUri;
		}
		
		public VFSLeaf getLeaf() {
			return leaf;
		}
		
		public String getExportUri() {
			return exportUri;
		}
	}
}
