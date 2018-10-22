package org.yop.example;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.gen.Prepare;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.servlet.OpenAPIServlet;
import org.yop.rest.servlet.YopRestServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

/**
 * A proxy for Yop REST requirements.
 * <ul>
 *     <li>rest → {@link YopRestServlet}</li>
 *     <li>openapi → {@link OpenAPIServlet}</li>
 *     <li>swagger → {@link org.yop.swaggerui.servlet.YopSwaggerUIServlet}</li>
 * </ul>
 * <br>
 * Neither {@link OpenAPIServlet} nor {@link YopRestServlet} are actually exposed.
 * They are initialized, tweaked and this proxy explicitly calls
 * {@link javax.servlet.http.HttpServlet#service(HttpServletRequest, HttpServletResponse)}.
 * <br>
 * <b>N.B.</b><br>
 * It was easier to copy the behavior of
 * {@link org.yop.swaggerui.servlet.YopSwaggerUIServlet#doGet(HttpServletRequest, HttpServletResponse)}
 * so it can be adapted.
 */
public class RestServletProxy {

	private static final Logger logger = LoggerFactory.getLogger(RestServletProxy.class);

	/** 1 proxy per UID → 1 DB */
	private File db;

	/** REST proxy */
	private YopRestServlet yopRestServlet = new YopRestServlet().withConnector(this::getConnection);

	/** OpenAPI proxy */
	private OpenAPIServlet openAPIServlet = new OpenAPIServlet();

	/** A class loader that includes the user code compiled class */
	private ClassLoader fakeRootClassLoader;

	/**
	 * Init the proxies for REST and open API.
	 * <ul>
	 *     <li>init {@link #fakeRootClassLoader}</li>
	 *     <li>Prepare SQLite database using {@link #fakeRootClassLoader} so the user code can be found</li>
	 *     <li>
	 *         init fake REST and OpenAPI servlets. <br>
	 *         The REST path must be faked :
	 *         we do not receive REST request on the path the user configured in his code. <br>
	 *         That's because we are behind a preliminary servlet : see {@link YopDemoServlet}
	 *     </li>
	 * </ul>
	 * @param fakeRoot      the user code compile directory root
	 * @param servletConfig the demo servlet config
	 * @param packageName   the user code package
	 */
	public void init(Path fakeRoot, ServletConfig servletConfig, String packageName) {
		try {
			List<URL> newCP = new ArrayList<>();
			newCP.add(fakeRoot.toUri().toURL());
			this.fakeRootClassLoader = URLClassLoader.newInstance(
				newCP.toArray(new URL[0]),
				this.getClass().getClassLoader()
			);

			String dbName = fakeRoot.getName(fakeRoot.getNameCount() - 1).toString();
			this.db = Prepare.createSQLiteDatabase(dbName, this.fakeRootClassLoader, packageName);

			Map<String, Class> yopables = this.initYopRestServlet();
			this.initOpenAPIServlet(servletConfig, yopables);

			// FIXME : this is a silly workaround to map Rest resources to a fake 'rest' context
			String fakePath = Paths
				.get("/")
				.relativize(Paths.get(servletConfig.getServletContext().getContextPath(), "rest"))
				.toString();
			Set<String> paths = new HashSet<>(yopables.keySet());
			paths.forEach(path -> yopables.put(Paths.get(fakePath, path).toString(), yopables.get(path)));
		} catch (IOException | SQLException | ClassNotFoundException | ServletException | RuntimeException e) {
			throw new RuntimeException("Could not init fake REST servlet for path [" + fakeRoot.toString() + "]", e);
		}
	}

