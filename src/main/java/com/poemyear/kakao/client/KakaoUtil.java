package com.poemyear.kakao.client;

import java.io.StringReader;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class KakaoUtil {
	public static JsonObject toJson(String str) {
		JsonReader jsonReader = Json.createReader(new StringReader(str));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		return object;
	}
	
	public static Category getCategory(String str) {
		if (str.contains("art"))
			return Category.ART;
		if (str.contains("sport"))
			return Category.SPORT;
		if (str.contains("social"))
			return Category.SOCIAL;
		if (str.contains("news"))
			return Category.NEWS;
		if (str.contains("blog"))
			return Category.BLOG;
		return null;
	}
	
	public static String getIds(HashSet<String> idSet) {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		for (String id : idSet) {
			if (i == 0) {
				buffer.append(id);
				if (idSet.size() > 1) {
					buffer.append("[");
				}
			} else {
				buffer.append("," + id);
			}
			i++;
		}
		String ids = buffer.append("]").toString();
		return ids;
	}
}
