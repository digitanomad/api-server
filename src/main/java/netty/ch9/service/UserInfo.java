package netty.ch9.service;

import java.util.Map;

import netty.ch9.core.ApiRequestTemplate;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("users")
// 스프링의 Scope 어노테이션은 스프링 컨텍스트가 객체를 생성할 때 싱글톤으로 생성할 것인지 아니면 객체를 요청할 때마다 새로 생성할 것인지를 설정한다.
// 여기에 설정된 prototype 값은 요청할 때마다 새로 생성한다는 의미며 이 어노테이션을 지정하지 않으면 싱글톤으로 생성된다.
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
			throw new RequestParamException("email이 없습니다.");
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
