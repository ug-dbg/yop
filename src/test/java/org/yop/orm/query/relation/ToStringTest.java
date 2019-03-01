package org.yop.orm.query.relation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.query.sql.SQLJoin;
import org.yop.orm.simple.model.Extra;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;

public class ToStringTest {

	@Test(expected = RuntimeException.class)
	public void test_no_joincolumn_relation_tostring() {
		// Pojo → Jopo is not @JoinColumn relation
		Pojo pojo = new Pojo();
		pojo.setJopos(Sets.newHashSet(new Jopo()));

		JoinColumnRelation<Pojo, Jopo> relation = new JoinColumnRelation<>(
			Lists.newArrayList(pojo),
			SQLJoin.toN(Pojo::getJopos)
		);
		Assert.assertNotNull(relation);
	}

	@Test(expected = RuntimeException.class)
	public void test_no_jointable_relation_tostring() {
	// Other → Extra is not @JoinTable relation
	Other other = new Other();
	other.setExtra(new Extra());

		JoinTableRelation<Other, Extra> relation = new JoinTableRelation<>(
			Lists.newArrayList(other),
			SQLJoin.to(Other::getExtra)
		);
		Assert.assertNotNull(relation);
	}

	@Test
	public void test_joincolumn_relation_tostring() {
		// Other → Extra is a @JoinColumn relation
		Other other = new Other();
		other.setExtra(new Extra());

		JoinColumnRelation<Other, Extra> emptyRelation = new JoinColumnRelation<>(
			Lists.newArrayList(),
			SQLJoin.to(Other::getExtra)
		);
		Assert.assertTrue(emptyRelation.toString().contains("From(?)→To(?)"));

		JoinColumnRelation<Other, Extra> relation = new JoinColumnRelation<>(
			Lists.newArrayList(other),
			SQLJoin.to(Other::getExtra)
		);
		Assert.assertNotNull(relation);
		String toString = relation.toString();
		Assert.assertTrue(toString.contains("From(org.yop.orm.simple.model.Other)→To(org.yop.orm.simple.model.Extra)"));
		Assert.assertTrue(toString.contains("relations={null→[null]}"));

		other.setId(1L);
		other.getExtra().setId(18L);
		toString = relation.toString();
		Assert.assertTrue(toString.contains("From(org.yop.orm.simple.model.Other)→To(org.yop.orm.simple.model.Extra)"));
		Assert.assertTrue(toString.contains("relations={1→[18]}"));
	}

	@Test
	public void test_jointable_relation_tostring() {
		// Pojo → Jopo is a @JoinTable relation
		Pojo pojo = new Pojo();
		pojo.setJopos(Sets.newHashSet(new Jopo()));

		JoinTableRelation<Pojo, Jopo> relation = new JoinTableRelation<>(
			Lists.newArrayList(pojo),
			SQLJoin.toN(Pojo::getJopos)
		);

		Assert.assertNotNull(relation);
		String toString = relation.toString();
		Assert.assertTrue(toString.contains("From(org.yop.orm.simple.model.Pojo)→To(org.yop.orm.simple.model.Jopo)"));
		Assert.assertTrue(toString.contains("relations={null→[null]}"));

		pojo.setId(2L);
		pojo.getJopos().iterator().next().setId(19L);
		toString = relation.toString();
		Assert.assertTrue(toString.contains("From(org.yop.orm.simple.model.Pojo)→To(org.yop.orm.simple.model.Jopo)"));
		Assert.assertTrue(toString.contains("relations={2→[19]}"));
	}
}
