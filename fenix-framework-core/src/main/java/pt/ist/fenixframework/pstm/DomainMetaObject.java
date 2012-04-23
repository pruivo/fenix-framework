package pt.ist.fenixframework.pstm;

import java.util.Set;

import jvstm.cps.Depended;
import pt.ist.fenixframework.pstm.consistencyPredicates.DomainDependenceRecord;
import pt.ist.fenixframework.pstm.consistencyPredicates.NoDomainMetaData;

/**
 * Each domain object is associated with one DomainMetaObject, which is a
 * representation of the domainObject inside the fenix-framework's own domain.
 * The DomainMetaObject is created when the domain object is created.
 * 
 * The DomainMetaObject stores all the dependencies to this domainObject from
 * {@link DomainDependenceRecord}s, and the the domain object's
 * {@link DomainMetaClass}.
 **/
@NoDomainMetaData
public class DomainMetaObject extends DomainMetaObject_Base implements Depended<DomainDependenceRecord> {

    public DomainMetaObject() {
	super();
    }

    /**
     * A DomainMetaObject should be deleted only when the associated DO is being
     * deleted. Therefore, we can remove it from its metaClass' existing objects
     * list.
     * 
     * Also, the DO is no longer connected to other objects. Thus, the only
     * DependenceRecords that will still depend on this DO are those defined by
     * it's own ConsistencyPredicates. These DependenceRecords should also be
     * deleted.
     **/
    protected void delete() {
	removeDomainMetaClass();

	for (DomainDependenceRecord dependingDependenceRecord : getDependingDependenceRecords()) {
	    removeDependingDependenceRecords(dependingDependenceRecord);
	}
	for (DomainDependenceRecord ownDependenceRecord : getOwnDependenceRecords()) {
	    ownDependenceRecord.delete();
	}

	removeDomainObject();

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
    public void addDependence(DomainDependenceRecord record) {
	addDependingDependenceRecords(record);
    }

    @Override
    public void removeDependence(DomainDependenceRecord record) {
	removeDependingDependenceRecords(record);
    }

    @Override
    public Set<DomainDependenceRecord> getDependenceRecords() {
	return getDependingDependenceRecordsSet();
    }
}
