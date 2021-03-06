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

package org.olat.modules.video.manager;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.services.image.Crop;
import org.olat.core.commons.services.image.ImageService;
import org.olat.core.commons.services.image.Size;
import org.olat.core.commons.services.video.MovieService;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.VFSStatus;
import org.olat.core.util.vfs.filters.VFSItemSuffixFilter;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.modules.video.VideoManager;
import org.olat.modules.video.VideoMeta;
import org.olat.modules.video.VideoMetadata;
import org.olat.modules.video.VideoModule;
import org.olat.modules.video.VideoTranscoding;
import org.olat.modules.video.model.TranscodingCount;
import org.olat.modules.video.model.VideoMetaImpl;
import org.olat.modules.video.model.VideoMetadataImpl;
import org.olat.modules.video.ui.VideoChapterTableRow;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryEntryImportExport.RepositoryEntryImport;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manager for Videoressource
 * 
 * @author dfurrer, dirk.furrer@frentix.com, http://www.frentix.com
 *
 */
@Service("videoManager")
public class VideoManagerImpl implements VideoManager {
	private static final String CR = System.lineSeparator();
	private static final String ENCODING = "utf-8";
	protected static final String DIRNAME_REPOENTRY = "repoentry";
	public static final String FILETYPE_MP4 = "mp4";
	private static final String FILETYPE_JPG = "jpg";
	private static final String FILETYPE_SRT = "srt";
	private static final String FILENAME_POSTER_JPG = "poster.jpg";
	private static final String FILENAME_VIDEO_MP4 = "video.mp4";
	private static final String FILENAME_CHAPTERS_VTT = "chapters.vtt";
	private static final String FILENAME_VIDEO_METADATA_XML = "video_metadata.xml";
	private static final String DIRNAME_MASTER = "master";
	public static final String TRACK = "track_";
	public static final String DOT = "." ;
	
	private static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss");
	private static final SimpleDateFormat vttDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

	@Autowired
	private MovieService movieService;
	@Autowired
	private VideoModule videoModule;
	@Autowired 
	private RepositoryManager repositoryManager;
	@Autowired
	private VideoTranscodingDAO videoTranscodingDao;
	@Autowired
	private VideoMetadataDAO videoMetadataDao;
	@Autowired
	private Scheduler scheduler;
	@Autowired
	private ImageService imageHelper;
	
	private static final OLog log = Tracing.createLoggerFor(VideoManagerImpl.class);

	/**
	 * get the configured posterframe
	 */
	@Override
	public VFSLeaf getPosterframe(OLATResource videoResource) {
		VFSLeaf posterFrame = resolveFromMasterContainer(videoResource, FILENAME_POSTER_JPG);
		return posterFrame;
	}

	/**
	 * set a specific VFSLeaf as posterframe in video metadata
	 */
	@Override
	public void setPosterframe(OLATResource videoResource, VFSLeaf posterframe){
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf newPoster = VFSManager.resolveOrCreateLeafFromPath(masterContainer, FILENAME_POSTER_JPG);
		VFSManager.copyContent(posterframe, newPoster);
		
		// Update also repository entry image, use new posterframe
		VFSLeaf posterImage = (VFSLeaf)masterContainer.resolve(FILENAME_POSTER_JPG);
		if (posterImage != null) {
			RepositoryEntry repoEntry = repositoryManager.lookupRepositoryEntry(videoResource, true);
			repositoryManager.setImage(posterImage, repoEntry);
		}
	}
	
