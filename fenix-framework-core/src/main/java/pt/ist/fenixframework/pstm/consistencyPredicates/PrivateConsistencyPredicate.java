package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;

import pt.ist.fenixframework.pstm.PersistentDomainMetaClass;

/**
 * A PrivateConsistencyPredicate is a KnownConsistencyPredicate that represents
 * predicate methods that are private. It can neither override, nor be
 * overridden other KnownConsistencyPredicates.
 * 
 * Therefore, on creation, the new PrivateConsistencyPredicate is executed for
 * all instances of the declaring domain class and subclasses. Likewise, on
 * deletion, all it's dependence records are removed.
 **/
@CannotUseConsistencyPredicates
public class PrivateConsistencyPredicate extends PrivateConsistencyPredicate_Base {

    public PrivateConsistencyPredicate(Method predicateMethod, PersistentDomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setPersistentDomainMetaClass(metaClass);
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
    public void initKnownConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    @Override
    public void updateKnownConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(PersistentDomainMetaClass metaClass) {
	executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (PersistentDomainMetaClass metaSubclass : metaClass.getPersistentDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }
}
