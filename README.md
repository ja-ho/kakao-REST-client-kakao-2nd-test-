# Author: Hyeonsoo Shin (poemyear.dev@gmail.com)
# Environment: 
	Java Project - Gradle + Jax-rs (Jersey v2) + Javax.json + logback
	
# Program flow: 
	카카오 API 흐름을 따르며, Doc list에서 50개씩 Set으로 묶은 뒤, Save / Delete를 수행한다. 50개의 Feature API로 받은 JsonArray data를 그대로 Save의 body로 넣기 때문에 u int 64의 처리를 하지 않는다. 
	
	각 Category에 대한 수행을 Thread로 나눠서 5개의 Thread가 일을 수행한다. 특정 	Category에서의 doc list가 로드 되지 않을 경우, 1초의 시간을 sleep하여 retry한다. 
	
	Pagination은 link형태이기 때문에, load되지 않을 경우 다음, 또는 다다음으로 넘어갈 수 없기 때문에 현재의 Category의 로드를 대기한다. 

	잦은 테스트를 위하여 token값을 file r/w으로 이용한다. 만료시 재발급 한다. 

# 보완책 
	- 이에 대한 보완책으로 blockingQueue를 구현하여 모든 category의 next_url을 queue에 넣어둔 후, N개의 Thread가 현재의 페이지를 처리하고 난 이후, 이후 url을 queue에서 가져와서 처리하는 방식으로 구현하고자 하였으나, 시간 상 완성하지 못하였다. 
	
	- API call cnt ( 50/s ) 에 대한 조건을 활용하고자, 
	Save, Delete에 대한 api call을 각 call당 최대개수(50)으로 하여 콜하기 위하여Collector, Sender로 나눠서 구성하는 것을 개선방안으로 두었다. 
	Doc List에서 add, del에 대해 queue에 쌓은 후, 다른 thread가 queue를 polling하여 50개 단위로 post/delete 하는 구조이다. 



P.S.
Save / Delete request의 body의 예시가 당연히 Json형태라 판단하여 Application/Json 재차 시도하였으나, text 형태임을 종료 30분전에 알게되어서 테스트의 시간이 많이 부족하여 아쉬움이 남는다. POST, DELETE의 경우 단순 http response 이외의 response body등을 server에서 제공해주었으면 하는 아쉬움이 있다. 



# Dependencies 
	// jax-rs (jersey v2)
	compile group: 'javax.ws.rs', name: 'javax.ws.rs-api', version: '2.1'
	compile group: 'org.glassfish.jersey.core', name: 'jersey-common', version: '2.26'
	compile group: 'org.glassfish.jersey.core', name: 'jersey-client', version: '2.26'
	compile group: 'org.glassfish.jersey.core', name: 'jersey-server', version: '2.26'
	compile group: 'org.glassfish.jersey.containers', name: 'jersey-container-servlet', version: '2.26'
	compile group: 'org.glassfish.jersey.inject', name: 'jersey-hk2', version: '2.26'
	compile group: 'org.glassfish.jersey.media', name: 'jersey-media-json-jackson', version: '2.26'
	providedCompile "javax.servlet:javax.servlet-api:3.1.0"
	
	// json  (Gson 미사용)
	compile group: 'com.google.code.gson', name: 'gson', version: '2.8.2'
	compile group: 'org.json', name: 'json', version: '20160810'
	// compile group: 'javax.json', name: 'javax.json-api', version: '1.1'
	compile group: 'org.glassfish', name: 'javax.json', version: '1.0.4'
	
	
	// logback
	compile group: 'org.slf4j', name: 'slf4j-api', version: '1.7.25'
	compile group: 'ch.qos.logback', name: 'logback-core', version: '1.2.3'
	testCompile group: 'ch.qos.logback', name: 'logback-classic', version: '1.2.3'
	
