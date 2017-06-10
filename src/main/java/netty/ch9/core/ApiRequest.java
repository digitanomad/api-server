package netty.ch9.core;

import com.google.gson.JsonObject;

import netty.ch9.service.RequestParamException;
import netty.ch9.service.ServiceException;

public interface ApiRequest {
	// API�� ȣ���ϴ� HTTP ��û�� �Ķ���� ���� �ԷµǾ����� �����ϴ� �޼ҵ�
	public void requestParamValidation() throws RequestParamException;
	
	// �� API ���񽺿� ���� ���� ���� �޼ҵ�
	public void service() throws ServiceException;
	
	// ���� API�� ȣ����� �޼ҵ�
	public void executeService();
	
	// API ������ ó������� ��ȸ�ϴ� �޼ҵ�
	public JsonObject getApiResult();
}
