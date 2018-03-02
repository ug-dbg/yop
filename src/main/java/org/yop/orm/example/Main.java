package org.yop.orm.example;

import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

public class Main {

	public static void main(String[] args) throws Exception {
		Select<Pojo> select = Select
			.from(Pojo.class)
			.join(
				Join.to(Pojo::getParent)
					.where(Where.compare(Pojo::getVersion, Operator.GE, 1))
					.where(Where.isNotNull(Pojo::getId))
					.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
			)
			.join(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
			.join(JoinSet.to(Pojo::getJopos).where(Where.compare(Jopo::getName, Operator.GE, "jopo2")))
			.where(Where.isNotNull(Pojo::getVersion))
			.or(Where.isNull(Pojo::getVersion), Where.isNotNull(Pojo::getId))
			;

		Pojo reference = new Pojo();
		reference.setVersion(2038);
		Select<Pojo> selectByNaturalId = Select
			.from(Pojo.class)
			.join(Join.to(Pojo::getParent).join(JoinSet.to(Pojo::getJopos)))
			.join(JoinSet.to(Pojo::getJopos))
			.where(Where.naturalId(reference))
			;

		Delete<Pojo> delete = Delete
			.from(Pojo.class)
			.join(Delete.JoinSet.to(Pojo::getJopos).join(Delete.Join.to(Jopo::getPojo)));

		Class.forName("com.mysql.jdbc.Driver");
		String connectionString = "jdbc:mysql://localhost:3306/yop?useUnicode=true&characterEncoding=utf-8";
		try (Connection connection = DriverManager.getConnection(connectionString, "root", "root")) {
			Set<Pojo> elementsWithExists = select.execute(connection, Select.STRATEGY.EXISTS);
			Set<Pojo> elementsWithIN     = select.execute(connection, Select.STRATEGY.IN);
			Set<Pojo> elements2Queries   = select.executeWithTwoQueries(connection);
			System.out.println(elementsWithIN.equals(elements2Queries));
			System.out.println(elementsWithIN.equals(elementsWithExists));


			if(elementsWithExists.size() > 0) {
				Pojo pojo = elementsWithExists.iterator().next();
				System.out.println(pojo.toJson());
			}

			Set<Pojo> found = selectByNaturalId.execute(connection, Select.STRATEGY.EXISTS);
			System.out.println(found);

			//delete.executeQuery(connection);

			Pojo newPojo = new Pojo();
			newPojo.setVersion(1337);
			newPojo.setType(Pojo.Type.FOO);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);
			Other other = new Other();
			other.setName("otheeer !");
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			newPojo.getOthers().add(other);

			Upsert.from(Pojo.class).onto(newPojo).join(JoinSet.to(Pojo::getJopos)).join(JoinSet.to(Pojo::getOthers)).checkNaturalID().execute(connection);

			found = Select.from(Pojo.class).where(Where.naturalId(newPojo)).joinAll().execute(connection, Select.STRATEGY.EXISTS);
			if(found.size() > 0) {
				newPojo = found.iterator().next();
				newPojo.setType(Pojo.Type.BAR);
				Upsert.from(Pojo.class).onto(newPojo).execute(connection);
				System.out.println(newPojo.getId());
				Delete.from(Pojo.class).where(Where.naturalId(newPojo)).executeQuery(connection);
			}
		}
	}

}
