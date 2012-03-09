package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.PersistentDomainMetaClass;

/**
 * A PublicConsistencyPredicate is a KnownConsistencyPredicate that represents
 * predicate methods that are either public or protected. It can override and be
 * overridden by other PublicConsistencyPredicates.
 * 
 * Therefore, on creation, the dependence records of the overridden predicate
 * (if any) are removed from this class downward, and the new
 * PublicConsistencyPredicate is executed for all instances of the declaring
 * domain class and subclasses that do not override the predicate method.
 * Likewise, on deletion, all it's dependence records are removed, and the
 * overridden predicate (if any) is executed for all instances of the declaring
 * domain class and subclasses that do not override the predicate method.
 **/
@CannotUseConsistencyPredicates
public class PublicConsistencyPredicate extends PublicConsistencyPredicate_Base {

    public PublicConsistencyPredicate() {
	super();
    }

    public PublicConsistencyPredicate(Method predicateMethod, PersistentDomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setPersistentDomainMetaClass(metaClass);
	System.out.println("[ConsistencyPredicates] Created a " + getClass().getSimpleName() + " for " + getPredicate());
    }

    @Override
    public boolean isPublic() {
	return true;
    }

    @Override
    public boolean isPrivate() {
	return false;
    }

    @Override
    public boolean isFinal() {
	return false;
    }

    @Override
    public void setPublicConsistencyPredicateOverridden(PublicConsistencyPredicate publicConsistencyPredicateOverridden) {
	checkFrameworkNotInitialized();
	super.setPublicConsistencyPredicateOverridden(publicConsistencyPredicateOverridden);
    }

    @Override
    public void removePublicConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
	super.removePublicConsistencyPredicateOverridden();
    }

    @Override
    public void addPublicConsistencyPredicatesOverriding(PublicConsistencyPredicate publicConsistencyPredicatesOverriding) {
	checkFrameworkNotInitialized();
	super.addPublicConsistencyPredicatesOverriding(publicConsistencyPredicatesOverriding);
    }

    @Override
    public void removePublicConsistencyPredicatesOverriding(PublicConsistencyPredicate publicConsistencyPredicatesOverriding) {
	checkFrameworkNotInitialized();
	super.removePublicConsistencyPredicatesOverriding(publicConsistencyPredicatesOverriding);
    }

    @Override
    public void initKnownConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
	PublicConsistencyPredicate overriddenPredicate = findOverriddenPredicate();
	if (overriddenPredicate == null) {
	    return;
	}

	overriddenPredicate.removePersistentDependenceRecordsForMetaClassAndSubclasses(getPersistentDomainMetaClass());
	setPublicConsistencyPredicateOverridden(overriddenPredicate);

	System.out.println("[ConsistencyPredicates] Initializing overridden predicate of " + getPredicate() + " to "
		+ overriddenPredicate.getPredicate());
    }

    private void removePersistentDependenceRecordsForMetaClassAndSubclasses(PersistentDomainMetaClass metaClass) {
	removePersistentDependenceRecordsForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (PersistentDomainMetaClass metaSubclass : metaClass.getPersistentDomainMetaSubclasses()) {
	    removePersistentDependenceRecordsForMetaClassAndSubclasses(metaSubclass);
	}
    }

    private void removePersistentDependenceRecordsForExistingDomainObjects(List<AbstractDomainObject> existingObjects) {
	for (PersistentDependenceRecord dependenceRecord : getPersistentDependenceRecords()) {
	    if (existingObjects.contains(dependenceRecord.getDependent())) {
		dependenceRecord.delete();
	    }
	}
    }

    @Override
    public void updateKnownConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
	PublicConsistencyPredicate overriddenPredicate = findOverriddenPredicate();
	if (overriddenPredicate == getPublicConsistencyPredicateOverridden()) {
	    return;
	}
	setPublicConsistencyPredicateOverridden(overriddenPredicate);

	System.out.println("[ConsistencyPredicates] Updating overridden predicate of " + getPredicate() + " to "
		+ ((overriddenPredicate == null) ? "null" : overriddenPredicate.getPredicate()));
    }

    private PublicConsistencyPredicate findOverriddenPredicate() {
	PersistentDomainMetaClass metaSuperclass = getPersistentDomainMetaClass().getPersistentDomainMetaSuperclass();
	while (metaSuperclass != null) {
	    Method overriddenMethod = null;
	    try {
		overriddenMethod = metaSuperclass.getDomainClass().getDeclaredMethod(getPredicate().getName());
	    } catch (NoSuchMethodException e) {
		metaSuperclass = metaSuperclass.getPersistentDomainMetaSuperclass();
		continue;
	    }
	    if (!overriddenMethod.isAnnotationPresent(ConsistencyPredicate.class)
		    && !overriddenMethod.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class)) {
		return null;
	    }
	    if (Modifier.isPrivate(overriddenMethod.getModifiers())) {
		return null;
	    }

	    return KnownConsistencyPredicate.readKnownConsistencyPredicate(overriddenMethod);
	}
	return null;
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(PersistentDomainMetaClass metaClass) {
	if (metaClass == getPersistentDomainMetaClass()) {
	    // The metaClass is this very predicate's declaring class, so it is not a subclass yet.
	    executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());
	} else {
	    try {
		metaClass.getDomainClass().getDeclaredMethod(getPredicate().getName());
		// The method is being overridden from this class downward, so stop and don't search subclasses.
		return;
	    } catch (NoSuchMethodException e) {
		// The method is not being overridden here, so proceed with the execution.
		executeConsistencyPredicateForExistingDomainObjects(metaClass.getExistingDomainObjects());
	    }
	}

	for (PersistentDomainMetaClass metaSubclass : metaClass.getPersistentDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }

    @Override
    public void delete() {
	PublicConsistencyPredicate overriddenPredicate = getPublicConsistencyPredicateOverridden();
	if (overriddenPredicate != null) {
	    overriddenPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(getPersistentDomainMetaClass());

	    for (PublicConsistencyPredicate predicatesOverriding : getPublicConsistencyPredicatesOverriding()) {
		predicatesOverriding.setPublicConsistencyPredicateOverridden(overriddenPredicate);
	    }
	}

	classDelete();
    }

    @Override
    public void classDelete() {
	for (PublicConsistencyPredicate predicatesOverriding : getPublicConsistencyPredicatesOverriding()) {
	    removePublicConsistencyPredicatesOverriding(predicatesOverriding);
	}
	removePublicConsistencyPredicateOverridden();

	super.classDelete();
    }
}
