package com.poemyear.kakao;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poemyear.kakao.client.DocThread;
import com.poemyear.kakao.client.KakaoClient;

public class KakaoMain {
	private static Logger log = LoggerFactory.getLogger(KakaoClient.class);

	public static void main(String[] args) throws InterruptedException {
		KakaoClient client = new KakaoClient("Uqg1y06vp87vNUdr036kN427gIgX1YE1446kJsXqxNxzgGYZE");

		// refreseh every 10 mins
		String submitToken = client.getSubmitToken();

		// seed
		String[] docList = client.doSeed(submitToken);

		BlockingQueue<String> queue = client.getLinkedBlockingQueue();
		// doc
		DocThread[] threads = new DocThread[5];

		int i = 0;
		for (String docUrl : docList) {
			log.info("docUrl: {}", docUrl);
			threads[i] = new DocThread(submitToken, docUrl, queue);
			threads[i++].start();
		}

	}
}
