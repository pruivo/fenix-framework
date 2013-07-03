package test;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.distribution.wrappers.CustomStatsInterceptor;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.backend.infinispan.InfinispanBackEnd;
import pt.ist.fenixframework.backend.infinispan.InfinispanConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author Pedro Ruivo
 * @since 2.8
 */
@RunWith(JUnit4.class)
public class ReadOnlyTest {

    private static final int NUMBER_ELEMENTS = 600;
    private static final int DIVIDE_RATIO = 10;
    private static final int NUMBER_OF_TRANSACTIONS = 1000;

    @Test
    public void testReadOnly() throws Exception {
        InfinispanConfig infinispanConfig = getInfinispanConfig();
        ConfigurationBuilderHolder holder = getDefaultConfigurations();
        CustomStatsInterceptor interceptor = new CustomStatsInterceptor();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(holder.getDefaultConfigurationBuilder().build());
        builder.transaction().transactionManagerLookup(new JBossStandaloneJTAManagerLookup());
        builder.customInterceptors().addInterceptor().index(0).interceptor(interceptor);
        infinispanConfig.setDefaultConfiguration(builder.build());
        infinispanConfig.setGlobalConfiguration(holder.getGlobalConfigurationBuilder().build());

        FenixFramework.initialize(infinispanConfig);
        populate();
        interceptor.resetStatistics();

        long totalDuration = 0;
        int numberOtTx = 0;
        int ignoredSum = 0;

        for (int i = 0; i < NUMBER_OF_TRANSACTIONS; ++i) {
            long start = System.nanoTime();
            int ignored = FenixFramework.getTransactionManager().withTransaction(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int ignored = 0;
                    DomainRoot domainRoot = FenixFramework.getDomainRoot();
                    for (Book book : domainRoot.getTheBooks()) {
                        ignored += book.getId();
                        ignored += book.getPrice();
                    }

                    for (Author author : domainRoot.getTheAuthors()) {
                        ignored += author.getId();
                        ignored += author.getAge();
                    }

                    for (Publisher publisher : domainRoot.getThePublishers()) {
                        ignored += publisher.getId();
                    }
                    return ignored;
                }
            });
            ignoredSum += ignored;
            totalDuration += (System.nanoTime() - start);
            numberOtTx++;
        }

        long applicationAverageDuration = totalDuration / numberOtTx;
        double averageIgnored = ignoredSum * 1.0 / numberOtTx;
        long ispnAverageDuration = interceptor.getAvgReadOnlyTxDuration();
        long ispnNumberOfTx = interceptor.getNumberOfCommits();
        long ispnNumberOfGets = interceptor.getNumberOfGets();
        long ispnNumberOfGetsPerTx = interceptor.getAvgGetsPerROTransaction();
        double ispnThroughput = interceptor.getThroughput();

        System.out.println("total_time[milliseconds]=" + TimeUnit.NANOSECONDS.toMillis(totalDuration));
        System.out.println("app_avg_time[microseconds]= " + TimeUnit.NANOSECONDS.toMicros(applicationAverageDuration));
        System.out.println("ispn_avg_time[microseconds]=" + ispnAverageDuration);
        System.out.println("app_num_of_tx= " + numberOtTx);
        System.out.println("ispn_num_of_tx=" + ispnNumberOfTx);
        System.out.println("app_throughput= " + numberOtTx * 1.0 / TimeUnit.NANOSECONDS.toSeconds(totalDuration));
        System.out.println("ispn_throughput=" + ispnThroughput);
        System.out.println("ispn_avg_get_per_tx=" + ispnNumberOfGetsPerTx);
        System.out.println("ispn_num_of_gets=" + ispnNumberOfGets);
        System.out.println("ispn_num_of_gets_per_second=" + ispnNumberOfGets * 1.0 / TimeUnit.NANOSECONDS.toSeconds(totalDuration));
        System.out.println("ignored=" + averageIgnored);

        javax.transaction.TransactionManager tm = ((InfinispanBackEnd) FenixFramework.getConfig().getBackEnd()).getInfinispanCache().getAdvancedCache().getTransactionManager();
        System.out.println("tm used: class=" + tm.getClass() + ", toString=" + tm.toString());

        Assert.assertTrue(applicationAverageDuration >= ispnAverageDuration);
    }

    @After
    public void shutdown() {
        FenixFramework.shutdown();
    }

    @Atomic
    private void populate() {
        DomainRoot domainRoot = FenixFramework.getDomainRoot();

        for (int i = 0; i < NUMBER_ELEMENTS; i++) {
            domainRoot.addTheBooks(new Book(i, i));
            domainRoot.addTheAuthors(new Author(i % (NUMBER_ELEMENTS / DIVIDE_RATIO), i));
            domainRoot.addThePublishers(new Publisher(i));
        }
    }

    private ConfigurationBuilderHolder getDefaultConfigurations() throws Exception {
        ConfigurationBuilderHolder holder = new ParserRegistry(Thread.currentThread().getContextClassLoader()).parseFile("ispn.xml");
        holder.getDefaultConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC);
        return holder;
    }

    private InfinispanConfig getInfinispanConfig() {
        InfinispanConfig config = new InfinispanConfig();
        config.appNameFromString("fenix-framework-test-backend-ispn");
        return config;
    }


}
