package org.yop.rest.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import io.swagger.oas.annotations.responses.ApiResponse;
import io.swagger.oas.models.ExternalDocumentation;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.Paths;
import io.swagger.oas.models.info.Contact;
import io.swagger.oas.models.info.Info;
import io.swagger.oas.models.info.License;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.YopOpenAPIException;
import org.yop.rest.servlet.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * An utility class to map @Rest Yopables resources to an OpenAPI description.
 * <br>
 * See <a href="https://github.com/OAI/OpenAPI-Specification">https://github.com/OAI/OpenAPI-Specification</a>.
 * <br><br>
 * OpenAPI annotations ({@link io.swagger.oas.annotations} can be used onto the @Rest Yopables.
 * <br>
 * This class tries to figure out the most accurate description from both {@link Rest} annotations and OpenAPI ones.
 */
public class OpenAPIUtil {

	private static final Logger logger = LoggerFactory.getLogger(OpenAPIUtil.class);

	/**
	 * Rough java type → JSON schema type/format equivalents.
	 */
	private static final Map<Class, SchemaModel> JSON_SCHEMAS = new HashMap<Class, SchemaModel>() {{
		this.put(Integer.class,            new SchemaModel("integer"));
		this.put(Long.class,               new SchemaModel("integer"));
		this.put(BigInteger.class,         new SchemaModel("integer"));
		this.put(Float.class,              new SchemaModel("number", "float"));
		this.put(Double.class,             new SchemaModel("number", "double"));
		this.put(BigDecimal.class,         new SchemaModel("number", "double"));
		this.put(Character.class,          new SchemaModel("string"));
		this.put(String.class,             new SchemaModel("string"));
		this.put(Date.class,               new SchemaModel("string", "date"));
		this.put(java.sql.Date.class,      new SchemaModel("string", "date"));
		this.put(java.sql.Time.class,      new SchemaModel("string", "time"));
		this.put(java.sql.Timestamp.class, new SchemaModel("string", "date-time"));
		this.put(LocalDateTime.class,      new SchemaModel("string", "date-time"));
		this.put(LocalDate.class,          new SchemaModel("string", "date"));
		this.put(ZonedDateTime.class,      new SchemaModel("string", "date-time"));
		this.put(LocalTime.class,          new SchemaModel("string", "time"));
		this.put(Void.class,               new SchemaModel("string"));
	}};

	/**
	 * Generate an OpenAPI model from the given Yopables.
	 * <br>
	 * Both default behavior (GET/POST/PUT/HEAD/DELETE) and custom (custom @Rest methods) are inserted in the model.
	 * @param yopables the REST Yopables resources
	 * @return an OpenAPI model populated with the Yopables annotated documentation.
	 */
	public static OpenAPI generate(Collection<Class<? extends Yopable>> yopables) {
		OpenAPI api = new OpenAPI();
		Info info = new Info();
		info.setTitle("Yop unrestful REST API");
		info.setDescription("Yop unrestful REST API - default behavior. See http://maven.y-op.org");
		Contact contact = new Contact();
		contact.setEmail("dev@null.me");
		contact.setName("Dev Null");
		info.setContact(contact);
		info.setVersion("1");
		License license = new License();
		license.setName("Postcard Public License");
		info.setLicense(license);
		api.setInfo(info);

		ExternalDocumentation documentation = new ExternalDocumentation();
		documentation.setUrl("http://maven.y-op.org/");
		documentation.setDescription("YOP default documentation");
		api.setExternalDocs(documentation);

		api.setTags(new ArrayList<>());
		api.setPaths(new Paths());
		yopables.forEach(y -> addResourceDefaultBehavior(y, api));
		yopables.forEach(y -> addResourceCustomBehavior(y, api));
		return api;
	}

	/**
	 * Convert the OpenAPI model into a YAML String representation, using {@link com.fasterxml.jackson}
	 * @param api the OpenAPI model to convert
	 * @return a YAML String representation of the API.
	 */
	public static String toYAML(OpenAPI api) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		try {
			return mapper.writeValueAsString(api);
		} catch (JsonProcessingException e) {
			throw new YopOpenAPIException(
				"Could not convert Open API object to YAML [" + Objects.toString(api) + "]",
				e
			);
		}
	}

	/**
	 * Create an OpenAPI {@link Schema} object from a {@link Yopable} type.
	 * <br>
	 * This is done by recursively iterating on the yopable fields.
	 * <br>
	 * See {@link #forColumnField(Field)} when the field is
	 * neither a {@link Yopable} nor a collection of {@link Yopable}.
	 * @param clazz the Yopable type
	 * @return the generated schema for this type. Null if the type is not a yopable
	 */
	public static Schema<?> forResource(Class<? extends Yopable> clazz) {
		if (Yopable.class.isAssignableFrom(clazz)) {
			Schema<?> schema = new Schema<>().properties(new HashMap<>());
			List<Field> fields = Reflection.getFields(clazz, true);
			for (Field field : fields) {
				Schema property;
				if (ORMUtil.isCollection(field)) {
					property = new ArraySchema().items(forResource(Reflection.getTarget(field)));
				} else if (ORMUtil.isYopable(field)) {
					property = forResource(Reflection.getTarget(field));
				} else {
					property = forColumnField(field);
				}
				schema.getProperties().put(field.getName(), property);
			}
			return schema;
		}
		return null;
	}

	/**
	 * Create an OpenAPI {@link Schema} object from the given Field.
	 * <br>
	 * @param field the target field.
	 *              Should be a @Column field.
	 *              Neither a {@link Yopable} nor collection of {@link Yopable}
	 * @return the generated schema for the type
	 */
	private static Schema forColumnField(Field field) {
		Class<?> fieldType = field.getType();
		BigDecimal minValue = null;
		boolean nullable = true;
		Integer maxLength = null;
		if (ORMUtil.isIdField(field)) {
			minValue = new BigDecimal(1);
		} else {
			nullable = ! ORMUtil.isColumnNotNullable(field);
			maxLength = ORMUtil.getColumnLength(field);
		}

		return JSON_SCHEMAS
			.getOrDefault(fieldType, JSON_SCHEMAS.get(Void.class))
			.toSchema()
			.nullable(nullable)
			.minimum(minValue)
			.maxLength(maxLength);
	}

	/**
	 * Add the YOP REST default behavior (GET/POST/PUT/DELETE/HEAD) into the target OpenAPI.
	 * <br>
	 * The REST resource path is read from {@link Rest#path()}.
	 * <br><br>
	 * Default behavior is :
	 * <ul>
	 *  <li> GET    /path : get all resource elements </li>
	 *  <li> HEAD   /path : get all resource elements, no content returned, only set content length </li>
	 *  <li> POST   /path : execute a custom YOP query for the resource type</li>
	 *  <li> PUT    /path : upsert an array of resource elements</li>
	 *  <li> DELETE /path : delete all resource elements</li>
	 *  <li> GET    /path/{id} : get resource element by ID</li>
	 *  <li> HEAD   /path/{id} : get resource element by ID, no content returned, only set content length</li>
	 *  <li> DELETE /path/{id} : delete resource element by ID</li>
	 * </ul>
	 * @param yopable the yopable resource type
	 * @param api the target OpenAPI object
	 */
	private static void addResourceDefaultBehavior(Class<? extends Yopable> yopable, OpenAPI api) {
		Rest rest = yopable.getAnnotation(Rest.class);
		if (rest == null) {
			return;
		}

		String resource = yopable.getSimpleName();
		List<String> tags = Collections.singletonList(resource);
		String path = java.nio.file.Paths.get("/", rest.path()).toString();
		PathItem item = new PathItem();
		item.setSummary("YOP default REST operations for [" + resource + "]");
		item.setDescription(rest.description());
		item.post(HttpMethod.instance("POST").openAPIDefaultModel(yopable)).getPost().setTags(tags);
		item.put(HttpMethod.instance("PUT").openAPIDefaultModel(yopable)).getPut().setTags(tags);
		item.get(HttpMethod.instance("GET").openAPIDefaultModel(yopable)).getGet().setTags(tags);
		item.delete(HttpMethod.instance("DELETE").openAPIDefaultModel(yopable)).getDelete().setTags(tags);
		item.head(HttpMethod.instance("HEAD").openAPIDefaultModel(yopable)).getHead().setTags(tags);
		api.getPaths().addPathItem(path, item);

		path = path + "/{id}";
		item = new PathItem();
		item.setSummary("YOP default REST operations for [" + resource + "] for a target ID");
		item.get(HttpMethod.instance("GET").openAPIDefaultModel(yopable)).getGet().setTags(tags);
		item.delete(HttpMethod.instance("DELETE").openAPIDefaultModel(yopable)).getDelete().setTags(tags);
		item.head(HttpMethod.instance("HEAD").openAPIDefaultModel(yopable)).getHead().setTags(tags);
		item.getGet().getParameters().add(HttpMethod.idParameter(resource));
		item.getDelete().getParameters().add(HttpMethod.idParameter(resource));
		item.getHead().getParameters().add(HttpMethod.idParameter(resource));
		api.getPaths().addPathItem(path, item);

		api.getTags().add(new Tag().name(resource).description("Resource : " + resource));
	}

	/**
	 * Add the YOP REST custom behavior into the target OpenAPI.
	 * <br>
	 * The REST resource path is read from {@link Rest#path()}.
	 * <br>
	 * The custom behavior is any specific @Rest method in the target resource.
	 * <br>
	 * If swagger annotations are present ({@link Operation}, {@link ApiResponse}...) we try to use them.
	 * @param yopable the yopable resource type
	 * @param api the target OpenAPI object
	 */
	private static void addResourceCustomBehavior(Class<? extends Yopable> yopable, OpenAPI api) {
		String resource = yopable.getSimpleName();
		List<String> tags = Collections.singletonList(resource);
		Rest classAnnotation = yopable.getAnnotation(Rest.class);
		Set<Method> methods = Reflection.getMethods(yopable);
		for (Method method : methods) {
			if (!method.isAnnotationPresent(Rest.class)) {
				continue;
			}
			Rest methodAnnotation = method.getAnnotation(Rest.class);
			String path = java.nio.file.Paths.get("/", classAnnotation.path(), methodAnnotation.path()).toString();
			String description = methodAnnotation.description();
			Set<String> httpMethods = new HashSet<>(Arrays.asList(methodAnnotation.methods()));
			if (httpMethods.isEmpty()) {
				httpMethods.add("GET");
			}
			String summary = methodAnnotation.summary();

			if (method.isAnnotationPresent(Operation.class)) {
				Operation operationAnnotation = method.getAnnotation(Operation.class);
				description = operationAnnotation.description();
				summary = operationAnnotation.summary();
				httpMethods.add(operationAnnotation.method());
			}

			ApiResponses responses = new ApiResponses();
			if (method.isAnnotationPresent(ApiResponse.class)) {
				ApiResponse responseAnnotation = method.getAnnotation(ApiResponse.class);
				responses.put(
					responseAnnotation.responseCode(),
					fromAnnotation(responseAnnotation, io.swagger.oas.models.responses.ApiResponse.class)
				);
			}

			for (String httpMethod : httpMethods) {
				api.getPaths().putIfAbsent(path, new PathItem());
				PathItem item = api.getPaths().get(path);
				io.swagger.oas.models.Operation operation = new io.swagger.oas.models.Operation();
				operation.setDescription(description);
				operation.setSummary(summary);
				operation.setResponses(responses);
				operation.setTags(tags);

				if (method.isAnnotationPresent(Parameter.class)) {
					operation
						.parameters(new ArrayList<>())
						.getParameters()
						.add(fromAnnotation(
							method.getAnnotation(Parameter.class),
							io.swagger.oas.models.parameters.Parameter.class)
						);
				}

				item.operation(PathItem.HttpMethod.valueOf(StringUtils.upperCase(httpMethod)), operation);
			}
		}
	}

	/**
	 * Try to convert an OpenAPI annotation into a given OpenAPI model.
	 * <br>
	 * Find the target model setters that matches the annotation methods.
	 * @param annotation the OpenAPI annotation instance
	 * @param target the target OpenAPI model type
	 * @param <T> the target OpenAPI model type
	 * @param <A> the source OpenAPI annotation type
	 * @return a new instance of the target model with the annotation data, null if annotation is null or bad target.
	 */
	private static <T, A extends Annotation> T fromAnnotation(A annotation, Class<T> target) {
		if (annotation == null) {
			return null;
		}

		T onto;
		try {
			onto = Reflection.newInstanceNoArgs(target);
		} catch (YopRuntimeException e) {
			logger.debug("Not an instantiable OpenAPI class [{}]", target.getName());
			return null;
		}

		for (Method method : annotation.annotationType().getDeclaredMethods()) {
			Method set = Reflection.getMethod(
				onto.getClass(),
				"set" + StringUtils.capitalize(method.getName()),
				method.getReturnType()
			);
			if (set != null) {
				try {
					set.invoke(onto, method.invoke(annotation));
				} catch (ReflectiveOperationException | RuntimeException e) {
					logger.warn(
						"Could not map Annotation [{}] to [{}] for method [{}]",
						annotation,
						onto.getClass().getName(),
						method.getName()
					);
				}
			}
		}
		return onto;
	}

	/**
	 * A very light schema model for a given type/format.
	 * <br>
	 * See {@link #toSchema()} to generate an OpenAPI schema instance from the model.
	 */
	private static class SchemaModel {
		private String type;
		private String format;

		private SchemaModel(String type) {
			this.type = type;
		}

		private SchemaModel(String type, String format) {
			this.type = type;
			this.format = format;
		}

		private Schema toSchema() {
			return new Schema().type(this.type).format(this.format);
		}
	}
}
