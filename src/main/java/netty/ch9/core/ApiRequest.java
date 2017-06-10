package netty.ch9.core;

import com.google.gson.JsonObject;

import netty.ch9.service.RequestParamException;
import netty.ch9.service.ServiceException;

public interface ApiRequest {
	// API를 호출하는 HTTP 요청의 파라미터 값이 입력되었는지 검증하는 메소드
	public void requestParamValidation() throws RequestParamException;
	
	// 각 API 서비스에 따른 개별 구현 메소드
	public void service() throws ServiceException;
	
	// 서비스 API의 호출시작 메소드
	public void executeService();
	
	// API 서비스의 처리결과를 조회하는 메소드
	public JsonObject getApiResult();
}
