package org.yop.orm.simple;

import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.In;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.evaluation.Or;
import org.yop.orm.evaluation.Path;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Where;
import org.yop.orm.simple.model.*;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;

import static org.yop.orm.Yop.*;

public class SimpleQueryToJSONTest extends DBMSSwitch {

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testSerializeSelect() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("other name :)");
			newPojo.getOthers().add(other);

			Extra extra = new Extra();
			extra.setStyle("rad");
			extra.setUserName("roger");
			extra.setOther(other);
			other.setExtra(extra);

			SuperExtra superExtra = new SuperExtra();
			superExtra.setSize(123456789L);
			extra.setSuperExtra(superExtra);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.checkNaturalID()
				.execute(connection);

			Path<Pojo, String> jopoName = Path.pathSet(Pojo::getJopos).to(Jopo::getName);
			Select<Pojo> select = select(Pojo.class)
				.where(new Or(
					Where.compare(Pojo::getVersion, Operator.EQ, 10564337),
					new In(Pojo::getType, Arrays.asList(Pojo.Type.FOO, Pojo.Type.BAR))
				))
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).where(Where.compare(Other::getName, Operator.NE, jopoName)));;

			Set<Pojo> found_ref = select.execute(connection);

			JsonObject selectJSON = select.toJSON();
			Select<Pojo> selectFromJSON = Select.fromJSON(selectJSON.toString());
			Set<Pojo> found = selectFromJSON.execute(connection);

			Assert.assertEquals(found_ref, found);
		}
	}

	@Test
	public void testSerializeUpsert() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("other name :)");
			newPojo.getOthers().add(other);

			Extra extra = new Extra();
			extra.setStyle("rad");
			extra.setUserName("roger");
			extra.setOther(other);
			other.setExtra(extra);

			SuperExtra superExtra = new SuperExtra();
			superExtra.setSize(123456789L);
			extra.setSuperExtra(superExtra);

			Upsert<Pojo> upsert = upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.checkNaturalID();
			JsonObject upsertJSON = upsert.toJSON();
			Upsert upsertFromJsON = Upsert.fromJSON(upsertJSON.toString());
			upsertFromJsON.execute(connection);

			Pojo fromDB = Select.from(Pojo.class)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.uniqueResult(connection);
			Assert.assertEquals(newPojo, fromDB);
			Assert.assertTrue(!fromDB.getOthers().isEmpty());
			Assert.assertTrue(!fromDB.getJopos().isEmpty());
			Assert.assertTrue(fromDB.getOthers().iterator().next().getExtra() != null);

			Assert.assertTrue(fromDB.getOthers().iterator().next().getExtra().getSuperExtra() != null);
		}
	}

}