	/**
	 * Sets the posterframe resize uploadfile. Tries to fit image to dimensions of video.
	 *
	 * @param videoResource the video resource
	 * @param posterframe the newPosterFile
	 */
	public void setPosterframeResizeUploadfile(OLATResource videoResource, VFSLeaf newPosterFile) {
		VideoMeta videoMetadata = getVideoMetadata(videoResource);
		Size posterRes = imageHelper.getSize(newPosterFile, FILETYPE_JPG);
		// file size needs to be bigger than target resolution, otherwise use image as it comes
		if (posterRes != null 
				&& posterRes.getHeight() != 0 
				&& posterRes.getWidth() != 0
				&& posterRes.getHeight() >= videoMetadata.getHeight() 
				&& posterRes.getWidth() >= videoMetadata.getWidth()) {
			VFSLeaf oldPosterFile = getPosterframe(videoResource);
			oldPosterFile.delete();
			VFSContainer masterContainer = getMasterContainer(videoResource);
			LocalFileImpl newPoster = (LocalFileImpl) masterContainer.createChildLeaf(FILENAME_POSTER_JPG);
			// to shrink image file, resolution ratio needs to be equal, otherwise crop from top left corner
			if (posterRes.getHeight() / posterRes.getWidth() == videoMetadata.getHeight() / videoMetadata.getWidth()) {
				imageHelper.scaleImage(newPosterFile, newPoster, videoMetadata.getWidth(), videoMetadata.getHeight(), true);
			} else {
				Crop cropSelection = new Crop(0, 0, videoMetadata.getHeight(), videoMetadata.getWidth());
				imageHelper.cropImage(((LocalFileImpl) newPosterFile).getBasefile(), newPoster.getBasefile(), cropSelection);
			}
		} else {
			setPosterframe(videoResource, newPosterFile);
		}
	}

//	/**
//	 * add a subtitle-track to the videoresource
//	 */
//	@Override
//	public void addTrack(OLATResource videoResource, String lang, VFSLeaf trackFile){
//		VideoMetadata metaData = readVideoMetadataFile(videoResource);
//		metaData.addTrack(lang, trackFile.getName());
//		writeVideoMetadataFile(metaData, videoResource);
//	}

	/**
	 * get a specific subtitle-track of the videoresource
	 */
	@Override
	public VFSLeaf getTrack(OLATResource videoResource, String lang) {
		String path = TRACK + lang + DOT + FILETYPE_SRT;
		return resolveFromMasterContainer(videoResource, path);
	}
	
	/**
	 * remove a specific track from the videoresource
	 */
	@Override
	public void removeTrack(OLATResource videoResource, String lang){
		VFSContainer vfsContainer = getMasterContainer(videoResource);
		for (VFSItem item : vfsContainer.getItems(new VFSItemSuffixFilter(new String[]{FILETYPE_SRT}))) {
			if (item.getName().contains(lang)) {
				item.delete();
			}
		}
	}
	
	/**
	 * get all tracks saved in the video metadata as map
	 */
	@Override
	public Map<String, VFSLeaf> getAllTracks(OLATResource videoResource) {
		Map<String, VFSLeaf> tracks = new HashMap<>();
		VFSContainer vfsContainer = getMasterContainer(videoResource);
		for (VFSItem item : vfsContainer.getItems(new VFSItemSuffixFilter(new String[]{FILETYPE_SRT}))) {
			String itemname = item.getName();
			String key = itemname.substring(itemname.indexOf("_") + 1, itemname.indexOf("."));
			tracks.put(key, resolveFromMasterContainer(videoResource, itemname));
		}
//		VideoMetadata metaData = readVideoMetadataFile(videoResource);
//		for(Entry<String, String> trackEntry : metaData.getAllTracks().entrySet()){
//		for(Entry<String, String> trackEntry : alltracks.entrySet()){
//			tracks.put(trackEntry.getKey(), resolveFromMasterContainer(videoResource, trackEntry.getValue()));
//		}
		return tracks;
	}
	
	/**
	 * return the chapter file as a VFSLeaf
	 */
	@Override
	public boolean hasChapters(OLATResource videoResource){
		VFSContainer vfsContainer = getMasterContainer(videoResource);
		VFSLeaf webvtt = (VFSLeaf) vfsContainer.resolve(FILENAME_CHAPTERS_VTT);
		return (webvtt != null && webvtt.getSize() > 0);
	}

