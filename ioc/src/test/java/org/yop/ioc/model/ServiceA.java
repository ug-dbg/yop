package org.yop.ioc.model;

import org.yop.ioc.Singleton;

public class ServiceA implements InterfaceA {

	private Singleton<InterfaceB> b = Singleton.of(InterfaceB.class);
	private Singleton<InterfaceC> c = Singleton.of(InterfaceC.class);

	@Override
	public String doA() {
		return "A";
	}

	@Override
	public String all() {
		return this.doA() + this.b.get().doB() + this.c.get().doC();
	}
}
