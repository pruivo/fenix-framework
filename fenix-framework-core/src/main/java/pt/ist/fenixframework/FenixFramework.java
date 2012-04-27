package pt.ist.fenixframework;

import jvstm.TransactionalCommand;
import pt.ist.fenixframework.pstm.DataAccessPatterns;
import pt.ist.fenixframework.pstm.DomainFenixFrameworkRoot;
import pt.ist.fenixframework.pstm.MetadataManager;
import pt.ist.fenixframework.pstm.PersistentRoot;
import pt.ist.fenixframework.pstm.Transaction;
import pt.ist.fenixframework.pstm.repository.RepositoryBootstrap;
import dml.DomainModel;

/**
 * This class provides a method to initialize the entire Fenix Framework. To do
 * it, programmers should call the static <code>initialize(Config)</code> method
 * with a proper instance of the <code>Config</code> class.
 * 
 * After initialization, it is possible to get an instance of the
 * <code>DomainModel</code> class representing the structure of the
 * application's domain.
 * 
 * @see Config
 * @see dml.DomainModel
 */
public class FenixFramework {

    private static final String DOMAIN_FENIX_FRAMEWORK_ROOT_KEY = "DomainFenixFrameworkRoot";
    private static final Object INIT_LOCK = new Object();
    private static boolean bootstrapped = false;
    private static boolean initialized = false;
    private static boolean createDomainMetaObjects = false;

    private static Config config;

    public static void initialize(Config config) {
        bootStrap(config);
	createDomainMetaObjects = config.canCreateDomainMetaObjects;
        initialize();
    }

    public static void bootStrap(Config config) {
	synchronized (INIT_LOCK) {
	    if (bootstrapped) {
		throw new Error("Fenix framework already initialized");
	    }

	    FenixFramework.config = ((config != null) ? config : new Config());
	    config.checkConfig();
	    MetadataManager.init(config);
	    new RepositoryBootstrap(config).updateDataRepositoryStructureIfNeeded();
	    DataAccessPatterns.init(config);
	    bootstrapped = true;
	}
    }

    public static void initialize() {
	synchronized (INIT_LOCK) {
	    if (isInitialized()) {
		throw new Error("Fenix framework already initialized");
	    }

	    initDomainFenixFrameworkRoot();
	    PersistentRoot.initRootIfNeeded(config);
	    FenixFrameworkPlugin[] plugins = config.getPlugins();
	    if (plugins != null) {
		for (final FenixFrameworkPlugin plugin : plugins) {
		    Transaction.withTransaction(new TransactionalCommand() {
			
			@Override
			public void doIt() {
			    plugin.initialize();
			}

		    });
		}
	    }
	    initialized = true;
	}
    }

    public static boolean isInitialized() {
	return initialized;
    }

    private static void initDomainFenixFrameworkRoot() {
	Transaction.withTransaction(new TransactionalCommand() {
	    @Override
	    public void doIt() {
		if (getDomainFenixFrameworkRoot() == null) {
		    try {
			DomainFenixFrameworkRoot fenixFrameworkRoot = new DomainFenixFrameworkRoot();
			PersistentRoot.addRoot(DOMAIN_FENIX_FRAMEWORK_ROOT_KEY, fenixFrameworkRoot);
		    } catch (Exception exc) {
			throw new Error(exc);
		    }
		}

		getDomainFenixFrameworkRoot().initialize(getDomainModel());
	    }
	});
    }

    public static DomainFenixFrameworkRoot getDomainFenixFrameworkRoot() {
	return PersistentRoot.getRoot(DOMAIN_FENIX_FRAMEWORK_ROOT_KEY);
    }

    public static Config getConfig() {
	return config;
    }

    public static DomainModel getDomainModel() {
	return MetadataManager.getDomainModel();
    }

    public static <T extends DomainObject> T getRoot() {
	return (T) PersistentRoot.getRoot();
    }

    public static boolean canCreateDomainMetaObjects() {
	return createDomainMetaObjects;
    }
}
