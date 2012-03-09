package pt.ist.fenixframework.example.bankconsistency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import jvstm.cps.DependenceRecord;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.PersistenceFenixFrameworkRoot;
import pt.ist.fenixframework.pstm.PersistentDomainMetaClass;
import pt.ist.fenixframework.pstm.PersistentDomainMetaObject;
import pt.ist.fenixframework.pstm.consistencyPredicates.KnownConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.PersistentDependenceRecord;
import pt.ist.fenixframework.pstm.consistencyPredicates.PublicConsistencyPredicate;

public class BankConsistencyApplication extends BankConsistencyApplication_Base {

    public static BankConsistencyApplication getInstance() {
	return FenixFramework.getRoot();
    }

    public BankConsistencyApplication() {
	super();
	checkIfIsSingleton();
    }

    private void checkIfIsSingleton() {
	if (FenixFramework.getRoot() != null && FenixFramework.getRoot() != this) {
	    throw new Error("There can be only one instance of BankConsistencyApplication");
	}
    }

    public static class DomainPrinter {
	public static void printDomain() {
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out
		    .println("=========================== DOMAIN METACLASSES AND KNOWN CONSISTENCY PREDICATES ===========================");
	    Set<PersistentDomainMetaClass> visitedMetaClasses = new HashSet<PersistentDomainMetaClass>();
	    for (PersistentDomainMetaClass metaClass : PersistenceFenixFrameworkRoot.getInstance()
		    .getPersistentDomainMetaClasses()) {
		if (visitedMetaClasses.contains(metaClass)) {
		    continue;
		}
		System.out.println();
		System.out.println();
		System.out.println();
		while (metaClass.getPersistentDomainMetaSuperclass() != null) {
		    metaClass = metaClass.getPersistentDomainMetaSuperclass();
		}
		printMetaClassAndSubclasses(metaClass, visitedMetaClasses);
	    }
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println("=========================== CURRENT DOMAIN DATA ===========================");
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println("=========================== CLIENTS AND ACCOUNTS ===========================");
	    for (Client client : BankConsistencyApplication.getInstance().getClients()) {
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println(client);
		printDomainObjectDependencies(client);
		System.out.println(client.getClientInfo());
		printDomainObjectDependencies(client.getClientInfo());
		if (client.hasAnyAccounts()) {
		    System.out.println("Owned accounts:");
		    for (Account account : client.getAccounts()) {
			System.out.println(account);
			printDomainObjectDependencies(account);
		    }
		    System.out.println();
		}
	    }
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println("=========================== COMPANIES AND INVOLVED CLIENTS ===========================");
	    for (Company company : BankConsistencyApplication.getInstance().getCompanies()) {
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println(company);
		printDomainObjectDependencies(company);
		System.out.println("Associated Clients:");
		for (Client client : company.getClients()) {
		    System.out.println(client);
		}
		System.out.println();
	    }
	}

	private static void printMetaClassAndSubclasses(PersistentDomainMetaClass metaClass,
		Set<PersistentDomainMetaClass> visitedMetaClasses) {
	    System.out.println();
	    String classDescription = metaClass.getDomainClass().toString();
	    PersistentDomainMetaClass metaSuperclass = metaClass.getPersistentDomainMetaSuperclass();
	    if (metaSuperclass != null) {
		classDescription += " extends ^ " + metaSuperclass.getDomainClass();
	    }
	    System.out.println(classDescription);
	    System.out.println(metaClass.getExistingPersistentDomainMetaObjectsCount() + " existing objects");
	    printDeclaredKnownConsistencyPredicates(metaClass);
	    visitedMetaClasses.add(metaClass);

	    for (PersistentDomainMetaClass metaSubclass : metaClass.getPersistentDomainMetaSubclasses()) {
		printMetaClassAndSubclasses(metaSubclass, visitedMetaClasses);
	    }
	}

