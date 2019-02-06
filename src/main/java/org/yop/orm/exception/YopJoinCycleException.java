package org.yop.orm.exception;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The {@link org.yop.orm.query.AbstractRequest#joinAll()} cuts cycles in the fetch graph.
 * <br>
 * Any cycle can be explicitly cut using the {@link org.yop.orm.annotations.YopTransient} or the 'transient' keyword.
 * <br>
 * The 'joinAll' method uses this exception (with as much details as possible) when cutting cycles.
 */
@SuppressWarnings("unused")
public class YopJoinCycleException extends YopRuntimeException {

	private Field detectedCycleField;

	private List<IJoin> processedJoins = new ArrayList<>();

	public YopJoinCycleException(Field field) {
		super("There is a cycle in your join request starting @[" + Reflection.fieldToString(field) + "]. ");
		this.detectedCycleField = field;
	}

	/**
	 * Add joins that were processed before the cycle was detected. They will be printed in {@link #getMessage()}.
	 * @param processedJoins the processed joins that were successfully processed
	 */
	public <T extends Yopable> void addProcessedJoins(Collection<IJoin<T, ? extends Yopable>> processedJoins) {
		this.processedJoins.addAll(processedJoins);
	}

	/**
	 * Get the field on which a cycle was detected.
	 * @return {@link #detectedCycleField}
	 */
	public Field getDetectedCycleField() {
		return this.detectedCycleField;
	}

	/**
	 * Get the joins that were processed before the cycle was detected.
	 * @return {@link #processedJoins}
	 */
	public List<IJoin> getProcessedJoins() {
		return this.processedJoins;
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * The message will contain {@link #processedJoins} and {@link #detectedCycleField}.
	 */
	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (! this.processedJoins.isEmpty()) {
			message += "The joins that were already processed are " + this.processedJoins;
		}
		return message;
	}
}
