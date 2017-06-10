package netty.ch9.core;

import java.util.Map;

import netty.ch9.service.RequestParamException;
import netty.ch9.service.ServiceException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public abstract class ApiRequestTemplate implements ApiRequest {
	protected Logger logger;
	
	protected Map<String, String> reqData;
	
	protected JsonObject apiResult;
	
	// HTTP 요청에서 추출한 필드의 이름과 값을 API 서비스 클래스의 생성자로 전달한다.
	public ApiRequestTemplate(Map<String, String> reqData) {
		this.logger = LogManager.getLogger(this.getClass());
		this.apiResult = new JsonObject();
		this.reqData = reqData;
		
		logger.info("request data : " + this.reqData);
	}
	
	public void executeService() {
		try {
			// API 서비스 클리스의 인수로 입력된 HTTP 요청 맵의 정합성을 검증한다.
			this.requestParamValidation();
			
			// 각 API 서비스 클래스가 제공할 기능을 구현해야 한다.
			this.service();
		} catch (RequestParamException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "405");
		} catch (ServiceException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "501");
		}
	}
	
	public JsonObject getApiResult() {
		return this.apiResult;
	}
}
