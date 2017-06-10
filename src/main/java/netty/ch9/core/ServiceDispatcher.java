package netty.ch9.core;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ServiceDispatcher {
	private static ApplicationContext springContext;
	
	@Autowired
	public void init(ApplicationContext springContext) {
		ServiceDispatcher.springContext = springContext;
	}
	
	protected Logger logger = LogManager.getLogger(this.getClass());
	
	public static ApiRequest dispatch(Map<String, String> requestMap) {
		String serviceUri = requestMap.get("REQUEST_URI");
		String beanName = null;
		
		if (serviceUri == null) {
			beanName = "notFound";
		}
		
		// HTTP 요청 URI가 /token으로 시작되면 beanName에 토큰을 처리하는 API 서비스 클래스 중에서 하나를 선택한다.
		if (serviceUri.startsWith("/tokens")) {
			String httpMethod = requestMap.get("REQUEST_METHOD");
			
			switch (httpMethod) {
				case "POST":
					beanName = "tokenIssue";
					break;
				case "DELETE":
					beanName = "tokenExpire";
					break;
				case "GET":
					beanName = "tokenVerify";
					break;
				
				default:
					beanName = "notFound";
					break;
			}
		} else if (serviceUri.startsWith("/users")) {
			beanName = "users";
		} else {
			beanName = "notFound";
		}
		
		ApiRequest service = null;
		try {
			service = (ApiRequest) springContext.getBean(beanName, requestMap);
		} catch (Exception e) {
			e.printStackTrace();
			service = (ApiRequest) springContext.getBean("notFound", requestMap);
		}
		
		return service;
	}
}