	/**
	 * Do serve a REST servlet request : {@link YopRestServlet#service(HttpServletRequest, HttpServletResponse)}.
	 * @param request  the HTTP request
	 * @param response the HTTP response
	 */
	public void rest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.yopRestServlet.service(request, response);
	}

	/**
	 * Do serve an OpenAPI servlet request : {@link OpenAPIServlet#service(HttpServletRequest, HttpServletResponse)}.
	 * @param request  the HTTP request
	 * @param response the HTTP response
	 */
	public void openAPI(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.openAPIServlet.service(request, response);
	}

	/**
	 * Serve a Swagger resource. This code mostly comes from {@link org.yop.swaggerui.servlet.YopSwaggerUIServlet}.
	 * @param req  the HTTP request
	 * @param resp the HTTP response
	 * @throws IOException an I/O exception occurred reading/writing the resource
	 */
	public void swagger(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = StringUtils.removeStart(req.getRequestURI(), req.getContextPath());
		path = StringUtils.removeStart(path, req.getServletPath());

		if (StringUtils.equalsAny(path, "", "/swagger")) {
			logger.debug("Redirect to index.html");
			resp.sendRedirect(Paths.get(req.getContextPath(), req.getServletPath(), path, "index.html").toString());
			return;
		}

		path = StringUtils.removeFirst(path, "/swagger/");
		InputStream classPathStream = this.getClass().getResourceAsStream(
			Paths.get("/META-INF/resources/webjars/swagger-ui/3.18.2", path).toString()
		);

		if (classPathStream == null) {
			logger.info("No YOP swagger-ui resource for [{}]", path);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String html = IOUtils.toString(classPathStream, StandardCharsets.UTF_8);
		String openAPIURL = StringUtils.removeEnd(req.getRequestURL().toString(), "/swagger/index.html") + "/openapi";
		if (html != null) {
			if (StringUtils.equalsAny(path, "/index.html", "index.html")) {
				html = html.replaceAll("url: \"https://.*.json\"", "url: \"" + openAPIURL + "\"");
				logger.debug("YOP swagger-ui Open API config set into swagger-ui index.html");
			}
			resp.getWriter().write(html);
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			logger.info("No YOP swagger-ui resource content for [{}]", path);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Read the yopable paths map from a servlet (either {@link YopRestServlet} or {@link OpenAPIServlet}.
	 * @param clazz the servlet type
	 * @param onto  the servlet instance
	 * @return the yopable paths map
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Class> readYopablePaths(Class clazz, Object onto) {
		return (Map) Reflection.readField(Reflection.get(clazz, "yopablePaths"), onto);
	}

	/**
	 * Init {@link #yopRestServlet}.
	 * Its yopable path ({@link YopRestServlet#yopablePaths} are faked so it can run in the demo.
	 * @return a path → Yopable map
	 */
	private Map<String, Class> initYopRestServlet() {
		Map<String, Class> yopables = this.readYopablePaths(YopRestServlet.class, this.yopRestServlet);

		Collection<Class<? extends Yopable>> classes = ORMUtil.yopables(this.fakeRootClassLoader);

		for (Class yopableClass : classes) {
			if (Yopable.class.isAssignableFrom(yopableClass) && yopableClass.isAnnotationPresent(Rest.class))
			yopables.put(
				StringUtils.removeStart(((Rest)yopableClass.getAnnotation(Rest.class)).path(), "/"),
				yopableClass
			);
		}
		return yopables;
	}

	/**
	 * Init the OpenAPI servlet.
	 * @param servletConfig the demo servlet config
	 * @param yopables      a path → Yopable map
	 * @throws ServletException exception occurred when initializing {@link #openAPIServlet}
	 */
	private void initOpenAPIServlet(ServletConfig servletConfig, Map<String, Class> yopables) throws ServletException {
		this.openAPIServlet.init(servletConfig);
		this.openAPIServlet.setExpositionPath("rest");
		this.readYopablePaths(OpenAPIServlet.class, this.openAPIServlet).putAll(yopables);
	}

	/**
	 * Get the connection to user session's SQLite database.
	 * @return {@link Prepare#getConnection(File)} for {@link #db}
	 */
	private IConnection getConnection() {
		try {
			return Prepare.getConnection(this.db);
		} catch (ClassNotFoundException | SQLException | RuntimeException e) {
			throw new RuntimeException("Could not open connection to SQLite db [" + this.db + "]", e);
		}
	}
}
