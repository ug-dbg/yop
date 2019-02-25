package org.yop.orm.query.serialize;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.serialize.xml.XML;

import java.util.ArrayList;
import java.util.Collection;

abstract class AbstractSerialize<Request extends AbstractSerialize, T extends Yopable> implements Serialize<Request, T>{

	/** The target Yopable class to serialize to JSON */
	private Class<T> target;

	/** The Yopable elements to serialize */
	//private XML.Yopables<T> elements = new XML.Yopables<>();

	/** Join clauses */
	private final Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

}
