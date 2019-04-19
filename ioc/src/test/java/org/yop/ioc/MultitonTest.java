package org.yop.ioc;

import org.junit.Assert;
import org.junit.Test;
import org.yop.ioc.model.Interface;
import org.yop.ioc.model.ServiceA;
import org.yop.ioc.model.ServiceB;
import org.yop.ioc.model.ServiceC;

public class MultitonTest {
	
	@Test
	public void test_multiton_register_class_and_get() {
		Singleton.register(Interface.class, "a", ServiceA.class);
		Singleton.register(Interface.class, "b", ServiceB.class);
		Singleton.register(Interface.class, "c", ServiceC.class);

		Assert.assertEquals(ServiceA.class, Singleton.of(Interface.class, "a").get().getClass());
		Assert.assertEquals(ServiceB.class, Singleton.of(Interface.class, "b").get().getClass());
		Assert.assertEquals(ServiceC.class, Singleton.of(Interface.class, "c").get().getClass());
	}
	
	@Test
	public void test_multiton_register_instance_and_get() {
		Singleton.register(Interface.class, "a", new ServiceA());
		Singleton.register(Interface.class, "b", new ServiceB());
		Singleton.register(Interface.class, "c", new ServiceC());

		Assert.assertEquals(ServiceA.class, Singleton.of(Interface.class, "a").get().getClass());
		Assert.assertEquals(ServiceB.class, Singleton.of(Interface.class, "b").get().getClass());
		Assert.assertEquals(ServiceC.class, Singleton.of(Interface.class, "c").get().getClass());
	}
	
	@Test
	public void test_multiton_register_instances_and_get() {
		Singleton.register(Interface.class, "any", new ServiceA(), new ServiceB(), new ServiceC());
		Assert.assertNotNull(Singleton.of(Interface.class, "any").get());
	}
	
}
