package logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FirstDeliverable {
	
	private static FileWriter fileWriter;
	private static final String PROJNAME ="STDCXX";
	private static final Integer STEP = 1000;
	private static final String PATH = "local path of STDCXX";
	
	private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
	   }
	
	private static Map<String, String> getTicketsMap() throws IOException, JSONException{
		//
		Integer start = 0;
		Integer end = 0;
		Integer total = 1;
		//using LinkedHashMap to maintain  the order of puts
		Map<String, String> tickets = new LinkedHashMap<>();
		
		do {
			end = start + STEP;
			
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
					+ PROJNAME + "%22AND%22resolution%22=%22fixed%22%20ORDER%20BY%20%20resolutiondate%20%20ASC&fields=key,resolutiondate,created&startAt="
					+ start.toString() + "&maxResults=" + end.toString();
			
			//Obtain the JSON
			JSONObject json = readJsonFromUrl(url);
			
			//starting get info from JSON
			total = json.getInt("total");
			JSONArray issues = json.getJSONArray("issues");
			
			
			for (; start < total && start < end; start++) {
				
				JSONObject tmp = issues.getJSONObject(start%STEP);
				JSONObject fields = tmp.getJSONObject("fields");
				String key = tmp.getString("key");
				//Getting LocalDate for possible future implementations
				LocalDate resolutionDate = LocalDate.parse(fields.getString("resolutiondate").subSequence(0, 10));
				// Get only yyyy-mm
				String truncateDate = resolutionDate.getYear()+"-"+resolutionDate.getMonthValue();

				//put key and date into the map
				if(checkCommit(key))
					tickets.put(key, truncateDate);
				
			}
			
		} while (start < total);
		
		
		
		return tickets;
	}
	
	
	//Function for extract a Json from a url
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	      InputStream is = new URL(url).openStream();	     
	      try( InputStreamReader tmp = new InputStreamReader(is, StandardCharsets.UTF_8);
	    	   BufferedReader rd = new BufferedReader(tmp);
	    	)  
	      
	      {
	         String jsonText = readAll(rd);
	         return new JSONObject(jsonText);
	         
	       } finally {
	         is.close();
	       }
	   }
	
	
	public static void main(String[] args) throws IOException, JSONException{
		
		
		Map<String, String> tickets = null;
		Logger logger = Logger.getLogger(FirstDeliverable.class.getSimpleName());
		
		
		//get tickets from URL specified in the function
		tickets = getTicketsMap();
		//check commit for tickets

		
		
		//save on csv file
		Integer i = 0;
		String fileName = PROJNAME + "_FixedTickets.csv";
		prepareOutputFile(fileName);
		
		
		for (Map.Entry<String,String> entry : tickets.entrySet()) {
			String key = entry.getKey();
			fileWriter.append(i.toString());
			fileWriter.append(",");
			fileWriter.append(key);
			fileWriter.append(",");
			fileWriter.append(entry.getValue());
			fileWriter.append("\n");
			i++;
		}
		
		closeFile();
		logger.log(Level.INFO,"File saved");
	
	}
	
	private static  boolean checkCommit(String ticket) throws GitAPIException {
		Git git = Git.open(localRepoFolder);
		File localRepoFolder;
		localRepoFolder = new File(PATH);
		Iterable<RevCommit> commits = git.log().call();
		
		Iterator<RevCommit> itr = commits.iterator();
		while (itr.hasNext()) {
			RevCommit element = itr.next();
			String comment = element.getFullMessage();

			if (comment.contains(ticket+".") || comment.contains(ticket+":") 
					|| comment.contains(ticket+" :")) {
			
				return true;
			}
			
		}
			
		return false;
		
	}
	
	private static void prepareOutputFile(String fileName) throws IOException {
		
		fileWriter = new FileWriter(fileName);
		fileWriter.append("Index,Tickets Key,Resolution Date");
        fileWriter.append("\n");
	}
	
	private static void closeFile() throws IOException {
		fileWriter.flush();
		fileWriter.close();
	}
	
}
