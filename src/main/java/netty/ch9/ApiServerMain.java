package netty.ch9;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class ApiServerMain {
	public static void main(String[] args) throws CertificateException, SSLException {
		AbstractApplicationContext springContext = null;
		try {
			springContext = new AnnotationConfigApplicationContext(ApiServerConfig.class);
			springContext.registerShutdownHook();
			
			ApiServer server = springContext.getBean(ApiServer.class);
			server.start();
			
		} finally {
			springContext.close();
		}
	}
}
