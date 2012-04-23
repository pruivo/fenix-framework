package pt.ist.fenixframework.pstm;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.consistencyPredicates.DomainConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.NoDomainMetaData;
import pt.ist.fenixframework.pstm.consistencyPredicates.PrivateConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.PublicConsistencyPredicate;
import pt.ist.fenixframework.pstm.repository.DbUtil;
import dml.runtime.Relation;
import dml.runtime.RelationAdapter;

/**
 * A DomainMetaClass is the domain entity that represents a class existing in
 * the application's domain model, declared in the DML.
 * 
 * These DomainMetaClasses are created or deleted only during the
 * {@link FenixFramework} initialization. A DomainMetaClass is linked to one
 * superclass and many subclasses, which are also DomainMetaClasses.
 * 
 * Each DomainMetaClass stores a set of all existing {@link DomainMetaObject}s
 * of it's class. Furthermore, a DomainMetaClass contains a set of all
 * {@link DomainConsistencyPredicate}s that are declared in its code.
 **/
@NoDomainMetaData
public class DomainMetaClass extends DomainMetaClass_Base {

    static {
	DomainMetaClassDomainMetaObjects.addListener(new RelationAdapter<DomainMetaClass, DomainMetaObject>() {
	    @Override
	    public void beforeRemove(Relation<DomainMetaClass, DomainMetaObject> rel, DomainMetaClass metaClass,
		    DomainMetaObject metaObject) {
		if (metaClass != null) {
		    metaClass.setMetaObjectCount(metaClass.getMetaObjectCount() - 1);
		}
	    }
	    
	    @Override
	    public void beforeAdd(Relation<DomainMetaClass, DomainMetaObject> rel, DomainMetaClass metaClass,
		    DomainMetaObject metaObject) {
		if (metaClass != null) {
		    metaClass.setMetaObjectCount(metaClass.getMetaObjectCount() + 1);
		}
	    }
	});
    }

