package pt.ist.fenixframework.pstm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.consistencyPredicates.ConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.DomainConsistencyPredicate;
import dml.DomainClass;
import dml.DomainModel;

/**
 * The DomainFenixFrameworkRoot is a singleton root object that is related to
 * all the {@link DomainMetaClass}es in the system.
 * 
 * The initialize method is called during the initialization of the
 * {@link FenixFramework}. This method is responsible for the initialization of
 * the {@link DomainMetaClass}es, and the {@link DomainConsistencyPredicates}.
 * It creates the persistent versions of new domain classes and predicates that
 * have been detected in the code, and deletes old ones that have been removed.
 **/
@NoDomainMetaObjects
public class DomainFenixFrameworkRoot extends DomainFenixFrameworkRoot_Base {

    public static final String ROOT_KEY = "pt.ist.fenixframework.pstm.DomainFenixFrameworkRoot";

    private static Map<Class<? extends AbstractDomainObject>, DomainClass> existingDMLDomainClasses;
    private static final Map<Class<? extends AbstractDomainObject>, DomainMetaClass> existingDomainMetaClasses = new HashMap<Class<? extends AbstractDomainObject>, DomainMetaClass>();

    public static DomainFenixFrameworkRoot getInstance() {
	return FenixFramework.getDomainFenixFrameworkRoot();
    }

    public DomainFenixFrameworkRoot() {
	super();
	checkIfIsSingleton();
    }

    private void checkIfIsSingleton() {
	if (FenixFramework.getDomainFenixFrameworkRoot() != null
		&& FenixFramework.getDomainFenixFrameworkRoot() != this) {
	    throw new Error("There can be only one instance of " + getClass().getSimpleName());
	}
    }

