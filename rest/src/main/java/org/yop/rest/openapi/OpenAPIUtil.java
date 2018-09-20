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
import io.swagger.oas.models.responses.ApiResponses;
import io.swagger.oas.models.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.YopOpenAPIException;
import org.yop.rest.servlet.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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
		item.get(HttpMethod.instance("GET").openAPIDefaultModel(resource)).getGet().setTags(tags);
		item.post(HttpMethod.instance("POST").openAPIDefaultModel(resource)).getPost().setTags(tags);
		item.put(HttpMethod.instance("PUT").openAPIDefaultModel(resource)).getPut().setTags(tags);
		item.delete(HttpMethod.instance("DELETE").openAPIDefaultModel(resource)).getDelete().setTags(tags);
		item.head(HttpMethod.instance("HEAD").openAPIDefaultModel(resource)).getHead().setTags(tags);

		api.getPaths().addPathItem(path, item);
		api.getTags().add(new Tag().name(resource).description("Resource : " + resource));
	}

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
	@SuppressWarnings("unchecked")
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
}
