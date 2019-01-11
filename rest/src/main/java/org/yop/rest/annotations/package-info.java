/**
 * YOP REST annotations.
 * <br>
 * Initially there only was ths @Rest annotation so the servlet could find the resources to expose.
 * <br>
 * This annotation had to be usable on either a class or a custom method to extend/override default behavior.
 * <br>
 * When annotating a custom method with @Rest, I could not rely on the parameter names or types to pass the context on.
 * Hence the other annotations (@PathParam, @RequestParam...) which are quite similar to javax.ws annotations.
 */
package org.yop.rest.annotations;