package edu.neu.fcn.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import edu.neu.fcn.CustCompare.LRUComparator;
import edu.neu.fcn.dto.ForJsonObject;
import edu.neu.fcn.dto.URLMapper;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

public class ServerWithThreads {

	static String context = System.getProperty("user.dir");
	static Long allFileSize = (long) 0;
	static long maxLimit = 224008; // 182,229

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// int port = Integer.parseInt(args[1]);
		// String originServer = args[3];
		String originServer="http://ec2-54-167-4-20.compute-1.amazonaws.com";
		int port=34567;
		
		// initialize map from file. map is used for all manipulation i.e.
		// searching using links.
		// maintains 10MB while continous update removes not used files. This
		// happens only when there s a server miss.
		// the LRU value should be updated frequently.
		System.out.println("Running ....");
		try {
			Long allFileSizes=(long) 0;
			ServerSocket server = new ServerSocket(port);
			Map<String, URLMapper> mapper = new LinkedHashMap<String, URLMapper>();
			// initialize the MAP
			ForJsonObject jsonWithMap = getMapFromFile();
			if (jsonWithMap == null)
				mapper = mapper;
			else {
				mapper = jsonWithMap.getAllLinks();
				allFileSizes=jsonWithMap.getFullSize();
				printMapContents(mapper);
			}
			boolean servedFromFile = false;
			boolean servedFromOrigin = false;

			while (true) {

				// System.out.println("Waiting ");
				Socket socket = server.accept();
				// Create New Threads rest of the functionality should be
				// handled by threads. and server must go back to accept.
				TryingThreads thread=new TryingThreads(socket, originServer, mapper);
				Thread t1=new Thread(thread);
				t1.start();
				Date today = new Date();
				t1.join();
				/*
				 * BufferedReader br = new BufferedReader(new
				 * InputStreamReader(socket.getInputStream()));
				 * 
				 * StringBuilder requestBuild = new StringBuilder(); String
				 * line; while ((line = br.readLine()) != null) { //
				 * System.out.println("reading lines "+line);
				 * requestBuild.append(line); if (!(line.length() > 0)) break; }
				 * // System.out.println(requestBuild.toString()); if
				 * (requestBuild.length() > 0) { String httpRequest = null;
				 * String getRequest = requestBuild.toString(); Pattern pat =
				 * Pattern.compile("GET .* HTTP"); Matcher mat =
				 * pat.matcher(getRequest); if (mat.find()) { httpRequest =
				 * getRequest.substring(mat.start() + 4, mat.end() - 4).trim();
				 * // httpRequest=httpRequest.substring(4, //
				 * httpRequest.length()-4); }
				 * 
				 * System.out.println("actual request from Client "+httpRequest)
				 * ; PrintWriter out = new PrintWriter(socket.getOutputStream(),
				 * true);
				 * 
				 * if (mapper.size()>0) {
				 * System.out.println("mapper size is greater than zero");
				 * System.out.println("does mapper has the requried key : "
				 * +mapper.containsKey(httpRequest)); if
				 * (mapper.containsKey(httpRequest)) { Httpline =
				 * getHTMLFileFromStorage(mapper.get(httpRequest).getContentPath
				 * ()); mapper.get(httpRequest).setLruCount("0"); servedFromFile
				 * = true; System.out.println("served from local cache"); }
				 * 
				 * } if (!servedFromFile) { try { if
				 * (originServer.contains("8080")) Httpline =
				 * getHTMLFileFromOrigin(originServer + httpRequest); else
				 * Httpline = getHTMLFileFromOrigin(originServer + ":8080" +
				 * httpRequest); // writeToAFile(Httpline,httpRequest);
				 * servedFromOrigin = true;
				 * System.out.println("served from origin"); } catch (Exception
				 * e) { e.printStackTrace();
				 * out.println("HTTP/1.1 404 Not Found"); out.print("\r\n\r\n");
				 * continue; } } System.out.println(Httpline);
				 * 
				 * out.println("HTTP/1.1 200 OK");
				 * out.println("Content-Length: " + Httpline.length());
				 * out.println("Content-Type: text/html");
				 * out.println("Connection: Closed"); out.print("\r\n\r\n");
				 * out.println(Httpline);
				 * 
				 * if (servedFromFile) { mapper=updateFileFromMap(httpRequest,
				 * mapper); } else if (servedFromOrigin) {
				 * mapper=addRecordAndHTMLPage(Httpline, httpRequest, mapper); }
				 * httpRequest=""; servedFromFile=false; servedFromOrigin=false;
				 * 
				 * }
				 * 
				 * 
				 * 
				 * }
				 */
				
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	private static void printMapContents(Map<String, URLMapper> mapper) {

		for (Map.Entry<String, URLMapper> entry : mapper.entrySet()) {
			System.out.println(entry.getKey() + " ==> " + entry.getValue().getContentPath() + " => "
					+ entry.getValue().getLruCount() + " -> size of the file: " + entry.getValue().getSize());
		}

	}

	private static Map<String, URLMapper> addRecordAndHTMLPage(String httpline, String httpRequest,
			Map<String, URLMapper> mapper) {

		long sizeOfFile = (long) httpline.length();
		long allFileSizeTemp = allFileSize;
		System.out.println("Function Name addRecordAndHTMLPage : variable httpRequest :" + httpRequest);
		String newPath = httpRequest.substring(1).replaceAll("/", "-");
		newPath = newPath.trim();
		URLMapper newFileMap = new URLMapper();
		newFileMap.setContentPath(newPath);
		System.out.println("Size of File " + sizeOfFile);
		newFileMap.setSize(sizeOfFile);
		newFileMap.setLruCount("0");

		allFileSizeTemp = allFileSize + sizeOfFile;

		if (allFileSizeTemp > maxLimit) {
			mapper = performRemovalOfFiles(mapper, sizeOfFile);
		}
		// mapper=performRemovalOfFiles(mapper,sizeOfFile);
		mapper.put(httpRequest, newFileMap);
		mapper = updateFileFromMap(httpRequest, mapper);
		allFileSize = allFileSize + sizeOfFile;

		try {

			File file = new File(context + "\\HTMLFiles\\" + newPath + ".html");
			FileWriter writer;
			try {
				writer = new FileWriter(file, false);
				PrintWriter printer = new PrintWriter(writer);
				printer.write(httpline);

				printer.close();
				writer.close();
				return mapper;
			} catch (Exception e) {
				e.printStackTrace();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return mapper;
	}

	private static Map<String, URLMapper> performRemovalOfFiles(Map<String, URLMapper> mapper, long sizeOfFile) {

		System.out.println("===============================================================");
		List<String> removeKeyList = new ArrayList<String>();
		System.out.println("Inside Perform Removal Of Files");
		Set<Entry<String, URLMapper>> entries = mapper.entrySet();
		List<Entry<String, URLMapper>> listOfEntries = new ArrayList<Entry<String, URLMapper>>(entries);
		if (mapper.size() > 1) {

			long size = 0;
			for (int i = mapper.size() - 1; i > 0; i--) {
				Entry enterLast = listOfEntries.get(i); // change this to N
														// where n is calculated
														// to find if a single
														// file can be swapped
				URLMapper urlMapper = (URLMapper) enterLast.getValue();
				size = size + urlMapper.getSize();
				if (sizeOfFile <= size) {
					removeKeyList.add((String) enterLast.getKey());
					break;
				} else {
					removeKeyList.add((String) enterLast.getKey());
				}
			}
			long totSizeRemoved = 0;
			for (String removeElem : removeKeyList) {
				URLMapper urlMapper = mapper.remove(removeElem);
				totSizeRemoved += urlMapper.getSize();
				removeOldFiles(urlMapper.getContentPath());
			}
			System.out.println(
					"The size of the incoming file " + sizeOfFile + " the size that is freed " + totSizeRemoved);

			printMapContents(mapper);
			System.out.println("===============================================================");
		}
		return mapper;
	}

	private static void removeOldFiles(String contentPath) {
		System.out.println("===============================================================");
		System.out.println("Inside Remove Old Files");
		String fileName = context + "\\HTMLFiles\\" + contentPath + ".html";
		File file = new File(fileName);
		boolean fileDeleted=false ;
		while(!fileDeleted)
		{
			fileDeleted= file.delete();
		}
		System.out.println("file deleted " + fileName + " deletion message " + fileDeleted);

		System.out.println("===============================================================");

	}

	private static Map<String, URLMapper> updateFileFromMap(String httpRequest, Map<String, URLMapper> mapper) {

		mapper = checkMapSorting(mapper);
		long allFileSize = (long) 0;

		System.out.println("Received Request " + httpRequest);

		for (Map.Entry<String, URLMapper> entry : mapper.entrySet()) {
			System.out.println(entry.getKey() + " ==> " + entry.getValue().getContentPath() + " => "
					+ entry.getValue().getLruCount() + " -> size of the file: " + entry.getValue().getSize());
		}

		for (Map.Entry<String, URLMapper> entry : mapper.entrySet()) {
			Integer lruCount = 0;
			if (!entry.getKey().equalsIgnoreCase(httpRequest)) {
				lruCount = Integer.parseInt((entry.getValue().getLruCount())) + 1;
			}

			entry.getValue().setLruCount(lruCount.toString());
			System.out.println("content " + entry.getValue().getContentPath());
			System.out.println("size " + entry.getValue().getSize());
			allFileSize += entry.getValue().getSize();

		}

		String fileName = context + "\\mapper\\cdnMapper.txt";

		ForJsonObject mapToJson = new ForJsonObject();
		mapToJson.setAllLinks(mapper);
		mapToJson.setFullSize(allFileSize);

		Gson gson = new Gson();
		String json = gson.toJson(mapToJson);

		try {

			File file = new File(fileName);
			FileWriter writer;
			writer = new FileWriter(file, false);
			PrintWriter printer = new PrintWriter(writer);
			printer.write(json);
			printer.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mapper;
	}

	private static String getHTMLFileFromStorage(String contentPath) {

		String fileName = context + "\\HTMLFiles\\" + contentPath + ".html";
		StringBuilder strBuild = new StringBuilder();
		try {
			File text = new File(fileName);
			Scanner scanner = new Scanner(text);
			while (scanner.hasNext()) {
				strBuild.append(scanner.next());
			}
			scanner.close();
			return strBuild.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static ForJsonObject getMapFromFile() {

		String fileName = context + "\\mapper\\cdnMapper.txt";
		StringBuilder strBuild = new StringBuilder();
		Gson gson = new Gson();
		try {
			File text = new File(fileName);
			
			if (text.createNewFile()){
		        System.out.println("File is created!");
		      }else{
		        System.out.println("File already exists.");
		      }
			
			
			Scanner scanner = new Scanner(text);
			while (scanner.hasNext()) {
				strBuild.append(scanner.next());
			}
			scanner.close();

			ForJsonObject response = gson.fromJson(strBuild.toString(), ForJsonObject.class);
			if (response != null)
				allFileSize = response.getFullSize();
			else
				allFileSize = (long) 0;
			return response;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static String getHTMLFileFromOrigin(String urlToRead) throws Exception {
		try {
			StringBuilder result = new StringBuilder();
			URL url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
			conn.disconnect();
			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static Map checkMapSorting(Map<String, URLMapper> mapper) {

		Set<Entry<String, URLMapper>> entries = mapper.entrySet();
		List<Entry<String, URLMapper>> listOfEntries = new ArrayList<Entry<String, URLMapper>>(entries);

		Collections.sort(listOfEntries, new LRUComparator());

		LinkedHashMap<String, URLMapper> sortedByLRU = new LinkedHashMap<String, URLMapper>(listOfEntries.size());

		for (Entry<String, URLMapper> entry : listOfEntries) {
			sortedByLRU.put(entry.getKey(), entry.getValue());
		}
		Set<Entry<String, URLMapper>> entrySetSortedByValue = sortedByLRU.entrySet();
		for (Entry<String, URLMapper> mapping : entrySetSortedByValue) {
			System.out.println(mapping.getKey() + " ==> " + mapping.getValue().getContentPath() + " => "
					+ mapping.getValue().getLruCount());
		}

		return sortedByLRU;

	}



}
