package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;

import pt.ist.fenixframework.pstm.DomainMetaClass;

/**
 * A PrivateConsistencyPredicate is a {@link DomainConsistencyPredicate} that
 * represents predicate methods that are private. It can neither override, nor
 * be overridden by other consistency predicates.
 * 
 * Therefore, on creation, the new PrivateConsistencyPredicate is executed for
 * all instances of the declaring domain class and subclasses. Likewise, on
 * deletion, all it's dependence records are removed.
 **/
@NoDomainMetaData
public class PrivateConsistencyPredicate extends PrivateConsistencyPredicate_Base {

    public PrivateConsistencyPredicate(Method predicateMethod, DomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setDomainMetaClass(metaClass);
	System.out.println("[ConsistencyPredicates] Created a " + getClass().getSimpleName() + " for " + getPredicate());
    }

    @Override
    public boolean isPublic() {
	return false;
    }

    @Override
    public boolean isPrivate() {
	return true;
    }

    @Override
    public boolean isFinal() {
	return false;
    }

    @Override
    public void initConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    @Override
    public void updateConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(DomainMetaClass metaClass) {
	executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (DomainMetaClass metaSubclass : metaClass.getDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }
}
