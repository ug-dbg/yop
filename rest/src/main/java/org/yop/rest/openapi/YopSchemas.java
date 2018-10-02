package org.yop.rest.openapi;

import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Schema;
import org.yop.orm.evaluation.*;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.Where;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Delete;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI schemas for YOP query elements.
 * <ul>
 *     <li>{@link Select}</li>
 *     <li>{@link Upsert}</li>
 *     <li>{@link Delete}</li>
 *     <li>{@link IJoin}</li>
 *     <li>{@link Where}</li>
 *     <li>{@link Evaluation} and its implementations</li>
 * </ul>
 * Any constant (e.g. {@link #JOIN} in this class should be a Schema key in {@link #YOP_SCHEMAS}.
 * <br><br>
 * These schemas should be added in the OpenAPI {@link io.swagger.oas.models.Components}
 * so they can be referenced from {@link io.swagger.oas.models.Operation} using {@link Schema#$ref}.
 */
public class YopSchemas {

	public static final String SELECT = "select";
	public static final String UPSERT = "upsert";
	public static final String DELETE = "delete";

	private static final String JOIN  = "join";
	private static final String WHERE = "where";

	private static final String COMPARISON  = "comparison";
	private static final String ID_IN       = "idIn";
	private static final String IN          = "in";
	private static final String NATURAL_KEY = "naturalKey";
	private static final String OR          = "or";

	private static final String OPERATOR = "operator";

	static final Map<String, Schema> YOP_SCHEMAS = new LinkedHashMap<String, Schema>() {{
		this.put(SELECT,      selectSchema());
		this.put(UPSERT,      upsertSchema());
		this.put(DELETE,      deleteSchema());
		this.put(JOIN,        joinSchema());
		this.put(WHERE,       whereSchema());
		this.put(COMPARISON,  comparisonSchema());
		this.put(ID_IN,       idInSchema());
		this.put(IN,          inSchema());
		this.put(NATURAL_KEY, naturalKeySchema());
		this.put(OR,          orSchema());
		this.put(OPERATOR,    operatorSchema());
	}};

	private static Schema<Select> selectSchema() {
		Schema<Select> select = new Schema<>();
		select.setName(SELECT);
		select.addProperties("target", OpenAPIUtil.JSON_SCHEMAS.get(Class.class).toSchema());
		select.addProperties("joins", new ArraySchema().items(new Schema().$ref("join")));
		select.addProperties("where", new Schema().$ref("where"));
		return select;
	}

	private static Schema<Select> upsertSchema() {
		Schema<Select> upsert = new Schema<>();
		upsert.setName(UPSERT);
		upsert.addProperties("target", OpenAPIUtil.JSON_SCHEMAS.get(Class.class).toSchema());
		upsert.addProperties("joins", new ArraySchema().items(new Schema().$ref("join")));
		return upsert;
	}

	private static Schema<Select> deleteSchema() {
		Schema<Select> delete = new Schema<>();
		delete.setName(DELETE);
		delete.addProperties("target", OpenAPIUtil.JSON_SCHEMAS.get(Class.class).toSchema());
		delete.addProperties("joins", new ArraySchema().items(new Schema().$ref("join")));
		delete.addProperties("where", new Schema().$ref("where"));
		return delete;
	}

	private static Schema joinSchema() {
		Schema join = new Schema();
		join.setName(JOIN);
		join.addProperties(IJoin.FIELD, OpenAPIUtil.JSON_SCHEMAS.get(String.class).toSchema());
		join.addProperties("joins", new ArraySchema().items(new Schema().$ref("join")));
		join.addProperties("where", new Schema().$ref("where"));
		return join;
	}

	private static Schema whereSchema() {
		Schema where = new Schema();
		where.setName(WHERE);
		ComposedSchema evaluations = new ComposedSchema();
		where.addProperties("evaluations", evaluations);
		evaluations.anyOf(Arrays.asList(
			new Schema().$ref("comparison"),
			new Schema().$ref("idIn"),
			new Schema().$ref("in"),
			new Schema().$ref("naturalKey"),
			new Schema().$ref("or")
		));
		return where;
	}

	private static Schema comparisonSchema() {
		Schema comparison = new Schema();
		comparison.setName(COMPARISON);
		comparison.addProperties(Evaluation.FIELD, new Schema().type("string"));
		comparison.addProperties("operator", new Schema().$ref(OPERATOR));
		comparison.addProperties("ref", new Schema().type("string"));

		Schema<String> refType = new Schema<>();
		refType.setType("string");
		refType.setEnum(Collections.singletonList("path"));
		comparison.addProperties(Comparison.REF_TYPE, refType);
		return comparison;
	}

	private static Schema idInSchema() {
		Schema idIn = new Schema();
		idIn.setName(ID_IN);
		idIn.addProperties(IdIn.VALUES, new ArraySchema().items(new Schema().type("string")));
		return idIn;
	}

	private static Schema inSchema() {
		Schema in = new Schema();
		in.setName(IN);
		in.addProperties(In.VALUES, new ArraySchema().items(new Schema()));
		in.addProperties(Evaluation.FIELD, new Schema().type("string"));
		return in;
	}

	private static Schema naturalKeySchema() {
		Schema naturalKey = new Schema();
		naturalKey.setName(NATURAL_KEY);
		naturalKey.addProperties(NaturalKey.REFERENCE, new Schema());
		return naturalKey;
	}

	private static Schema orSchema() {
		return new ArraySchema().items(new ComposedSchema().anyOf(Arrays.asList(
			new Schema().$ref(COMPARISON),
			new Schema().$ref(ID_IN),
			new Schema().$ref(IN),
			new Schema().$ref(NATURAL_KEY),
			new Schema().$ref(OR)
		)).name(OR));
	}

	private static Schema operatorSchema() {
		Schema<Operator> operator = new Schema<>();
		operator.setName(OPERATOR);
		operator.setType("string");
		operator.setEnum(Arrays.asList(Operator.values()));
		return operator;
	}
}