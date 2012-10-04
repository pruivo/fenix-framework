package pt.ist.fenixframework.indexes;

import java.util.List;
import java.util.Set;

import pt.ist.fenixframework.Config;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.adt.bplustree.BPlusTree;
import pt.ist.fenixframework.dml.AnnotatedSlot;
import pt.ist.fenixframework.dml.Annotation;

public abstract class IndexesConfig extends Config {

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
	Set<String> persistedIndexedSlots = (Set<String>)rootIndex.getKeys();
	for (AnnotatedSlot annSlot : indexedSlots) {
	    String key = annSlot.getDomainClass().getFullName() + "." + annSlot.getSlot().getName();
	    BPlusTree slotIndexTree = rootIndex.get(key);
	    if (slotIndexTree == null) {
		slotIndexTree = new BPlusTree();
		rootIndex.insert(key, slotIndexTree);
	    }
	    
	    // Remove this slots' key from the known ones, meaning its still being used
	    persistedIndexedSlots.remove(key);
	}
	
	// For each key left out, we know that the corresponding slot is no longer annotated. Thus, delete its index tree.
	for (String key : persistedIndexedSlots) {
	    rootIndex.remove(key);
	}
    }

}
