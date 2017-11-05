package com.poemyear.kakao.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KakaoClient {

	private static Logger log = LoggerFactory.getLogger(KakaoClient.class);

	
	final BlockingQueue<String> linkedBlockingQueue;

	
	private final String header = "X-Auth-Token";
	
	private long tokenGeneratedTime;
	private String submitToken;
	private String userToken;
	
	private JerseyClient client;
	private JerseyWebTarget target;

	private static HashMap<String, Status> artMap = new HashMap<>();
	private static HashMap<String, Status> sportMap = new HashMap<>();
	private static HashMap<String, Status> socialMap = new HashMap<>();
	private static HashMap<String, Status> newsMap = new HashMap<>();
	private static HashMap<String, Status> blogMap = new HashMap<>();

	private String fileName = "./token.txt";
	
	public KakaoClient(String token) {
		userToken = token;
		client = JerseyClientBuilder.createClient().property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
		target = client.target("http://api.welcome.kakao.com/");
		linkedBlockingQueue = new LinkedBlockingQueue<String>();
	}

	public BlockingQueue<String> getLinkedBlockingQueue() {
		return linkedBlockingQueue;
	}
	
	public static HashMap<String, Status> getCategoryMap(Category category) {
		switch(category) {
		case ART:
			return artMap;
		case SPORT:
			return sportMap;
		case SOCIAL:
			return socialMap;
		case NEWS:
			return newsMap;
		case BLOG:
			return blogMap;
		default:
			break;
		}
		return null;
	}

	public String fileRead() {
		String token = null;
		try {
			FileReader fileReader = new FileReader(new File(fileName));
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			tokenGeneratedTime = Long.valueOf(bufferedReader.readLine());
			if (tokenGeneratedTime + 10 * 60 * 1000 > System.currentTimeMillis()) {
				token = bufferedReader.readLine();
				bufferedReader.close();
				return token;
			}
			bufferedReader.close();
			token = null;
			log.info("date: {}, token: {}", tokenGeneratedTime, token);
			return token;

		} catch (IOException e) {
			log.error("", e);
		}
		return token;
	}
	
	public void fileWrite(long time, String newToken) {
        try{
            File file = new File(fileName) ;
            FileWriter fw = new FileWriter(file, false) ;
            fw.write(Long.toString(time)+"\n");
            fw.write(newToken);
            fw.flush();
            fw.close();
        }catch(Exception e){
        	log.error("{}", e);
        }
	}
	
	public String getSubmitToken() {
		submitToken = fileRead();
//		submitToken = "TJBMpg4wn8zwRClqWGBk75Vn2clORWpR880XquBN2R2ZM56Y6F7zYeJ0kQ-nECD1nDnpEXkQOfld";
		if (submitToken == null) {
			long time = System.currentTimeMillis();
			submitToken = target.path("token").path(userToken).request(MediaType.TEXT_PLAIN).get(String.class);
			fileWrite(time, submitToken);
			log.info("response: {}", submitToken);
		}
		return this.submitToken;
	}

	
	public String[] doSeed(String submitToken) {
		String str = target.path("seed").request(MediaType.TEXT_PLAIN).header(header, this.submitToken).get(String.class);
		log.info("response: {}", str.toString());
		return str.split("\n");
	}

	}
