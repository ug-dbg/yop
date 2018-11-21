package org.yop.rest.servlet;

import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.rest.openapi.OpenAPIUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A very rough Servlet that exposes an OpenAPI YAML API description from the @Rest annotated Yopable types.
 * <br>
 * Parameters :
 * <ul>
 *     <li>{@link #PACKAGE_INIT_PARAM} is required to find the @Rest Yopable from the class path</li>
 *     <li>{@link #EXPOSITION_PATH_PARAM} is required to generate the correct server endpoints in the OpenAPI model</li>
 * </ul>
 */
public class OpenAPIServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(OpenAPIServlet.class);

	public static final String PACKAGE_INIT_PARAM    = "packages";
	public static final String EXPOSITION_PATH_PARAM = "exposition_path";

	protected final Yopables yopablePaths = new Yopables();
	protected String expositionPath;

	/**
	 * Set the REST resources exposition path. This will be used to generate the server info.
	 * @param expositionPath the REST exposition path
	 */
	public void setExpositionPath(String expositionPath) {
		this.expositionPath = expositionPath;
	}

	@Override
	public void init() throws ServletException {
		super.init();
		this.yopablePaths.fromPackage(this.getInitParameter(PACKAGE_INIT_PARAM));
		this.expositionPath = this.getInitParameter(EXPOSITION_PATH_PARAM);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		OpenAPI api = this.getAPIDescription();
		api.setServers(new ArrayList<>());

		try {
			URI requestURI = new URI(req.getRequestURL().toString());
			api.getServers().add(this.generateServerInfo(requestURI.getScheme(), requestURI.getAuthority()));
		} catch (URISyntaxException e) {
			logger.warn(
				"Could not read URI from request URL [{}]. API description will be partial.",
				req.getRequestURL(),
				e
			);
		}

		String openAPIYAML = this.getYAMLAPIDescription(api);
		resp.getWriter().write(openAPIYAML);
		resp.getWriter().close();
		resp.setContentLength(openAPIYAML.getBytes().length);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Generate the OpenAPI model from the {@link #yopablePaths} read from {@link #PACKAGE_INIT_PARAM} servlet config.
	 * <br>
	 * Override me if you want to have another OpenAPI model.
	 * @return the OpenAPI description for the YOP REST resources
	 */
	protected OpenAPI getAPIDescription() {
		return OpenAPIUtil.generate(this.yopablePaths.values());
	}

	/**
	 * Serialize the OpenAPI model into a YAML representation.
	 * <br>
	 * Override me if you want to have another OpenAPI serialized model.
	 * @param computed the computed OpenAPI model. See {@link #getAPIDescription()}
	 * @return a YAML String OpenAPI model representation
	 */
	protected String getYAMLAPIDescription(OpenAPI computed) {
		return OpenAPIUtil.toYAML(computed);
	}

	/**
	 * Generate the current server info section of the OpenAPI model from the servlet context.
	 * <br>
	 * Override me if you want to set another server info section.
	 * @param scheme    the protocol scheme (e.g. http, https)
	 * @param authority the server authority (e.g. y-op.org:8080)
	 * @return the server section of the OpenAPI model
	 */
	protected Server generateServerInfo(String scheme, String authority) {
		ServletContext servletContext = this.getServletConfig().getServletContext();
		Server server = new Server();
		server.setDescription("Current server");
		String url = scheme + "://" + Paths.get(authority, servletContext.getContextPath(), this.expositionPath);
		server.setUrl(url);
		return server;
	}
}
