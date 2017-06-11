package netty.ch9.service;

import java.util.Map;

import netty.ch9.core.ApiRequestTemplate;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("users")
// �������� Scope ������̼��� ������ ���ؽ�Ʈ�� ��ü�� ������ �� �̱������� ������ ������ �ƴϸ� ��ü�� ��û�� ������ ���� ������ �������� �����Ѵ�.
// ���⿡ ������ prototype ���� ��û�� ������ ���� �����Ѵٴ� �ǹ̸� �� ������̼��� �������� ������ �̱������� �����ȴ�.
@Scope("prototype")
public class UserInfo extends ApiRequestTemplate {
	@Autowired
	private SqlSession sqlSession;
	
	public UserInfo(Map<String, String> reqData) {
		super(reqData);
	}

	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(this.reqData.get("email"))) {
			throw new RequestParamException("email�� �����ϴ�.");
		}
	}

	@Override
	public void service() throws ServiceException {
		Map<String, Object> result = sqlSession.selectOne("users.userInfoByEmail", this.reqData);
		
		if (result != null) {
			String userNo = String.valueOf(result.get("USERNO"));
			
			apiResult.addProperty("resultCode", "200");
			apiResult.addProperty("message", "Success");
			apiResult.addProperty("userNo", userNo);
		} else {
			apiResult.addProperty("resultCode", "404");
			apiResult.addProperty("message", "Fail");
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
