package org.yop.ioc.model;

public interface Interface {

	default void print() {
		System.out.println(this.all());
	}

	String all();

}
