package pt.ist.fenixframework.dml;

import java.io.PrintWriter;
import java.util.Iterator;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.adt.bplustree.BPlusTree;

/**
 * This code generator enhances the default generation by adding indexation to fields 
 * annotated to have that behavior. To do so, it produces three tasks:
 *  - Change setters to update the index (and initialize the index tree if needed)
 *  - Add a static method to allow an index search by the field
 * @author nmld
 */
public class IndexesCodeGenerator extends DefaultCodeGenerator {

    public static final String FENIX_FRAMEWORK_FULL_CLASS = FenixFramework.class.getName();
    /** Cannot refer directly to the BPlusTree.class because that would load the class into the VM, and thus load the base class.
     * That is a problem because this class (the CodeGenerator) is loaded when passed to the DmlCompiler. And only after that, will 
     * the base class ever be generated. Thus we have a cyclic dependency and must break it by only using the BPlusTree name. */
    public static final String BPLUS_TREE_FULL_CLASS = "pt.ist.fenixframework.adt.bplustree.BPlusTree";

    public IndexesCodeGenerator(CompilerArgs compArgs, DomainModel domainModel) {
	super(compArgs, domainModel);
    }

    protected void generateIndexationInSetter(DomainClass domainClass, Slot slot, PrintWriter out) {
	if (slot.hasIndexedAnnotation()) {
	    generateIndexationInSetter(domainClass.getFullName(), slot, out);
	}
    }

    protected void generateIndexationInSetter(String fullDomainClassName, Slot slot, PrintWriter out) {
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, " index = ");
	print(out, "((");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, ">)");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot()).get(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	println(out, ");");
	
	// Check if the previous field was null. If not, remove it from the index.
	boolean slotMayBeNull = mayBeNull(slot.getSlotType());
	if (slotMayBeNull) {
	    print(out, "if (");
	    print(out, "get" + capitalize(slot.getName() + "()"));
	    print(out, " != null)");
	    newBlock(out);
	}
	
	print(out, "index.remove(");
	print(out, "get" + capitalize(slot.getName() + "()"));
	println(out, ");");
	
	if (slotMayBeNull) {
	    closeBlock(out);
	}

	// Check if the new field value is null. If not, add it to the index.
	if (slotMayBeNull) {
	    print(out, "if (");
	    print(out, slot.getName());
	    print(out, " != null)");
	    newBlock(out);
	}
	
	print(out, "index.insert(");
	print(out, slot.getName());
	println(out, ", this);");
	
	if (slotMayBeNull) {
	    closeBlock(out);
	}
    }

    private static final String[] primitiveTypes = { "byte", "short", "int", "long", "float", "double", "boolean", "char" };

    private boolean mayBeNull(ValueType slotType) {
	String name = slotType.getFullname();
	for (String primitiveType : primitiveTypes) {
	    if (primitiveType.equals(name)) {
		return false;
	    }
	}
	return true;
    }

    protected String getIndexedFieldKey(String fullDomainClassName, String slotName) {
	return "\"" + fullDomainClassName + "." + slotName + "\"";
    }

    protected void generateIndexMethods(DomainClass domainClass, PrintWriter out) {
	Iterator<Slot> slotsIter = domainClass.getSlots();
	while (slotsIter.hasNext()) {
	    generateSlotSearchIndex(domainClass.getFullName(), (Slot) slotsIter.next(), out);
	}
    }

    protected void generateSlotSearchIndex(String fullDomainClassName, Slot slot, PrintWriter out) {
	if (slot.hasIndexedAnnotation()) {
	    generateStaticIndexMethod(fullDomainClassName, slot, out);
	}
    }

    protected void generateStaticIndexMethod(String fullDomainClassName, Slot slot, PrintWriter out) {
	newline(out);

	printFinalMethod(out, "public static", fullDomainClassName, getStaticIndexMethodName(slot), makeArg(slot.getTypeName(), slot.getName()));

	startMethodBody(out);
	generateStaticIndexMethodBody(fullDomainClassName, slot, out);
	endMethodBody(out);
    }

    protected void generateStaticIndexMethodBody(String fullDomainClassName, Slot slot, PrintWriter out) {
	// Generate the search
	print(out, "return ");
	print(out, "((");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, fullDomainClassName);
	print(out, ">)");
	print(out, "((");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, ">)");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot()).get(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	print(out, ")).get(");
	print(out, slot.getName());
	print(out, ");");
    }

    protected String getStaticIndexMethodName(Slot slotName) {
	return "findBy" + slotName.getName().substring(0, 1).toUpperCase() + slotName.getName().substring(1);
    }

}
