package org.yop.orm.sql;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.exception.YopIncoherentQueryException;
import org.yop.orm.exception.YopRuntimeException;

/**
 * Testing {@link SQLExpression}.
 */
public class SQLExpressionTest {

	@Test
	public void test_ConstructorWithNoParameter() {
		String sql = "SELECT * FROM USERS";
		SQLExpression select = new SQLExpression(sql);
		Assert.assertEquals(0, select.getParameters().size());
		Assert.assertTrue(select.isCoherent());
		Assert.assertEquals(sql.length(), select.length());
		Assert.assertEquals(sql.charAt(5), select.charAt(5));
		Assert.assertEquals(sql, select.toString());
	}

	@Test(expected = YopIncoherentQueryException.class)
	public void test_ConstructorWithNotMatchedParameter() {
		new SQLExpression("SELECT * FROM USERS WHERE ID = ?");
	}

	@Test
	public void test_ConstructorWithSingleParameter() {
		SQLExpression select = new SQLExpression(
			"SELECT * FROM USERS WHERE ID = ?", "ID", 1, null, false, Config.DEFAULT
		);
		Assert.assertTrue(select.isCoherent());
		Assert.assertEquals(1, select.getParameters().size());
	}

	@Test(expected = YopIncoherentQueryException.class)
	public void test_ConstructorWithNotMatchedSingleParameter() {
		new SQLExpression("SELECT * FROM USERS WHERE ID = ? AND NAME = ?", "ID", 1, null, false, Config.DEFAULT);
	}

	@Test
	public void test_ConstructorWithSeveralParameters() {
		SQLExpression select = new SQLExpression(
			"SELECT * FROM USERS WHERE ID = ? AND NAME = ?",
			new Parameters().addParameter("ID", () -> 1).addParameter("NAME", () -> "ADMIN")
		);
		Assert.assertTrue(select.isCoherent());
		Assert.assertEquals(2, select.getParameters().size());
	}

	@Test(expected = YopIncoherentQueryException.class)
	public void test_ConstructorWithNotMatchedSeveralParameters() {
		new SQLExpression(
			"SELECT * FROM USERS WHERE ID = ? AND NAME = ?",
			new Parameters().addParameter("ID", () -> 1)
		);
	}

	@Test
	public void test_append() {
		SQLExpression select = new SQLExpression("SELECT * FROM USERS")
			.append("WHERE")
			.append("ID =")
			.append(SQLExpression.parameter("ID", () -> 1))
			.append("AND")
			.append("NAME =")
			.append(SQLExpression.parameter("NAME", () -> "ADMIN"));
		Assert.assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ?", select.toString());
		Assert.assertEquals(2, select.getParameters().size());
	}

	@Test(expected = YopRuntimeException.class)
	public void test_appendBadParameter() {
		new SQLExpression("SELECT * FROM USERS").append("WHERE").append("ID = ?");
	}

	@Test
	public void test_subSequence() {
		SQLExpression select = new SQLExpression("SELECT * FROM USERS")
			.append("WHERE")
			.append("ID =")
			.append(SQLExpression.parameter("ID", () -> 1))
			.append("AND")
			.append("NAME =")
			.append(SQLExpression.parameter("NAME", () -> "ADMIN"));

		SQLExpression sub = select.subSequence(0, 19);
		Assert.assertEquals("SELECT * FROM USERS", sub.toString());
		Assert.assertEquals(0, sub.getParameters().size());

		sub = select.subSequence(0, 32);
		Assert.assertEquals("SELECT * FROM USERS WHERE ID = ?", sub.toString());
		Assert.assertEquals(1, sub.getParameters().size());

		sub = select.subSequence(20, 32);
		Assert.assertEquals("WHERE ID = ?", sub.toString());
		Assert.assertEquals(1, sub.getParameters().size());
	}

	@Test
	public void test_forPattern() {
		String pattern = "SELECT {:columns} FROM {:table} WHERE {:col} = {:col_value}";
		String columns = "ID, NAME, ACTIVE";
		String table = "USERS";
		String col = "ID";
		SQLExpression value = SQLExpression.parameter("ID", () -> 1);

		SQLExpression select = SQLExpression.forPattern(pattern, columns, table, col, value);
		Assert.assertEquals("SELECT ID, NAME, ACTIVE FROM USERS WHERE ID = ?", select.toString());
	}

	@Test(expected = YopRuntimeException.class)
	public void test_forPatternTooFewParts() {
		String pattern = "SELECT {:columns} FROM {:table} WHERE {:col} = {:col_value} AND {:col} = {:col_value}";
		String columns = "ID, NAME, ACTIVE";
		String table = "USERS";
		String col = "ID";
		SQLExpression value = SQLExpression.parameter("ID", () -> 1);

		try {
			SQLExpression.forPattern(pattern, columns, table, col, value);
			Assert.fail("A YopRuntimeException exception should have been raised");
		} catch (YopRuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("#[4]"));
			Assert.assertTrue(e.getMessage().contains("{:col}"));
			throw e;
		}
	}

	@Test(expected = YopRuntimeException.class)
	public void test_forPatternTooManyParts() {
		String pattern = "SELECT {:columns} FROM {:table} WHERE ID = 1";
		String columns = "ID, NAME, ACTIVE";
		String table = "USERS";
		String col = "ID";
		SQLExpression value = SQLExpression.parameter("ID", () -> 1);

		try {
			SQLExpression.forPattern(pattern, columns, table, col, value);
			Assert.fail("A YopRuntimeException exception should have been raised");
		} catch (YopRuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("#[2]"));
			throw e;
		}
	}
}
