package org.yop.ioc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yop.ioc.model.*;
import org.yop.reflection.Reflection;
import org.yop.reflection.ReflectionException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PrototypeTest {

	@Before
	public void setUp() {
		Singleton.register(InterfaceA.class, ServiceA.class, -1);
		Singleton.register(InterfaceB.class, ServiceB.class, 1);
		Singleton.register(InterfaceC.class, ServiceC.class, 1);

		Singleton.register(Interface.class, () -> "lambda1", () -> "lambda2", () -> "lambda3");
	}

	@Test
	public void testPrototypeFromClassRegistration() {
		String fromA = Singleton.of(InterfaceA.class).get().all();
		String fromB = Singleton.of(InterfaceB.class).get().all();
		String fromC = Singleton.of(InterfaceC.class).get().all();

		Assert.assertEquals(fromA, fromB);
		Assert.assertEquals(fromA, fromC);
	}

	@Test
	public void testPrototypeFromInstanceRegistration() {
		Assert.assertTrue(Singleton.of(Interface.class).get().all().startsWith("lambda"));
	}

	@Test
	public void testPrototypeFromExplicitClass() {
		Assert.assertEquals(new ServiceA().all(), Singleton.of(ServiceA.class).get().all());
	}

	@Test
	public void testPrototypeFromOverriddenInstanceRegistration() {
		Singleton.register(Interface.class, new ServiceA());
		Assert.assertEquals(new ServiceA().all(), Singleton.of(Interface.class).get().all());
	}

	@Test(expected = ReflectionException.class)
	public void testPrototypeFromInterfaceRegistration() {
		Singleton.register(Interface.class, InterfaceB.class);
		Singleton.of(Interface.class).get().all();
	}

	@Test
	public void testPrototypeFromMultiThreads() throws InterruptedException {
		Singleton.register(Interface.class, ServiceA.class, -1);
		ExecutorService executor = Executors.newFixedThreadPool(10);
		Set<Interface> instances = Collections.synchronizedSet(new HashSet<>());
		Runnable task = () -> {
			Interface instance = Singleton.of(Interface.class).get();
			instances.add(instance);
		};
		for (int taskNum = 0; taskNum < 10; taskNum++) {
			executor.submit(task);
		}
		executor.awaitTermination(100, TimeUnit.MILLISECONDS);
		Assert.assertEquals(10, instances.size());

		// ServiceA is prototype for 'Interface' but 1 ServiceB and 1 ServiceC.
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
