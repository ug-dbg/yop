package org.yop.ioc;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.yop.reflection.Reflection;

import java.util.Collection;
import java.util.Objects;

/**
 * Simple singleton/multiton instances management.
 * <br><br>
 * A singleton is a reference to a target {@link #instance}.
 * <br>
 * Singleton holds a static map of the instances for all the registered target types : {@link #MULTITONS}.
 * <br>
 * The target type singleton instantiation can be :
 * <ul>
 *     <li>explicit : {@link #register(Class, Object[])}</li>
 *     <li>on demand, lazy : {@link #register(Class, Class, int)}</li>
 * </ul>
 * <br>
 * When registering a singleton for a class, you can set an optional ID and the number of instances.
 * <br>
 * Default ID is {@link Key#DEFAULT_KEY} : a <b>singleton</b>.
 * <br>
 * Default number of instances is 1 : a <b>singleton</b>.
 * <br>
 * If the number of instances is > 1, and all instances are equivalent, let's call it a <b>pool</b>.
 * <br>
 * If every call to {@link #get()} should return a new instance (number of instances = -1), 
 * let's call it a <b>prototype</b>.
 * <br>
 * If a singleton can have several instances identified by an ID, this is a <b>multiton</b>.
 * <br><br>
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
 * If you need a multiton, use the methods to register or get a singleton that explicitly require an id.
 * <br>
 * Example :
 * <pre>
 * {@code
 * public class ServiceA implements IServiceA {
 *  private Singleton<IServiceB> b = Singleton.of(IServiceB.class, "b");
 *  private Singleton<IServiceB> b_prime = Singleton.of(IServiceB.class, "b_prime");
 * 
 *  public void executeA(boolean usePrime) {
 *   if (usePrime) {
 *    this.b_prime.get().executeB();
 *   } else {
 *    this.b.get().executeB();
 *   }
 *  }
 * }
 * }
 * {@code
 * public class Main {
 *  public static void main(String[] args) {
 *    Singleton.register(IServiceA.class, ServiceA.class);
 *    Singleton.register(IServiceB.class, "b", ServiceB.class);
 *    Singleton.register(IServiceB.class, "b_prime", ServiceBPrime.class);
 *  }
 * }
 * }
 * </pre>
 * 
 * 
 * @param <T> the target type of the Singleton instance
 */
@SuppressWarnings({"unused"})
public class Singleton<T> {

	private static final MultiValuedMap<Key, Singleton<?>> MULTITONS = new ArrayListValuedHashMap<>();
	private static final Object lock = "lock";

	/**
	 * A unique key to handle multiton instances.
	 * <br>
	 * The key is simply the target type and a comparable object.
	 * <br>
	 * For singletons that are not multitons, {@link #DEFAULT_KEY} is used.
	 * @param <T> the multiton target type
	 */
	private static class Key<T> {
		private static final String DEFAULT_KEY = "default";
		private final Class<? extends T> clazz;
		private final Comparable id;

