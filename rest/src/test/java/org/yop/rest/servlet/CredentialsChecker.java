package org.yop.rest.servlet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.sql.Select;
import org.yop.orm.query.sql.Where;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopForbiddenException;
import org.yop.rest.exception.YopNoAuthException;
import org.yop.rest.users.model.User;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Dummy {@link org.yop.rest.servlet.YopRestServlet.RequestChecker} implementation.
 * It searches for "read"/"write" {@link org.yop.rest.users.model.Action}(s) associated to the current user.
 */
public class CredentialsChecker implements YopRestServlet.RequestChecker {

	private static final Logger logger = LoggerFactory.getLogger(CredentialsChecker.class);

	@Override
	public void init(ServletConfig config) {
		logger.info("Initializing Credentials Checker with servlet [{}]", config.getServletName());
	}

	@Override
	public <T> void checkResource(RestRequest<T> request, IConnection connection) {
		logger.info("Checking resource [{}]", request);

		Object user = request.getRequest().getSession().getAttribute("user");
		if (user == null) {
			user = readUserFromCookies(request.getRequest(), connection);
		}
		if (user == null) {
			throw new YopNoAuthException("No user logged in !");
		}

		switch (request.getMethod()) {
			case "HEAD"   : canRead((User) user); break;
			case "GET"    : canRead((User) user); break;
			case "PUT"    : canWrite((User) user); break;
			case "UPSERT" : canWrite((User) user); break;
			case "POST"   : canWrite((User) user); break;
			case "DELETE" : canWrite((User) user); break;
			default: throw new UnsupportedOperationException("Unsupported operation [" + request.getMethod() + "]");
		}
	}

	private static User readUserFromCookies(HttpServletRequest request, IConnection connection) {
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			Optional<Cookie> login = Arrays
				.stream(cookies)
				.filter(cookie -> StringUtils.equals("login", cookie.getName()))
				.findFirst();

			if (login.isPresent()) {
				return Select
					.from(User.class)
					.where(Where.compare(User::getEmail, Operator.EQ, login.get().getValue()))
					.joinAll()
					.uniqueResult(connection);
			}
		}

		return null;
	}

	private static void canRead(User user) {
		boolean canRead = user
			.getProfiles()
			.stream()
			.anyMatch(
				profile -> profile.getActionsForProfile().stream().anyMatch(action -> action.getName().equals("read"))
			);
		if (canRead) {
			logger.info("User logged in [{}] can read !", user.getEmail());
		} else {
			throw new YopForbiddenException("User [" + user.getEmail() + "] is not allowed to read resource !");
		}
	}

	private static void canWrite(User user) {
		boolean canWrite = user
			.getProfiles()
			.stream()
			.anyMatch(
				profile -> profile.getActionsForProfile().stream().anyMatch(action -> action.getName().equals("write"))
			);
		if (canWrite) {
			logger.info("User logged in [{}] can write !", user.getEmail());
		} else {
			throw new YopForbiddenException("User [" + user.getEmail() + "] is not allowed to write resource !");
		}
	}
}
