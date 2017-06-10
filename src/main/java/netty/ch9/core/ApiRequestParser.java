package netty.ch9.core;


import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {

	private static final Logger logger = LogManager.getLogger(ApiRequestParser.class);
	
	private HttpRequest request;
	private JsonObject apiResult;
	
	private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
	
	// 사용자가 전송한 HTTP 요청의 본문을 추출할 디코더를 멤버 변수로 등록한다.
	private HttpPostRequestDecoder decoder;
	// 사용자가 전송한 HTTP 요청의 파라미터를 업무 처리 클래스로 전달하려면 맵 객체를 멤버 변수로 등록한다.
	private Map<String, String> reqData = new HashMap<String, String>();
	// 클라이언트가 전송한 HTTP 헤더 중에서 사용할 헤더의 이름 목록을 새 객체에 저장하고 HTTP 요청 데이터의 헤더 정보를 추출할 때 이 멤버 변수에 포함된 필드만 사용한다.
	private static final Set<String> usingHeader = new HashSet<String>();
	static {
		usingHeader.add("token");
		usingHeader.add("email");
	}
	
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg)
			throws Exception {
		// Request header 처리
		if (msg instanceof HttpRequest) {
			this.request = (HttpRequest) msg;
			
			if (HttpHeaders.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}
			
			// HTTP 요청의 헤더 정보를 추출한다.
			HttpHeaders headers = request.headers();
			if (!headers.isEmpty()) {
				for (Map.Entry<String, String> h : headers) {
					String key = h.getKey();
					// 추출한 헤더 정보 중에서 usingHeader에 지정된 값만 추출한다.
					if (usingHeader.contains(key)) {
						reqData.put(key, h.getValue());
					}
				}
			}
			
			reqData.put("REQUEST_URI", request.getUri());
			reqData.put("REQUEST_METHOD", request.getMethod().name());
		}
		
		if (msg instanceof HttpContent) {
			// HttpContent의 상위 인터페이스인 LastHttpContent는 모든 HTTP 메시지가 디코딩되었고 HTTP 프로토콜의 마지막 데이터임을 알리는 인터페이스다.
			if (msg instanceof LastHttpContent) {
				logger.debug("LastHttpContent message received. " + request.getUri());
				
				LastHttpContent trailer = (LastHttpContent) msg;
				
				// HTTP 본문에서 HTTP Post 데이터를 추출한다.
				readPostData();
				
				// HTTP 프로토콜에서 필요한 데이터의 추출이 완료되면 reqData 맵을 ServiceDispatcher 클래스의 dispatch 메소드를 호출하여 HTTP 요청에 맞는 API 서비스 클래스를 생성한다.
				ApiRequest service = ServiceDispatcher.dispatch(reqData);
				
				try {
					service.executeService();
					
					apiResult = service.getApiResult();
				} finally {
					reqData.clear();
				}
				
				// apiResult 멤버 변수에 저장된 API 처리 결과를 클라이언트 채널의 송신 버퍼에 기록한다.
				if (!writeResponse(trailer, ctx)) {
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
					   .addListener(ChannelFutureListener.CLOSE);
				}
				reset();
			}
		}
	}
	
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("요청 처리 완료");
		ctx.flush();
	}
	
	
	private void readPostData() {
		try {
			// HttpRequest 객체에 포함된 HTTP 본문 중에서 POST 메소드로 수신된 데이터를 추출하기 위한 디코더를 생성한다.
			decoder = new HttpPostRequestDecoder(factory, request);
			for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
				if (HttpDataType.Attribute == data.getHttpDataType()) {
					try {
						// 디코더를 통해서 추출된 데이터 목록을 Attribute 객체로 캐스팅한다.
						Attribute attribute = (Attribute) data;
						// Attribute의 이름과 값을 reqData 맵에 저장한다.
						// 클라이언트가 HTML의 FORM 엘리먼트를 사용하여 전송한 데이터를 추출한다. ASP 또는 JSP의 request.QueryString 메소드와 동일한 동작을 수행한다.
						reqData.put(attribute.getName(), attribute.getValue());
					} catch (IOException e) {
						logger.error("BODY Attribute: " + data.getHttpDataType().name(), e);
						return;
					}
				} else {
					logger.info("BODY data : " + data.getHttpDataType().name() + ": " + data);
				}
			}
		} catch (ErrorDataDecoderException e) {
			logger.error(e);
		} finally {
			if (decoder != null) {
				decoder.destroy();
			}
		}
	}
	
	
	private void reset() {
		request = null;
	}
	
	
	private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
		// 연결을 종료시킬지 말지 정함
		boolean keepAlive = HttpHeaders.isKeepAlive(request);
		// 응답 객체 생성
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, 
				currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(apiResult.toString(), CharsetUtil.UTF_8));
		
		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		
		if (keepAlive) {
			// keep-alive 상태일 경우 'Content-Length'를 헤더에 추가시킴
			response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);
		
		return keepAlive;
	}
	
	
	public static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
		ctx.write(response);
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.error(cause);
		ctx.close();
	}

}
