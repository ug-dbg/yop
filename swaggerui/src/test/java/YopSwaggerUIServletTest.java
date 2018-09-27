import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.junit.Test;
import org.yop.rest.servlet.RestServletTest;
import org.yop.swaggerui.servlet.YopSwaggerUIServlet;

public class YopSwaggerUIServletTest extends RestServletTest {

	private static final String KEEP_RUNNING_FLAG = "yop.swaggerui.rest.test.keep_running";

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.rest.users.model,org.yop.rest.simple.model";
	}

	@Override
	protected void onContextCreation(Context context) {
		String openAPIURL =
			"http://"
			+ this.tomcat.getServer().getAddress()
			+ ":"
			+ this.tomcat.getConnector().getPort()
			+ "/yop/openapi";
		createYopSwaggerUIServlet(context, openAPIURL);
		super.onContextCreation(context);
	}

	@Test
	public void run() throws InterruptedException {
		if (! "true".equals(System.getProperty(KEEP_RUNNING_FLAG))) {
			return;
		}

		// This keeps the tomcat server running, e.g. if you want to use the browser
		while (true) {
			Thread.sleep(1000);
			if (false) break;
		}
	}

	private static void createYopSwaggerUIServlet(Context rootContext, String openAPIURL) {
		Wrapper defaultServlet = rootContext.createWrapper();
		String servletName = "yop_swaggerui_servlet";
		defaultServlet.setName(servletName);
		defaultServlet.addInitParameter(YopSwaggerUIServlet.YOP_OPENAPI_URL_INIT_PARAM, openAPIURL);
		defaultServlet.setServletClass(YopSwaggerUIServlet.class.getName());
		defaultServlet.setLoadOnStartup(1);
		rootContext.addChild(defaultServlet);
		rootContext.addServletMappingDecoded("/", servletName);
	}
}