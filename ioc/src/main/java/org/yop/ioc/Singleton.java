package org.yop.ioc;

import org.yop.reflection.Reflection;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple singleton instances management.
 * <br><br>
 * A singleton is a reference to a target {@link #instance}.
 * <br>
 * Singleton holds a static map of Singleton instances for all the registered target types : {@link #INSTANCES}.
 * <br>
 * The target type singleton instantiation can be :
 * <ul>
 *     <li>explicit : {@link #register(Class, Object)}</li>
 *     <li>on demand, lazy : {@link #register(Class, Class)}</li>
 * </ul>
 * <br>
 * A singleton of a target class/interface can be set as a field to achieve IoC.
 * <br>
 * Example :
 * <pre>
 * {@code
 * public class ServiceA implements IServiceA {
 *  private Singleton<IServiceB> b = Singleton.of(IServiceB.class);
 *
 *  public void executeA() {
 *   this.b.get().executeB();
 *  }
 * }
 * }
 * {@code
 * public class Main {
 *  public static void main(String[] args) {
 *    Singleton.register(IServiceA.class, ServiceA.class);
 *    Singleton.register(IServiceB.class, ServiceB.class);
 *  }
 * }
 * }
 * </pre>
 * @param <T> the target type of the Singleton instance
 */
public class Singleton<T> {

	private static final Map<Class, Singleton<?>> INSTANCES = new HashMap<>();
	private static final Object lock = "lock";

	private Class<? extends T> clazz;
	private volatile T instance;

	/**
	 * Private constructor. Please use {@link #of(Class)}.
	 * @param clazz the target type
	 */
	private Singleton(Class<? extends T> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Get a reference to the target type singleton instance.
	 * @param clazz the singleton target class
	 * @param <T> the singleton target type
	 * @return a reference to the target type singleton
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> Singleton<T> of(Class<? extends T> clazz) {
		if (! INSTANCES.containsKey(clazz)) {
			Singleton<T> singleton = new Singleton<>(clazz);
			INSTANCES.put(clazz, singleton);
		}
		return (Singleton<T>) INSTANCES.get(clazz);
	}

	/**
	 * Register the implementation for a given class.
	 * <br>
	 * Using this registration method, the target type implementation will be on demand.
	 * @param clazz          the singleton class (key for {@link #INSTANCES})
	 * @param implementation the implementation to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	public static synchronized <T, Impl extends T> void register(Class<T> clazz, Class<Impl> implementation) {
		INSTANCES.put(clazz, new Singleton<>(implementation));
	}

	/**
	 * Explicitly register the implementation instance for a given class.
	 * <br>
	 * @param clazz    the singleton class (key for {@link #INSTANCES})
	 * @param instance the implementation instance to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T, Impl extends T> void register(Class<T> clazz, Impl instance) {
		Singleton<Impl> singleton = new Singleton<>((Class<? extends Impl>) instance.getClass());
		singleton.instance = instance;
		INSTANCES.put(clazz, singleton);
	}

	/**
	 * Get the singleton target type instance.
	 * If registration was marked 'on demand' and this is the first call, instantiation is done here.
	 * <br>
	 * There is a double checked locking. Not sure if really useful/effective.
	 * @return the singleton instance for the target type
	 */
	public T get() {
		if (this.instance == null) {
			synchronized (lock) {
				if (this.instance == null) {
					this.instance = Reflection.newInstanceNoArgs(this.clazz);
				}
			}
		}
		return this.instance;
	}
}
