package org.yop.rest.servlet;

import org.apache.commons.codec.digest.Crypt;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.Select;
import org.yop.orm.query.Where;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopNoAuthException;
import org.yop.rest.users.model.User;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * A very basic Login servlet :
 * <ul>
 *     <li>answer to POST requests</li>
 *     <li>read login/password from parameters</li>
 *     <li>check the user credentials into the {@link User} table</li>
 *     <li>put the associated {@link User} into the session</li>
 *     <li>add a Cookie with the login</li>
 * </ul>
 */
public class LoginServlet extends HttpServlet {

	public static final String SALT = "yop";

	protected IConnection getConnection() throws ClassNotFoundException, SQLException {
		throw new UnsupportedOperationException("Override to give me an ad-hoc connection !");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String login = req.getParameter("login");
		String password = req.getParameter("password");
		String passwordHash = Crypt.crypt(password, SALT);

		try (IConnection connection = this.getConnection()){
			User user = Select.from(User.class)
				.where(Where.compare(User::getEmail, Operator.EQ, login))
				.where(Where.compare(User::getPasswordHash, Operator.EQ, passwordHash))
				.joinAll()
				.uniqueResult(connection);
			if (user == null) {
				throw new YopNoAuthException("Unknown user [" + login + "] or bad credentials !");
			}

			req.getSession().setAttribute("user", user);

			// Setting cookie in session to expiry in 30 minutes
			Cookie loginCookie = new Cookie("login", login);
			loginCookie.setMaxAge(30 * 60);
			resp.addCookie(loginCookie);
			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (SQLException | ClassNotFoundException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		}
	}
}