	/**
	 * write the the given frame at frameNumber in the frame leaf
	 * @param videoResource videoresource
	 * @param frameNumber the frameNumber at which the frame should be taken from
	 * @param frame the VFSLeaf to write the picked image to
	 */
	@Override
	public boolean getFrame(OLATResource videoResource, int frameNumber, VFSLeaf frame) {
		File videoFile = ((LocalFileImpl)getMasterVideoFile(videoResource)).getBasefile();
		
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(videoFile, "r")) {
			FileChannel ch = randomAccessFile.getChannel();
			FileChannelWrapper in = new FileChannelWrapper(ch);
			FrameGrab frameGrab = new FrameGrab(in).seekToFrameSloppy(frameNumber);
			OutputStream frameOutputStream = frame.getOutputStream(false);

			BufferedImage bufImg = frameGrab.getFrame();
			ImageIO.write(bufImg, "JPG", frameOutputStream);

			// close everything to prevent resource leaks
			frameOutputStream.close();
			in.close();
			ch.close();

			return true;
		} catch (Exception | AssertionError e) {
			log.error("Could not get frame::" + frameNumber + " for video::" + videoFile.getAbsolutePath(), e);
			return false;
		} 
	}

	/**
	 * get the File of the videoresource 
	 */
	@Override
	public File getVideoFile(OLATResource videoResource) {
		VFSContainer masterContainer = getMasterContainer(videoResource);
		LocalFileImpl videoFile = (LocalFileImpl) masterContainer.resolve(FILENAME_VIDEO_MP4);
		return videoFile.getBasefile();
	}


	/**
	 * Resolve the given path to a file in the master directory and return it
	 * 
	 * @param videoResource
	 *            corresponding videoresource
	 * @param path
	 *            path to the videofile
	 * @return VFSLeaf of videofile of resource
	 */
	private VFSLeaf resolveFromMasterContainer(OLATResource videoResource, String path){
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSItem item = masterContainer.resolve(path);
		if(item instanceof VFSLeaf){
			return (VFSLeaf) item;
		}else{
			return null;
		}
	}

