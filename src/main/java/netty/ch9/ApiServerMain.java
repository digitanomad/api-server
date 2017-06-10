package netty.ch9;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class ApiServerMain {
	public static void main(String[] args) {
		AbstractApplicationContext springContext = null;
		try {
			springContext = new AnnotationConfigApplicationContext(ApiServerConfig.class);
			springContext.registerShutdownHook();
			
		} finally {
			springContext.close();
		}
	}
}
