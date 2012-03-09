package pt.ist.fenixframework.pstm;

import java.util.Set;

import jvstm.cps.Depended;
import pt.ist.fenixframework.pstm.consistencyPredicates.CannotUseConsistencyPredicates;
import pt.ist.fenixframework.pstm.consistencyPredicates.PersistentDependenceRecord;

/**
 * Each DomainObject is associated with one PersistentDomainMetaObject, which is
 * a persistent representation of the domainObject inside the fenix-framework.
 * The PersistentDomainMetaObject is created when the domainObject is created.
 * 
 * The PersistentDomainMetaObject stores all the dependencies to this
 * domainObject from DependenceRecords, and the the domainObject's
 * PersistentDomainMetaClass.
 **/
@CannotUseConsistencyPredicates
public class PersistentDomainMetaObject extends PersistentDomainMetaObject_Base implements Depended<PersistentDependenceRecord> {

    public PersistentDomainMetaObject() {
	super();
    }

    /**
     * A PersistentDomainMetaObject should be deleted only when the associated
     * DO is being deleted. Therefore, we can remove it from its metaClass'
     * existing objects list.
     * 
     * Also, the DO is no longer connected to other objects. Thus, the only
     * DependenceRecords that will still depend on this DO are those defined by
     * it's own ConsistencyPredicates. These DependenceRecords should also be
     * deleted.
     **/
    protected void delete() {
	removePersistentDomainMetaClass();

	for (PersistentDependenceRecord dependingDependenceRecord : getDependingDependenceRecords()) {
	    removeDependingDependenceRecords(dependingDependenceRecord);
	}
	for (PersistentDependenceRecord ownDependenceRecord : getOwnDependenceRecords()) {
	    ownDependenceRecord.delete();
	}

	removeDomainObject();

	//Deletes THIS metaObject, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    /**
     * The deleteFromClass method should be invoked when the MetaClass of this
     * MetaObject is being deleted. This means that the domain class of the
     * object no longer exists in the code. So, the domain object is no longer
     * accessible, even though it may still exist in the database.
     **/
    protected void deleteFromClass() {
	removePersistentDomainMetaClass();

	for (PersistentDependenceRecord dependingDependenceRecord : getDependingDependenceRecords()) {
	    removeDependingDependenceRecords(dependingDependenceRecord);
	}
	for (PersistentDependenceRecord ownDependenceRecord : getOwnDependenceRecords()) {
	    ownDependenceRecord.delete();
	}

	super.setDomainObject(null);

	//Deletes THIS metaObject, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    @Override
    public void setDomainObject(AbstractDomainObject domainObject) {
	// These two sets are needed because the relation between a domainObject
	// and it's metaObject is only partially implemented in DML.
	super.setDomainObject(domainObject);
	domainObject.justSetMetaObject(this);
    }

    @Override
    public void removeDomainObject() {
	AbstractDomainObject domainObject = getDomainObject();

	// These two sets are needed because the relation between a domainObject
	// and it's metaObject is only partially implemented in DML.
	domainObject.justSetMetaObject(null);
	super.setDomainObject(null);
    }

    // Depended interface implemented below:
    @Override
    public void addDependence(PersistentDependenceRecord record) {
	addDependingDependenceRecords(record);
    }

    @Override
    public void removeDependence(PersistentDependenceRecord record) {
	removeDependingDependenceRecords(record);
    }

    @Override
    public Set<PersistentDependenceRecord> getDependenceRecords() {
	return getDependingDependenceRecordsSet();
    }
}
