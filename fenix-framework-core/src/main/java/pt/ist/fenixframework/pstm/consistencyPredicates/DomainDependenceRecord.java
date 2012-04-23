package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import jvstm.cps.Depended;
import jvstm.cps.DependenceRecord;
import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.DomainMetaObject;

/**
 * A DomainDependenceRecord represents the result of the execution of a
 * consistency predicate method for one domain object instance. The
 * DomainDependenceRecord has a set of depended {@link DomainMetaObject}s, that
 * represent the domain objects that were read to execute the consistency
 * predicate.
 **/
@NoDomainMetaData
public class DomainDependenceRecord extends DomainDependenceRecord_Base implements
	DependenceRecord<DomainMetaObject> {

    public DomainDependenceRecord(Object dependent, DomainConsistencyPredicate predicate, Set<Depended> depended,
	    boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getMetaObject());
	setDomainConsistencyPredicate(predicate);
	for (Depended<DomainDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((DomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    public DomainDependenceRecord(Object dependent, Method predicate, Set<Depended> depended, boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getMetaObject());
	setPredicate(predicate);
	for (Depended<DomainDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((DomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    @Override
    public void setConsistent(Boolean consistent) {
	if (consistent) {
	    if ((getConsistent() != null) && !isConsistent()) {
		// Setting to consistent after being inconsistent
		getDomainConsistencyPredicate().removeInconsistentDependenceRecords(this);
	    }
	} else {
	    if ((getConsistent() == null) || isConsistent()) {
		// Setting to inconsistent after being consistent, or for the first time
		getDomainConsistencyPredicate().addInconsistentDependenceRecords(this);
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
	for (DomainMetaObject dependedMetaObject : getDependedDomainMetaObjects()) {
	    removeDependedDomainMetaObjects(dependedMetaObject);
	}
	if (!isConsistent()) {
	    getDomainConsistencyPredicate().removeInconsistentDependenceRecords(this);
	}
	removeDomainConsistencyPredicate();
	removeDependentDomainMetaObject();

	//Deletes THIS DependenceRecord, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    // DependenceRecord interface implemented below:
    @Override
    public void addDepended(DomainMetaObject dependedMetaObject) {
	addDependedDomainMetaObjects(dependedMetaObject);
    }

    @Override
    public Iterator<DomainMetaObject> getDepended() {
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
	setDomainConsistencyPredicate(DomainConsistencyPredicate.readDomainConsistencyPredicate(predicateMethod));
    }

    @Override
    public Method getPredicate() {
	return getDomainConsistencyPredicate().getPredicate();
    }
}
