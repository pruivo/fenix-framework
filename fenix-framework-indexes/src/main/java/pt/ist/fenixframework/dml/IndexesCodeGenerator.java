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
    public static final String BPLUS_TREE_FULL_CLASS = BPlusTree.class.getName();
    
    public IndexesCodeGenerator(CompilerArgs compArgs, DomainModel domainModel) {
	super(compArgs, domainModel);
    }

    /*
     * The following mimics the abstract CodeGenerator structure to produce the 
     * setter. However, it now passes around both the DomainClass and Slot instances
     * so that we can use them within the method body production to check if the slot 
     * is being indexed and in that case add the indexation behavior.
     */
    
    @Override
    protected void generateBaseClassBody(DomainClass domClass, PrintWriter out) {
        generateStaticSlots(domClass, out);
        newline(out);

        generateSlots(domClass.getSlots(), out);
        newline(out);

        generateRoleSlots(domClass.getRoleSlots(), out);
        newline(out);

        generateInitInstance(domClass, out);

        // constructors
        newline(out);
        printMethod(out, "protected", "", domClass.getBaseName());
        startMethodBody(out);
        generateBaseClassConstructorsBody(domClass, out);
        endMethodBody(out);
        
        // slots getters/setters
        // Note: difference is in the following two calls
        generateSlotsAccessors(domClass, out);
        
        generateIndexMethods(domClass, out);
        
        // roles methods
        generateRoleSlotsMethods(domClass.getRoleSlots(), out);

        // // generate slot consistency predicates
        // generateSlotConsistencyPredicates(domClass, out);
    }
    
    protected void generateSlotsAccessors(DomainClass domClass, PrintWriter out) {
	Iterator<Slot> slotsIter = domClass.getSlots();
        while (slotsIter.hasNext()) {
            generateSlotAccessors(domClass.getFullName(), (Slot) slotsIter.next(), out);
        }
    }
    
    protected void generateSlotAccessors(String fullDomainClassName, Slot slot, PrintWriter out) {
        generateSlotGetter(slot.getName(), slot.getTypeName(), out);
        generateSlotSetter(fullDomainClassName, slot, out);
    }
    
    protected void generateSlotSetter(String fullDomainClassName, Slot slot, PrintWriter out) {
        generateSetter(fullDomainClassName, slot, "public", "set" + capitalize(slot.getName()), out);
    }
    
    protected void generateSetter(String fullDomainClassName, Slot slot, String visibility, String setterName, PrintWriter out) {
        newline(out);

        printFinalMethod(out, visibility, "void", setterName, makeArg(slot.getTypeName(), slot.getName()));

        startMethodBody(out);
        generateSetterBody(fullDomainClassName, slot, setterName, out);
        endMethodBody(out);            
    }
    
    protected void generateSetterBody(String fullDomainClassName, Slot slot, String setterName, PrintWriter out) {
	if (slot.hasIndexedAnnotation()) {
	    generateIndexationInSetter(fullDomainClassName, slot, out);
	}
        printWords(out, getSlotExpression(slot.getName()), "=", slot.getName() + ";");
    }
    
    protected void generateIndexationInSetter(String fullDomainClassName, Slot slot, PrintWriter out) {
	// Initialize the Index trees if needed.
	// TODO move this code to the bootstrap process
	generateIndexesInitialization(fullDomainClassName, slot, out);
	
	// Check if the previous field was null. If not, remove it from the index.
	print(out, "if (");
	print(out, getSlotExpression(slot.getName()));
	print(out, "!= null)");
	newBlock(out);
	print(out, "((");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, ">)");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot()).get(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	print(out, ").remove(");
	print(out, getSlotExpression(slot.getName()));
	print(out, ");");
	closeBlock(out);
	
	// Check if the new field value is null. If not, add it to the index.
	print(out, "if (");
	print(out, slot.getName());
	print(out, "!= null)");
	newBlock(out);
	print(out, "((");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, ">)");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot()).get(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	print(out, ").insert(");
	print(out, slot.getName());
	print(out, ", this);");
	closeBlock(out);
    }

    protected void generateIndexesInitialization(String fullDomainClassName, Slot slot, PrintWriter out) {
	print(out, "if (");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot()");
	print(out, " == null)");
	newBlock(out);
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().setIndexRoot(new ");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, ">());");
	closeBlock(out);
	
	print(out, "if (");
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot().get(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	print(out, ") == null)");
	newBlock(out);
	print(out, FENIX_FRAMEWORK_FULL_CLASS);
	print(out, ".getDomainRoot().getIndexRoot().insert(");
	print(out, getIndexedFieldKey(fullDomainClassName, slot.getName()));
	print(out, ", new ");
	print(out, BPLUS_TREE_FULL_CLASS);
	print(out, "<");
	print(out, fullDomainClassName);
	print(out, ">());");
	closeBlock(out);
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
	// Initialize the Index trees if needed.
	// TODO move this code to the bootstrap process
	generateIndexesInitialization(fullDomainClassName, slot, out);
	
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
	return "findBy" + slotName.getName();
    }
    
}
