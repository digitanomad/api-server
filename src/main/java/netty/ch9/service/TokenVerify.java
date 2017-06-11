package netty.ch9.service;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import redis.clients.jedis.Jedis;
import netty.ch9.core.ApiRequestTemplate;
import netty.ch9.core.JedisHelper;

@Service("tokenVerify")
@Scope("prototype")
public class TokenVerify extends ApiRequestTemplate {
	private static final JedisHelper helper = JedisHelper.getInstance();

	public TokenVerify(Map<String, String> reqData) {
		super(reqData);
	}

	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(reqData.get("token"))) {
			throw new RequestParamException("token이 없습니다.");
		}

	}

	@Override
	public void service() throws ServiceException {
		Jedis jedis = null;
		try {
			jedis = helper.getConnection();
			String tokenString = jedis.get(reqData.get("token"));
			
			if (tokenString == null) {
				apiResult.addProperty("resultCode", "404");
				apiResult.addProperty("message", "Fail");
			} else {
				Gson gson = new Gson();
				JsonObject token = gson.fromJson(tokenString, JsonObject.class);
				
				apiResult.addProperty("resultCode", "200");
				apiResult.addProperty("message", "Success");
				apiResult.add("issueDate", token.get("issueDate"));
				apiResult.add("issueDate", token.get("email"));
				apiResult.add("issueDate", token.get("userNo"));
			}
			
		} catch (Exception e) {
			helper.returnResource(jedis);
		}

	}

}