    public static Comparator<DomainMetaClass> COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN = new Comparator<DomainMetaClass>() {
	@Override
	public int compare(DomainMetaClass metaClass1, DomainMetaClass metaClass2) {
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

    public DomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	super();
	checkFrameworkNotInitialized();
	setDomainClass(domainClass);
	DomainFenixFrameworkRoot.getInstance().addDomainMetaClasses(this);
	setMetaObjectCount(0);

	System.out.println("[MetaClasses] Creating new MetaClass: " + domainClass);
    }

    public DomainMetaClass(Class<? extends AbstractDomainObject> domainClass, DomainMetaClass metaSuperclass) {
	this(domainClass);
	setDomainMetaSuperclass(metaSuperclass);
    }

    private void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    @Override
    public void setDomainFenixFrameworkRoot(DomainFenixFrameworkRoot domainFenixFrameworkRoot) {
	checkFrameworkNotInitialized();
	super.setDomainFenixFrameworkRoot(domainFenixFrameworkRoot);
    }

    @Override
    public void removeDomainFenixFrameworkRoot() {
	checkFrameworkNotInitialized();
	super.removeDomainFenixFrameworkRoot();
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
	for (DomainMetaObject metaObject : getExistingDomainMetaObjects()) {
	    existingDomainObjects.add(metaObject.getDomainObject());
	}
	return existingDomainObjects;
    }

    public <PredicateType extends DomainConsistencyPredicate> PredicateType getDeclaredConsistencyPredicate(Method predicateMethod) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.getPredicate().equals(predicateMethod)) {
		return (PredicateType) declaredConsistencyPredicate;
	    }
	}
	return null;
    }

    public <PredicateType extends DomainConsistencyPredicate> PredicateType getDeclaredConsistencyPredicate(String predicateName) {
	try {
	    Method predicateMethod = getDomainClass().getDeclaredMethod(predicateName);
	    return (PredicateType) getDeclaredConsistencyPredicate(predicateMethod);
	} catch (NoSuchMethodException e) {
	    return null;
	}
    }

    @Override
    public void addDeclaredConsistencyPredicates(DomainConsistencyPredicate declaredConsistencyPredicates) {
	checkFrameworkNotInitialized();
	super.addDeclaredConsistencyPredicates(declaredConsistencyPredicates);
    }

    @Override
    public void removeDeclaredConsistencyPredicates(DomainConsistencyPredicate declaredConsistencyPredicates) {
	checkFrameworkNotInitialized();
	super.removeDeclaredConsistencyPredicates(declaredConsistencyPredicates);
    }

    /**
     * This initExistingDomainObjects() method should be called during the init
     * of the FenixFramework, for each new DomainMetaClass.
     * 
     * This method fills the existing DomainObjects of this class, using a query
     * to the persistent backend.
     */
    protected void initExistingDomainObjects() {
	checkFrameworkNotInitialized();
	for (String oid : getExistingOIDsForClass(getDomainClass())) {
	    AbstractDomainObject existingDO = (AbstractDomainObject) AbstractDomainObject.fromOID(Long.valueOf(oid));
	    DomainMetaObject metaObject = new DomainMetaObject();
	    metaObject.setDomainObject(existingDO);
	    addExistingDomainMetaObjects(metaObject);
	}
    }

    private List<String> getExistingOIDsForClass(Class<? extends AbstractDomainObject> domainClass) {
	DomainMetaClass topSuperClass = this;
	while (topSuperClass.hasDomainMetaSuperclass()) {
	    topSuperClass = topSuperClass.getDomainMetaSuperclass();
	}
	String tableName = DbUtil.convertToDBStyle(topSuperClass.getDomainClass().getSimpleName());
	String className = getDomainClass().getName();

	String query = "select OID from " + tableName + ", FF$DOMAIN_CLASS_INFO where OID >> 32 = DOMAIN_CLASS_ID and DOMAIN_CLASS_NAME = '" + className + "'";
	
	ArrayList<String> oids = new ArrayList<String>();
	try {
	    Statement statement = Transaction.getCurrentJdbcConnection().createStatement();
	    ResultSet resultSet = statement.executeQuery(query);
	    while (resultSet.next()) {
		oids.add(resultSet.getString("OID"));
	    }
	} catch (SQLException e) {
	    throw new Error(e);
	}

	return oids;
    }

    @Override
    public void addDomainMetaSubclasses(DomainMetaClass domainMetaSubclasses) {
	checkFrameworkNotInitialized();
	super.addDomainMetaSubclasses(domainMetaSubclasses);
    }

    @Override
    public void removeDomainMetaSubclasses(DomainMetaClass domainMetaSubclasses) {
	checkFrameworkNotInitialized();
	super.removeDomainMetaSubclasses(domainMetaSubclasses);
    }

    @Override
    public void setDomainMetaSuperclass(DomainMetaClass domainMetaSuperclass) {
	checkFrameworkNotInitialized();
	super.setDomainMetaSuperclass(domainMetaSuperclass);
    }

    @Override
    public void removeDomainMetaSuperclass() {
	checkFrameworkNotInitialized();
	super.removeDomainMetaSuperclass();
    }

    public void initDomainMetaSuperclass(DomainMetaClass metaSuperclass) {
	checkFrameworkNotInitialized();
	setDomainMetaSuperclass(metaSuperclass);
	System.out.println("[MetaClasses] Initializing MetaSuperClass of " + getDomainClass() + " to "
		+ metaSuperclass.getDomainClass());

	List<PrivateConsistencyPredicate> privatePredicates = new ArrayList<PrivateConsistencyPredicate>();
	Map<String, PublicConsistencyPredicate> publicPredicates = new HashMap<String, PublicConsistencyPredicate>();
	getDomainMetaSuperclass().fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates,
		publicPredicates);

	for (DomainConsistencyPredicate newPredicate : privatePredicates) {
	    newPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(this);
	}
	for (DomainConsistencyPredicate newPredicate : publicPredicates.values()) {
	    newPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(this);
	}
    }

    private void fillConsistencyPredicatesOfThisClassAndSuperclasses(List<PrivateConsistencyPredicate> privatePredicates,
	    Map<String, PublicConsistencyPredicate> publicPredicates) {
	DomainMetaClass metaSuperclass = getDomainMetaSuperclass();
	if (metaSuperclass != null) {
	    metaSuperclass.fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates, publicPredicates);
	}
	getPrivateConsistencyPredicates(privatePredicates);
	getPublicConsistencyPredicates(publicPredicates);
    }

    private void getPrivateConsistencyPredicates(List<PrivateConsistencyPredicate> privatePredicates) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPrivate()) {
		privatePredicates.add((PrivateConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    private void getPublicConsistencyPredicates(Map<String, PublicConsistencyPredicate> publicPredicates) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPublic()) {
		publicPredicates.put(declaredConsistencyPredicate.getPredicate().getName(),
			(PublicConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    public Set<DomainConsistencyPredicate> getAllConsistencyPredicates() {
	Set<DomainConsistencyPredicate> allPredicates = new HashSet<DomainConsistencyPredicate>();

	List<PrivateConsistencyPredicate> privatePredicates = new ArrayList<PrivateConsistencyPredicate>();
	Map<String, PublicConsistencyPredicate> publicPredicates = new HashMap<String, PublicConsistencyPredicate>();
	fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates, publicPredicates);

	allPredicates.addAll(privatePredicates);
	allPredicates.addAll(publicPredicates.values());
	return allPredicates;
    }

    /**
     * A DomainMetaClass should be deleted only when the corresponding domain
     * class is removed from the DML. In this case, we can delete all the
     * DomainConsistencyPredicates associated with this DomainMetaClass.
     **/
    protected void delete() {
	checkFrameworkNotInitialized();
	if (getMetaObjectCount() != 0) {
	    throw new Error("Cannot delete a domain class that has existing domain objects");
	}

	// If we are deleting this class, then the previous subclass will have changed extends
	// and should also be deleted.
	for (DomainMetaClass metaSubclass : getDomainMetaSubclasses()) {
	    metaSubclass.delete();
	}

	System.out.println("[MetaClasses] Deleted metaClass "
		+ ((getDomainClass() == null) ? "" : getDomainClass().getSimpleName()));
	for (DomainConsistencyPredicate domainConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    domainConsistencyPredicate.classDelete();
	}
	removeDomainMetaSuperclass();

	DomainFenixFrameworkRoot root = getDomainFenixFrameworkRoot();
	if (root != null) {
	    root.removeDomainMetaClasses(this);
	}
	//Deletes THIS metaClass, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    public static DomainMetaClass readDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	return DomainFenixFrameworkRoot.getInstance().getDomainMetaClass(domainClass);
    }

}
