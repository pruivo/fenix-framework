package pt.ist.fenixframework.example.helloworld;

import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.Callable;

// import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Config;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.MultiConfig;
import pt.ist.fenixframework.ValueTypeSerializer;
import pt.ist.fenixframework.core.adt.bplustree.AbstractNode;
import pt.ist.fenixframework.core.adt.bplustree.BPlusTree;
import pt.ist.fenixframework.core.adt.bplustree.InnerNode;
import pt.ist.fenixframework.core.adt.bplustree.LeafNode;
import pt.ist.fenixframework.backend.infinispan.InfinispanConfig;
import pt.ist.fenixframework.backend.mem.MemConfig;
import pt.ist.fenixframework.dml.DomainModel;


import pt.ist.fenixframework.core.AbstractDomainObject;
import pt.ist.fenixframework.core.Externalization;

public class MainWithTransaction {
    private static final Logger logger = Logger.getLogger(MainWithTransaction.class);

    static HelloWorldApplication app;

    public static void main(final String[] args) {
        try {
            // doit(args);
        } finally {
            FenixFramework.shutdown();
        }
        System.out.println("Last line of main()");
    }

    public static void doit(String [] args) {
        Callable call = new Callable() {
                public Object call() {
                    System.out.println("Calling me!");
                    return null;
                }
            };

        // AtomicContext ctx = new AtomicContext() {
        //         public void doTransactional
        //     };
        // Transaction.withTransaction(,);

    }

    @Atomic
    public static void also() {
        System.out.println("here??!?!?!?!??!?!?!!??!!?!?!??!!?");
    }

}
