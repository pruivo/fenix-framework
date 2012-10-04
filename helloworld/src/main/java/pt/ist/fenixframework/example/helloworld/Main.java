package pt.ist.fenixframework.example.helloworld;

import java.io.Serializable;
import java.lang.annotation.Annotation;
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
import pt.ist.fenixframework.backend.mem.DefaultConfig;
import pt.ist.fenixframework.dml.DomainModel;


import pt.ist.fenixframework.core.AbstractDomainObject;
import pt.ist.fenixframework.core.Externalization;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    static HelloWorldApplication app;

    public static void main(final String[] args) {
        // Config config = new DefaultConfig() {{
        //     domainModelURLs = resourceToURLArray("helloworld.dml");
        //     // rootClass = HelloWorldApplication.class;
        // }};
        MultiConfig configs = new MultiConfig();
        Config mem = new DefaultConfig() {{
            domainModelURLs = resourcesToURLArray("fenix-framework-domain-root.dml", "fenix-framework-bplustree-domain-object.dml", "helloworld.dml");
            // rootClass = HelloWorldApplication.class;
        }};
        configs.add(mem);

        Config ispn = new InfinispanConfig() {{
            domainModelURLs = resourcesToURLArray("fenix-framework-domain-root.dml", "fenix-framework-bplustree-domain-object.dml", "helloworld.dml");
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
            doit(args);
            doitWithTransaction();
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

    @Atomic
    public static void doit(String [] args) {
        // logger.getLogger("pt.ist.fenixframework").setLevel(Level.TRACE);

        // FenixFramework.initialize(null);
        
        // System.out.println("domain model urls:");
        // for (java.net.URL url : FenixFramework.getConfig().getDomainModelURLs()) {
        //     System.out.println(url);
        // }
        // System.out.println("done");

        // DomainModel domainModel = FenixFramework.getDomainModel();
        // System.out.println("DDD: " + domainModel);


        System.out.println("============================ check relation to 1");
        A a = new A();
        B b = new B();

        System.out.println("a.setB(b)");
        a.setB(b);
        System.out.println("done");

        System.out.println("b.getA() = " + b.getA());
        System.out.println("a.getB() = " + a.getB());

        System.out.println("a.addBs(b)");
        a.addBs(b);
        System.out.println("done");

        System.out.println("b.getA1() = " + b.getA1());
        System.out.println("a.getBs() = " + a.getBs());
        System.out.println("a.getBs().size() = " + a.getBs().size());

        System.out.println("============================= Will get the DomainRoot");

        DomainRoot root = FenixFramework.getDomainRoot();
        DomainRoot root2 = FenixFramework.getDomainRoot();

        System.out.println("Root = " + root);
        System.out.println("Root2 = " + root2);

        app = new HelloWorldApplication();
        addNewPeople(app, args);
        greetAll(app);

        System.out.println("App = " + app);
        
        HelloWorldApplication directApp = FenixFramework.getDomainObject(app.getExternalId());
        System.out.println("App again = " + directApp);

        // HelloWorldApplication missingApp = FenixFramework.getDomainObject("4985");  // core backend
        // HelloWorldApplication missingApp = FenixFramework.getDomainObject("pt.ist.fenixframework.example.helloworld.HelloWorldApplication@495cfec4-c697-492b-9c8d-aaaaaaaaaaaa");  // infinispan backend
        // System.out.println("Missing App = " + missingApp);

        System.out.println("====================================== + DomainRoot relation");
        // Person p1 = new Person("eu", app);
        // app.addPpp(p1);
            
        // System.out.println("Relation1 has " + app.getPpp().size() + " element(s).");
        // System.out.println("Relation2 has " + p1.getOwners().size() + " element(s).");

        app.addDomainObjects(app);
        app.addDomainObjects(directApp);

        System.out.println("Relation has " + app.getDomainObjects().size() + " element(s).");
        assert (app.getDomainObjects().size() == 1) : "Wrong number of elements";

        // e agora o print
        for (DomainObject domObject : app.getDomainObjects()) {
            System.out.println("this one has: " + domObject.getExternalId());
        }
        
        // test externalization
        System.out.println("====================================== + Externalization");

        byte[] obj1 = Externalization.externalizeObject(null);
        byte[] obj2 = Externalization.externalizeObject(app);
        byte[] obj3 = Externalization.externalizeObject(app);

        System.out.println("obj1: " + obj1);
        System.out.println("obj2: " + obj2);
        System.out.println("obj3: " + obj3);

        DomainObject return1 = Externalization.internalizeObject(obj1);
        DomainObject return2 = Externalization.internalizeObject(obj2);
        DomainObject return3 = Externalization.internalizeObject(obj3);
        
        System.out.println("null? " + (null == return1));
        System.out.println("app? " + (return3 == return2));

        // OID vs External ID

        AbstractDomainObject person0 = new Person("person0", app);
        AbstractDomainObject person1 = new Person("person1", app);
        AbstractDomainObject person2 = new Person("person2", app);
        AbstractDomainObject person3 = new Person("person3", app);
        AbstractDomainObject person4 = new Person("person4", app);
        AbstractDomainObject person5 = new Person("person5", app);
        AbstractDomainObject person6 = new Person("person6", app);
        AbstractDomainObject person7 = new Person("person7", app);
        AbstractDomainObject person8 = new Person("person8", app);

        System.out.println("OID=" + person0.getOid() + "; ExternalId=" + person0.getExternalId());

        // use B+Tree
        System.out.println("====================================== B+Tree");

        root.setIndex(new BPlusTree<AbstractDomainObject>());

        try {
            root.getIndex().insert(person1);
            root.getIndex().insert(person3);
            root.getIndex().insert(person5);
            root.getIndex().insert(person2);
            root.getIndex().insert(person0);
            root.getIndex().insert(person4);
            root.getIndex().insert(person8);
            root.getIndex().insert(person8);
            root.getIndex().insert(person7);
            root.getIndex().insert(person6);
            root.getIndex().insert(person8);
            root.getIndex().insert(root);


            System.out.println("Will iterate");
            Iterator it = root.getIndex().iterator();
            while (it.hasNext()) {
                AbstractDomainObject ado = (AbstractDomainObject)it.next();
                System.out.println(ado);
            }
            System.out.println("Done iterating");


            // for (int i = 0; i < 302; i++) {
            //     Person p = new Person("Ze", app);
            //     root.getIndex().insert((Comparable)p.getOid(), p);
            // }

            // System.out.println("Registering");
            // for (int i = 0; i < 1; i++) {
            //     Person p = new Person("Ze", app);
            //     root.getIndex().insert((Comparable)p.getOid(), p);
            // }

            System.out.println(root.getIndex().size());
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("====================================== TreeMap externalization");
        LeafNode n = (LeafNode)root.getIndex().getRoot();
        System.out.println("Leafnode: " + n);
            
        TreeMap<Comparable,pt.ist.fenixframework.core.AbstractDomainObject> entries = n.getEntries();
        displayTreeMap(entries);

        // byte[] extern = AbstractNode.externalizeTreeMap(n.getEntries());
        Serializable extern = AbstractNode.externalizeTreeMap(n.getEntries());
        TreeMap t1 = AbstractNode.internalizeTreeMap(extern);
        displayTreeMap(t1);

        System.out.println("Extern/Inter ok?: " + t1.equals(n.getEntries()));

        byte[] extern2 = Externalization.externalizeObject(ValueTypeSerializer.serialize$TreeMapWithKeyOID(entries));
        byte[] extern3 = Externalization.externalizeObject(ValueTypeSerializer.serialize$GenericTreeMap(entries));

        TreeMap t2 = ValueTypeSerializer.deSerialize$TreeMapWithKeyOID((Serializable)Externalization.internalizeObject(extern2));
        TreeMap t3 = ValueTypeSerializer.deSerialize$GenericTreeMap((Serializable)Externalization.internalizeObject(extern3));

        displayTreeMap(t2);
        displayTreeMap(t3);
        System.out.println("Extern/Inter ok?: " + t1.equals(n.getEntries()));
    }


    @Atomic
    public static void doitWithTransaction() {
        Callable command = new Callable() {
                public Object call() {
                    System.out.println("This was called!!!!!!!!!!!!!!!");
                    return null;
                }
            };
        Atomic atomic = new Atomic() {
                public boolean readOnly() { return false; }
                public boolean canFail() { return true; }
                public boolean speculativeReadOnly() { return true; }
                public Class<? extends pt.ist.fenixframework.atomic.ContextFactory> contextFactory() { return null; }
                public Class<? extends Annotation> annotationType() { return pt.ist.fenixframework.Atomic.class; }
            };

        FenixFramework.getTransactionManager().withTransaction(command, atomic);
    }
}
