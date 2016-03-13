package com.intita.wschat.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import utils.RandomString;

@Controller
public class FileController {

	private final static Logger log = LoggerFactory.getLogger(FileController.class);
	private final int DEFAULT_FILE_PREFIX_LENGTH = 15;
	static final private ObjectMapper mapper = new ObjectMapper();
	@Value("${crmchat.upload_dir}")
	private String uploadDir;
	@RequestMapping(method = RequestMethod.POST, value = "/upload_file/{roomId}")
	@ResponseBody
	public void saveFile(MultipartHttpServletRequest request,
			HttpServletResponse response,Principal principal,@PathVariable("roomId") Long roomId) {
		//0. notice, we have used MultipartHttpServletRequest

		//1. get the files from the request object
		Iterator<String> itr =  request.getFileNames();
		ArrayList<String> downloadLinks = new ArrayList<String>();
		log.info("hasNext:"+itr.hasNext());

		while (itr.hasNext())
		{
			MultipartFile mpf = request.getFile(itr.next());
			System.out.println(mpf.getOriginalFilename() +" uploaded!");
			String mainDir = ""+roomId;
			String subDir = principal.getName();
			String realPathtoUploads =  uploadDir+File.separator+mainDir+File.separator+subDir+File.separator;
			File dir = new File(realPathtoUploads);
			boolean exists = dir.exists();
			if(!exists)
				dir.mkdirs();
			String orgName=null;
			try {
				//just temporary save file info into ufile

				orgName = randomizeFileName(mpf.getOriginalFilename());
				String filePath = realPathtoUploads + orgName;
				File dest = new File(filePath);
				mpf.transferTo(dest);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
			String hostPart = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
			String downloadLink =  hostPart+"/"+ String.format("download_file?owner_id=%1$s&room_id=%2$s&file_name=%3$s",principal.getName(),roomId,orgName);
			downloadLinks.add(downloadLink);
			log.info("downloadLink:"+downloadLink);
		}
		//
		try {
			String responseStr = mapper.writeValueAsString(downloadLinks);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentLength(responseStr.length());
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(responseStr);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return downloadLink;
		//////
	}
	/**
	 * Size of a byte buffer to read/write file
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Path of the file to be downloaded, relative to application's directory
	 */

	/**
	 * Method for handling file download request from client
	 */
	@RequestMapping(method = RequestMethod.GET, value="/download_file")
	public void doDownload(HttpServletRequest request,
			HttpServletResponse response,@RequestParam Long owner_id,@RequestParam Long room_id, @RequestParam String file_name) throws IOException {

		// get absolute path of the application
		ServletContext context = request.getServletContext();
		// String appPath = context.getRealPath("");

		// construct the complete absolute path of the file
		String mainDir = ""+room_id;
		String subDir = ""+owner_id;
		String fullPath = uploadDir +File.separator+mainDir+File.separator+subDir+File.separator+ file_name;      
		File downloadFile = new File(fullPath);
		if (!downloadFile.exists()){
			log.debug("No such file:"+fullPath);
			return;
		}
		FileInputStream inputStream = new FileInputStream(downloadFile);

		// get MIME type of the file
		String mimeType = context.getMimeType(fullPath);
		if (mimeType == null) {
			// set to binary type if MIME mapping not found
			mimeType = "application/octet-stream";
		}
		System.out.println("MIME type: " + mimeType);

		// set content attributes for the response
		response.setContentType(mimeType);
		response.setContentLength((int) downloadFile.length());

		// set headers for the response
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"",
				deRandomizeFileName(file_name));
		response.setHeader(headerKey, headerValue);

		// get output stream of the response
		OutputStream outStream = response.getOutputStream();

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;

		// write bytes read from the input stream into the output stream
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}

		inputStream.close();
		outStream.close();

	}
	private String randomizeFileName(String fileName){
		RandomString randString = new RandomString(DEFAULT_FILE_PREFIX_LENGTH);
		String nameSufix = randString.nextString();
		return fileName + nameSufix;
	}
	private String deRandomizeFileName(String randomizedFileName){
		return randomizedFileName.substring(0,randomizedFileName.length()-DEFAULT_FILE_PREFIX_LENGTH);
	}
}
