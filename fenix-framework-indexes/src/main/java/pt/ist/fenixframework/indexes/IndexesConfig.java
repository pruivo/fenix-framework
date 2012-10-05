package pt.ist.fenixframework.indexes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.ist.fenixframework.Config;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.adt.bplustree.BPlusTree;
import pt.ist.fenixframework.dml.AnnotatedSlot;
import pt.ist.fenixframework.dml.Annotation;
import pt.ist.fenixframework.dml.DomainClass;
import pt.ist.fenixframework.dml.DomainModel;
import pt.ist.fenixframework.dml.Role;

public abstract class IndexesConfig extends Config {

    private static final String domainRootClassName = DomainRoot.class.getName();
    
    /** This option states whether the initialization should lookout for objects that should be 
     * indexed but are not. An example of why this may happen: the persistence was already 
     * populated a priori and an indexed annotation was now put in some slot. **/
    private boolean updateLostIndexes = false;
    
    public boolean isUpdateLostIndexes() {
	return updateLostIndexes;
    }
    
    protected void updateIndexes() {
	// Ensure the root index tree exists
	DomainRoot domRoot = FenixFramework.getDomainRoot();
	BPlusTree<BPlusTree> rootIndex = domRoot.getIndexRoot();
	if (rootIndex == null) {
	    rootIndex = new BPlusTree<BPlusTree>();
	    domRoot.setIndexRoot(rootIndex);
	}
	
	// Now check for new and unused slot indexes
	List<AnnotatedSlot> indexedSlots = FenixFramework.getDomainModel().getAnnotatedSlots().get(Annotation.INDEX_ANNOTATION);
	Set<AnnotatedSlot> newlyIndexedSlots = new HashSet<AnnotatedSlot>();
	Set<String> persistedIndexedSlots = (Set<String>)rootIndex.getKeys();
	for (AnnotatedSlot annSlot : indexedSlots) {
	    String key = annSlot.getDomainClass().getFullName() + "." + annSlot.getSlot().getName();
	    BPlusTree slotIndexTree = rootIndex.get(key);
	    if (slotIndexTree == null) {
		slotIndexTree = new BPlusTree();
		rootIndex.insert(key, slotIndexTree);
		
		// This slot is newly annotated. Do we want to recover the index?
		if (updateLostIndexes) {
		    newlyIndexedSlots.add(annSlot);
		}
	    }
	    
	    // Remove this slots' key from the known ones, meaning its still being used
	    persistedIndexedSlots.remove(key);
	}
	
	// For each key left out, we know that the corresponding slot is no longer annotated. Thus, delete its index tree.
	for (String key : persistedIndexedSlots) {
	    rootIndex.remove(key);
	}
	
	// Recover the indexes for newly annotated slots
	if (updateLostIndexes) {
	    DomainModel domainModel = FenixFramework.getDomainModel();
	    DomainClass rootClass = domainModel.findClass(domainRootClassName);
	    List<Role> roles = rootClass.getRoleSlotsList();
	}
	
    }

    
    
}
