package org.yop.ioc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yop.ioc.model.*;
import org.yop.reflection.ReflectionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingletonTest {

	@Before
	public void setUp() {
		Singleton.register(InterfaceA.class, ServiceA.class);
		Singleton.register(InterfaceB.class, ServiceB.class);
		Singleton.register(InterfaceC.class, ServiceC.class);

		Singleton.register(Interface.class, () -> "lambda");
	}

	@Test
	public void testSingletonFromClassRegistration() {
		String fromA = Singleton.of(InterfaceA.class).get().all();
		String fromB = Singleton.of(InterfaceB.class).get().all();
		String fromC = Singleton.of(InterfaceC.class).get().all();

		Assert.assertEquals(fromA, fromB);
		Assert.assertEquals(fromA, fromC);
	}

	@Test
	public void testSingletonFromInstanceRegistration() {
		Assert.assertEquals("lambda", Singleton.of(Interface.class).get().all());
	}

	@Test
	public void testSingletonFromExplicitClass() {
		Assert.assertEquals(new ServiceA().all(), Singleton.of(ServiceA.class).get().all());
	}

	@Test
	public void testSingletonFromOverriddenInstanceRegistration() {
		Singleton.register(Interface.class, new ServiceA());
		Assert.assertEquals(new ServiceA().all(), Singleton.of(Interface.class).get().all());
	}

	@Test(expected = ReflectionException.class)
	public void testSingletonFromInterfaceRegistration() {
		Singleton.register(Interface.class, InterfaceB.class);
		Singleton.of(Interface.class).get().all();
	}

	@Test
	public void testSingletonFromMultiThreads() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		List<Interface> instances = Collections.synchronizedList(new ArrayList<>());
		Runnable task = () -> {
			Interface instance = Singleton.of(Interface.class).get();
			instances.add(instance);
		};
		for (int taskNum = 0; taskNum < 10; taskNum++) {
			executor.submit(task);
		}
		executor.awaitTermination(100, TimeUnit.MILLISECONDS);
		Assert.assertEquals(10, instances.size());
		Interface ref = instances.iterator().next();
		for (Interface instance : instances) {
			if(instance != ref) {
				Assert.fail("different instances [" + instance + "] and [" + ref + "]");
			}
		}

	}

}
