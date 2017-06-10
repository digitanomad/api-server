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
	
	// ����ڰ� ������ HTTP ��û�� ������ ������ ���ڴ��� ��� ������ ����Ѵ�.
	private HttpPostRequestDecoder decoder;
	// ����ڰ� ������ HTTP ��û�� �Ķ���͸� ���� ó�� Ŭ������ �����Ϸ��� �� ��ü�� ��� ������ ����Ѵ�.
	private Map<String, String> reqData = new HashMap<String, String>();
	// Ŭ���̾�Ʈ�� ������ HTTP ��� �߿��� ����� ����� �̸� ����� �� ��ü�� �����ϰ� HTTP ��û �������� ��� ������ ������ �� �� ��� ������ ���Ե� �ʵ常 ����Ѵ�.
	private static final Set<String> usingHeader = new HashSet<String>();
	static {
		usingHeader.add("token");
		usingHeader.add("email");
	}
	
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg)
			throws Exception {
		// Request header ó��
		if (msg instanceof HttpRequest) {
			this.request = (HttpRequest) msg;
			
			if (HttpHeaders.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}
			
			// HTTP ��û�� ��� ������ �����Ѵ�.
			HttpHeaders headers = request.headers();
			if (!headers.isEmpty()) {
				for (Map.Entry<String, String> h : headers) {
					String key = h.getKey();
					// ������ ��� ���� �߿��� usingHeader�� ������ ���� �����Ѵ�.
					if (usingHeader.contains(key)) {
						reqData.put(key, h.getValue());
					}
				}
			}
			
			reqData.put("REQUEST_URI", request.getUri());
			reqData.put("REQUEST_METHOD", request.getMethod().name());
		}
		
		if (msg instanceof HttpContent) {
			// HttpContent�� ���� �������̽��� LastHttpContent�� ��� HTTP �޽����� ���ڵ��Ǿ��� HTTP ���������� ������ ���������� �˸��� �������̽���.
			if (msg instanceof LastHttpContent) {
				logger.debug("LastHttpContent message received. " + request.getUri());
				
				LastHttpContent trailer = (LastHttpContent) msg;
				
				// HTTP �������� HTTP Post �����͸� �����Ѵ�.
				readPostData();
				
				// HTTP �������ݿ��� �ʿ��� �������� ������ �Ϸ�Ǹ� reqData ���� ServiceDispatcher Ŭ������ dispatch �޼ҵ带 ȣ���Ͽ� HTTP ��û�� �´� API ���� Ŭ������ �����Ѵ�.
				ApiRequest service = ServiceDispatcher.dispatch(reqData);
				
				try {
					service.executeService();
					
					apiResult = service.getApiResult();
				} finally {
					reqData.clear();
				}
				
				// apiResult ��� ������ ����� API ó�� ����� Ŭ���̾�Ʈ ä���� �۽� ���ۿ� ����Ѵ�.
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
		logger.info("��û ó�� �Ϸ�");
		ctx.flush();
	}
	
	
	private void readPostData() {
		try {
			// HttpRequest ��ü�� ���Ե� HTTP ���� �߿��� POST �޼ҵ�� ���ŵ� �����͸� �����ϱ� ���� ���ڴ��� �����Ѵ�.
			decoder = new HttpPostRequestDecoder(factory, request);
			for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
				if (HttpDataType.Attribute == data.getHttpDataType()) {
					try {
						// ���ڴ��� ���ؼ� ����� ������ ����� Attribute ��ü�� ĳ�����Ѵ�.
						Attribute attribute = (Attribute) data;
						// Attribute�� �̸��� ���� reqData �ʿ� �����Ѵ�.
						// Ŭ���̾�Ʈ�� HTML�� FORM ������Ʈ�� ����Ͽ� ������ �����͸� �����Ѵ�. ASP �Ǵ� JSP�� request.QueryString �޼ҵ�� ������ ������ �����Ѵ�.
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
		// ������ �����ų�� ���� ����
		boolean keepAlive = HttpHeaders.isKeepAlive(request);
		// ���� ��ü ����
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, 
				currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(apiResult.toString(), CharsetUtil.UTF_8));
		
		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		
		if (keepAlive) {
			// keep-alive ������ ��� 'Content-Length'�� ����� �߰���Ŵ
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
