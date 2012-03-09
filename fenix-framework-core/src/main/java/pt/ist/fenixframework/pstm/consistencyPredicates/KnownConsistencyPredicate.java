package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import jvstm.cps.ConsistencyCheckTransaction;
import jvstm.cps.Depended;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.FenixConsistencyCheckTransaction;
import pt.ist.fenixframework.pstm.PersistentDomainMetaClass;
import pt.ist.fenixframework.pstm.TopLevelTransaction.Pair;
import pt.ist.fenixframework.pstm.Transaction;

/**
 * A KnownConsistencyPredicate represents a ConsistencyPredicate method that is
 * already known, and has previously been initialized by the system. By storing
 * which predicates are known and by detecting which predicates exist in the
 * code, we can determine which predicates are new, and which have been removed.
 **/
@CannotUseConsistencyPredicates
public abstract class KnownConsistencyPredicate extends KnownConsistencyPredicate_Base {

    public KnownConsistencyPredicate() {
	super();
	checkFrameworkNotInitialized();
    }

    protected void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    public abstract boolean isPublic();

    public abstract boolean isPrivate();

    public abstract boolean isFinal();

    @Override
    public Method getPredicate() {
	Method predicateMethod = super.getPredicate();
	if (predicateMethod != null) {
	    predicateMethod.setAccessible(true);
	}
	return predicateMethod;
    }

    @Override
    public void setPredicate(Method predicate) {
	checkFrameworkNotInitialized();
	super.setPredicate(predicate);
    }

    @Override
    public void setPersistentDomainMetaClass(PersistentDomainMetaClass persistentDomainMetaClass) {
	checkFrameworkNotInitialized();
	super.setPersistentDomainMetaClass(persistentDomainMetaClass);
    }

    @Override
    public void removePersistentDomainMetaClass() {
	checkFrameworkNotInitialized();
	super.removePersistentDomainMetaClass();
    }

    public abstract void initKnownConsistencyPredicateOverridden();

    public abstract void updateKnownConsistencyPredicateOverridden();

    public abstract void executeConsistencyPredicateForMetaClassAndSubclasses(PersistentDomainMetaClass metaClass);

    protected void executeConsistencyPredicateForExistingDomainObjects(List<AbstractDomainObject> domainObjects) {
	checkFrameworkNotInitialized();
	if (domainObjects.isEmpty() || getPredicate() == null) {
	    return;
	}
	System.out.println("[ConsistencyPredicates] Executing startup Consistency Predicate: " + getPredicate().getName());
	for (AbstractDomainObject existingDomainObject : domainObjects) {
	    Pair pair = executePredicateForOneObject(existingDomainObject, getPredicate());
	    if (pair != null) {
		new PersistentDependenceRecord(existingDomainObject, this, (Set<Depended>) pair.first, (Boolean) pair.second);
	    }
	}
    }

    // Executes a predicate for a single object
    private static Pair executePredicateForOneObject(Object obj, Method predicate) {
	ConsistencyCheckTransaction tx = new FenixConsistencyCheckTransaction(Transaction.currentFenixTransaction(), obj);
	tx.start();

	try {
	    Boolean predicateResult = (Boolean) predicate.invoke(obj);
	    Transaction.commit();
	    return new Pair(tx.getDepended(), predicateResult);
	} catch (InvocationTargetException ite) {
	    Transaction.commit();
	    return new Pair(tx.getDepended(), false);
	} catch (Throwable t) {
	    // any other kind of throwable is an Error in the framework that should
	    // be fixed
	    throw new Error(t);
	}
    }

    /**
     * A KnownConsistencyPredicate should be deleted when the
     * ConsistencyPredicate method is removed from the code, or the containing
     * class is removed. In either case, we can delete all the associated
     * DependenceRecords.
     * 
     * This method is called when the predicate is being removed, and not the
     * class.
     **/
    public void delete() {
	classDelete();
    }

    /**
     * A KnownConsistencyPredicate should be deleted when the
     * ConsistencyPredicate method is removed from the code, or the containing
     * class is removed. In either case, we can delete all the associated
     * DependenceRecords.
     * 
     * This method is called when the predicate's class is being removed.
     **/
    public void classDelete() {
	checkFrameworkNotInitialized();
	System.out.println("[ConsistencyPredicates] Deleting predicate " + getPredicate()
		+ ((getPredicate() == null) ? " of " + getPersistentDomainMetaClass().getDomainClass() : ""));
	for (PersistentDependenceRecord dependenceRecord : getPersistentDependenceRecords()) {
	    dependenceRecord.delete();
	}
	removePersistentDomainMetaClass();
	deleteDomainObject();
    }

    public static <PredicateType extends KnownConsistencyPredicate> PredicateType readKnownConsistencyPredicate(
	    Class<? extends AbstractDomainObject> domainClass, String predicateName) {
	return (PredicateType) PersistentDomainMetaClass.readPersistentDomainMetaClass(domainClass)
		.getDeclaredConsistencyPredicate(predicateName);
    }

    public static <PredicateType extends KnownConsistencyPredicate> PredicateType readKnownConsistencyPredicate(
	    Method predicateMethod) {
	return (PredicateType) PersistentDomainMetaClass.readPersistentDomainMetaClass(
		(Class<? extends AbstractDomainObject>) predicateMethod.getDeclaringClass()).getDeclaredConsistencyPredicate(
		predicateMethod);
    }

    public static <PredicateType extends KnownConsistencyPredicate> PredicateType createNewKnownConsistencyPredicate(
	    Method predicateMethod, PersistentDomainMetaClass metaClass) {
	KnownConsistencyPredicate newKnownConsistencyPredicate;
	int methodModifiers = predicateMethod.getModifiers();
	if (Modifier.isPrivate(methodModifiers)) {
	    newKnownConsistencyPredicate = new PrivateConsistencyPredicate(predicateMethod, metaClass);
	} else if (Modifier.isFinal(methodModifiers)) {
	    newKnownConsistencyPredicate = new FinalConsistencyPredicate(predicateMethod, metaClass);
	} else {
	    newKnownConsistencyPredicate = new PublicConsistencyPredicate(predicateMethod, metaClass);
	}
	return (PredicateType) newKnownConsistencyPredicate;
    }
}