	private static void printDeclaredKnownConsistencyPredicates(PersistentDomainMetaClass metaClass) {
	    if (metaClass.getDeclaredConsistencyPredicatesCount() == 0) {
		return;
	    }
	    System.out.println("Declaring predicates: ");
	    for (KnownConsistencyPredicate declaredConsistencyPredicate : metaClass.getDeclaredConsistencyPredicates()) {
		if (declaredConsistencyPredicate.isPrivate()) {
		    System.out.println(declaredConsistencyPredicate.getPredicate());
		} else if (declaredConsistencyPredicate.isPublic()) {
		    PublicConsistencyPredicate pcp = (PublicConsistencyPredicate) declaredConsistencyPredicate;
		    String predicateDescription = pcp.getPredicate().toString();
		    PublicConsistencyPredicate overriddenPredicate = pcp.getPublicConsistencyPredicateOverridden();
		    if (overriddenPredicate != null) {
			predicateDescription += " overrides: " + overriddenPredicate.getPredicate();
		    }
		    System.out.println(predicateDescription);
		}
		printAssociatedDependenceRecords(declaredConsistencyPredicate);
	    }
	}

	private static void printAssociatedDependenceRecords(KnownConsistencyPredicate knownConsistencyPredicate) {
	    if (knownConsistencyPredicate.getPersistentDependenceRecordsCount() == 0) {
		return;
	    }
	    System.out.println("Existing DependenceRecords: (all, including inconsistent)");
	    for (PersistentDependenceRecord dependenceRecord : knownConsistencyPredicate.getPersistentDependenceRecords()) {
		System.out.println("\tDependent object " + dependenceRecord.getDependent() + " is "
			+ ((dependenceRecord.isConsistent()) ? "consistent" : "INCONSISTENT!"));
		System.out.println("\tDepended Objects: ");
		for (PersistentDomainMetaObject dependedObject : dependenceRecord.getDependedDomainMetaObjects()) {
		    System.out.println("\t\t" + dependedObject.getDomainObject());
		}
	    }

	    if (knownConsistencyPredicate.getInconsistentDependenceRecordsCount() == 0) {
		return;
	    }
	    System.out.println("INCONSISTENT DependenceRecords: (inconsistent only)");
	    for (PersistentDependenceRecord inconsistentDependenceRecord : knownConsistencyPredicate
		    .getInconsistentDependenceRecords()) {
		if (inconsistentDependenceRecord.isConsistent()) {
		    throw new Error("A CONSISTENT DependenceRecord was found in the inconsistent DependenceRecords list of "
			    + knownConsistencyPredicate.getPersistentDomainMetaClass().getDomainClass());
		}
		System.out.println("\tDependent object " + inconsistentDependenceRecord.getDependent() + " is INCONSISTENT!");
		System.out.println("\tDepended Objects: ");
		for (PersistentDomainMetaObject dependedObject : inconsistentDependenceRecord.getDependedDomainMetaObjects()) {
		    System.out.println("\t\t" + dependedObject.getDomainObject());
		}
	    }
	}

	private static void printDomainObjectDependencies(AbstractDomainObject obj) {
	    PersistentDomainMetaObject dependedMetaObject = null;
	    try {
		Method getMetaObjectMethod = AbstractDomainObject.class.getDeclaredMethod("getMetaObject");
		getMetaObjectMethod.setAccessible(true);
		dependedMetaObject = (PersistentDomainMetaObject) getMetaObjectMethod.invoke(obj);
	    } catch (SecurityException e) {
		e.printStackTrace();
		return;
	    } catch (NoSuchMethodException e) {
		e.printStackTrace();
		return;
	    } catch (InvocationTargetException e) {
		e.printStackTrace();
		return;
	    } catch (IllegalAccessException e) {
		e.printStackTrace();
		return;
	    }

	    if (dependedMetaObject != null) {
		System.out.println('\t' + "This DO defines the consistency of:");
		for (DependenceRecord dependenceRecord : dependedMetaObject.getDependenceRecords()) {
		    System.out.println('\t' + dependenceRecord.getPredicate().getName() + "() "
			    + dependenceRecord.getDependent().toString());
		}
		System.out.println();
	    }
	}
    }
}
