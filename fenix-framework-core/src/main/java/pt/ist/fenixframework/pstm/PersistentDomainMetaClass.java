package pt.ist.fenixframework.pstm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.consistencyPredicates.CannotUseConsistencyPredicates;
import pt.ist.fenixframework.pstm.consistencyPredicates.KnownConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.PrivateConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.PublicConsistencyPredicate;

/**
 * A PersistentDomainMetaClass is the persistent domain entity that represents a
 * class existing in the application's domain model, declared in the DML.
 * 
 * These PersistentDomainMetaClasses are created or deleted only during the
 * FenixFramework initialization. A PersistentDomainMetaClass is linked to one
 * superclass and many subclasses, that are also PersistentDomainMetaClasses.
 * 
 * Each PersistentDomainMetaClass stores a set of all existing domainObjects of
 * it's class. Furthermore, a PersistentDomainMetaClass contains a set of all
 * KnownConsistencyPredicates that are declared in its code.
 **/
@CannotUseConsistencyPredicates
public class PersistentDomainMetaClass extends PersistentDomainMetaClass_Base {

    public static Comparator<PersistentDomainMetaClass> COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN = new Comparator<PersistentDomainMetaClass>() {
	@Override
	public int compare(PersistentDomainMetaClass metaClass1, PersistentDomainMetaClass metaClass2) {
	    if (metaClass1 == metaClass2) {
		return 0;
	    }
	    if (metaClass1.getDomainClass().isAssignableFrom(metaClass2.getDomainClass())) {
		return -1;
	    }
	    if (metaClass2.getDomainClass().isAssignableFrom(metaClass1.getDomainClass())) {
		return 1;
	    }
	    return metaClass1.getDomainClass().toString().compareTo(metaClass2.getDomainClass().toString());
	}
    };

    public PersistentDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	super();
	checkFrameworkNotInitialized();
	setDomainClass(domainClass);
	PersistenceFenixFrameworkRoot.getInstance().addPersistentDomainMetaClasses(this);

