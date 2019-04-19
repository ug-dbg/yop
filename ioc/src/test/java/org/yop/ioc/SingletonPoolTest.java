package org.yop.ioc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yop.ioc.model.*;
import org.yop.reflection.Reflection;
import org.yop.reflection.ReflectionException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingletonPoolTest {

	@Before
	public void setUp() {
		Singleton.register(InterfaceA.class, ServiceA.class, 5);
		Singleton.register(InterfaceB.class, ServiceB.class, 1);
		Singleton.register(InterfaceC.class, ServiceC.class, 1);

		Singleton.register(Interface.class, () -> "lambda1", () -> "lambda2", () -> "lambda3");
	}

	@Test
	public void testMultitonFromClassRegistration() {
		String fromA = Singleton.of(InterfaceA.class).get().all();
		String fromB = Singleton.of(InterfaceB.class).get().all();
		String fromC = Singleton.of(InterfaceC.class).get().all();

		Assert.assertEquals(fromA, fromB);
		Assert.assertEquals(fromA, fromC);
	}

	@Test
	public void testMultitonFromInstanceRegistration() {
		Assert.assertTrue(Singleton.of(Interface.class).get().all().startsWith("lambda"));
	}

	@Test
	public void testMultitonFromExplicitClass() {
		Assert.assertEquals(new ServiceA().all(), Singleton.of(ServiceA.class).get().all());
	}

	@Test
	public void testMultitonFromOverriddenInstanceRegistration() {
		Singleton.register(Interface.class, new ServiceA());
		Assert.assertEquals(new ServiceA().all(), Singleton.of(Interface.class).get().all());
	}

	@Test(expected = ReflectionException.class)
	public void testMultitonFromInterfaceRegistration() {
		Singleton.register(Interface.class, InterfaceB.class);
		Singleton.of(Interface.class).get().all();
	}

	@Test
	public void testMultitonFromMultiThreads() throws InterruptedException {
		Singleton.register(Interface.class, ServiceA.class, 3);
		ExecutorService executor = Executors.newFixedThreadPool(10);
		Set<Interface> instances = Collections.synchronizedSet(new HashSet<>());
		Runnable task = () -> {
			Interface instance = Singleton.of(Interface.class).get();
			instances.add(instance);
		};
		for (int taskNum = 0; taskNum < 10; taskNum++) {
			executor.submit(task);
		}
		executor.awaitTermination(500, TimeUnit.MILLISECONDS);
		Assert.assertTrue(instances.size() <= 3);

		// 3 ServiceA for 'Interface' but 1 ServiceB and 1 ServiceC.
		Set<Interface> bs = new HashSet<>();
		Set<Interface> cs = new HashSet<>();
		for (Interface instance : instances) {
			Assert.assertTrue(instance instanceof ServiceA);
			bs.add((Interface) ((Singleton) Reflection.readField("b", instance)).get());
			cs.add((Interface) ((Singleton) Reflection.readField("c", instance)).get());
		}
		Assert.assertEquals(1, bs.size());
		Assert.assertEquals(1, cs.size());
	}
}
