package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jvstm.cps.ConsistencyException;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConsistencyPredicate {
    Class<? extends ConsistencyException> value() default ConsistencyException.class;

    boolean inconsistencyTolerant() default false;
}