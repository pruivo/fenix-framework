package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.DomainMetaClass;

/**
 * A PublicConsistencyPredicate is a DomainConsistencyPredicate that represents
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
@NoDomainMetaData
public class PublicConsistencyPredicate extends PublicConsistencyPredicate_Base {

    public PublicConsistencyPredicate() {
	super();
    }

    public PublicConsistencyPredicate(Method predicateMethod, DomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setDomainMetaClass(metaClass);
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
    public void initConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
	PublicConsistencyPredicate overriddenPredicate = findOverriddenPredicate();
	if (overriddenPredicate == null) {
	    return;
	}

	overriddenPredicate.removeDomainDependenceRecordsForMetaClassAndSubclasses(getDomainMetaClass());
	setPublicConsistencyPredicateOverridden(overriddenPredicate);

	System.out.println("[ConsistencyPredicates] Initializing overridden predicate of " + getPredicate() + " to "
		+ overriddenPredicate.getPredicate());
    }

    private void removeDomainDependenceRecordsForMetaClassAndSubclasses(DomainMetaClass metaClass) {
	removeDomainDependenceRecordsForExistingDomainObjects(metaClass.getExistingDomainObjects());

	for (DomainMetaClass metaSubclass : metaClass.getDomainMetaSubclasses()) {
	    removeDomainDependenceRecordsForMetaClassAndSubclasses(metaSubclass);
	}
    }

    private void removeDomainDependenceRecordsForExistingDomainObjects(List<AbstractDomainObject> existingObjects) {
	for (DomainDependenceRecord dependenceRecord : getDomainDependenceRecords()) {
	    if (existingObjects.contains(dependenceRecord.getDependent())) {
		dependenceRecord.delete();
	    }
	}
    }

    @Override
    public void updateConsistencyPredicateOverridden() {
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
	DomainMetaClass metaSuperclass = getDomainMetaClass().getDomainMetaSuperclass();
	while (metaSuperclass != null) {
	    Method overriddenMethod = null;
	    try {
		overriddenMethod = metaSuperclass.getDomainClass().getDeclaredMethod(getPredicate().getName());
	    } catch (NoSuchMethodException e) {
		metaSuperclass = metaSuperclass.getDomainMetaSuperclass();
		continue;
	    }
	    if (!overriddenMethod.isAnnotationPresent(ConsistencyPredicate.class)
		    && !overriddenMethod.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class)) {
		return null;
	    }
	    if (Modifier.isPrivate(overriddenMethod.getModifiers())) {
		return null;
	    }

	    return DomainConsistencyPredicate.readDomainConsistencyPredicate(overriddenMethod);
	}
	return null;
    }

    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(DomainMetaClass metaClass) {
	if (metaClass == getDomainMetaClass()) {
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

	for (DomainMetaClass metaSubclass : metaClass.getDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }

    @Override
    public void delete() {
	PublicConsistencyPredicate overriddenPredicate = getPublicConsistencyPredicateOverridden();
	if (overriddenPredicate != null) {
	    overriddenPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(getDomainMetaClass());

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
