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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.PathUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.io.ShieldInputStream;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.fileresource.types.ImsQTI21Resource.PathResourceLocator;
import org.olat.ims.qti.qpool.QTIMetadataConverter;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.model.IdentifierGenerator;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.AssessmentItemMetadata;
import org.olat.ims.qti21.model.xml.ManifestBuilder;
import org.olat.ims.qti21.model.xml.ManifestMetadataBuilder;
import org.olat.imscp.xml.manifest.ResourceType;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionType;
import org.olat.modules.qpool.TaxonomyLevel;
import org.olat.modules.qpool.manager.QEducationalContextDAO;
import org.olat.modules.qpool.manager.QItemTypeDAO;
import org.olat.modules.qpool.manager.QLicenseDAO;
import org.olat.modules.qpool.manager.QPoolFileStorage;
import org.olat.modules.qpool.manager.QuestionItemDAO;
import org.olat.modules.qpool.manager.TaxonomyLevelDAO;
import org.olat.modules.qpool.model.QEducationalContext;
import org.olat.modules.qpool.model.QItemType;
import org.olat.modules.qpool.model.QLicense;
import org.olat.modules.qpool.model.QuestionItemImpl;

import uk.ac.ed.ph.jqtiplus.node.content.xhtml.image.Img;
import uk.ac.ed.ph.jqtiplus.node.content.xhtml.object.Object;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.reading.AssessmentObjectXmlLoader;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlReader;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.utils.QueryUtils;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