    public DomainMetaClass getDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	return existingDomainMetaClasses.get(domainClass);
    }

    @Override
    public void removeDomainMetaClasses(DomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	if (domainClass != null) {
	    existingDomainMetaClasses.remove(metaClass.getDomainClass());
	    existingDomainMetaClasses.remove(metaClass.getDomainClass().getSuperclass());
	}
	super.removeDomainMetaClasses(metaClass);
    }

    private void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    @Override
    public void addDomainMetaClasses(DomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	existingDomainMetaClasses.put(metaClass.getDomainClass(), metaClass);
	// The metaClass for the base class is the same as the regular domain class
	existingDomainMetaClasses.put((Class<? extends AbstractDomainObject>) metaClass.getDomainClass().getSuperclass(),
		metaClass);
	super.addDomainMetaClasses(metaClass);
    }

    // Init methods called during the FenixFramework init
    public void initialize(DomainModel domainModel) {
	checkFrameworkNotInitialized();
	if (FenixFramework.canCreateDomainMetaObjects()) {
	    initializeDomainMetaClasses(domainModel);
	    initializeDomainConsistencyPredicates();
	} else {
	    deleteAllMetaData();
	}
    }

    // Init methods for DomainMetaClasses
    private void initializeDomainMetaClasses(DomainModel domainModel) {
	existingDMLDomainClasses = getExistingDomainClasses(domainModel);
	Set<DomainMetaClass> oldMetaClassesToRemove = new HashSet<DomainMetaClass>();

	for (DomainMetaClass metaClass : getDomainMetaClasses()) {
	    Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	    if ((domainClass == null) || (!existingDMLDomainClasses.keySet().contains(domainClass))) {
		oldMetaClassesToRemove.add(metaClass);
	    } else {
		existingDomainMetaClasses.put(domainClass, metaClass);
		//The base class has the same meta class as the regular domain class.
		existingDomainMetaClasses.put((Class<? extends AbstractDomainObject>) domainClass.getSuperclass(),
			metaClass);
	    }
	}

	if (!oldMetaClassesToRemove.isEmpty()) {
	    processOldClasses(oldMetaClassesToRemove);
	}

	Set<DomainMetaClass> existingMetaClassesToUpdate = new TreeSet<DomainMetaClass>(
		DomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	existingMetaClassesToUpdate.addAll(existingDomainMetaClasses.values());
	if (!existingMetaClassesToUpdate.isEmpty()) {
	    processExistingMetaClasses(existingMetaClassesToUpdate);
	}

	Set<Class<? extends AbstractDomainObject>> newClassesToAdd = new HashSet<Class<? extends AbstractDomainObject>>(
		existingDMLDomainClasses.keySet());
	newClassesToAdd.removeAll(existingDomainMetaClasses.keySet());
	if (!newClassesToAdd.isEmpty()) {
	    processNewClasses(newClassesToAdd);
	}
    }

    private Map<Class<? extends AbstractDomainObject>, DomainClass> getExistingDomainClasses(DomainModel domainModel) {
	Map<Class<? extends AbstractDomainObject>, DomainClass> existingDomainClasses = new HashMap<Class<? extends AbstractDomainObject>, DomainClass>();
	Iterator<DomainClass> domainClassesIterator = domainModel.getClasses();
	try {
	    while (domainClassesIterator.hasNext()) {
		DomainClass dmlDomainClass = domainClassesIterator.next();
		Class<? extends AbstractDomainObject> domainClass = (Class<? extends AbstractDomainObject>) Class
			.forName(dmlDomainClass.getFullName());

		if (!domainClass.isAnnotationPresent(NoDomainMetaObjects.class)) {
		    existingDomainClasses.put(domainClass, dmlDomainClass);
		}
	    }
	    return existingDomainClasses;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    private void processOldClasses(Collection<DomainMetaClass> oldMetaClassesToRemove) {
	deleteOldMetaClasses(oldMetaClassesToRemove);
    }

    private void deleteOldMetaClasses(Collection<DomainMetaClass> oldMetaClassesToRemove) {
	for (DomainMetaClass metaClass : oldMetaClassesToRemove) {
	    metaClass.delete();
	}
    }

    private void processExistingMetaClasses(Collection<DomainMetaClass> existingMetaClassesToUpdate) {
	updateExistingMetaClasses(existingMetaClassesToUpdate);
    }

    private void updateExistingMetaClasses(Collection<DomainMetaClass> existingMetaClassesToUpdate) {
	for (DomainMetaClass metaClass : existingMetaClassesToUpdate) {
	    if (!metaClass.hasDomainMetaSuperclass()) {
		if (hasSuperclassInDML(metaClass)) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    } else {
		DomainMetaClass currentMetaSuperclass = null;
		if (hasSuperclassInDML(metaClass)) {
		    currentMetaSuperclass = getDomainMetaSuperclassFromDML(metaClass);
		}
		if (currentMetaSuperclass != metaClass.getDomainMetaSuperclass()) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    }
	}
    }

    private boolean hasSuperclassInDML(DomainMetaClass metaClass) {
	DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass()).getSuperclass();
	return (dmlDomainSuperclass != null);
    }

    private DomainMetaClass getDomainMetaSuperclassFromDML(DomainMetaClass metaClass) {
	try {
	    DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass())
		    .getSuperclass();
	    Class<? extends AbstractDomainObject> domainSuperclass = (Class<? extends AbstractDomainObject>) Class
		    .forName(dmlDomainSuperclass.getFullName());
	    return existingDomainMetaClasses.get(domainSuperclass);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    private void processNewClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToAdd) {
	createNewMetaClasses(newClassesToAdd);
    }

    private void createNewMetaClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToAdd) {
	Set<DomainMetaClass> createdMetaClasses = new TreeSet<DomainMetaClass>(
		DomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	for (Class<? extends AbstractDomainObject> domainClass : newClassesToAdd) {
	    DomainMetaClass newDomainMetaClass = new DomainMetaClass(domainClass);
	    createdMetaClasses.add(newDomainMetaClass);
	}

	for (DomainMetaClass metaClass : createdMetaClasses) {
	    Transaction.beginTransaction();
	    if (hasSuperclassInDML(metaClass)) {
		metaClass.initDomainMetaSuperclass(getDomainMetaSuperclassFromDML(metaClass));
	    }
	    metaClass.initExistingDomainObjects();
	}
    }

    // End of init methods for DomainMetaClasses

    // Init methods for DomainConsistencyPredicates
    private void initializeDomainConsistencyPredicates() {
	Set<Method> newPredicatesToAdd = new HashSet<Method>();
	Set<DomainConsistencyPredicate> oldPredicatesToRemove = new HashSet<DomainConsistencyPredicate>();
	Map<Method, DomainConsistencyPredicate> existingDomainPredicates = new HashMap<Method, DomainConsistencyPredicate>();

	Set<DomainMetaClass> existingMetaClassesSorted = new TreeSet<DomainMetaClass>(
		DomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	existingMetaClassesSorted.addAll(existingDomainMetaClasses.values());
	for (DomainMetaClass metaClass : existingMetaClassesSorted) {

	    Set<Method> existingPredicates = getDeclaredConsistencyPredicateMethods(metaClass);

	    for (DomainConsistencyPredicate declaredConsistencyPredicate : metaClass.getDeclaredConsistencyPredicates()) {
		Method predicateMethod = declaredConsistencyPredicate.getPredicate();
		if ((predicateMethod == null)
			|| (!predicateMethod.isAnnotationPresent(ConsistencyPredicate.class) && !predicateMethod
				.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class))) {
		    oldPredicatesToRemove.add(declaredConsistencyPredicate);
		} else {
		    existingDomainPredicates.put(declaredConsistencyPredicate.getPredicate(), declaredConsistencyPredicate);
		}
	    }

	    newPredicatesToAdd.addAll(existingPredicates);
	    newPredicatesToAdd.removeAll(existingDomainPredicates.keySet());

	    if (!newPredicatesToAdd.isEmpty()) {
		processNewPredicates(newPredicatesToAdd, metaClass);
		newPredicatesToAdd.clear();
	    }
	    if (!oldPredicatesToRemove.isEmpty()) {
		processOldPredicates(oldPredicatesToRemove, metaClass);
		oldPredicatesToRemove.clear();
	    }
	    if (!existingDomainPredicates.isEmpty()) {
		processExistingPredicates(existingDomainPredicates.values(), metaClass);
		existingDomainPredicates.clear();
	    }
	}
    }

    private Set<Method> getDeclaredConsistencyPredicateMethods(DomainMetaClass metaClass) {
	Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	Class<? extends AbstractDomainObject> baseClass = (Class<? extends AbstractDomainObject>) domainClass.getSuperclass();
	Set<Method> declaredMethods = getDeclaredConsistencyPredicateMethods(domainClass);
	declaredMethods.addAll(getDeclaredConsistencyPredicateMethods(baseClass));
	return declaredMethods;
    }

    private Set<Method> getDeclaredConsistencyPredicateMethods(Class<? extends AbstractDomainObject> domainClass) {
	Set<Method> declaredMethods = new HashSet<Method>();
	for (Method predicateMethod : domainClass.getDeclaredMethods()) {
	    if (!predicateMethod.isAnnotationPresent(ConsistencyPredicate.class)
		    && !predicateMethod.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class)) {
		continue;
	    }
	    if (predicateMethod.getParameterTypes().length != 0) {
		throw new Error("Consistency Predicates cannot have parameters! " + predicateMethod);
	    }
	    if (!predicateMethod.getReturnType().toString().equals("boolean")) {
		throw new Error("Consistency Predicates must return a boolean value! " + predicateMethod);
	    }
	    if (!Modifier.isPrivate(predicateMethod.getModifiers()) && !Modifier.isProtected(predicateMethod.getModifiers())
		    && !Modifier.isPublic(predicateMethod.getModifiers())) {
		throw new Error("Consistency Predicates must be private, protected or public! " + predicateMethod);
	    }
	    if (Modifier.isAbstract(predicateMethod.getModifiers())) {
		continue;
	    }

	    declaredMethods.add(predicateMethod);
	}
	return declaredMethods;
    }

    private void processNewPredicates(Set<Method> newPredicatesToAdd, DomainMetaClass metaClass) {
	createAndExecuteNewPredicates(newPredicatesToAdd, metaClass);
    }

    private void createAndExecuteNewPredicates(Set<Method> newPredicatesToAdd, DomainMetaClass metaClass) {
	Set<DomainConsistencyPredicate> createdPredicates = new HashSet<DomainConsistencyPredicate>();

	// First, create all new predicates
	for (Method predicateMethod : newPredicatesToAdd) {
	    createdPredicates.add(DomainConsistencyPredicate.createNewDomainConsistencyPredicate(predicateMethod, metaClass));
	}

	// Second, initialize each predicate's overridden predicate
	for (DomainConsistencyPredicate knownConsistencyPredicate : createdPredicates) {
	    knownConsistencyPredicate.initConsistencyPredicateOverridden();
	}

	// Third, execute the predicates for the affected classes
	for (DomainConsistencyPredicate knownConsistencyPredicate : createdPredicates) {
	    Transaction.beginTransaction();
	    knownConsistencyPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(metaClass);
	}
    }

    private void processOldPredicates(Set<DomainConsistencyPredicate> oldPredicatesToRemove, final DomainMetaClass metaClass) {
	deleteOldPredicates(oldPredicatesToRemove, metaClass);
    }

    private void deleteOldPredicates(Set<DomainConsistencyPredicate> oldPredicatesToRemove, DomainMetaClass metaClass) {
	for (DomainConsistencyPredicate knownConsistencyPredicate : oldPredicatesToRemove) {
	    knownConsistencyPredicate.delete();
	}
    }

    private void processExistingPredicates(Collection<DomainConsistencyPredicate> existingKnownPredicates,
	    DomainMetaClass metaClass) {
	updateExistingPredicates(existingKnownPredicates, metaClass);
    }

    private void updateExistingPredicates(Collection<DomainConsistencyPredicate> existingKnownPredicates,
	    DomainMetaClass metaClass) {
	for (DomainConsistencyPredicate knownConsistencyPredicate : existingKnownPredicates) {
	    knownConsistencyPredicate.updateConsistencyPredicateOverridden();
	}
    }
    // End of init methods for DomainConsistencyPredicates

    private void deleteAllMetaData() {
	for (DomainMetaClass metaClass : getDomainMetaClasses()) {
	    Transaction.beginTransaction();
	    metaClass.massDelete();
	}
    }
}
