package org.yop.example;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YOP demo servlet. This answers all the HTTP requests from the demo (see index.html).
 * <br>
 * It holds a context for every session where the custom code is compiled and made available for runtime.
 * See {@link #proxiesForUIDs}.
 * <br>
 * Then it can dispatch to the {@link RestServletProxy} associated to the session.
 */
public class YopDemoServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(YopDemoServlet.class);

	private static final String UID = "UID";
	private static final Integer SESSION_TIMEOUT = 10;

	/** Regex pattern to match package name ('package org.yop.test;') */
	private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
		"^(\\s|\\n)*" + Pattern.quote("package") + "(\\s|\\n)+"
		+ "(?<packageName>([\\w.]+))"
		+ "(\\s|\\n)*"
		+ Pattern.quote(";")
	);

	/** Regex pattern to match package name ('public class Book implements Yopable {') */
	private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
		"(?<=\\n|\\A)(?:public\\s)(class)(\\s|\\n)+"
		+ "(?<className>\\w+)"
		+ "(\\s|\\n)+"
		+ Pattern.quote("implements") + "(\\s|\\n)+"
		+ Pattern.quote("Yopable") + "(\\s|\\n)+"
		+ Pattern.quote("{")
	);

	/** UID → proxy */
	private final Map<String, RestServletProxy> proxiesForUIDs = new HashMap<>();

	/** UID → compile/DB path */
	private final Map<String, Path> pathsForUIDs = new HashMap<>();

	/** UID → last request timestamp */
	private final Map<String, LocalDateTime> pingTimeForUIDs = new HashMap<>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	/**
	 * Create/Update session's {@link #UID}. Then :
	 * <ul>
	 *   <li>/rest → dispatch to {@link #doRest(RestServletProxy, HttpServletRequest, HttpServletResponse)}</li>
	 *   <li>else → use super method that will eventually dispatch to doGet and doPost</li>
	 * </ul>
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uid = updateCookie(req, resp);
		this.updatePings(uid);

		if (StringUtils.equals("/rest", req.getServletPath())) {
			RestServletProxy restProxy = this.proxiesForUIDs.get(uid);
			if (restProxy == null) {
				resp.getWriter().write("Session expired. Please re-submit your code.");
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			doRest(restProxy, req, resp);
			return;
		}

		super.service(req, resp);
	}

	/**
	 * Dispatch the GET request.
	 * <ul>
	 *     <li>/default_code → write the default code in the response and return</li>
	 *     <li>/ → redirects to index.html</li>
	 *     <li>/openapi → use {@link #doOpenAPI(RestServletProxy, HttpServletRequest, HttpServletResponse)}</li>
	 *     <li>anything else → use {@link #doSwagger(RestServletProxy, HttpServletRequest, HttpServletResponse)} </li>
	 * </ul>
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String requestURI = req.getRequestURI();
		logger.info("GET [{}]", requestURI);

		if (StringUtils.equals("/default_code", req.getPathInfo())) {
			resp.getWriter().write(IOUtils.toString(this.getClass().getResourceAsStream("/Book.java")));
			resp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		if (req.getPathInfo() == null && StringUtils.isEmpty(req.getServletPath())) {
			resp.sendRedirect(Paths.get(req.getContextPath(), "index.html").toString());
			return;
		}

		String uid = (String) req.getSession().getAttribute(UID);
		RestServletProxy proxy = this.proxiesForUIDs.get(uid);
		if (StringUtils.endsWith(req.getPathInfo(), "/openapi")) {
			doOpenAPI(proxy, req, resp);
			return;
		}
		if (req.getPathInfo() != null) {
			doSwagger(proxy, req, resp);
			return;
		}

		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Initialize the context for the user code.
	 * <ol>
	 *     <li>Create compile directory for the UID if it does not exist</li>
	 *     <li>Write the code</li>
	 *     <li>Compile the code</li>
	 *     <li>If compilation failed : return the compilation error in the response</li>
	 *     <li>Else : init a {@link RestServletProxy} for the current UID and code</li>
	 * </ol>
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String content = IOUtils.toString(req.getInputStream());
			String uid = (String) req.getSession().getAttribute(UID);
			logger.info("POST UID [{}]@[{}] → [{}]", uid, req.getRequestURI(), StringUtils.abbreviate(content, 30));

			// Create/Clean compilation directory and write source code
			if (!this.pathsForUIDs.containsKey(uid)) {
				this.pathsForUIDs.put(uid, Files.createTempDirectory("yop_demo_" + uid));
			}
			Path fakeRootPath = this.pathsForUIDs.get(uid);
			File fakeRootFolder = fakeRootPath.toFile();
			FileUtils.cleanDirectory(fakeRootFolder);
			fakeRootFolder.deleteOnExit();

			String packageName = packageName(content);
			String packagePath = StringUtils.replace(packageName, ".", File.separator);
			Path packageDirectory = Paths.get(fakeRootPath.toString(), packagePath);
			boolean mkdirs = packageDirectory.toFile().mkdirs();
			logger.info("Created packageDirectory structure [{}] : [{}]", packageDirectory.toString(), mkdirs);
			Path javaFile = Files.createFile(Paths.get(packageDirectory.toString(), className(content) + ".java"));

			FileUtils.write(javaFile.toFile(), content);
			logger.info("Java file written to [{}]", javaFile.toString());

			// Compile source code
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(fakeRootFolder));
			String classpath = System.getProperty("java.class.path");
			classpath = classpath + this.webAppClasspath();
			logger.debug("Compilation classpath [{}]", classpath);
			ByteArrayOutputStream compilationOutput = new ByteArrayOutputStream();
			int status = compiler.run(null, null, compilationOutput, "-classpath", classpath, javaFile.toString());

			// Return the compilation error so it can be displayed to the user
			if (status != 0) {
				String errors = new String(compilationOutput.toByteArray(), StandardCharsets.UTF_8);
				IOUtils.write(errors, resp.getOutputStream());
				logger.error("Could not compile java code ! Compilation errors : [{}]", errors);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			// Initialize a REST proxy for every session.
			try {
				this.proxiesForUIDs.put(uid, this.initProxy(fakeRootPath, packageName));
			} catch (RuntimeException e) {
				logger.error("Could not instantiate REST servlet proxy !", e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} catch (IOException | RuntimeException e) {
			logger.error("Error initializing user code context !", e);
			resp.getWriter().write(
				"Error initializing context for your code :-("
				+ "\n\n"
				+ ExceptionUtils.getStackTrace(e)
			);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		resp.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Update the last request timestamp for this UID with now.
	 * <br>
	 * Wipe all the expired sessions data from {@link #pingTimeForUIDs} {@link #pathsForUIDs}, {@link #proxiesForUIDs}.
	 * @param uid the current session UID
	 */
	private void updatePings(String uid) {
		LocalDateTime now = LocalDateTime.now();
		this.pingTimeForUIDs.put(uid, now);
		Set<String> expiredUIDs = new HashSet<>();
		for (Map.Entry<String, LocalDateTime> lastPing : this.pingTimeForUIDs.entrySet()) {
			if (lastPing.getValue().until(now, ChronoUnit.MINUTES) > SESSION_TIMEOUT) {
				expiredUIDs.add(lastPing.getKey());
			}
		}
		for (String expiredUID : expiredUIDs) {
			logger.info("Removing session proxies for expired session UID [{}]", uid);
			this.pingTimeForUIDs.remove(expiredUID);
			this.pathsForUIDs.remove(expiredUID);
			this.proxiesForUIDs.remove(expiredUID);
		}
	}

	/**
	 * Update the session cookie for the current session.
	 * If no session yet, create a cookie and an {@link #UID} session attribute.
	 * @param req  the HTTP request
	 * @param resp the HTTP response
	 * @return the current or generated UID.
	 */
	private static String updateCookie(HttpServletRequest req, HttpServletResponse resp) {
		Object uid = req.getSession().getAttribute(UID);
		if (uid == null) {
			// Could maybe have used JSESSIONID. But I kinda like having my own stuff.
			uid = RandomStringUtils.randomAlphanumeric(5);
			req.getSession().setAttribute(UID, uid);
		}
		Cookie loginCookie = new Cookie(UID, (String) uid);
		loginCookie.setMaxAge(SESSION_TIMEOUT * 60);
		resp.addCookie(loginCookie);
		return (String) uid;
	}

	/**
	 * Get the web-app class path.
	 * @return a classpath string for {@link #webAppClassLoaderURLs()}
	 */
	private String webAppClasspath() {
		StringBuilder out = new StringBuilder();
		for (URL url : this.webAppClassLoaderURLs()) {
			out.append(url.toString()).append(":");
		}
		return out.toString();
	}

	/**
	 * Get this class' classloader URLs.
	 * @return {@link URLClassLoader#getURLs()} from this class' {@link Class#getClassLoader()}
	 */
	private URL[] webAppClassLoaderURLs() {
		return ((URLClassLoader) this.getClass().getClassLoader()).getURLs();
	}

	/**
	 * Find the package name of the java code that was submitted.
	 * @param code the java code
	 * @return the package name, or an empty string if default package was used
	 */
	private static String packageName(String code) {
		Matcher matcher = PACKAGE_NAME_PATTERN.matcher(code);
		if (matcher.find()) {
			return matcher.group("packageName").trim();
		}
		return "";
	}

	/**
	 * Find the name of the public class that implements Yopable in the code that was sent.
	 * @param code the java code
	 * @return the public class name
	 * @throws RuntimeException no public Yopable class
	 */
	private static String className(String code) {
		Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
		if (matcher.find()) {
			return matcher.group("className").trim();
		}
		throw new RuntimeException("Could not find Yopable public class name for given piece of code !");
	}

	/**
	 * Initialize a {@link RestServletProxy} for a path and a package name
	 * @param path        the session fake root path (compilation)
	 * @param packageName the package name of the code we received
	 * @return a new REST proxy instance
	 */
	private RestServletProxy initProxy(Path path, String packageName) {
		RestServletProxy proxy = new RestServletProxy();
		proxy.init(path, this.getServletConfig(), packageName);
		return proxy;
	}

	/**
	 * Invoke {@link RestServletProxy#rest(HttpServletRequest, HttpServletResponse)}.
	 * @param proxy    the rest proxy
	 * @param request  the incoming HTTP request
	 * @param response the outgoing HTTP request
	 * @throws RuntimeException if any exception occurs
	 */
	private static void doRest(RestServletProxy proxy, HttpServletRequest request, HttpServletResponse response) {
		try {
			proxy.rest(request, response);
		} catch (ServletException | IOException | RuntimeException e) {
			throw new RuntimeException("Could not invoke REST servlet !", e);
		}
	}

	/**
	 * Invoke {@link RestServletProxy#openAPI(HttpServletRequest, HttpServletResponse)}.
	 * @param proxy    the rest proxy
	 * @param request  the incoming HTTP request
	 * @param response the outgoing HTTP request
	 * @throws RuntimeException if any exception occurs
	 */
	private static void doOpenAPI(RestServletProxy proxy, HttpServletRequest request, HttpServletResponse response) {
		try {
			proxy.openAPI(request, response);
		} catch (ServletException | IOException | RuntimeException e) {
			throw new RuntimeException("Could not invoke OpenAPI servlet !", e);
		}
	}

	/**
	 * Invoke {@link RestServletProxy#swagger(HttpServletRequest, HttpServletResponse)}.
	 * @param proxy    the rest proxy
	 * @param request  the incoming HTTP request
	 * @param response the outgoing HTTP request
	 * @throws RuntimeException if any exception occurs
	 */
	private static void doSwagger(RestServletProxy proxy, HttpServletRequest request, HttpServletResponse response) {
		try {
			proxy.swagger(request, response);
		} catch (IOException | RuntimeException e) {
			throw new RuntimeException("Could not invoke Swagger servlet !", e);
		}
	}
}
