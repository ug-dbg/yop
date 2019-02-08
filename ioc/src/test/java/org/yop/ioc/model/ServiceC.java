package org.yop.ioc.model;

import org.yop.ioc.Singleton;

public class ServiceC implements InterfaceC {

	private Singleton<InterfaceA> a = Singleton.of(InterfaceA.class);
	private Singleton<InterfaceB> b = Singleton.of(InterfaceB.class);

	@Override
	public String doC() {
		return "C";
	}

	@Override
	public String all() {
		return this.a.get().doA() + this.b.get().doB() + this.doC();
	}
}
