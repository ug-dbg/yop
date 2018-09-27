package org.yop.swaggerui.servlet;

import org.apache.commons.io.FileUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * A very basic file servlet to expose the SwaggerUI static content, configured for YOP REST OpenAPI.
 * <br>
 * Please configure {@link #yopOpenAPIURL} using servlet init param {@link #YOP_OPENAPI_URL_INIT_PARAM}.
 */
public class YopSwaggerUIServlet extends HttpServlet {

	public static final String YOP_OPENAPI_URL_INIT_PARAM = "yop_openapi_url";

	private String yopOpenAPIURL;

	public YopSwaggerUIServlet() {}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.yopOpenAPIURL = this.getInitParameter(YOP_OPENAPI_URL_INIT_PARAM);
	}

	/**
	 * Serve the file for the given path, if exists. Returns a 404 else.
	 * <br>
	 * If the file is the swagger-ui index page, replace the configured url with {@link #yopOpenAPIURL}.
	 * @param req  the input HTTP request
	 * @param resp the output HTTP response
	 * @throws IOException could not read file or could not write content in output stream
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		File target = new File(this.getServletContext().getRealPath(req.getServletPath()));
		if (target.exists()) {
			String html = FileUtils.readFileToString(target);

			String path = req.getServletPath();
			if (path.matches(".*/swagger-ui/index.html")) {
				html = html.replaceAll("url: \"https://.*.json\"", "url: \"" + this.yopOpenAPIURL + "\"");
			}
			resp.getWriter().write(html);
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}