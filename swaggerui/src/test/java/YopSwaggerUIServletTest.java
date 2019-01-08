import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.yop.rest.servlet.RestServletTest;
import org.yop.rest.servlet.YopRestServlet;
import org.yop.rest.simple.model.SimpleModelRestTest;
import org.yop.swaggerui.servlet.YopSwaggerUIServlet;

import java.io.IOException;

public class YopSwaggerUIServletTest extends RestServletTest {

	private static final String KEEP_RUNNING_FLAG = "yop.swaggerui.rest.test.keep_running";

	@Override
	protected String getPackageNames() {
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

	@Override
	protected void addLoginServlet(Context context) {}

	@Override
	protected void addYopServlet(Context context) {
		Wrapper wrapper = Tomcat.addServlet(
			context,
			YopRestServletWithConnection.class.getSimpleName(),
			new YopRestServletWithConnection()
		);
		wrapper.addInitParameter(YopRestServlet.PACKAGE_INIT_PARAM, "org.yop");
		context.addServletMappingDecoded("/yop/rest/*", YopRestServletWithConnection.class.getSimpleName());
	}

	@Test
	public void run() throws InterruptedException, IOException {
		if ("true".equals(System.getProperty(KEEP_RUNNING_FLAG))) {
			// This keeps the tomcat server running, e.g. if you want to use the browser
			while (true) {
				Thread.sleep(1000);
				if (false) break;
			}
		}

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet("http://localhost:1234/yop-swagger");
			SimpleModelRestTest.Response response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertTrue(StringUtils.contains(response.content, "url: \"http://localhost:1234/yop/openapi\""));
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
		rootContext.addServletMappingDecoded("/yop-swagger/*", servletName);
	}
}