		private Key(Class<? extends T> clazz, Comparable id) {
			this.clazz = clazz;
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || this.getClass() != o.getClass()) return false;
			Key<?> key1 = (Key<?>) o;
			return this.clazz.equals(key1.clazz) && this.id.equals(key1.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.clazz, this.id);
		}
	}
	
	Class<? extends T> clazz;
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
	 * <br>
	 * If there are more than 1 instance in {@link #MULTITONS}, a random one is returned.
	 * @param clazz the singleton target class
	 * @param <T> the singleton target type
	 * @return a reference to the target type singleton
	 */
	public static synchronized <T> Singleton<T> of(Class<? extends T> clazz) {
		return of(clazz, Key.DEFAULT_KEY);
	}
	
	/**
	 * Get a reference to the target type singleton instance.
	 * <br>
	 * If there are more than 1 instance in {@link #MULTITONS}, a random one is returned.
	 * @param clazz the singleton target class
	 * @param id    the singleton id 
	 * @param <T> the singleton target type
	 * @return a reference to the target type singleton
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> Singleton<T> of(Class<? extends T> clazz, Comparable id) {
		Key<T> key = new Key<>(clazz, id); 
		if (! MULTITONS.containsKey(key)) {
			Singleton<T> singleton = new Singleton<>(clazz);
			MULTITONS.put(key, singleton);
		}
		Collection<Singleton<T>> values = (Collection) MULTITONS.get(key);
		return values.stream().skip((int) (values.size() * Math.random())).findFirst().orElse(null);
	}

	/**
	 * Register the implementation for a given class.
	 * <br>
	 * Using this registration method, the target type implementation will be on demand.
	 * @param clazz          the singleton class (key for {@link #MULTITONS})
	 * @param implementation the implementation to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	public static synchronized <T, Impl extends T> void register(Class<T> clazz, Class<Impl> implementation) {
		register(clazz, Key.DEFAULT_KEY, implementation);
	}
	
	/**
	 * Register the implementation for a given class.
	 * <br>
	 * Using this registration method, the target type implementation will be on demand.
	 * @param clazz          the singleton class (key for {@link #MULTITONS})
	 * @param id             the singleton class id
	 * @param implementation the implementation to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	public static synchronized <T, Impl extends T> void register(
		Class<T> clazz, 
		Comparable id,
		Class<Impl> implementation) {
		
		Key<T> key = new Key<>(clazz, id);
		MULTITONS.get(key).clear();
		MULTITONS.put(key, new Singleton<>(implementation));
	}
	

	/**
	 * Register the implementation for a given class.
	 * <br>
	 * Using this registration method, the target type implementation will be on demand.
	 * @param clazz          the singleton class (key for {@link #MULTITONS})
	 * @param implementation the implementation to use
	 * @param nbOfInstances  the number of instances to add. If -1 : every {@link #get()} creates a new instance.
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	public static synchronized <T, Impl extends T> void register(
		Class<T> clazz,
		Class<Impl> implementation,
		int nbOfInstances) {
		register(clazz, Key.DEFAULT_KEY, implementation, nbOfInstances);
	}

	/**
	 * Register the implementation for a given class.
	 * <br>
	 * Using this registration method, the target type implementation will be on demand.
	 * @param clazz          the singleton class (key for {@link #MULTITONS})
	 * @param id             the singleton class id
	 * @param implementation the implementation to use
	 * @param nbOfInstances  the number of instances to add. If -1 : every {@link #get()} creates a new instance.
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	public static synchronized <T, Impl extends T> void register(
		Class<T> clazz,
		Comparable id,
		Class<Impl> implementation,
		int nbOfInstances) {
		Key<T> key = new Key<>(clazz, id);
		MULTITONS.get(key).clear();

		if (nbOfInstances == -1) {
			MULTITONS.put(key, new Prototype<>(implementation));
		}

		for (int i = 0; i < nbOfInstances; i++) {
			MULTITONS.put(key, new Singleton<>(implementation));
		}
	}
	
	/**
	 * Explicitly register the implementation instance for a given class.
	 * <br>
	 * @param clazz     the singleton class (key for {@link #MULTITONS})
	 * @param instances the implementation instances to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T, Impl extends T> void register(Class<T> clazz, Impl... instances) {
		register(clazz, Key.DEFAULT_KEY, instances);
	}
	
	/**
	 * Explicitly register the implementation instance for a given class and id.
	 * <br>
	 * @param clazz     the singleton class (key for {@link #MULTITONS})
	 * @param id        the singleton class id 
	 * @param instances the implementation instances to use
	 * @param <T> the registered type
	 * @param <Impl> the implementation type
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T, Impl extends T> void register(Class<T> clazz, Comparable id, Impl... instances) {
		Key<T> key = new Key<>(clazz, id);
		MULTITONS.get(key).clear();
		for (Impl instance : instances) {
			Singleton<Impl> singleton = new Singleton<>((Class<? extends Impl>) instance.getClass());
			singleton.instance = instance;
			MULTITONS.put(key, singleton);
		}
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

	/**
	 * A prototype is the opposite of the singleton.
	 * For every call to {@link #get()}, a new instance of the target implementation is returned.
	 * @param <T> the target type
	 */
	private static class Prototype<T> extends Singleton<T> {
		private Prototype(Class<? extends T> clazz) {
			super(clazz);
		}

		@Override
		public T get() {
			return Reflection.newInstanceNoArgs(this.clazz);
		}
	}
}
