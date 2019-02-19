/**
 * Tools to serialize/deserialize data.
 * <p>
 * {@link org.yop.rest.serialize.Deserializers},
 * {@link org.yop.rest.serialize.PartialDeserializers}
 * and {@link org.yop.rest.serialize.Serializers}
 * are shells to the correct {@link org.yop.orm.query.serialize.Serialize} implementation for content types.
 * <br>
 * See also {@link org.yop.rest.serialize.MIMEParse} to find the best content-type match for a given mime type header.
 */
package org.yop.rest.serialize;