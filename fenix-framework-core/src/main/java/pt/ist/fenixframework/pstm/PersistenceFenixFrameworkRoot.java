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
import pt.ist.fenixframework.pstm.consistencyPredicates.CannotUseConsistencyPredicates;
import pt.ist.fenixframework.pstm.consistencyPredicates.ConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.KnownConsistencyPredicate;
import dml.DomainClass;
import dml.DomainModel;

/**
 * The PersistenceFenixFrameworkRoot is a singleton root object that is related
 * to all the PresistentDomainMetaClasses in the system.
 * 
 * The initialize method is called during the initialization of the
 * FenixFramework. This method is responsible for the initialization of the
 * PersistentDomainMetaClasses, and the KnownConsistencyPredicates. It creates
 * the persistent versions of new domain Classes and predicates that have been
 * detected in the code, and deletes old ones that have been removed.
 **/
@CannotUseConsistencyPredicates
public class PersistenceFenixFrameworkRoot extends PersistenceFenixFrameworkRoot_Base {

    private static Map<Class<? extends AbstractDomainObject>, DomainClass> existingDMLDomainClasses;
    private static final Map<Class<? extends AbstractDomainObject>, PersistentDomainMetaClass> existingPersistentDomainMetaClasses = new HashMap<Class<? extends AbstractDomainObject>, PersistentDomainMetaClass>();

    public static PersistenceFenixFrameworkRoot getInstance() {
	return FenixFramework.getPersistenceFenixFrameworkRoot();
    }

    public PersistenceFenixFrameworkRoot() {
	super();
	checkIfIsSingleton();
    }

    private void checkIfIsSingleton() {
	if (FenixFramework.getPersistenceFenixFrameworkRoot() != null
		&& FenixFramework.getPersistenceFenixFrameworkRoot() != this) {
	    throw new Error("There can be only one instance of " + getClass().getSimpleName());
	}
    }

