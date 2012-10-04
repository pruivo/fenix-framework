package pt.ist.fenixframework.example.helloworld;

import java.util.Iterator;
import java.util.TreeMap;

// import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Config;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.MultiConfig;
import pt.ist.fenixframework.adt.bplustree.AbstractNode;
import pt.ist.fenixframework.adt.bplustree.BPlusTree;
import pt.ist.fenixframework.adt.bplustree.InnerNode;
import pt.ist.fenixframework.adt.bplustree.LeafNode;
import pt.ist.fenixframework.backend.infinispan.InfinispanConfig;
import pt.ist.fenixframework.backend.infinispan.OID;
import pt.ist.fenixframework.backend.mem.DefaultConfig;
import pt.ist.fenixframework.core.*;
import pt.ist.fenixframework.dml.DomainModel;


import pt.ist.fenixframework.core.AbstractDomainObject;
import pt.ist.fenixframework.core.Externalization;

public class Main2 {
    private static final Logger logger = Logger.getLogger(Main2.class);

    static HelloWorldApplication app;

    public static void main(final String[] args) {
        // Config config = new DefaultConfig() {{
        //     domainModelURLs = resourceToURLArray("helloworld.dml");
        //     // rootClass = HelloWorldApplication.class;
        // }};
        MultiConfig configs = new MultiConfig();
        Config mem = new DefaultConfig() {{
            domainModelURLs = resourcesToURLArray("fenix-framework-domain-root.dml", "fenix-framework-adt-bplustree.dml", "helloworld.dml");
            // rootClass = HelloWorldApplication.class;
        }};
        configs.add(mem);

        Config ispn = new InfinispanConfig() {{
            domainModelURLs = resourcesToURLArray("fenix-framework-domain-root.dml", "fenix-framework-adt-bplustree.dml", "helloworld.dml");
            ispnConfigFile = "infinispanNoFile.xml";
        }};
        configs.add(ispn);


        if (FenixFramework.isInitialized()) {
            logger.info("Framework automagically initialized!!!");
        } else {
            logger.info("Initializing Framework manually");
            FenixFramework.initialize(configs);
        }

        try {
            // Root r = FenixFramework.getDomainRoot();
            doit();
            

            
        } finally {
            FenixFramework.shutdown();
        }
        System.out.println("Last line of main()");
    }

    public static void displayTreeMap(TreeMap t) {
        System.out.println("Displaying TreeMap pairs");
        for (Object o : t.keySet()) {
            System.out.println("<" + o + "  - " + t.get(o) + ">");

        }
    }



    @Atomic
    private static void doit() {

        HelloWorldApplication app2 = new HelloWorldApplication();
        
        // SharedIdentityMap m = SharedIdentityMap.getCache();
        // TreeMap tm = new TreeMap(pt.ist.fenixframework.core.adt.bplustree.BPlusTree.COMPARATOR_SUPPORTING_LAST_KEY);
        // OID oid1 = new OID("java.lang.String@123");
        // OID oid2 = new OID("java.lang.String@123");

        // System.out.println("  ==  ?: " + (oid1 == oid2));
        // System.out.println("EQUALS?: " + oid1.equals(oid2));
        // System.out.println("HASH1  : " + oid1.hashCode());
        // System.out.println("HASH2  : " + oid2.hashCode());
            
        // AbstractDomainObject o1 = new Person("p1", app2);
        // AbstractDomainObject o2 = new Person("p2", app2);
        // System.out.println("o1 = " + o1);
        // System.out.println("o2 = " + o2);
            

        // Object ret1 = m.cache(oid1, o1);
        // Object ret2 = m.cache(oid2, o2);
        // System.out.println("ret1 = " + (AbstractDomainObject)ret1);
        // System.out.println("ret2 = " + (AbstractDomainObject)ret2);

        // System.out.println("put. previous = " + tm.put(oid1, o1));
        // System.out.println("put. previous = " + tm.put(oid2, o2));
        // System.out.println("map.size = " + tm.size());

        BPlusTree bpt = new BPlusTree();
        // bpt.insert("1", "whatever");
        // bpt.insert("2", app2);
        // bpt.insert("1", app2);
        // bpt.insert("1", "yet");
        // bpt.insert("2", "extra");
        bpt.insert(1.1, "whatever");
        bpt.insert(2.1, app2);
        bpt.insert(1.1, app2);
        bpt.insert(1.1, "yet");
        bpt.insert(2.2, "extra");

        bpt.remove(1.1);
        bpt.remove(2.1);
        bpt.remove(2.2);


        bpt.insert("1", "whatever");
        bpt.insert("2", app2);
        bpt.insert("1", app2);
        bpt.insert("1", "yet");
        bpt.insert("2", "extra");

        System.out.println("SIZE = " + bpt.size());
        
        Iterator it = bpt.iterator();
        while (it.hasNext()) {
            System.out.println(" -> " + it.next());
        }


        System.out.println("====================================== TreeMap externalization");
        LeafNode n = (LeafNode)bpt.getRoot();
        System.out.println("Leafnode: " + n);
            
        TreeMap entries = n.getEntries();
        displayTreeMap(entries);

        byte[] extern = Externalization.externalizeObject(entries);
        TreeMap t1 = Externalization.internalizeObject(extern);
        displayTreeMap(t1);

        bpt.insert("2", new String("extrsa"));
        System.out.println("Extern/Inter ok?: " + t1.equals(n.getEntries()));
        
    }

    @Atomic
    private static void addNewPeople(HelloWorldApplication app, String[] args) {
        for (String name : args) {
            app.addPerson(new Person(name, app));
        }
        greetAll(app);
    }

    @Atomic
    private static void greetAll(HelloWorldApplication app) {
        app.sayHello();
    }
}
