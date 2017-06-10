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
	
	// HTTP ��û���� ������ �ʵ��� �̸��� ���� API ���� Ŭ������ �����ڷ� �����Ѵ�.
	public ApiRequestTemplate(Map<String, String> reqData) {
		this.logger = LogManager.getLogger(this.getClass());
		this.apiResult = new JsonObject();
		this.reqData = reqData;
		
		logger.info("request data : " + this.reqData);
	}
	
	public void executeService() {
		try {
			// API ���� Ŭ������ �μ��� �Էµ� HTTP ��û ���� ���ռ��� �����Ѵ�.
			this.requestParamValidation();
			
			// �� API ���� Ŭ������ ������ ����� �����ؾ� �Ѵ�.
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
