package org.yop.example;

/**
 * Some session variables that must be accessed on behalf of a thread.
 */
public class ThreadLocalSession {

	private static final ThreadLocal<String> THREAD_LOCAL_UID = new ThreadLocal<>();
	private static final ThreadLocal<String> THREAD_LOCAL_PACKAGE = new ThreadLocal<>();

	/**
	 * Set the session UID associated to the current thread
	 * @param uid the session UID
	 */
	public static void setUID(String uid) {
		THREAD_LOCAL_UID.set(uid);
	}

	/**
	 * Get the session UID associated to the current thread
	 * @return the session UID for the current thread
	 */
	public static String getUID() {
		return THREAD_LOCAL_UID.get();
	}

	/**
	 * Set the session user code package name associated to the current thread
	 * @param packageName the session package name
	 */
	public static void setPackageName(String packageName) {
		THREAD_LOCAL_PACKAGE.set(packageName);
	}

	/**
	 * Get the session user code package name associated to the current thread
	 * @return the session package name
	 */
	public static String getPackageName() {
		return THREAD_LOCAL_PACKAGE.get();
	}
}