    public PersistentDomainMetaClass getPersistentDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	return existingPersistentDomainMetaClasses.get(domainClass);
    }

    @Override
    public void removePersistentDomainMetaClasses(PersistentDomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	if (domainClass != null) {
	    existingPersistentDomainMetaClasses.remove(metaClass.getDomainClass());
	    existingPersistentDomainMetaClasses.remove(metaClass.getDomainClass().getSuperclass());
	}
	super.removePersistentDomainMetaClasses(metaClass);
    }

    private void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    @Override
    public void addPersistentDomainMetaClasses(PersistentDomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	existingPersistentDomainMetaClasses.put(metaClass.getDomainClass(), metaClass);
	// The metaClass for the base class is the same as the regular domain class
	existingPersistentDomainMetaClasses.put((Class<? extends AbstractDomainObject>) metaClass.getDomainClass()
		.getSuperclass(), metaClass);
	super.addPersistentDomainMetaClasses(metaClass);
    }

    // Init methods called during the FenixFramework init
    public void initialize(DomainModel domainModel) {
	checkFrameworkNotInitialized();
	initializePersistentDomainMetaClasses(domainModel);
	initializeKnownConsistencyPredicates();
    }

    // Init methods for PersistentDomainMetaClasses
    private void initializePersistentDomainMetaClasses(DomainModel domainModel) {
	existingDMLDomainClasses = getExistingDomainClasses(domainModel);
	Set<PersistentDomainMetaClass> oldMetaClassesToRemove = new HashSet<PersistentDomainMetaClass>();

	for (PersistentDomainMetaClass metaClass : getPersistentDomainMetaClasses()) {
	    Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	    if ((domainClass == null) || (!existingDMLDomainClasses.keySet().contains(domainClass))) {
		oldMetaClassesToRemove.add(metaClass);
	    } else {
		existingPersistentDomainMetaClasses.put(domainClass, metaClass);
		//The base class has the same meta class as the regular domain class.
		existingPersistentDomainMetaClasses.put((Class<? extends AbstractDomainObject>) domainClass.getSuperclass(),
			metaClass);
	    }
	}

	if (!oldMetaClassesToRemove.isEmpty()) {
	    processOldClasses(oldMetaClassesToRemove);
	}

	Set<PersistentDomainMetaClass> existingMetaClassesToUpdate = new TreeSet<PersistentDomainMetaClass>(
		PersistentDomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	existingMetaClassesToUpdate.addAll(existingPersistentDomainMetaClasses.values());
	if (!existingMetaClassesToUpdate.isEmpty()) {
	    processExistingMetaClasses(existingMetaClassesToUpdate);
	}

	Set<Class<? extends AbstractDomainObject>> newClassesToAdd = new HashSet<Class<? extends AbstractDomainObject>>(
		existingDMLDomainClasses.keySet());
	newClassesToAdd.removeAll(existingPersistentDomainMetaClasses.keySet());
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

		if (!domainClass.isAnnotationPresent(CannotUseConsistencyPredicates.class)) {
		    existingDomainClasses.put(domainClass, dmlDomainClass);
		}
	    }
	    return existingDomainClasses;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    private void processOldClasses(Collection<PersistentDomainMetaClass> oldMetaClassesToRemove) {
	deleteOldMetaClasses(oldMetaClassesToRemove);
    }

    private void deleteOldMetaClasses(Collection<PersistentDomainMetaClass> oldMetaClassesToRemove) {
	for (PersistentDomainMetaClass metaClass : oldMetaClassesToRemove) {
	    metaClass.delete();
	}
    }

    private void processExistingMetaClasses(Collection<PersistentDomainMetaClass> existingMetaClassesToUpdate) {
	updateExistingMetaClasses(existingMetaClassesToUpdate);
    }

    private void updateExistingMetaClasses(Collection<PersistentDomainMetaClass> existingMetaClassesToUpdate) {
	for (PersistentDomainMetaClass metaClass : existingMetaClassesToUpdate) {
	    if (!metaClass.hasPersistentDomainMetaSuperclass()) {
		if (hasSuperclassInDML(metaClass)) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    } else {
		PersistentDomainMetaClass currentMetaSuperclass = null;
		if (hasSuperclassInDML(metaClass)) {
		    currentMetaSuperclass = getPersistentDomainMetaSuperclassFromDML(metaClass);
		}
		if (currentMetaSuperclass != metaClass.getPersistentDomainMetaSuperclass()) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    }
	}
    }

    private boolean hasSuperclassInDML(PersistentDomainMetaClass metaClass) {
	DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass()).getSuperclass();
	return (dmlDomainSuperclass != null);
    }

    private PersistentDomainMetaClass getPersistentDomainMetaSuperclassFromDML(PersistentDomainMetaClass metaClass) {
	try {
	    DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass())
		    .getSuperclass();
	    Class<? extends AbstractDomainObject> domainSuperclass = (Class<? extends AbstractDomainObject>) Class
		    .forName(dmlDomainSuperclass.getFullName());
	    return existingPersistentDomainMetaClasses.get(domainSuperclass);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    private void processNewClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToAdd) {
	createNewMetaClasses(newClassesToAdd);
    }

    private void createNewMetaClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToAdd) {
	Set<PersistentDomainMetaClass> createdMetaClasses = new HashSet<PersistentDomainMetaClass>();
	for (Class<? extends AbstractDomainObject> domainClass : newClassesToAdd) {
	    PersistentDomainMetaClass newPersistentDomainMetaClass = new PersistentDomainMetaClass(domainClass);
	    createdMetaClasses.add(newPersistentDomainMetaClass);
	}

	for (PersistentDomainMetaClass metaClass : createdMetaClasses) {
	    metaClass.initExistingDomainObjects();
	    if (hasSuperclassInDML(metaClass)) {
		metaClass.initPersistentDomainMetaSuperclass(getPersistentDomainMetaSuperclassFromDML(metaClass));
	    }
	}
    }

    // End of init methods for PersistentDomainMetaClasses

    // Init methods for KnownConsistencyPredicates
    private void initializeKnownConsistencyPredicates() {
	Set<Method> newPredicatesToAdd = new HashSet<Method>();
	Set<KnownConsistencyPredicate> oldPredicatesToRemove = new HashSet<KnownConsistencyPredicate>();
	Map<Method, KnownConsistencyPredicate> existingKnownPredicates = new HashMap<Method, KnownConsistencyPredicate>();

	Set<PersistentDomainMetaClass> existingMetaClassesSorted = new TreeSet<PersistentDomainMetaClass>(
		PersistentDomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	existingMetaClassesSorted.addAll(existingPersistentDomainMetaClasses.values());
	for (PersistentDomainMetaClass metaClass : existingMetaClassesSorted) {

	    Set<Method> existingPredicates = getDeclaredConsistencyPredicateMethods(metaClass);

	    for (KnownConsistencyPredicate declaredConsistencyPredicate : metaClass.getDeclaredConsistencyPredicates()) {
		Method predicateMethod = declaredConsistencyPredicate.getPredicate();
		if ((predicateMethod == null)
			|| (!predicateMethod.isAnnotationPresent(ConsistencyPredicate.class) && !predicateMethod
				.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class))) {
		    oldPredicatesToRemove.add(declaredConsistencyPredicate);
		} else {
		    existingKnownPredicates.put(declaredConsistencyPredicate.getPredicate(), declaredConsistencyPredicate);
		}
	    }

	    newPredicatesToAdd.addAll(existingPredicates);
	    newPredicatesToAdd.removeAll(existingKnownPredicates.keySet());

	    if (!newPredicatesToAdd.isEmpty()) {
		processNewPredicates(newPredicatesToAdd, metaClass);
		newPredicatesToAdd.clear();
	    }
	    if (!oldPredicatesToRemove.isEmpty()) {
		processOldPredicates(oldPredicatesToRemove, metaClass);
		oldPredicatesToRemove.clear();
	    }
	    if (!existingKnownPredicates.isEmpty()) {
		processExistingPredicates(existingKnownPredicates.values(), metaClass);
		existingKnownPredicates.clear();
	    }
	}
    }

    private Set<Method> getDeclaredConsistencyPredicateMethods(PersistentDomainMetaClass metaClass) {
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

    private void processNewPredicates(Set<Method> newPredicatesToAdd, PersistentDomainMetaClass metaClass) {
	createAndExecuteNewPredicates(newPredicatesToAdd, metaClass);
    }

    private void createAndExecuteNewPredicates(Set<Method> newPredicatesToAdd, PersistentDomainMetaClass metaClass) {
	Set<KnownConsistencyPredicate> createdPredicates = new HashSet<KnownConsistencyPredicate>();

	// First, create all new predicates
	for (Method predicateMethod : newPredicatesToAdd) {
	    createdPredicates.add(KnownConsistencyPredicate.createNewKnownConsistencyPredicate(predicateMethod, metaClass));
	}

	// Second, initialize each predicate's overridden predicate
	for (KnownConsistencyPredicate knownConsistencyPredicate : createdPredicates) {
	    knownConsistencyPredicate.initKnownConsistencyPredicateOverridden();
	}

	// Third, execute the predicates for the affected classes
	for (KnownConsistencyPredicate knownConsistencyPredicate : createdPredicates) {
	    knownConsistencyPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(metaClass);
	}
    }

    private void processOldPredicates(Set<KnownConsistencyPredicate> oldPredicatesToRemove, PersistentDomainMetaClass metaClass) {
	deleteOldPredicates(oldPredicatesToRemove, metaClass);
    }

    private void deleteOldPredicates(Set<KnownConsistencyPredicate> oldPredicatesToRemove, PersistentDomainMetaClass metaClass) {
	for (KnownConsistencyPredicate knownConsistencyPredicate : oldPredicatesToRemove) {
	    knownConsistencyPredicate.delete();
	}
    }

    private void processExistingPredicates(Collection<KnownConsistencyPredicate> existingKnownPredicates,
	    PersistentDomainMetaClass metaClass) {
	updateExistingPredicates(existingKnownPredicates, metaClass);
    }

    private void updateExistingPredicates(Collection<KnownConsistencyPredicate> existingKnownPredicates,
	    PersistentDomainMetaClass metaClass) {
	for (KnownConsistencyPredicate knownConsistencyPredicate : existingKnownPredicates) {
	    knownConsistencyPredicate.updateKnownConsistencyPredicateOverridden();
	}
    }
    // End of init methods for KnownConsistencyPredicates
}
