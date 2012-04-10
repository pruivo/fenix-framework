package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;

import pt.ist.fenixframework.pstm.DomainMetaClass;

/**
 * A FinalConsistencyPredicate is a PublicConsistencyPredicate that represents
 * predicate methods that are either public or protected, and are final. It can
 * override, but cannot be overridden by other PublicConsistencyPredicates.
 * 
 * Therefore, on creation, unlike the PublicConsistencyPredicate, the execution
 * of the new PublicConsistencyPredicate does not need to check subclasses for
 * overriding methods.
 **/
@NoDomainMetaData
public class FinalConsistencyPredicate extends FinalConsistencyPredicate_Base {

    public FinalConsistencyPredicate(Method predicateMethod, DomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setDomainMetaClass(metaClass);
	System.out.println("[ConsistencyPredicates] Created a " + getClass().getSimpleName() + " for " + getPredicate());
    }

    @Override
    public boolean isFinal() {
	return true;
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(DomainMetaClass metaClass) {
	executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (DomainMetaClass metaSubclass : metaClass.getDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }
}
