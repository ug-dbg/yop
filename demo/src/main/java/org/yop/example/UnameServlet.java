package org.yop.example;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.util.MessageUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * A servlet to display server info : demo version + build timestamp + platform info.
 * <br>
 * This should be used for instance in the footer. Or not.
 * <br>
 * uname command probably does not work on non-unix platforms, the servlet should not fail for all that.
 */
public class UnameServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(UnameServlet.class);

	private static final DefaultExecutor EXECUTOR = new DefaultExecutor();

	private String buildLabel;
	private String platformLabel;

	@Override
	public void init() throws ServletException {
		super.init();
		this.platformLabel = uname();
		this.buildLabel = this.buildLabel();
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String infoLabel = MessageUtil.join(" on ", this.buildLabel, this.platformLabel);
		resp.getWriter().write(infoLabel);
		resp.setContentType(ContentType.TEXT_PLAIN.getMimeType());
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Read the 'Build-Label' and 'Build-Timestamp' entries from the manifest.
	 * @return the build label + timestamp entry values, or an empty string if no/incorrect manifest.
	 */
	private String buildLabel() {
		Properties prop = new java.util.Properties();
		try {
			prop.load(this.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
			return MessageUtil.join("-", prop.getProperty("Build-Label"), prop.getProperty("Build-Timestamp"));
		} catch (IOException | RuntimeException e) {
			logger.warn("Could not read build label from MANIFEST", e);
		}
		return "";
	}

	/**
	 * Execute the {@code uname -rms} command. I guess it does not work well on Windows platforms.
	 * @return the output of the command, giving a hint on the platform details.
	 */
	private static String uname() {
		String cmd = "uname -rms";
		CommandLine cmdLine = CommandLine.parse(cmd);
		OutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		EXECUTOR.setExitValue(0);
		EXECUTOR.setStreamHandler(streamHandler);
		try {
			EXECUTOR.execute(cmdLine);
		} catch (IOException | RuntimeException e) {
			logger.warn("Could not execute command line [{}]", cmd, e);
		}
		return outputStream.toString();
	}

}
