package netty.ch9.service;

import java.util.Map;

import netty.ch9.core.ApiRequestTemplate;
import netty.ch9.core.JedisHelper;
import netty.ch9.core.KeyMaker;
import netty.ch9.service.dao.TokenKey;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.gson.JsonObject;

import redis.clients.jedis.Jedis;

@Service("tokenIssue")
@Scope("prototype")
public class TokenIssue extends ApiRequestTemplate {
	private static final JedisHelper helper = JedisHelper.getInstance();
	
	@Autowired
	private SqlSession sqlSession;
	
	public TokenIssue(Map<String, String> reqData) {
		super(reqData);
	}

	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(reqData.get("userNo"))) {
			throw new RequestParamException("userNo가 없습니다.");
		}
		
		if (StringUtils.isEmpty(reqData.get("password"))) {
			throw new RequestParamException("password가 없습니다.");
		}
	}

	@Override
	public void service() throws ServiceException {
		Jedis jedis = null;
		try {
			Map<String, Object> result = sqlSession.selectOne("users.userInfoByPassword", reqData);
			
			if (result != null) {
				final int threeHour = 60 * 60 * 3;
				long issueDate = System.currentTimeMillis() / 1000;
				String email = String.valueOf(result.get("USERID"));
				
				JsonObject token = new JsonObject();
				token.addProperty("issueDate", issueDate);
				token.addProperty("expireDate", issueDate + threeHour);
				token.addProperty("email", email);
				token.addProperty("userNo", reqData.get("userNo"));
				
				// 발급된 토큰을 레디스에 저장하고 조회
				KeyMaker tokenKey = new TokenKey(email, issueDate);
				jedis = helper.getConnection();
				// setex 명령은 지정된 사긴 이후에 데이터를 자동으로 삭제하는 명령이다.
				jedis.setex(tokenKey.getKey(), threeHour, token.toString());
				
				apiResult.addProperty("resultCode", "200");
				apiResult.addProperty("message", "success");
				apiResult.addProperty("token", tokenKey.getKey());
			} else {
				// 데이터 없음
				apiResult.addProperty("resultCode", "404");
			}
			
			helper.returnResource(jedis);
		} catch (Exception e) {
			helper.returnResource(jedis);
		}

	}

	
	
	
	
	
	
	
}
