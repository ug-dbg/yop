package org.yop.rest.tomcat.logging;

import org.apache.juli.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tomcat (>= 8) delegated logging service to SLF4j.
 * <br>
 * See <a href="https://stackoverflow.com/a/49266815">this Stackoverflow post</a>
 */
public class DelegateToSlf4jLogger implements Log {
	private final Logger logger;

	// constructor required by ServiceLoader
	public DelegateToSlf4jLogger() {
		this.logger = null;
	}

	public DelegateToSlf4jLogger(String name){
		this.logger = LoggerFactory.getLogger(name);
	}

	@Override
	public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}

	@Override
	public boolean isFatalEnabled() {
		return this.logger.isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return this.logger.isInfoEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return this.logger.isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return this.logger.isWarnEnabled();
	}

	@Override
	public void trace(Object message) {
		this.logger.debug(String.valueOf(message));
	}

	@Override
	public void trace(Object message, Throwable t) {
		this.logger.debug(String.valueOf(message), t);
	}

	@Override
	public void debug(Object message) {
		this.logger.debug(String.valueOf(message));
	}

	@Override
	public void debug(Object message, Throwable t) {
		this.logger.debug(String.valueOf(message), t);
	}

	@Override
	public void info(Object message) {
		this.logger.info(String.valueOf(message));
	}

	@Override
	public void info(Object message, Throwable t) {
		this.logger.info(String.valueOf(message), t);
	}

	@Override
	public void warn(Object message) {
		this.logger.warn(String.valueOf(message));
	}

	@Override
	public void warn(Object message, Throwable t) {
		this.logger.warn(String.valueOf(message), t);
	}

	@Override
	public void error(Object message) {
		this.logger.error(String.valueOf(message));
	}

	@Override
	public void error(Object message, Throwable t) {
		this.logger.error(String.valueOf(message), t);
	}

	@Override
	public void fatal(Object message) {
		this.logger.error(String.valueOf(message));
	}

	@Override
	public void fatal(Object message, Throwable t) {
		this.logger.error(String.valueOf(message), t);
	}
}
