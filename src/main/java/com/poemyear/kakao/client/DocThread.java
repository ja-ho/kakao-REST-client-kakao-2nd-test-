package com.poemyear.kakao.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocThread extends Thread {
	private static Logger log = LoggerFactory.getLogger(KakaoClient.class);

	protected BlockingQueue<String> blockingQueue;

	private final String header = "X-Auth-Token";

	private String submitToken;
	private String docUrl;

	private JerseyClient client;
	private JerseyWebTarget target;

	private long sleepInterval = 3 * 1000;
	private Category category;

	public DocThread(String submitToken, String docUrl, BlockingQueue<String> blockingQueue) {
		this.submitToken = submitToken;
		this.docUrl = docUrl;
		this.category = KakaoUtil.getCategory(docUrl);
		this.blockingQueue = blockingQueue;
		client = JerseyClientBuilder.createClient().property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION,
				true);
		target = client.target("http://api.welcome.kakao.com/");
	}

	@Override
	public void run() {
		log.info("Starting thread: {}", this.getId());
		int retry = 0;
		// String nextUrl = doDoc(docUrl);

		while (true) {
			try {
//				log.info("processing documents. url: {}", docUrl);
				// nextUrl = blockingQueue.poll();
				String nextUrl = doDoc(docUrl);
				if (nextUrl == null)
					Thread.sleep(1000);
				// doDoc(nextUrl);

				if (nextUrl == docUrl) {
					retry++;
					Thread.sleep(1000);
//					log.info("Wait for refreshing category. url: {}, retry: {}", docUrl, retry);
				} else {
//					log.info("finish processing documents. url: {}, next:{}", docUrl, nextUrl);
					retry = 0;
					docUrl = nextUrl;
				}
				Thread.sleep(150);
			} catch (ProcessingException pe) {
				client = JerseyClientBuilder.createClient()
						.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
				target = client.target("http://api.welcome.kakao.com/");
				log.error("SocketException occurred.", pe);
			} catch (javax.ws.rs.ServiceUnavailableException se) {
				client = JerseyClientBuilder.createClient()
						.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
				target = client.target("http://api.welcome.kakao.com/");
				log.error("SocketException occurred.", se);
			} catch (Exception e) {
				log.error("Exception occurred.", e);
			}
		}
	}

	public String getDoc(String url) {
		String str = target.path(url).request(MediaType.APPLICATION_JSON_TYPE).header(header, this.submitToken)
				.get(String.class);
		JsonObject response = null;
		try {
			response = KakaoUtil.toJson(str);
//			log.info("response: {}", str.toString());
		} catch (Exception e) {
			return null;
		}

		String next_url = response.getString("next_url");
		JsonArray images = response.getJsonArray("images");

		try {
			if (next_url == url && images.isEmpty()) {
				blockingQueue.put(url);
			} else {
				blockingQueue.put(next_url);
			}
		} catch (InterruptedException e) {
		}
		return null;
	}

	public String doDoc(String url) {

		String str = target.path(url).request(MediaType.APPLICATION_JSON_TYPE).header(header, this.submitToken)
				.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
		JsonObject response = null;
		try {
			response = KakaoUtil.toJson(str);
//			log.info("response: {}", str.toString());
		} catch (Exception e) {
			return null;
		}

		String next_url = response.getString("next_url");
		JsonArray images = response.getJsonArray("images");

		if (next_url == url && images.isEmpty()) {
			return url;
		}

		ArrayList<HashSet<String>> addSet = new ArrayList<>();
		ArrayList<HashSet<String>> deleteSet = new ArrayList<>();
		HashSet<String> forAdd = new HashSet<>();
		HashSet<String> forDelete = new HashSet<>();

		// maximum 100, can be null
//		log.info("get Category documents. image_count: {}", images.size());
		int addCnt = 0;
		int deleteCnt = 0;
		for (JsonValue value : images) {
			JsonObject obj = (JsonObject) value;
			String type = obj.getString("type");
			String id = obj.getString("id");

			if (addCnt > 0 && addCnt % 50 == 0) {
				addSet.add(forAdd);
				forAdd = new HashSet<>();
			}
			if (deleteCnt > 0 && deleteCnt % 50 == 0) {
				deleteSet.add(forDelete);
				forDelete = new HashSet<>();
			}

			if (type.equals("add")) {
				// if (!KakaoClient.getCategoryMap(category).containsKey(id)) {
				forAdd.add(id);
				addCnt++;
				// }
			} else if (type.equals("del")) {
				// if (KakaoClient.getCategoryMap(category).containsKey(id)) {
				// forDelete.add(id);
				// deleteCnt++;
				// } else {
				for (HashSet<String> tempSet : addSet) {
					if (tempSet.contains(id)) {
						tempSet.remove(id);
						break;
					} else {
						forDelete.add(id);
						deleteCnt++;
						break;
					}
				}
			}
		}
		if (!forAdd.isEmpty())
			addSet.add(forAdd);
		if (!forDelete.isEmpty())
			deleteSet.add(forDelete);

		/*
		 * int addcnt=0; for (HashSet<String> set : addSet) { addcnt+= set.size(); } int
		 * delcnt = 0; for (HashSet<String> set : deleteSet) { delcnt+= set.size(); }
		 * 
		 * log.info("Will be added: {} / Will be deleted: {}", addcnt, delcnt);
		 */
		// feature for adding & save
		feature(addSet);

		// Delete
		delete(deleteSet);

		return next_url;
	}

	private void feature(ArrayList<HashSet<String>> addSetList) {
		if (addSetList.isEmpty())
			return;
		// request FEATURE API
		for (HashSet<String> set : addSetList) {
			String ids = KakaoUtil.getIds(set);
			String str = target.path("image").path("feature").queryParam("id", ids)
					.request(MediaType.APPLICATION_JSON_TYPE).header(header, this.submitToken).get(String.class);
			JsonObject response = KakaoUtil.toJson(str);

			if (response != null && response.containsKey("features")) {
				JsonArray features = response.getJsonArray("features");
				if (features.isEmpty())
					return;
				save(features);
			}
		}
	}

	private void save(JsonArray features) {
		if (features == null || features.isEmpty())
			return;
		// SAVE API (POST)
		JsonObject requestJson = Json.createObjectBuilder().add("data", features).build();
		Response response = target.path("image").path("feature")
				.request(MediaType.TEXT_PLAIN)
				.header(header, this.submitToken)
				.accept(MediaType.TEXT_PLAIN)
				.post(Entity.text(requestJson.toString()));
		int status = response.getStatus();
//		log.info("save done. request_status:{}, saved_images: {}", status, features.size());

		// add to map
		for (JsonValue value : features) {
			String id = ((JsonObject) value).getString("id");
			KakaoClient.getCategoryMap(category).put(id, Status.ADDED);
		}
	}

	private void delete(ArrayList<HashSet<String>> deleteSet) {
		if (deleteSet == null || deleteSet.isEmpty())
			return;
		// DELETE API (DELETE)
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		for (HashSet<String> hashSet : deleteSet) {
			for (String id : hashSet) {
				arrayBuilder.add(Json.createObjectBuilder().add("id", id));
			}
			JsonObject requestJson = Json.createObjectBuilder().add("data", arrayBuilder).build();
			Response response = target.path("image").path("feature")
					.request(MediaType.TEXT_PLAIN)
					.header(header, this.submitToken)
					.accept(MediaType.TEXT_PLAIN)
					.method("DELETE", Entity.text(requestJson.toString()));


			int status = response.getStatus();
//			log.info("delete done. request_status:{}, deleted_images: {}", status, deleteSet.size());

			for (String id : hashSet) {
				if (KakaoClient.getCategoryMap(category).containsKey(id)) {
					KakaoClient.getCategoryMap(category).remove(id);
				}
			}
		}
	}
}
