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
import pt.ist.fenixframework.pstm.DomainMetaClass;
import pt.ist.fenixframework.pstm.FenixConsistencyCheckTransaction;
import pt.ist.fenixframework.pstm.TopLevelTransaction.Pair;
import pt.ist.fenixframework.pstm.Transaction;

/**
 * A DomainConsistencyPredicate represents a ConsistencyPredicate method that is
 * already known, and has previously been initialized by the system. By storing
 * which predicates are known and by detecting which predicates exist in the
 * code, we can determine which predicates are new, and which have been removed.
 **/
@NoDomainMetaData
public abstract class DomainConsistencyPredicate extends DomainConsistencyPredicate_Base {

    public DomainConsistencyPredicate() {
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
    public void setDomainMetaClass(DomainMetaClass domainMetaClass) {
	checkFrameworkNotInitialized();
	super.setDomainMetaClass(domainMetaClass);
    }

    @Override
    public void removeDomainMetaClass() {
	checkFrameworkNotInitialized();
	super.removeDomainMetaClass();
    }

    public abstract void initConsistencyPredicateOverridden();

    public abstract void updateConsistencyPredicateOverridden();

    public abstract void executeConsistencyPredicateForMetaClassAndSubclasses(DomainMetaClass metaClass);

    protected void executeConsistencyPredicateForExistingDomainObjects(List<AbstractDomainObject> domainObjects) {
	checkFrameworkNotInitialized();
	if (domainObjects.isEmpty() || getPredicate() == null) {
	    return;
	}
	System.out.println("[ConsistencyPredicates] Executing startup Consistency Predicate: " + getPredicate().getName());
	for (AbstractDomainObject existingDomainObject : domainObjects) {
	    Pair pair = executePredicateForOneObject(existingDomainObject, getPredicate());
	    if (pair != null) {
		new DomainDependenceRecord(existingDomainObject, this, (Set<Depended>) pair.first, (Boolean) pair.second);
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
     * A DomainConsistencyPredicate should be deleted when the
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
     * A DomainConsistencyPredicate should be deleted when the
     * ConsistencyPredicate method is removed from the code, or the containing
     * class is removed. In either case, we can delete all the associated
     * DependenceRecords.
     * 
     * This method is called when the predicate's class is being removed.
     **/
    public void classDelete() {
	checkFrameworkNotInitialized();
	System.out.println("[ConsistencyPredicates] Deleting predicate " + getPredicate()
		+ ((getPredicate() == null) ? " of " + getDomainMetaClass().getDomainClass() : ""));
	for (DomainDependenceRecord dependenceRecord : getDomainDependenceRecords()) {
	    dependenceRecord.delete();
	}
	removeDomainMetaClass();
	deleteDomainObject();
    }

    public static <PredicateType extends DomainConsistencyPredicate> PredicateType readDomainConsistencyPredicate(
	    Class<? extends AbstractDomainObject> domainClass, String predicateName) {
	return (PredicateType) DomainMetaClass.readDomainMetaClass(domainClass)
		.getDeclaredConsistencyPredicate(predicateName);
    }

    public static <PredicateType extends DomainConsistencyPredicate> PredicateType readDomainConsistencyPredicate(
	    Method predicateMethod) {
	return (PredicateType) DomainMetaClass.readDomainMetaClass(
		(Class<? extends AbstractDomainObject>) predicateMethod.getDeclaringClass()).getDeclaredConsistencyPredicate(
		predicateMethod);
    }

    public static <PredicateType extends DomainConsistencyPredicate> PredicateType createNewDomainConsistencyPredicate(
	    Method predicateMethod, DomainMetaClass metaClass) {
	DomainConsistencyPredicate newDomainConsistencyPredicate;
	int methodModifiers = predicateMethod.getModifiers();
	if (Modifier.isPrivate(methodModifiers)) {
	    newDomainConsistencyPredicate = new PrivateConsistencyPredicate(predicateMethod, metaClass);
	} else if (Modifier.isFinal(methodModifiers)) {
	    newDomainConsistencyPredicate = new FinalConsistencyPredicate(predicateMethod, metaClass);
	} else {
	    newDomainConsistencyPredicate = new PublicConsistencyPredicate(predicateMethod, metaClass);
	}
	return (PredicateType) newDomainConsistencyPredicate;
    }
}