/**
 * 
 * Initial date: 05.02.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI21ImportProcessor {
	
	private static final OLog log = Tracing.createLoggerFor(QTI21ImportProcessor.class);
	
	private final Identity owner;
	private final Locale defaultLocale;
	
	private final QItemTypeDAO qItemTypeDao;
	private final QLicenseDAO qLicenseDao;
	private final QTI21Service qtiService;
	private final QuestionItemDAO questionItemDao;
	private final QPoolFileStorage qpoolFileStorage;
	private final TaxonomyLevelDAO taxonomyLevelDao;
	private final QEducationalContextDAO qEduContextDao;
	
	public QTI21ImportProcessor(Identity owner, Locale defaultLocale,
			QuestionItemDAO questionItemDao, QItemTypeDAO qItemTypeDao, QEducationalContextDAO qEduContextDao,
			TaxonomyLevelDAO taxonomyLevelDao, QLicenseDAO qLicenseDao, QPoolFileStorage qpoolFileStorage, QTI21Service qtiService) {
		this.owner = owner;
		this.qtiService = qtiService;
		this.defaultLocale = defaultLocale;
		this.qLicenseDao = qLicenseDao;
		this.qItemTypeDao = qItemTypeDao;
		this.qEduContextDao = qEduContextDao;
		this.questionItemDao = questionItemDao;
		this.qpoolFileStorage = qpoolFileStorage;
		this.taxonomyLevelDao = taxonomyLevelDao;
	}

	public List<QuestionItem> process(File file) {
		//export zip file
		List<QuestionItem> items = new ArrayList<>();
		try {
			Path fPath = FileSystems.newFileSystem(file.toPath(), null).getPath("/");
			if(fPath != null) {
				ImsManifestVisitor visitor = new ImsManifestVisitor();
			    Files.walkFileTree(fPath, visitor);
			    
			    List<Path> imsmanifests = visitor.getImsmanifestFiles();
			    for(Path imsmanifest:imsmanifests) {
			    	InputStream in = Files.newInputStream(imsmanifest);
			    	ManifestBuilder manifestBuilder = ManifestBuilder.read(new ShieldInputStream(in));
			    	List<ResourceType> resources = manifestBuilder.getResourceList();
					for(ResourceType resource:resources) {
						ManifestMetadataBuilder metadataBuilder = manifestBuilder.getMetadataBuilder(resource, true);
						QuestionItem qitem = processResource(resource, imsmanifest, metadataBuilder);
						if(qitem != null) {
							items.add(qitem);
						}
					}
			    }
			}
		} catch (IOException e) {
			log.error("", e);
		}
		return items;
	}
	
	private QuestionItem processResource(ResourceType resource, Path imsmanifestPath, ManifestMetadataBuilder metadataBuilder) {
		try {
			String href = resource.getHref();
			Path parentPath = imsmanifestPath.getParent();
			Path assessmentItemPath = parentPath.resolve(href);
			if(Files.notExists(assessmentItemPath)) {
				return null;
			}
			
			QtiXmlReader qtiXmlReader = new QtiXmlReader(qtiService.jqtiExtensionManager());
			ResourceLocator fileResourceLocator = new PathResourceLocator(parentPath);
			ResourceLocator inputResourceLocator = 
					ImsQTI21Resource.createResolvingResourceLocator(fileResourceLocator);
			
			URI assessmentObjectSystemId = new URI("zip", href, null);
			AssessmentObjectXmlLoader assessmentObjectXmlLoader = new AssessmentObjectXmlLoader(qtiXmlReader, inputResourceLocator);
			ResolvedAssessmentItem resolvedAssessmentItem = assessmentObjectXmlLoader.loadAndResolveAssessmentItem(assessmentObjectSystemId);
			AssessmentItem assessmentItem = resolvedAssessmentItem.getRootNodeLookup().extractIfSuccessful();
			
			AssessmentItemMetadata metadata = new AssessmentItemMetadata(metadataBuilder);

			String editor = null;
			String editorVersion = null;
			if(StringHelper.containsNonWhitespace(assessmentItem.getToolName())) {
				editor = assessmentItem.getToolName();
			}
			if(StringHelper.containsNonWhitespace(assessmentItem.getToolVersion())) {
				editorVersion = assessmentItem.getToolVersion();
			}

			QuestionItemImpl qitem = processItem(assessmentItem, null, href,
					editor, editorVersion, metadata);
			
			//storage
			File itemStorage = qpoolFileStorage.getDirectory(qitem.getDirectory());
			PathUtils.copyFileToDir(assessmentItemPath, itemStorage, href);
			
			//create manifest
			ManifestBuilder manifest = ManifestBuilder.createAssessmentItemBuilder();
			String itemId = IdentifierGenerator.newAsIdentifier("item").toString();
			ResourceType importedResource = manifest.appendAssessmentItem(itemId, href);
			ManifestMetadataBuilder importedMetadataBuilder = manifest.getMetadataBuilder(importedResource, true);
			importedMetadataBuilder.setMetadata(metadataBuilder.getMetadata());
			manifest.write(new File(itemStorage, "imsmanifest.xml"));
			
			//process material
			List<String> materials = getMaterials(assessmentItem);
			for(String material:materials) {
				if(material.indexOf("://") < 0) {// material can be an external URL
					Path materialFile = assessmentItemPath.getParent().resolve(material);
					PathUtils.copyFileToDir(materialFile, itemStorage, material);
				}
			}
			return qitem;
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
	}

	protected List<String> getMaterials(AssessmentItem item) {
		List<String> materials = new ArrayList<>();
		QueryUtils.search(Img.class, item).forEach((img) -> {
			if(img.getSrc() != null) {
				materials.add(img.getSrc().toString());
			}
		});

		QueryUtils.search(Object.class, item).forEach((object) -> {
			if(StringHelper.containsNonWhitespace(object.getData())) {
				materials.add(object.getData());
			}
		});
		return materials;
	}

	protected QuestionItemImpl processItem(AssessmentItem assessmentItem, String comment, String originalItemFilename,
			String editor, String editorVersion, AssessmentItemMetadata metadata) {
		//filename
		String filename;
		String ident = assessmentItem.getIdentifier();
		if(originalItemFilename != null) {
			filename = originalItemFilename;
		} else if(StringHelper.containsNonWhitespace(ident)) {
			filename = StringHelper.transformDisplayNameToFileSystemName(ident) + ".xml";
		} else {
			filename = "item.xml";
		}
		String dir = qpoolFileStorage.generateDir();
		
		//title
		String title = assessmentItem.getTitle();
		if(!StringHelper.containsNonWhitespace(title)) {
			title = assessmentItem.getLabel();
		}
		if(!StringHelper.containsNonWhitespace(title)) {
			title = ident;
		}

		QuestionItemImpl poolItem = questionItemDao.create(title, QTI21Constants.QTI_21_FORMAT, dir, filename);
		//description
		poolItem.setDescription(comment);
		//language from default
		poolItem.setLanguage(defaultLocale.getLanguage());
		//question type first
		if(StringHelper.containsNonWhitespace(editor)) {
			poolItem.setEditor(editor);
			poolItem.setEditorVersion(editorVersion);
		}
		//if question type not found, can be overridden by the metadatas
		processItemMetadata(poolItem, metadata);
		if(poolItem.getType() == null) {
			QItemType defType = convertType(assessmentItem);
			poolItem.setType(defType);
		}
		/*if(docInfos != null) {
			processSidecarMetadata(poolItem, docInfos);
		}*/
		if(metadata != null) {
			//processItemMetadata(poolItem, metadata);
		}
		questionItemDao.persist(owner, poolItem);
		return poolItem;
	}
	
	protected QItemType convertType(AssessmentItem assessmentItem) {
		QTI21QuestionType qti21Type = QTI21QuestionType.getType(assessmentItem);
		switch(qti21Type) {
			case sc: return qItemTypeDao.loadByType(QuestionType.SC.name());
			case mc: return qItemTypeDao.loadByType(QuestionType.MC.name());
			case kprim: return qItemTypeDao.loadByType(QuestionType.KPRIM.name());
			case match: return qItemTypeDao.loadByType(QuestionType.MATCH.name());
			case fib: return qItemTypeDao.loadByType(QuestionType.FIB.name());
			case numerical: return qItemTypeDao.loadByType(QuestionType.NUMERICAL.name());
			case hotspot: return qItemTypeDao.loadByType(QuestionType.HOTSPOT.name());
			case essay: return qItemTypeDao.loadByType(QuestionType.ESSAY.name());
			case upload: return qItemTypeDao.loadByType(QuestionType.UPLOAD.name());
			default: return qItemTypeDao.loadByType(QuestionType.UNKOWN.name());
		}
	}
	
	protected void processItemMetadata(QuestionItemImpl poolItem, AssessmentItemMetadata metadata) {
		//non heuristic set of question type
		String typeStr = null;	
		QTI21QuestionType questionType = metadata.getQuestionType();
		if(questionType != null && questionType.getPoolQuestionType() != null) {
			typeStr = questionType.getPoolQuestionType().name();
		}
		if(typeStr != null) {
			QItemType type = qItemTypeDao.loadByType(typeStr);
			if(type != null) {
				poolItem.setType(type);
			}
		}
				
		String coverage = metadata.getCoverage();
		if(StringHelper.containsNonWhitespace(coverage)) {
			poolItem.setCoverage(coverage);
		}
		
		String language = metadata.getLanguage();
		if(StringHelper.containsNonWhitespace(language)) {
			poolItem.setLanguage(language);
		}
		
		String keywords = metadata.getKeywords();
		if(StringHelper.containsNonWhitespace(keywords)) {
			poolItem.setKeywords(keywords);
		}
		
		String taxonomyPath = metadata.getTaxonomyPath();
		if(StringHelper.containsNonWhitespace(taxonomyPath)) {
			QTIMetadataConverter converter = new QTIMetadataConverter(qItemTypeDao, qLicenseDao, taxonomyLevelDao, qEduContextDao);
			TaxonomyLevel taxonomyLevel = converter.toTaxonomy(taxonomyPath);
			poolItem.setTaxonomyLevel(taxonomyLevel);
		}
		
		String level = metadata.getLevel();
		if(StringHelper.containsNonWhitespace(level)) {
			QTIMetadataConverter converter = new QTIMetadataConverter(qItemTypeDao, qLicenseDao, taxonomyLevelDao, qEduContextDao);
			QEducationalContext educationalContext = converter.toEducationalContext(level);
			poolItem.setEducationalContext(educationalContext);
		}
				
		String time = metadata.getTypicalLearningTime();
		if(StringHelper.containsNonWhitespace(time)) {
			poolItem.setEducationalLearningTime(time);
		}
		
		String editor = metadata.getEditor();
		if(StringHelper.containsNonWhitespace(editor)) {
			poolItem.setEditor(editor);
		}
		
		String editorVersion = metadata.getEditorVersion();
		if(StringHelper.containsNonWhitespace(editorVersion)) {
			poolItem.setEditorVersion(editorVersion);
		}
		
		int numOfAnswerAlternatives = metadata.getNumOfAnswerAlternatives();
		if(numOfAnswerAlternatives > 0) {
			poolItem.setNumOfAnswerAlternatives(numOfAnswerAlternatives);
		}
		
		poolItem.setDifficulty(metadata.getDifficulty());
		poolItem.setDifferentiation(metadata.getDifferentiation());
		poolItem.setStdevDifficulty(metadata.getStdevDifficulty());
		
		String license = metadata.getLicense();
		if(StringHelper.containsNonWhitespace(license)) {
			QTIMetadataConverter converter = new QTIMetadataConverter(qItemTypeDao, qLicenseDao, taxonomyLevelDao, qEduContextDao);
			QLicense qLicense = converter.toLicense(license);
			poolItem.setLicense(qLicense);
		}
	}
	
	public static class ImsManifestVisitor extends SimpleFileVisitor<Path> {
		
		private final List<Path> imsmanifestFiles = new ArrayList<>();
		
		public List<Path> getImsmanifestFiles() {
			return imsmanifestFiles;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
		throws IOException {
			String name = file.getFileName().toString().toLowerCase();
			if(name != null && name.equals("imsmanifest.xml")) {
				imsmanifestFiles.add(file);
			}
	        return FileVisitResult.CONTINUE;
		}
	}
}
