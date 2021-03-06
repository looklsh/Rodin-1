package rodin.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import rodin.repository.vo.FontVo;
import rodin.repository.vo.PosterVo;
import rodin.repository.vo.UserVo;
import rodin.service.AnalysisService;
import rodin.service.FontService;
import rodin.util.Client;
import rodin.util.HandlerFile;

@Controller
@RequestMapping("/analysis")
public class AnalysisController {
	private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

	@Value("${access_key}")
	private String accessKey;
	
	@Value("${secret_key}")
	private String secretKey;
	
	@Value("${bucket_name}")
	private String bucketName;
	
	@Autowired
	AnalysisService analysisService;
	
	@Autowired
	FontService fontService;
	
	@RequestMapping(value="", method=RequestMethod.GET)
	public String analysis(Model model, HttpSession session, ModelAndView mv) throws Exception {
		//model.addAttribute("serverTime");
		UserVo user = (UserVo) session.getAttribute("user");
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
//		AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
		AmazonS3 s3Client = new AmazonS3Client(credentials);
		
		if (user == null) {
			return "redirect:/";
		} else {
			logger.debug("Get Image List");
			//List<PosterVo> posterList = analysisService.selectAllFile(user.getEmail());
			List<String> posterList = analysisService.selectAllFileNameS3(s3Client, bucketName, session);
			System.err.println("List : " + posterList.toString());
			model.addAttribute("posterList", posterList);			
			return "analysis/analysis";
		}
	}
	
	/*
	 * Send to Server
	 */
	/*
	@RequestMapping(value="", method=RequestMethod.POST)
	public String analysisAction(HttpSession session) {
		logger.debug("Send File to Server");
		logger.debug("Must be fixed! - Send Cropped Image to Server");
		analysisService.sendFile(session);
		return "redirect:/analysis";
	}
	*/

	@RequestMapping(value="", method=RequestMethod.POST)
	public String analysisAction() {
		//logger.debug("Send File to Server");
		//logger.debug("Must be fixed! - Send Cropped Image to Server");
		System.err.println("Cropped Image");
		logger.debug("Get Cropped Image");
		// analysisService.sendFile(session);
		return "redirect:/analysis";
	}
	
	/*
	 * Upload Action
	 * */
	/*
	@RequestMapping(value="/upload", method=RequestMethod.POST)
	public String uploadAction(
			MultipartHttpServletRequest multipartRequest,
			//HttpServletRequest request,
			//HttpServletResponse response,
			Model model,
			HttpSession session) {
		
		logger.debug("Upload Action");
		
		analysisService.uploadFile(multipartRequest, session);
		
		return "redirect:/analysis";
	}
	*/
	
//	@ResponseBody
//	@RequestMapping(value="/flask", method=RequestMethod.POST) // IOException - 파일이 없을 때 발생할 에러.
//	public String submitReport1(@RequestParam("file1") MultipartFile picture) throws IOException {
//	
	//@ResponseBody
	@RequestMapping(value="/flask", method=RequestMethod.GET)
	public String getSentImg() {
		System.err.println("flask GET");
		return "redirect:/analysis";
	}

