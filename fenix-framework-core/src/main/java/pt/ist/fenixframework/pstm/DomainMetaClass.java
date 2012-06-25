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

import pt.ist.fenixframework.Config;
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

    private static final int MAX_NUM_OF_META_OBJECTS_TO_CREATE = 200000;

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

    public Class<? extends AbstractDomainObject> getDomainClass() {
	String[] strings = getDomainClassName().split(" ");
	String fullyQualifiedClassName = strings[1];
	strings = fullyQualifiedClassName.split("[.]");

	try {
	    Class<?> domainClass = Class.forName(fullyQualifiedClassName);
	    return (Class<? extends AbstractDomainObject>) domainClass;
	} catch (ClassNotFoundException e) {
	    System.out.println("The following domain class has been removed:");
	    System.out.println(e.getMessage());
	    System.out.println();
	    System.out.flush();
	}
	return null;
    }

    public void setDomainClass(Class<? extends AbstractDomainObject> domainClass) {
	checkFrameworkNotInitialized();
	setDomainClassName(domainClass.toString());
    }

    @Override
    public void setDomainClassName(String domainClassName) {
	checkFrameworkNotInitialized();
	super.setDomainClassName(domainClassName);
    }

    /**
     * @return true if this <code>DomainMetaClass</code> was already fully
     *         initialized
     */
    protected boolean isFinalized() {
	return getFinalized();
    }

    @Override
    public void setFinalized(Boolean finalized) {
	checkFrameworkNotInitialized();
	super.setFinalized(finalized);
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

    public <PredicateT extends DomainConsistencyPredicate> PredicateT getDeclaredConsistencyPredicate(String predicateName) {
	try {
	    Method predicateMethod = getDomainClass().getDeclaredMethod(predicateName);
	    return (PredicateT) getDeclaredConsistencyPredicate(predicateMethod);
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
     * {@link FenixFramework}, for each new <code>DomainMetaClass</code>.
     * 
     * Creates a {@link DomainMetaObject} for each existing
     * {@link AbstractDomainObject} of the specified class, and associates the
     * new {@link DomainMetaObject} to its <code>DomainMetaClass</code>.
     */
    protected void initExistingDomainObjects() {
	checkFrameworkNotInitialized();
	AbstractDomainObject existingDO = null;
	List<String> oids = getExistingOIDsWithoutMetaObject(getDomainClass());
	while (!oids.isEmpty()) {
	    for (String oid : oids) {
		try {
		    existingDO = (AbstractDomainObject) AbstractDomainObject.fromOID(Long.valueOf(oid));
		} catch (Exception ex) {
		    System.out.println("WARNING - An exception was thrown while allocating the object: " + getDomainClass()
			    + " - " + oid);
		    ex.printStackTrace();
		    continue;
		}
		DomainMetaObject metaObject = new DomainMetaObject();
		metaObject.setDomainObject(existingDO);
		addExistingDomainMetaObjects(metaObject);
	    }

	    // Commits the current, and starts a new write transaction.
	    // This is necessary to split the load of the mass creation of DomainMetaObjects among several transactions.
	    // Each transaction processes a maximum of 200k objects in order to avoid OutOfMemory exceptions.
	    // Because the JDBC query only returns objects that have no DomainMetaObjects, there is no problem with
	    // processing only an incomplete part of the objects of this class.
	    Transaction.beginTransaction();
	    oids = getExistingOIDsWithoutMetaObject(getDomainClass());
	}
    }

    /**
     * Uses a JDBC query to obtain the OIDs of the existing
     * {@link AbstractDomainObject}s of this class that do not yet have a
     * {@link DomainMetaObject}.<br>
     * <br>
     * This method only returns a <strong>maximum of
     * MAX_NUM_OF_META_OBJECTS_TO_CREATE</strong> OIDs.
     * 
     * @param domainClass
     *            the <code>Class</code> for which to obtain the existing
     *            objects OIDs
     * 
     * @return the <code>List</code> of <code>Strings</code> containing the OIDs
     *         of all the {@link AbstractDomainObject}s of the given class,
     *         without {@link DomainMetaObject}.
     */
    private static List<String> getExistingOIDsWithoutMetaObject(Class<? extends AbstractDomainObject> domainClass) {
	String tableName = getTableName(domainClass);
	String className = domainClass.getName();

	String query = "select OID from " + tableName
		+ ", FF$DOMAIN_CLASS_INFO where OID >> 32 = DOMAIN_CLASS_ID and DOMAIN_CLASS_NAME = '" + className
		+ "' and OID_DOMAIN_META_OBJECT is null limit " + MAX_NUM_OF_META_OBJECTS_TO_CREATE;

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

    private static String getTableName(Class<? extends AbstractDomainObject> domainClass) {
	Class<?> topSuperClass = domainClass;
	while (!topSuperClass.getSuperclass().getSuperclass().equals(OneBoxDomainObject.class)) {
	    //skip to the next non-base superclass
	    topSuperClass = topSuperClass.getSuperclass().getSuperclass();
	}
	return DbUtil.convertToDBStyle(topSuperClass.getSimpleName());
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

	// If we are deleting this class, then the previous subclass will have changed its superclass
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
     * 
     * @see Config#canCreateMetaObjects
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
