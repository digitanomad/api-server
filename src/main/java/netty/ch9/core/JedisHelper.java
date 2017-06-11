package netty.ch9.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisHelper {
	protected static final String REDIS_HOST = "127.0.0.1";
	protected static final int REDIS_PORT = 6379;
	private final Set<Jedis> connectionList = new HashSet<Jedis>();
	private final JedisPool pool;
	
	// JedisHelper 클래스는 싱글톤으로 작성되었으므로 외부에서 생성자를 호출할 수 없도록 private 접근 지정자를 사용했다.
	private JedisHelper() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(20);
		config.setBlockWhenExhausted(true);
		
		pool = new JedisPool(config, REDIS_HOST, REDIS_PORT);
	}
	
	private static class LazyHolder {
		private static final JedisHelper INSTANCE = new JedisHelper();
	}
	
	public static JedisHelper getInstance() {
		return LazyHolder.INSTANCE;
	}
	
	final public Jedis getConnection() {
		Jedis jedis = pool.getResource();
		connectionList.add(jedis);
		
		return jedis;
	}
	
	final public void returnResource(Jedis jedis) {
		jedis.close();
	}
	
	final public void destoryPool() {
		Iterator<Jedis> jedisList = connectionList.iterator();
		while (jedisList.hasNext()) {
			Jedis jedis = jedisList.next();
			jedis.close();
		}
		
		pool.destroy();
	}

}
