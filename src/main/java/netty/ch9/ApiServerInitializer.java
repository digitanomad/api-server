package netty.ch9;

import netty.ch9.core.ApiRequestParser;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class ApiServerInitializer extends ChannelInitializer<SocketChannel> {

	private final SslContext sslCtx;

	public ApiServerInitializer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		if (sslCtx != null) {
			p.addLast(sslCtx.newHandler(ch.alloc()));
		}
		
		// HttpRequestDecoder�� HTTP ��û�� ó���ϴ� ���ڴ���. �� Ŭ���̾�Ʈ�� ������ HTTP ���������� ��Ƽ�� ����Ʈ ���۷� ��ȯ�ϴ� �۾��� �����Ѵ�.
		p.addLast(new HttpRequestDecoder());
		// HttpObjectAggregator�� HTTP �������ݿ��� �߻��ϴ� �޽��� ����ȭ�� ó���ϴ� ���ڴ���.
		// HTTP ���������� �����ϴ� �����Ͱ� ����� ���ŵǾ��� �� �����͸� �ϳ��� �����ִ� ������ �����Ѵ�.
		// ���ڷ� �Էµ� 65536�� �Ѳ����� ó�� ������ �ִ� ������ ũ���. 65Kbyte �̻��� �����Ͱ� �ϳ��� HTTP ��û���� ���ŵǸ� TooLongFrameException ���ܰ� �߻��Ѵ�.
		p.addLast(new HttpObjectAggregator(65536));
		// HttpResponseEncoder�� ���ŵ� HTTP ��û�� ó������� Ŭ���̾�Ʈ�� ������ �� HTTP �������ݷ� ��ȯ���ִ� ���ڴ���.
		p.addLast(new HttpResponseEncoder());
		// HttpContentCompressor�� HTTP �������ݷ� �ۼ��ŵǴ� HTTP�� ���� �����͸� gzip ���� �˰����� ����Ͽ� ����� ���� ������ �����Ѵ�.
		// ����, ChannelDuplexHandler Ŭ������ ��ӹޱ� ������ �ιٿ��� �ƿ��ٿ�忡�� ��� ȣ��ȴ�.
		p.addLast(new HttpContentCompressor());
		p.addLast(new ApiRequestParser());
	}

}
