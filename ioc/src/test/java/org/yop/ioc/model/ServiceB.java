package org.yop.ioc.model;

import org.yop.ioc.Singleton;

public class ServiceB implements InterfaceB {

	private Singleton<InterfaceA> a = Singleton.of(InterfaceA.class);
	private Singleton<InterfaceC> c = Singleton.of(InterfaceC.class);

	@Override
	public String doB() {
		return "B";
	}

	@Override
	public String all() {
		return this.a.get().doA() + this.doB() + this.c.get().doC();
	}
}
