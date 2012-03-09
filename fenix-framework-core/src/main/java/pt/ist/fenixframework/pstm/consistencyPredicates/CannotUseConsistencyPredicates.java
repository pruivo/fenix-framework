package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * THIS ANNOTATION IS FOR INTERNAL USE OF THE FENIX-FRAMEWORK ONLY.
 * 
 * Annotates a class that cannot define any consistency predicates. The
 * framework will not create meta classes or meta objects for domain classes
 * with this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CannotUseConsistencyPredicates {
}