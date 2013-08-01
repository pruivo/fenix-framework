package pt.ist.fenixframework.adt.bplustree;

import java.io.Serializable;
import java.util.UUID;

import pt.ist.fenixframework.dml.runtime.DomainBasedMap;

import eu.cloudtm.Constants;
import eu.cloudtm.LocalityHints;

public class ColocatedBPlusTreeGhost<T extends Serializable> extends ColocatedBPlusTreeGhost_Base {

    public ColocatedBPlusTreeGhost() {
	// this is not used by the code generator
    }
    
    public ColocatedBPlusTreeGhost(LocalityHints localityHints, String relationName) {
    	super(new LocalityHints(new String[]{Constants.GROUP_ID, (localityHints == null ? UUID.randomUUID().toString() : localityHints.get(Constants.GROUP_ID)) +
    			DomainBasedMap.RELATION_NAME_SEPARATOR + relationName}));
    }

}
