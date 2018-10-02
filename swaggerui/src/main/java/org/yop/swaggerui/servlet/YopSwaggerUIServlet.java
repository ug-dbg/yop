package org.yop.swaggerui.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * A very basic file servlet to expose the SwaggerUI static content, configured for YOP REST OpenAPI.
 * <br>
 * Please configure {@link #yopOpenAPIURL} using servlet init param {@link #YOP_OPENAPI_URL_INIT_PARAM}.
 */
public class YopSwaggerUIServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(YopSwaggerUIServlet.class);

	public static final String YOP_OPENAPI_URL_INIT_PARAM = "yop_openapi_url";

	private String yopOpenAPIURL;

	public YopSwaggerUIServlet() {}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.yopOpenAPIURL = this.getInitParameter(YOP_OPENAPI_URL_INIT_PARAM);
		logger.debug("Init YopSwaggerUIServlet for Open API URL [{}]", this.yopOpenAPIURL);
	}

	/**
	 * Serve the resource for the given path, if exists. Returns a 404 else.
	 * <br>
	 * If the file is the swagger-ui index page, replace the configured url with {@link #yopOpenAPIURL}.
	 * @param req  the input HTTP request
	 * @param resp the output HTTP response
	 * @throws IOException could not read file or could not write content in output stream
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = StringUtils.removeStart(req.getRequestURI(), req.getContextPath());
		path = StringUtils.removeStart(path, req.getServletPath());

		if (StringUtils.equalsAny(path, "", "/")) {
			logger.debug("Redirect to index.html");
			resp.sendRedirect(Paths.get(req.getServletPath(), "index.html").toString());
			return;
		}

		path = Paths.get("/", "swagger-ui", path).toString();
		InputStream classPathStream = this.getClass().getResourceAsStream(path);

		if (classPathStream == null) {
			logger.info("No YOP swagger-ui resource for [{}]", path);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String html = IOUtils.toString(classPathStream, StandardCharsets.UTF_8);
		if (html != null) {
			if (path.matches(".*/swagger-ui/index.html")) {
				html = html.replaceAll("url: \"https://.*.json\"", "url: \"" + this.yopOpenAPIURL + "\"");
				logger.debug("YOP swagger-ui Open API config set into swagger-ui index.html");
			}
			resp.getWriter().write(html);
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			logger.info("No YOP swagger-ui resource content for [{}]", path);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}