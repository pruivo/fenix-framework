package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;

import pt.ist.fenixframework.pstm.PersistentDomainMetaClass;

/**
 * A FinalConsistencyPredicate is a PublicConsistencyPredicate that represents
 * predicate methods that are either public or protected, and are final. It can
 * override, but cannot be overridden by other PublicConsistencyPredicates.
 * 
 * Therefore, on creation, unlike the PublicConsistencyPredicate, the execution
 * of the new PublicConsistencyPredicate does not need to check subclasses for
 * overriding methods.
 **/
@CannotUseConsistencyPredicates
public class FinalConsistencyPredicate extends FinalConsistencyPredicate_Base {

    public FinalConsistencyPredicate(Method predicateMethod, PersistentDomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setPersistentDomainMetaClass(metaClass);
	System.out.println("[ConsistencyPredicates] Created a " + getClass().getSimpleName() + " for " + getPredicate());
    }

    @Override
    public boolean isFinal() {
	return true;
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(PersistentDomainMetaClass metaClass) {
	executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (PersistentDomainMetaClass metaSubclass : metaClass.getPersistentDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }
}
