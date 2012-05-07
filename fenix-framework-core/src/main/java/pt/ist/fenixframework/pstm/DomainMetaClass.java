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
import pt.ist.fenixframework.pstm.consistencyPredicates.PrivateConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.PublicConsistencyPredicate;
import pt.ist.fenixframework.pstm.repository.DbUtil;
import dml.runtime.Relation;
import dml.runtime.RelationAdapter;

/**
 * A <code>DomainMetaClass</code> is the domain entity that represents a class
 * existing in the application's domain model, declared in the DML.<br>
 * 
 * These DomainMetaClasses are created or deleted only during the
 * {@link FenixFramework} initialization. A <code>DomainMetaClass</code> is
 * linked to one superclass and many subclasses, which are also
 * <code>DomainMetaClasses</code>.<br>
 * 
 * Each <code>DomainMetaClass</code> stores a set of all existing
 * {@link DomainMetaObject}s of it's class. Furthermore, a
 * <code>DomainMetaClass</code> contains a set of all
 * {@link DomainConsistencyPredicate}s that are declared in its code.
 * 
 * @author João Neves - JoaoRoxoNeves@ist.utl.pt
 **/
@NoDomainMetaObjects
public class DomainMetaClass extends DomainMetaClass_Base {

    //This Listener keeps the metaObjectCount updated, according to the number of elements in the relation: existingDomainMetaObjects 
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

    /**
     * Compares two classes according to their hierarchies, such that a
     * superclass always appears before any of its subclasses. Classes of
     * different hierarchies are sorted alphabetically according to the names of
     * their successive superclasses (from top to bottom).
     */
    public static Comparator<Class<? extends AbstractDomainObject>> COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN = new Comparator<Class<? extends AbstractDomainObject>>() {
	@Override
	public int compare(Class<? extends AbstractDomainObject> class1, Class<? extends AbstractDomainObject> class2) {
	    if (class1.equals(class2)) {
		return 0;
	    }
	    if (class1.isAssignableFrom(class2)) {
		return -1;
	    }
	    if (class2.isAssignableFrom(class1)) {
		return 1;
	    }
	    return getHierarchyName(class1).compareTo(getHierarchyName(class2));
	}
    };

    /**
     * @return the fully qualified names of this class' successive superclasses
     *         (from top to bottom).
     */
    private static String getHierarchyName(Class<?> class1) {
	if (class1.equals(OneBoxDomainObject.class)) {
	    return "";
	}
	return getHierarchyName(class1.getSuperclass()) + "<-" + class1.getName();
    }