	System.out.println("[MetaClasses] Creating new MetaClass: " + domainClass);
    }

    public PersistentDomainMetaClass(Class<? extends AbstractDomainObject> domainClass, PersistentDomainMetaClass metaSuperclass) {
	this(domainClass);
	setPersistentDomainMetaSuperclass(metaSuperclass);
    }

    private void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    @Override
    public void setPersistenceFenixFrameworkRoot(PersistenceFenixFrameworkRoot persistenceFenixFrameworkRoot) {
	checkFrameworkNotInitialized();
	super.setPersistenceFenixFrameworkRoot(persistenceFenixFrameworkRoot);
    }

    @Override
    public void removePersistenceFenixFrameworkRoot() {
	checkFrameworkNotInitialized();
	super.removePersistenceFenixFrameworkRoot();
    }

    @Override
    public Class<? extends AbstractDomainObject> getDomainClass() {
	return super.getDomainClass();
    }

    @Override
    public void setDomainClass(Class domainClass) {
	checkFrameworkNotInitialized();
	super.setDomainClass(domainClass);
    }

    public List<AbstractDomainObject> getExistingDomainObjects() {
	List<AbstractDomainObject> existingDomainObjects = new ArrayList<AbstractDomainObject>();
	for (PersistentDomainMetaObject metaObject : getExistingPersistentDomainMetaObjects()) {
	    existingDomainObjects.add(metaObject.getDomainObject());
	}
	return existingDomainObjects;
    }

    public <PredicateType extends KnownConsistencyPredicate> PredicateType getDeclaredConsistencyPredicate(Method predicateMethod) {
	for (KnownConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.getPredicate().equals(predicateMethod)) {
		return (PredicateType) declaredConsistencyPredicate;
	    }
	}
	return null;
    }

    public <PredicateType extends KnownConsistencyPredicate> PredicateType getDeclaredConsistencyPredicate(String predicateName) {
	try {
	    Method predicateMethod = getDomainClass().getDeclaredMethod(predicateName);
	    return (PredicateType) getDeclaredConsistencyPredicate(predicateMethod);
	} catch (NoSuchMethodException e) {
	    return null;
	}
    }

    @Override
    public void addDeclaredConsistencyPredicates(KnownConsistencyPredicate declaredConsistencyPredicates) {
	checkFrameworkNotInitialized();
	super.addDeclaredConsistencyPredicates(declaredConsistencyPredicates);
    }

    @Override
    public void removeDeclaredConsistencyPredicates(KnownConsistencyPredicate declaredConsistencyPredicates) {
	checkFrameworkNotInitialized();
	super.removeDeclaredConsistencyPredicates(declaredConsistencyPredicates);
    }

    /**
     * This initExistingDomainObjects() method should be called during the init
     * of the FenixFramework, for each new PersistentDomainMetaClass.
     * 
     * This method fills the existing DomainObjects of this class, using a query
     * to the persistent backend.
     */
    protected void initExistingDomainObjects() {
	checkFrameworkNotInitialized();
	Set<String> existingOIDs = getExistingOIDsForClass(getDomainClass());
	for (String oid : existingOIDs) {
	    AbstractDomainObject existingDO = (AbstractDomainObject) AbstractDomainObject.fromOID(Long.valueOf(oid));
	    PersistentDomainMetaObject metaObject = new PersistentDomainMetaObject();
	    metaObject.setDomainObject(existingDO);
	    addExistingPersistentDomainMetaObjects(metaObject);
	}
    }

    private Set<String> getExistingOIDsForClass(Class<? extends AbstractDomainObject> domainClass) {
	// TODO:
	// Create a DB query to obtain all objects OIDs of this type
	return new HashSet<String>();
    }

    @Override
    public void addPersistentDomainMetaSubclasses(PersistentDomainMetaClass persistentDomainMetaSubclasses) {
	checkFrameworkNotInitialized();
	super.addPersistentDomainMetaSubclasses(persistentDomainMetaSubclasses);
    }

    @Override
    public void removePersistentDomainMetaSubclasses(PersistentDomainMetaClass persistentDomainMetaSubclasses) {
	checkFrameworkNotInitialized();
	super.removePersistentDomainMetaSubclasses(persistentDomainMetaSubclasses);
    }

    @Override
    public void setPersistentDomainMetaSuperclass(PersistentDomainMetaClass persistentDomainMetaSuperclass) {
	checkFrameworkNotInitialized();
	super.setPersistentDomainMetaSuperclass(persistentDomainMetaSuperclass);
    }

    @Override
    public void removePersistentDomainMetaSuperclass() {
	checkFrameworkNotInitialized();
	super.removePersistentDomainMetaSuperclass();
    }

    public void initPersistentDomainMetaSuperclass(PersistentDomainMetaClass metaSuperclass) {
	checkFrameworkNotInitialized();
	setPersistentDomainMetaSuperclass(metaSuperclass);
	System.out.println("[MetaClasses] Initializing MetaSuperClass of " + getDomainClass() + " to "
		+ metaSuperclass.getDomainClass());

	List<PrivateConsistencyPredicate> privatePredicates = new ArrayList<PrivateConsistencyPredicate>();
	Map<String, PublicConsistencyPredicate> publicPredicates = new HashMap<String, PublicConsistencyPredicate>();
	getPersistentDomainMetaSuperclass().fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates,
		publicPredicates);

	for (KnownConsistencyPredicate newPredicate : privatePredicates) {
	    newPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(this);
	}
	for (KnownConsistencyPredicate newPredicate : publicPredicates.values()) {
	    newPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(this);
	}
    }

    private void fillConsistencyPredicatesOfThisClassAndSuperclasses(List<PrivateConsistencyPredicate> privatePredicates,
	    Map<String, PublicConsistencyPredicate> publicPredicates) {
	PersistentDomainMetaClass metaSuperclass = getPersistentDomainMetaSuperclass();
	if (metaSuperclass != null) {
	    metaSuperclass.fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates, publicPredicates);
	}
	getPrivateConsistencyPredicates(privatePredicates);
	getPublicConsistencyPredicates(publicPredicates);
    }

    private void getPrivateConsistencyPredicates(List<PrivateConsistencyPredicate> privatePredicates) {
	for (KnownConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPrivate()) {
		privatePredicates.add((PrivateConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    private void getPublicConsistencyPredicates(Map<String, PublicConsistencyPredicate> publicPredicates) {
	for (KnownConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPublic()) {
		publicPredicates.put(declaredConsistencyPredicate.getPredicate().getName(),
			(PublicConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    public Set<KnownConsistencyPredicate> getAllConsistencyPredicates() {
	Set<KnownConsistencyPredicate> allPredicates = new HashSet<KnownConsistencyPredicate>();

	List<PrivateConsistencyPredicate> privatePredicates = new ArrayList<PrivateConsistencyPredicate>();
	Map<String, PublicConsistencyPredicate> publicPredicates = new HashMap<String, PublicConsistencyPredicate>();
	fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates, publicPredicates);

	allPredicates.addAll(privatePredicates);
	allPredicates.addAll(publicPredicates.values());
	return allPredicates;
    }

    /**
     * A PersistentDomainMetaClass should be deleted only when the corresponding
     * domain class is removed from the DML. In this case, we can delete all the
     * KnownConsistencyPredicates associated with this
     * PersistentDomainMetaClass.
     **/
    protected void delete() {
	checkFrameworkNotInitialized();
	if (hasAnyExistingPersistentDomainMetaObjects()) {
	    throw new Error("Cannot delete a domain class that has existing domain objects");
	}

	// If we are deleting this class, then, at best, the previous subclass will have changed extends
	// and should also be deleted.
	for (PersistentDomainMetaClass metaSubclass : getPersistentDomainMetaSubclasses()) {
	    metaSubclass.delete();
	}

	System.out.println("[MetaClasses] Deleted metaClass "
		+ ((getDomainClass() == null) ? "" : getDomainClass().getSimpleName()));
	for (KnownConsistencyPredicate knownConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    knownConsistencyPredicate.classDelete();
	}
	removePersistentDomainMetaSuperclass();

	PersistenceFenixFrameworkRoot root = getPersistenceFenixFrameworkRoot();
	if (root != null) {
	    root.removePersistentDomainMetaClasses(this);
	}
	//Deletes THIS metaClass, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    public static PersistentDomainMetaClass readPersistentDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	return PersistenceFenixFrameworkRoot.getInstance().getPersistentDomainMetaClass(domainClass);
    }

}
