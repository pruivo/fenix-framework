package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import jvstm.cps.Depended;
import jvstm.cps.DependenceRecord;
import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.PersistentDomainMetaObject;

/**
 * A PersistentDependenceRecord represents the result of the execution of a
 * ConsistencyPredicate method for one domainObject instance. The
 * PersistentDependenceRecord has a collection of PersistentDomainMetaObjects,
 * that represent the domainObjects read to execute the ConsistencyPredicate.
 **/
@CannotUseConsistencyPredicates
public class PersistentDependenceRecord extends PersistentDependenceRecord_Base implements
	DependenceRecord<PersistentDomainMetaObject> {

    public PersistentDependenceRecord(Object dependent, KnownConsistencyPredicate predicate, Set<Depended> depended,
	    boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getMetaObject());
	setKnownConsistencyPredicate(predicate);
	for (Depended<PersistentDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((PersistentDomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    public PersistentDependenceRecord(Object dependent, Method predicate, Set<Depended> depended, boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getMetaObject());
	setPredicate(predicate);
	for (Depended<PersistentDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((PersistentDomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    @Override
    public void setConsistent(Boolean consistent) {
	if (consistent) {
	    if ((getConsistent() != null) && !isConsistent()) {
		// Setting to consistent after being inconsistent
		getKnownConsistencyPredicate().removeInconsistentDependenceRecords(this);
	    }
	} else {
	    if ((getConsistent() == null) || isConsistent()) {
		// Setting to inconsistent after being consistent, or for the first time
		getKnownConsistencyPredicate().addInconsistentDependenceRecords(this);
	    }
	}
	super.setConsistent(consistent);
    }

    public boolean isConsistent() {
	return getConsistent();
    }

    /**
     * A DependenceRecord can be deleted in two cases:
     * 
     * 1 - When the dependentDO is being deleted. In this case, the DO's
     * metaObject will also be deleted, and will therefore call this method.
     * 
     * 2 - The ConsistencyPredicate was removed from the code. In this case, we
     * only remove the link to all the metaObjects, which are not deleted.
     **/
    public void delete() {
	for (PersistentDomainMetaObject dependedMetaObject : getDependedDomainMetaObjects()) {
	    removeDependedDomainMetaObjects(dependedMetaObject);
	}
	if (!isConsistent()) {
	    getKnownConsistencyPredicate().removeInconsistentDependenceRecords(this);
	}
	removeKnownConsistencyPredicate();
	removeDependentDomainMetaObject();

	//Deletes THIS DependenceRecord, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    // DependenceRecord interface implemented below:
    @Override
    public void addDepended(PersistentDomainMetaObject dependedMetaObject) {
	addDependedDomainMetaObjects(dependedMetaObject);
    }

    @Override
    public Iterator<PersistentDomainMetaObject> getDepended() {
	return getDependedDomainMetaObjectsIterator();
    }

    @Override
    public Object getDependent() {
	return getDependentDomainMetaObject().getDomainObject();
    }

    public <DomainClass extends AbstractDomainObject> DomainClass getDependentDomainObject() {
	return (DomainClass) getDependent();
    }

    public void setPredicate(Method predicateMethod) {
	setKnownConsistencyPredicate(KnownConsistencyPredicate.readKnownConsistencyPredicate(predicateMethod));
    }

    @Override
    public Method getPredicate() {
	return getKnownConsistencyPredicate().getPredicate();
    }
}