	@ResponseBody
	@RequestMapping(value="/flask", method=RequestMethod.POST) // IOException - 파일이 없을 때 발생할 에러.
	public ResponseEntity<JSONObject> sendCoppedImgtoServer(HttpSession session, MultipartHttpServletRequest multipartRequest) throws IOException {
	// public FontVo sendCoppedImgtoServer(HttpSession session, MultipartHttpServletRequest multipartRequest) throws IOException {
		
		System.err.println("flask POST");
		
		// MultipartFile picture = (MultipartFile) session.getAttribute("multipartFile");
		MultipartFile cropped = multipartRequest.getFile("croppedImage");
		// 플라스크한테 파일 보낼꺼야~(MULTIPART_FORM_DATA);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		
		//  BODY : 실제 데이터
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		
		/*
		UUID tempFileName = UUID.randomUUID();
		*/
        // String tempFileExtensionPlusDot = "."+FilenameUtils.getExtension(picture.getOriginalFilename());
		// String tempFileExtensionPlusDot = "."+FilenameUtils.getExtension(cropped.getOriginalFilename());
		// System.out.println(tempFileExtensionPlusDot);
		
		// @SuppressWarnings("rawtypes")
		// String fileName = (String) ((Map) session.getAttribute("fileName")).get("newName");
		
		// File tempFile = File.createTempFile(fileName, tempFileExtensionPlusDot);
		File tempFile = File.createTempFile("crop", "");
		//File tempFile = new File("cropped");
        // picture.transferTo(tempFile);
		cropped.transferTo(tempFile);
        FileSystemResource fileSystemResource = new FileSystemResource(tempFile);

		body.add("picture", fileSystemResource);
		
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		
		String serverUrl = "http://127.0.0.1:5000/predict_font";
		
		RestTemplate restTemplate = new RestTemplate();
		
		//ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);
		ResponseEntity<JSONObject> response = restTemplate.exchange(serverUrl, HttpMethod.POST, requestEntity, JSONObject.class);
		
		// System.out.println(response.getBody());
		System.err.println(response.getBody());
		JSONObject jsonObj = response.getBody();
		
		List<Map<String, List<Map<String, Object>>>> fontListMap = (List<Map<String, List<Map<String, Object>>>>) analysisService.listToMapList(jsonObj);
		
		/* Get Font Info */
		logger.debug("Get Font Infomation for adding session");
		String fontFullName = (String) fontListMap.get(0).get("accu_1st").get(0).get("font");
		// double accuracy = (double) fontListMap.get(0).get("accu_1st").get(0).get("accu");
		double fontAccuracy = (double) fontListMap.get(0).get("accu_1st").get(1).get("accu");
		logger.debug("FontFullName : " + fontFullName);

		String fontName = fontFullName.substring(0, fontFullName.length()-6);
		String ocr = fontFullName.substring(fontFullName.length()-5, fontFullName.length()-4);
		String extension = fontFullName.substring(fontFullName.length()-4);
		
		logger.debug("FontName : " + fontName);
		logger.debug("Accuracy : " + fontAccuracy);
		logger.debug("Ocr : " + ocr);
		logger.debug("Extension : " + extension);
		
		FontVo fvo = new FontVo();
		logger.debug("fvo is created : " + fvo);
		
		fvo = fontService.getFontInfo(fontName);
		if (fvo == null) {
			logger.debug("FontVo 없음");	
		} else {
			logger.debug("FontVo : " + fvo.toString());
		}
			
		
		logger.debug("Get Font Information Finished");
		fvo.setAccuracy(fontAccuracy);
		
		/* 폰트 이름에 '+' 문자가 있는 경우를 위한 전처리 */
		String tempStr = fontFullName;
		String fontPiece = tempStr.replaceAll("\\+", "%2B");
		
		fvo.setFontPiece(fontPiece);
		fvo.setOcr(ocr);
		logger.debug("FontVo : " + fvo.toString());
		
		session.setAttribute("accu_1st", fvo);
		
		// return response.getBody();
		return ResponseEntity.ok(jsonObj);
		// return fvo;
	}
	
	public static class FilenameAwareInputStreamResource extends InputStreamResource {
	    private final String filename;
	    private final long contentLength;

	    public FilenameAwareInputStreamResource(InputStream inputStream, long contentLength, String filename) {
	        super(inputStream);
	        this.filename = filename;
	        this.contentLength = contentLength;
	    }

	    @Override
	    public String getFilename() {
	        return filename;
	    }

	    @Override
	    public long contentLength() {
	        return contentLength;
	    }
	}
	
	private FilenameAwareInputStreamResource generateFilenameAwareByteArrayResource( MultipartFile agreement) {
	    try {
	        return new FilenameAwareInputStreamResource(agreement.getInputStream(), agreement.getSize(), String.format("%s", agreement.getOriginalFilename()));
	    } catch (Exception e) {
	        return null;
	    }
	}
	
	
	@RequestMapping(value="/s3upload", method=RequestMethod.POST)
	public String s3UploadAction(
			@RequestParam("file1") MultipartFile file,
			HttpSession session) throws Exception {
		System.err.println("s3Upload POST");
		System.err.println(accessKey);
		System.err.println(secretKey);
		
		session.setAttribute("multipartFile", file);
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
//		AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
		AmazonS3 s3Client = new AmazonS3Client(credentials);
		
		analysisService.uploadFileS3(s3Client, file, bucketName, session);
		
		return "redirect:/analysis";
	}
	
	@RequestMapping(value="/cropper", method=RequestMethod.GET)
	public String cropper(HttpSession session) {
		
		System.err.println("cropper GET");
		
		String url = (String) session.getAttribute("imgURL");
		System.err.println("url(GET) : " + url);
		// null값 받아오는 중
		return "analysis/cropper";
	}
	
	@RequestMapping(value="/cropper", method=RequestMethod.POST)
	public String sendDataToCropper(
			@RequestParam("url") String url,
			HttpSession session) {
		
		System.err.println("cropper POST");
			
		System.err.println("url(POST) : " + url);
		// 제대로 나옴
		session.setAttribute("imgURL", url);
		
		return "redirect:/analysis/cropper";
	}
	
	@ResponseBody
	@RequestMapping(value="/getFontInfo", method=RequestMethod.POST)
	public ModelMap getFontInfo(HttpSession session) {
		
		FontVo font = (FontVo) session.getAttribute("accu_1st");
		ModelMap fontMap = new ModelMap("font", font);
		System.err.println("fontMap : " + fontMap.toString());
		return fontMap;
	}
	
	
}