//	/**
//	 * Write the metdatadata-xml in the videoresource folder
//	 * @param metaData
//	 * @param videoResource
//	 */
//	private void writeVideoMetadataFile(VideoMetadata metaData, OLATResource videoResource){
//		VFSContainer baseContainer= FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
//		VFSLeaf metaDataFile = VFSManager.resolveOrCreateLeafFromPath(baseContainer, FILENAME_VIDEO_METADATA_XML);
//		XStreamHelper.writeObject(XStreamHelper.createXStreamInstance(), metaDataFile, metaData);
//	}

	@Override
	public boolean isMetadataFileValid(OLATResource videoResource) {
		VFSContainer baseContainer = FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		VFSLeaf metaDataFile = (VFSLeaf) baseContainer.resolve(FILENAME_VIDEO_METADATA_XML);
		try {
			VideoMetadata meta = (VideoMetadata) XStreamHelper.readObject(XStreamHelper.createXStreamInstance(), metaDataFile);
			return meta != null;
		} catch (Exception e) {
			log.error("Error while parsing XStream file for videoResource::" + videoResource, e);
			return false;
		}
	}

	@Override
	public VideoMetadata readVideoMetadataFile(OLATResource videoResource){
		VFSContainer baseContainer= FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		VFSLeaf metaDataFile = (VFSLeaf) baseContainer.resolve(FILENAME_VIDEO_METADATA_XML);
		try {
			return (VideoMetadata) XStreamHelper.readObject(XStreamHelper.createXStreamInstance(), metaDataFile);
		} catch (Exception e) {
			log.error("Error while parsing XStream file for videoResource::" + videoResource, e);
			// return an empty, so at least it displays something and not an error
			VideoMetadata meta =  new VideoMetadataImpl();
			meta.setWidth(800);
			meta.setHeight(600);
			return meta;
		}
	}
	
	@Override
	public void startTranscodingProcessIfEnabled(OLATResource video) {
		if (videoModule.isTranscodingEnabled()) {
			startTranscodingProcess(video);
		}
	}
	
	@Override
	public VideoTranscoding retranscodeFailedVideoTranscoding(VideoTranscoding videoTranscoding) {
		return videoTranscodingDao.updateTranscodingStatus(videoTranscoding);
	}
	
	@Override
	public void startTranscodingProcess(OLATResource video) {
		List<VideoTranscoding> existingTranscodings = getVideoTranscodings(video);
		VideoMeta videoMetadata = getVideoMetadata(video);
		int height = videoMetadata.getHeight();
		// 1) setup transcoding job for original file size
		createTranscodingIfNotCreatedAlready(video, height, VideoTranscoding.FORMAT_MP4, existingTranscodings);
		// 2) setup transcoding jobs for all configured sizes below the original size
		int[] resolutions = videoModule.getTranscodingResolutions();
		for (int resolution : resolutions) {
			if (height <= resolution) {
				continue;
			}
			createTranscodingIfNotCreatedAlready(video, resolution, VideoTranscoding.FORMAT_MP4, existingTranscodings);
		}
		// 3) Start transcoding immediately, force job execution
		if (videoModule.isTranscodingLocal()) {
			try {
				JobDetail detail = scheduler.getJobDetail("videoTranscodingJobDetail", Scheduler.DEFAULT_GROUP);
				scheduler.triggerJob(detail.getName(), detail.getGroup());
			} catch (SchedulerException e) {
				log.error("Error while starting video transcoding job", e);
			}			
		}
	}
	
	/**
	 * Helper to check if a transcoding already exists and only create if not
	 * @param video
	 * @param resolution
	 * @param format
	 * @param existingTranscodings
	 */
	private void createTranscodingIfNotCreatedAlready(OLATResource video, int resolution, String format, List<VideoTranscoding> existingTranscodings) {
		boolean found = false;
		for (VideoTranscoding videoTranscoding : existingTranscodings) {
			if (videoTranscoding.getResolution() == resolution) {
				found = true;
				break;
			}
		}
		if (!found) {
			videoTranscodingDao.createVideoTranscoding(video, resolution, format);
		}		
	}

	
	@Override
	public List<VideoTranscoding> getVideoTranscodings(OLATResource video){
		List<VideoTranscoding> videoTranscodings = videoTranscodingDao.getVideoTranscodings(video);
		return videoTranscodings;
	}
	
	@Override
	public List<VideoTranscoding> getAllVideoTranscodings() {
		List<VideoTranscoding> videoTranscodings = videoTranscodingDao.getAllVideoTranscodings();
		return videoTranscodings;
	}
	
	@Override 
	public List<TranscodingCount> getAllVideoTranscodingsCount() {
		List<TranscodingCount> allVideoTranscodings = videoTranscodingDao.getAllVideoTranscodingsCount();
		return allVideoTranscodings;
	}
	
	@Override 
	public List<TranscodingCount> getAllVideoTranscodingsCountSuccess(int errorcode) {
		List<TranscodingCount> allVideoTranscodings = videoTranscodingDao.getAllVideoTranscodingsCountSuccess(errorcode);
		return allVideoTranscodings;
	}
	
	@Override 
	public List<TranscodingCount> getAllVideoTranscodingsCountFails(int errorcode) {
		List<TranscodingCount> allVideoTranscodings = videoTranscodingDao.getAllVideoTranscodingsCountFails(errorcode);
		return allVideoTranscodings;
	}
	
	@Override
	public List<VideoTranscoding> getOneVideoResolution(int resolution) {
		List<VideoTranscoding> oneResolution = videoTranscodingDao.getOneVideoResolution(resolution);
		return oneResolution;
	}
	

	@Override
	public String getAspectRatio(int width, int height) {
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.FLOOR);
		String ratioCalculated = df.format(width / (height + 1.0));
		String ratioString = "unknown";
		
		switch (ratioCalculated) {
		case "1.2": 
			ratioString = "6:5 Fox Movietone";
			break;
		case "1.25": 
			ratioString = "5:4 TV";
			break;
		case "1.33": 
			ratioString = "4:3 TV";
			break;
		case "1.37": 
			ratioString = "11:8 Academy standard film";
			break;
		case "1.41": 
			ratioString = "A4";
			break;
		case "1.43": 
			ratioString = "IMAX";
			break;
		case "1.5": 
			ratioString = "3:2 35mm";
			break;
		case "1.6": 
			ratioString = "16:10 Computer";
			break;
		case "1.61": 
			ratioString = "16.18:10 The golden ratio";
			break;
		case "1.66": 
			ratioString = "5:3 Super 16mm";
			break;
		case "1.77": 
			ratioString = "16:9 HD video";
			break;
		case "1.78": 
			ratioString = "16:9 HD video";
			break;
		case "1.85": 
			ratioString = "1.85:1 Widescreen cinema";
			break;
		case "2.35": 
			ratioString = "2.35:1 Widescreen cinema";
			break;
		case "2.39": 
			ratioString = "2.39:1 Widescreen cinema";
			break;
		case "2.41": 
			ratioString = "2.414:1 The silver ratio";		
			break;
		default :
			ratioString = width + ":" + height;
		}
		return ratioString;
	}
	
	@Override
	public String getDisplayTitleForResolution(int resolution, Translator translator) {
		int[] resolutions = videoModule.getTranscodingResolutions();
		boolean knownResolution = IntStream.of(resolutions).anyMatch(x -> x == resolution);
		String title = (knownResolution ? translator.translate("quality.resolution." + resolution) : resolution + "p");
		return title;
	}
	
	@Override
	public boolean hasMasterContainer (OLATResource videoResource) {
		VFSContainer baseContainer =  FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		VFSContainer masterContainer = (VFSContainer) baseContainer.resolve(DIRNAME_MASTER);
		return masterContainer != null & masterContainer.exists();		
	}
	
	@Override
	public VFSContainer getMasterContainer(OLATResource videoResource) {
		VFSContainer baseContainer =  FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		VFSContainer masterContainer = VFSManager.resolveOrCreateContainerFromPath(baseContainer, DIRNAME_MASTER);
		return masterContainer;
	}

	
	@Override
	public VFSContainer getTranscodingContainer(OLATResource videoResource) {
		VFSContainer baseContainer = videoModule.getTranscodingBaseContainer();
		VFSContainer resourceTranscodingContainer = VFSManager.getOrCreateContainer(baseContainer,
				String.valueOf(videoResource.getResourceableId()));
		return resourceTranscodingContainer;
	}
	
	
	@Override
	public VFSLeaf getMasterVideoFile(OLATResource videoResource) {
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf videoFile = (VFSLeaf) masterContainer.resolve(FILENAME_VIDEO_MP4);
		return videoFile;
	}
	
	@Override
	public VideoExportMediaResource getVideoExportMediaResource(RepositoryEntry repoEntry) {
		OLATResource videoResource = repoEntry.getOlatResource();
		OlatRootFolderImpl baseContainer= FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		// 1) dump repo entry metadata to resource folder
		LocalFolderImpl repoentryContainer = (LocalFolderImpl)VFSManager.resolveOrCreateContainerFromPath(baseContainer, DIRNAME_REPOENTRY); 
		RepositoryEntryImportExport importExport = new RepositoryEntryImportExport(repoEntry, repoentryContainer.getBasefile());
		importExport.exportDoExportProperties();
		// 2) package everything in resource folder to streaming zip resource
		VideoExportMediaResource exportResource = new VideoExportMediaResource(baseContainer, repoEntry.getDisplayname());
		return exportResource;
	}

	@Override
	public void validateVideoExportArchive(File file,  ResourceEvaluation eval) {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(file);
			// 1) Check if it contains a metadata file
//			ZipEntry metadataEntry = zipFile.getEntry(VideoManagerImpl.FILENAME_VIDEO_METADATA_XML);
//			VideoMetadata videoMetadataImpl = null;
//			if (metadataEntry != null) {// does no harm
//				InputStream metaDataStream = zipFile.getInputStream(metadataEntry);
//				videoMetadataImpl = (VideoMetadata) XStreamHelper.readObject(XStreamHelper.createXStreamInstance(), metaDataStream);
//				if (videoMetadataImpl != null) {
//					eval.setValid(true);
//				}
//			}
			// 2) Propose title from repo metadata
			ZipEntry repoMetadataEntry = zipFile.getEntry(DIRNAME_REPOENTRY + "/" + RepositoryEntryImportExport.PROPERTIES_FILE);
			RepositoryEntryImport repoMetadata = null;
			if (repoMetadataEntry != null) {
				InputStream repoMetaDataStream = zipFile.getInputStream(repoMetadataEntry);
				repoMetadata = RepositoryEntryImportExport.getConfiguration(repoMetaDataStream);
				if (repoMetadata != null) {
					eval.setDisplayname(repoMetadata.getDisplayname());
				}
			}
			
			zipFile.close();
		} catch (Exception e) {
			log.error("Error while checking for video resource archive", e);
		}
	}
	
	@Override
	public boolean importFromMasterFile(RepositoryEntry repoEntry, VFSLeaf masterVideo) {
		OLATResource videoResource = repoEntry.getOlatResource();
		
		// 1) copy master video to final destination with standard name
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf targetFile = VFSManager.resolveOrCreateLeafFromPath(masterContainer, FILENAME_VIDEO_MP4);
		VFSManager.copyContent(masterVideo, targetFile);
		masterVideo.delete();

		// calculate video duration
		long duration = movieService.getDuration(targetFile, FILETYPE_MP4);
		if (duration != -1) {
			repoEntry.setExpenditureOfWork(Formatter.formatTimecode(duration));
		}
		// generate a poster image, use 20th frame as a default
		VFSLeaf posterResource = VFSManager.resolveOrCreateLeafFromPath(masterContainer, FILENAME_POSTER_JPG);
		getFrame(videoResource, 20, posterResource);

		// 2) Set poster image for repo entry
		VFSLeaf posterImage = (VFSLeaf)masterContainer.resolve(FILENAME_POSTER_JPG);
		if (posterImage != null) {
			repositoryManager.setImage(posterImage, repoEntry);
		}
				
		return true;
	}
	
	@Override
	public Size getVideoResolutionFromOLATResource (OLATResource videoResource) {
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf targetFile = (VFSLeaf) masterContainer.resolve(FILENAME_VIDEO_MP4);
		Size videoSize = movieService.getSize(targetFile, FILETYPE_MP4);
		if (videoSize == null) {
			videoSize = new Size(800, 600, false);
		}
		return videoSize;
	}
	
	@Override
	public void exchangePoster (OLATResource videoResource) {
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf posterResource = VFSManager.resolveOrCreateLeafFromPath(masterContainer, FILENAME_POSTER_JPG);
		getFrame(videoResource, 20, posterResource);
		// Update also repository entry image, use new posterframe
		VFSLeaf posterImage = (VFSLeaf)masterContainer.resolve(FILENAME_POSTER_JPG);
		if (posterImage != null) {
			RepositoryEntry repoEntry = repositoryManager.lookupRepositoryEntry(videoResource, true);
			repositoryManager.setImage(posterImage, repoEntry);
		}
	}
	
	@Override
	public void updateVideoMetadata (OLATResource videoResource,VFSLeaf uploadVideo) {	
		VideoMeta meta = getVideoMetadata(videoResource);

		Size dimensions = movieService.getSize(uploadVideo, VideoManagerImpl.FILETYPE_MP4);
		// update video duration
		long duration = movieService.getDuration(uploadVideo, VideoTranscoding.FORMAT_MP4);

		if (duration != -1) {
			String length = Formatter.formatTimecode(duration);
			meta.setSize(uploadVideo.getSize());
			meta.setWidth(dimensions.getWidth());
			meta.setHeight(dimensions.getHeight());
			meta.setFormat(FilenameUtils.getExtension(uploadVideo.getName()));
			meta.setLength(length);
		}
	}

	@Override
	public boolean importFromExportArchive(RepositoryEntry repoEntry, VFSLeaf exportArchive) {
		OLATResource videoResource = repoEntry.getOlatResource();
		// 1) unzip archive
		VFSContainer baseContainer= FileResourceManager.getInstance().getFileResourceRootImpl(videoResource);
		ZipUtil.unzip(exportArchive, baseContainer);
		exportArchive.delete();
		
		// 2) update metadata from the repo entry export
		LocalFolderImpl repoentryContainer = (LocalFolderImpl) baseContainer.resolve(DIRNAME_REPOENTRY); 
		if (repoentryContainer != null) {
			RepositoryEntryImportExport importExport = new RepositoryEntryImportExport(repoentryContainer.getBasefile());
			importExport.setRepoEntryPropertiesFromImport(repoEntry);
			// now delete the import folder, not used anymore
			repoentryContainer.delete();
		}
		
		// 3) Set poster image for repo entry
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf posterImage = (VFSLeaf)masterContainer.resolve(FILENAME_POSTER_JPG);
		if (posterImage != null) {
			repositoryManager.setImage(posterImage, repoEntry);
		}

		return true;
	}

	@Override
	public VideoTranscoding updateVideoTranscoding(VideoTranscoding videoTranscoding) {
		return videoTranscodingDao.updateTranscoding(videoTranscoding);
	}

	@Override
	public void copyVideo(OLATResource sourceResource, OLATResource targetResource) {
		// 1) Copy files on disk
		File sourceFileroot = FileResourceManager.getInstance().getFileResourceRootImpl(sourceResource).getBasefile();
		File targetFileroot = FileResourceManager.getInstance().getFileResourceRootImpl(targetResource).getBasefile();
		FileUtils.copyDirContentsToDir(sourceFileroot, targetFileroot, false, "copyVideoResource");
		// 2) Trigger transcoding in background
		if (videoModule.isTranscodingEnabled()) {
			startTranscodingProcess(targetResource);
		}
	}
	
	@Override
	public boolean deleteVideoTranscodings(OLATResource videoResource) {
		videoTranscodingDao.deleteVideoTranscodings(videoResource);
		VFSStatus deleteStatus = getTranscodingContainer(videoResource).delete();
		return (deleteStatus == VFSConstants.YES ? true : false);
	}
	
	@Override
	public boolean deleteVideoMetadata(OLATResource videoResource) {
		int deleted = videoMetadataDao.deleteVideoMetadata(videoResource);
		return 0 < deleted;
	}

	@Override
	public List<VideoTranscoding> getVideoTranscodingsPendingAndInProgress() {
		return videoTranscodingDao.getVideoTranscodingsPendingAndInProgress();
	}
	
	@Override
	public List<VideoTranscoding> getFailedVideoTranscodings() {
		return videoTranscodingDao.getFailedVideoTranscodings();
	}
	
	@Override
	public void deleteVideoTranscoding(VideoTranscoding videoTranscoding) {
		videoTranscodingDao.deleteVideoTranscoding(videoTranscoding);
		VFSContainer container = getTranscodingContainer(videoTranscoding.getVideoResource());
		VFSLeaf videoFile = (VFSLeaf) container.resolve(videoTranscoding.getResolution() + FILENAME_VIDEO_MP4);
		if( videoFile != null ) {
			videoFile.delete();
		}
	}

	@Override
	public List<Integer> getMissingTranscodings(OLATResource videoResource){
		//get resolutions which are turned on in the videomodule
		int[] configuredResolutions = videoModule.getTranscodingResolutions();
		//turn the int[]-Array into a List
		List<Integer> configResList = IntStream.of(configuredResolutions).boxed().collect(Collectors.toList());
		List<VideoTranscoding> videoTranscodings = getVideoTranscodings(videoResource);

		for(VideoTranscoding videoTranscoding:videoTranscodings){
			Integer resolution = videoTranscoding.getResolution();
			configResList.remove(resolution);
		}
		
		return configResList;
	}
	
	@Override
	public VideoTranscoding createTranscoding(OLATResource video, int resolution,String format) {
		return videoTranscodingDao.createVideoTranscoding(video, resolution, format);
	}
	
	@Override
	public void saveChapters (List<VideoChapterTableRow> chapters, OLATResource videoResource){
		displayDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		vttDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		VFSContainer vfsContainer = getMasterContainer(videoResource);
		VFSLeaf webvtt = (VFSLeaf) vfsContainer.resolve(FILENAME_CHAPTERS_VTT);		
		if (webvtt == null) {
			webvtt = vfsContainer.createChildLeaf(FILENAME_CHAPTERS_VTT);
		}		

		if (chapters.size() == 0){
			webvtt.delete();
			return;
		}

		StringBuilder vttString = new StringBuilder("WEBVTT").append(CR);
		for (int i = 0; i < chapters.size(); i++) {
			vttString.append(CR).append("Chapter "+ (i+1)).append(CR);
			vttString.append(vttDateFormat.format(chapters.get(i).getBegin()));
			vttString.append(" --> ");
			vttString.append(vttDateFormat.format(chapters.get(i).getEnd())).append(CR);
			vttString.append(chapters.get(i).getChapterName().replaceAll(CR, " "));
			vttString.append(CR);
			}
		
		final BufferedOutputStream bos = new BufferedOutputStream(webvtt.getOutputStream(false));
		FileUtils.save(bos, vttString.toString(), ENCODING);
		try {
			bos.close();
		} catch (IOException e) {
			log.error("chapter.vtt could not be saved for videoResource::" + videoResource, e);
		}
	}
	
	/**
	 * reads an existing webvtt file to provide for display and to further process.
	 *
	 * @param List<VideoChapterTableRow> chapters the chapters
	 * @param OLATResource videoResource the video resource
	 */
	public void loadChapters(List<VideoChapterTableRow> chapters, OLATResource videoResource) {
		chapters.clear();
		displayDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		vttDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		VFSContainer vfsContainer = getMasterContainer(videoResource);
		VFSLeaf webvtt = (VFSLeaf) vfsContainer.resolve(FILENAME_CHAPTERS_VTT);

		if (webvtt != null && webvtt.exists()) {
			try {
				BufferedReader webvttReader = new BufferedReader(new InputStreamReader(webvtt.getInputStream()));
				String thisLine, regex = " --> ";
				
				while ((thisLine = webvttReader.readLine()) != null) {
					if (thisLine.contains(regex)) {
						String[] interval = thisLine.split(regex);
						Date begin = vttDateFormat.parse(interval[0]);
						Date end = vttDateFormat.parse(interval[1]);

						StringBuilder chapterTitle = new StringBuilder();
						String title;
						
						while ((title = webvttReader.readLine()) != null) {
							if (title.isEmpty() || title.contains(regex))
								break;
							chapterTitle.append(title).append(CR);
						}
						chapters.add(new VideoChapterTableRow(chapterTitle.toString().replaceAll(CR, " "),
								displayDateFormat.format(begin), begin, end));
					}
				}
				webvttReader.close();
				
			} catch (Exception e) {
				log.error("Unable to load WEBVTT File for resource::" + videoResource,e);
			}
		}
	}
	
	@Override
	public long getVideoDuration (OLATResource videoResource){
		VFSContainer masterContainer = getMasterContainer(videoResource);
		VFSLeaf video = (VFSLeaf) masterContainer.resolve(FILENAME_VIDEO_MP4);	
		long duration = movieService.getDuration(video, FILETYPE_MP4);
		return duration;
	}
	
	@Override
	public List<VideoMetaImpl> getAllVideoResourcesMetadata() {
		List<VideoMetaImpl> metadata = videoMetadataDao.getAllVideoResourcesMetadata();
		return metadata;
	}
	
	@Override
	public VideoMetaImpl getVideoMetadata(OLATResource videoResource) {
		VideoMetaImpl meta = videoMetadataDao.getVideoMetadata(videoResource);
		return meta;
	}
	
	@Override 
	public VideoMeta createVideoMetadata(RepositoryEntry repoEntry, long size, String fileName) {
		return videoMetadataDao.createVideoMetadata(repoEntry, size, fileName); 
	}
	
	@Override
	public List<RepositoryEntry> getAllVideoRepoEntries(String typename) {
		return videoMetadataDao.getAllVideoRepoEntries(typename);
	}

	@Override
	public boolean hasVideoFile(OLATResource videoResource) {
		VFSContainer masterContainer = getMasterContainer(videoResource);
		LocalFileImpl videoFile = (LocalFileImpl) masterContainer.resolve(FILENAME_VIDEO_MP4);	
		return videoFile != null & videoFile.exists();
	}

}
