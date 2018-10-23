package org.yop.example;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In this demo, for an UID we have to store some details.
 * Namely :
 * <ul>
 *     <li>a REST/OpenAPI proxy → {@link #proxy}</li>
 *     <li>a path fake root for compilation → {@link #fakeRoot}</li>
 *     <li>a timestamp for last hit → {@link #lastHit}</li>
 *     <li>the package name of user code → {@link #userCodePackageName}</li>
 * </ul>
 * This class holds a static reference to all the sessions : {@link #SESSIONS}.
 * <br>
 * Expired sessions can be cleared using {@link #clean()}.
 */
class Session {

	static final Integer SESSION_TIMEOUT = 10;

	/** UID → User code session */
	private static final Map<String, Session> SESSIONS = new HashMap<>();

	private RestServletProxy proxy;
	private Path fakeRoot;
	private LocalDateTime lastHit;
	private String userCodePackageName;

	private Session() {}

	/**
	 * Wipe all sessions whose {@link #lastHit} + {@link #SESSION_TIMEOUT} < now.
	 */
	static void clean() {
		Collection<String> expired = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		for (Map.Entry<String, Session> session : SESSIONS.entrySet()) {
			if (session.getValue().lastHit.until(now, ChronoUnit.MINUTES) > SESSION_TIMEOUT) {
				expired.add(session.getKey());
			}
		}
		expired.forEach(SESSIONS::remove);
	}

	/**
	 * Check if a session exists
	 * @param uid the session UID
	 * @return true if {@link #SESSIONS} has an entry for the given uid.
	 */
	static boolean has(String uid) {
		return SESSIONS.containsKey(uid);
	}

	/**
	 * Get an existing session for the given UID or create it.
	 * @param uid the session UID
	 * @return a session for the given UID
	 */
	static Session get(String uid) {
		if (! SESSIONS.containsKey(uid)) {
			SESSIONS.put(uid, new Session());
		}
		return SESSIONS.get(uid);
	}

	/**
	 * Update the {@link #lastHit} of the session with now.
	 */
	public void ping(){
		this.lastHit = LocalDateTime.now();
	}

	/**
	 * Get this session's REST/OpenAPI proxy.
	 * @return the {@link #proxy} associated to the session
	 */
	public RestServletProxy getProxy() {
		return this.proxy;
	}

	/**
	 * Get this session's current fake root.
	 * @return the {@link #fakeRoot} path of the session
	 */
	public Path getFakeRoot() {
		return this.fakeRoot;
	}

	/**
	 * Get the package name of the user code for this session.
	 * @return the {@link #userCodePackageName} of this session.
	 */
	public String getUserCodePackageName() {
		return this.userCodePackageName;
	}

	/**
	 * Change the fake root of this session.
	 * @param path the new fake root path
	 */
	public void setRoot(Path path) {
		this.fakeRoot = path;
	}

	/**
	 * Change the REST/OpenAPI proxy for this session.
	 * @param proxy the {@link #proxy} to set.
	 */
	public void setProxy(RestServletProxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Change the package name of the user submitted code.
	 * @param packageName the user code package name
	 */
	public void setPackageName(String packageName) {
		this.userCodePackageName = packageName;
	}
}