    /**
     * Compares two <code>DomainMetaClasses</code> according to their
     * hierarchies, such that a superclass always appears before any of its
     * subclasses. <code>DomainMetaClasses</code> of different hierarchies are
     * sorted alphabetically according to the names of their successive
     * superclasses (from top to bottom).
     * 
     * @see DomainMetaClass#COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN
     */
    public static Comparator<DomainMetaClass> COMPARATOR_BY_META_CLASS_HIERARCHY_TOP_DOWN = new Comparator<DomainMetaClass>() {
	@Override
	public int compare(DomainMetaClass metaClass1, DomainMetaClass metaClass2) {
	    if (metaClass1 == metaClass2) {
		return 0;
	    }
	    return COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN.compare(metaClass1.getDomainClass(), metaClass2.getDomainClass());
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

    /**
     * Checks that the {@link FenixFramework} is not initialized, throws an
     * exception otherwise. Should be called before any changes are made to
     * {@link DomainMetaClass}es or to {@link DomainConsistencyPredicate}s.
     * 
     * @throws RuntimeException
     *             if the framework was already initialized
     */
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

    /**
     * @return the <code>List</code> of existing {@link AbstractDomainObject}s
     *         of this class.
     */
    public List<AbstractDomainObject> getExistingDomainObjects() {
	List<AbstractDomainObject> existingDomainObjects = new ArrayList<AbstractDomainObject>();
	for (DomainMetaObject metaObject : getExistingDomainMetaObjects()) {
	    existingDomainObjects.add(metaObject.getDomainObject());
	}
	return existingDomainObjects;
    }

    public <PredicateT extends DomainConsistencyPredicate> PredicateT getDeclaredConsistencyPredicate(Method predicateMethod) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.getPredicate().equals(predicateMethod)) {
		return (PredicateT) declaredConsistencyPredicate;
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
     * This method should be called only during the initialization of the
     * {@link FenixFramework}, for each new {@link DomainMetaClass}.
     * 
     * Creates a {@link DomainMetaObject} for each existing
     * {@link AbstractDomainObject} of this class, by using a query to the
     * persistent backend.
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

    /**
     * Uses a JDBC query to obtain all the OIDs of the existing
     * {@link AbstractDomainObject}s of this class.
     * 
     * @return the <code>List</code> of <code>Strings</code> containing the OIDs
     *         of all the {@link AbstractDomainObject}s of this class.
     */
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

    /**
     * This method should be called only during the initialization of the
     * {@link FenixFramework}.
     * 
     * Initializes the superclass of this DomainMetaClass to the metaSuperClass
     * passed as argument. Any inherited consistency predicates from the
     * superclasses that are not being overridden, will be executed for each
     * existing object of this DomainMetaClass.
     */
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

    /**
     * This method should be called only during the initialization of the
     * {@link FenixFramework}.
     * 
     * Initializes the superclass of this DomainMetaClass to the metaSuperClass
     * passed as argument. Any inherited consistency predicates from the
     * superclasses that are not being overridden, will be executed for each
     * existing object of this DomainMetaClass.
     */
    private void fillConsistencyPredicatesOfThisClassAndSuperclasses(List<PrivateConsistencyPredicate> privatePredicates,
	    Map<String, PublicConsistencyPredicate> publicPredicates) {
	DomainMetaClass metaSuperclass = getDomainMetaSuperclass();
	if (metaSuperclass != null) {
	    metaSuperclass.fillConsistencyPredicatesOfThisClassAndSuperclasses(privatePredicates, publicPredicates);
	}
	getPrivateConsistencyPredicates(privatePredicates);
	getPublicConsistencyPredicates(publicPredicates);
    }

    /**
     * Adds to the <code>List</code> of {@link PrivateConsistencyPredicate}s
     * passed as argument, all the {@link PrivateConsistencyPredicate}s declared
     * directly in this class.
     */
    private void getPrivateConsistencyPredicates(List<PrivateConsistencyPredicate> privatePredicates) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPrivate()) {
		privatePredicates.add((PrivateConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    /**
     * Adds to the <code>Map</code> of <code>Strings</code> to
     * {@link PublicConsistencyPredicate}s passed as argument, all the
     * {@link PublicConsistencyPredicate}s declared directly in this class,
     * associated to their method names.
     */
    private void getPublicConsistencyPredicates(Map<String, PublicConsistencyPredicate> publicPredicates) {
	for (DomainConsistencyPredicate declaredConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    if (declaredConsistencyPredicate.isPublic()) {
		publicPredicates.put(declaredConsistencyPredicate.getPredicate().getName(),
			(PublicConsistencyPredicate) declaredConsistencyPredicate);
	    }
	}
    }

    /**
     * Returns a <code>Set</code> with all the
     * {@link DomainConsistencyPredicate}s that affect the objects of this
     * class.
     * 
     * @return a <code>Set</code> of all the {@link DomainConsistencyPredicate}s
     *         declared directly by this class, or inherited and not overridden.
     */
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
     * This method should be called only during the initialization of the
     * {@link FenixFramework}. <br>
     * Deletes this <code>DomainMetaClass</code>, and all its metaSubclasses.
     * Also deletes all the declared {@link DomainConsistencyPredicate}s. <br>
     * A DomainMetaClass should be deleted only when the corresponding domain
     * class is removed from the DML, or the framework is configured not to
     * create meta objects.
     * 
     * @throws Error
     *             if this <code>DomainMetaClass</code> still has existing
     *             {@link DomainMetaObject}s. The framework does not support
     *             removing from DML a class that has existing objects. To
     *             remove a class, you must delete all the existing objects
     *             first, during runtime.
     **/
    protected void delete() {
	checkFrameworkNotInitialized();
	if (getMetaObjectCount() != 0) {
	    throw new Error("Cannot delete a domain class that has existing meta objects");
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

    /**
     * This method should be called only during the initialization of the
     * {@link FenixFramework}, when the framework is deleting all the existing
     * {@link DomainMetaClass}es and {@link DomainMetaObject}s. This happens
     * when the framework is configured not to create meta objects. <br>
     * Deletes this <code>DomainMetaClass</code>, and all its metaSubclasses.
     * Also deletes all the declared {@link DomainConsistencyPredicate}s. <br>
     * A DomainMetaClass should be deleted only when the corresponding domain
     * class is removed from the DML, or the framework is configured not to
     * create meta objects. <br>
     * In this case, a <code>DomainMetaClass</code> can be deleted even if it
     * has existing {@link DomainMetaObject}s.
     **/
    protected void massDelete() {
	checkFrameworkNotInitialized();
	for (DomainMetaClass metaSubclass : getDomainMetaSubclasses()) {
	    metaSubclass.massDelete();
	}

	System.out.println("[MetaClasses] Deleted metaClass "
		+ ((getDomainClass() == null) ? "" : getDomainClass().getSimpleName()));
	for (DomainConsistencyPredicate domainConsistencyPredicate : getDeclaredConsistencyPredicates()) {
	    domainConsistencyPredicate.classDelete();
	}

	for (DomainMetaObject metaObject : getExistingDomainMetaObjects()) {
	    metaObject.delete();
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
