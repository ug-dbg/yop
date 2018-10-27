package org.yop.example;

import org.yop.orm.exception.YopRuntimeException;
import org.yop.rest.exception.YopForbiddenException;
import org.yop.rest.exception.YopResourceInvocationException;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is a security manager that tries to prevent any damage that could be done by a malicious user of the demo.
 * <br>
 * Since the point of the demo is to allow execution of custom code and I'm such a bad security engineer,
 * I came up with this solution.
 * <br>
 * For some key methods of the security manager, we check in the stack trace ({@link #getClassContext()})
 * if we are executing custom code. Then we do some extra checking or deny permission immediately.
 */
public class Sandbox extends SecurityManager {

	/** A set of file path where no specific checking will be done. e.g. web app classpath */
	private final Set<Path> allowedPaths = new TreeSet<>();

	/**
	 * Default constructor. Initialize the {@link #allowedPaths} from the class loaders.
	 */
	Sandbox() {
		this.allowedPaths.add(
			Paths.get(ThreadLocalSession.class.getResource("ThreadLocalSession.class").toString())
		);
		this.allowedPaths.add(Paths.get(System.getProperty("java.home")));
		this.allowClassloader((URLClassLoader) this.getClass().getClassLoader());
	}

	@Override
	protected Class[] getClassContext() {
		return super.getClassContext();
	}

	@Override
	public Object getSecurityContext() {
		return super.getSecurityContext();
	}

	@Override
	public void checkPermission(Permission perm) {}

	@Override
	public void checkCreateClassLoader() {
		super.checkCreateClassLoader();
	}

	@Override
	public void checkAccess(Thread t) {
		super.checkAccess(t);
	}

	@Override
	public void checkAccess(ThreadGroup g) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopRuntimeException("Did you just try to spawn a new Thread ?");
		}
		super.checkAccess(g);
	}

	@Override
	public void checkExit(int status) {
		throw new YopResourceInvocationException("Somebody tried to exit the JVM !");
	}

	@Override
	public void checkExec(String cmd) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopRuntimeException("Please, no command line execution :-P");
		}
		super.checkExec(cmd);
	}

	@Override
	public void checkLink(String lib) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopRuntimeException("Please, do not link any library !");
		}
		super.checkLink(lib);
	}

	@Override
	public void checkRead(FileDescriptor fd) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopRuntimeException("Please, no file descriptor mess !");
		}
	}

	@Override
	public void checkRead(String file) {
		Path pathToCheck = Paths.get(file);
		String uid = ThreadLocalSession.getUID();
		if (pathToCheck != null && this.allowedPaths.stream().noneMatch(pathToCheck::startsWith)) {
			String packageName = ThreadLocalSession.getPackageName();
			if (Session.has(uid)) {
				Session session = Session.get(uid);
				if (this.isCustomCode(packageName) && !pathToCheck.startsWith(session.getFakeRoot())) {
					throw new YopForbiddenException("You tried to access files outside of the sandbox [" + file + "]");
				}
			}
		}
	}

	@Override
	public void checkWrite(String file) {
		this.checkRead(file);
		super.checkWrite(file);
	}

	@Override
	public void checkDelete(String file) {
		this.checkRead(file);
		super.checkDelete(file);
	}

	@Override
	public void checkConnect(String host, int port) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not connect elsewhere !");
		}
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not connect elsewhere !");
		}
	}

	@Override
	public void checkListen(int port) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not connect elsewhere !");
		}
	}

	@Override
	public void checkAccept(String host, int port) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not connect elsewhere !");
		}
	}

	@Override
	public void checkMulticast(InetAddress maddr) {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not connect elsewhere !");
		}
	}

	@Override
	public void checkPropertiesAccess() {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not change system properties !");
		}
		super.checkPropertiesAccess();
	}

	@Override
	public void checkPropertyAccess(String key) {
		if (!key.startsWith("yop.") && this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not mess with system properties !");
		}
		super.checkPropertyAccess(key);
	}

	@Override
	public void checkPrintJobAccess() {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not print anything !");
		}
		super.checkPrintJobAccess();
	}

	@Override
	public void checkPackageAccess(String pkg) {
		super.checkPackageAccess(pkg);
	}

	@Override
	public void checkPackageDefinition(String pkg) {
		super.checkPackageDefinition(pkg);
	}

	@Override
	public void checkSetFactory() {
		if (this.isCustomCode(ThreadLocalSession.getPackageName())) {
			throw new YopForbiddenException("Please do not change socket factory !");
		}
		super.checkSetFactory();
	}

	/**
	 * Allow all URL paths from a classloder in the Sandbox.
	 * @param classLoader the class loader to allow
	 */
	private void allowClassloader(URLClassLoader classLoader) {
		for (URL url : classLoader.getURLs()) {
			this.allowedPaths.add(Paths.get(url.getFile()));
		}
	}

	/**
	 * Check if there is user code in the current stack.
	 * @param packageName the user code package name
	 * @return true if one element of the stack trace matches the package name
	 */
	private boolean isCustomCode(String packageName) {
		for (Class stackTraceElement : this.getClassContext()) {
			String elementPackageName = "";
			if (stackTraceElement.getPackage() != null) {
				elementPackageName = stackTraceElement.getPackage().getName();
			}
			if (elementPackageName.equals(packageName)) {
				return true;
			}
		}
		return false;
	}
}
