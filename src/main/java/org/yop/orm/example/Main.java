package org.yop.orm.example;

import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.Join;
import org.yop.orm.query.JoinSet;
import org.yop.orm.query.Select;
import org.yop.orm.query.Where;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Set;

public class Main {

	public static void main(String[] args) throws Exception {
		Select<Pojo> select = Select
			.from(Pojo.class)
			.join(
				Join.to(Pojo::getParent)
					.where(Where.compare(Pojo::getVersion, Operator.GE, 1))
					.where(Where.isNotNull(Pojo::getId))
					.join(JoinSet.to(Pojo::getJopos))
			)
			.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
			.where(Where.isNotNull(Pojo::getVersion))
			.or(Where.isNull(Pojo::getVersion), Where.isNotNull(Pojo::getId))
			;

		Class.forName("com.mysql.jdbc.Driver");
		String connectionString = "jdbc:mysql://localhost:3306/yop?useUnicode=true&characterEncoding=utf-8";
		try (Connection connection = DriverManager.getConnection(connectionString, "root", "root")) {
			Set<Pojo> elementsWithIN     = select.execute(connection, Select.STRATEGY.IN);
			Set<Pojo> elementsWithExists = select.execute(connection, Select.STRATEGY.EXISTS);
			Set<Pojo> elements2Queries   = select.executeWithTwoQueries(connection);
			System.out.println(elementsWithIN.equals(elements2Queries));
			System.out.println(elementsWithIN.equals(elementsWithExists));


			Pojo pojo = elementsWithExists.iterator().next();
			System.out.println(pojo.toJson());
		}
	}

}
