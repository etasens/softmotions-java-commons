package com.softmotions.weboot.liquibase;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Start;

/**
 * Liquibase Guice integration.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBLiquibaseModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBLiquibaseModule.class);

    private final ServicesConfiguration cfg;

    public WBLiquibaseModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("liquibase").isEmpty()) {
            log.warn("No WBLiquibaseModule module configuration found. Skipping.");
            return;
        }
        bind(LiquibaseInitializer.class).asEagerSingleton();
    }

    public static class LiquibaseInitializer {

        @Inject
        DataSource ds;

        @Inject
        ServicesConfiguration cfg;

        @Start(order = 10)
        public void start() {
            HierarchicalConfiguration<ImmutableNode> xcfg = cfg.xcfg();
            HierarchicalConfiguration<ImmutableNode> lbCfg = xcfg.configurationAt("liquibase");
            if (lbCfg == null) {
                log.warn("No <liquibase> configuration found");
                return;
            }
            String changelogResource = lbCfg.getString("changelog");
            if (changelogResource == null) {
                throw new RuntimeException("Missing required attribute 'changelog' in <liquibase> configuration tag");
            }
            log.info("Using changelog: {}", changelogResource);

            try (Connection connection = ds.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                                                   .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(lbCfg.getString("defaultSchema"));
                Liquibase liquibase =
                        new Liquibase(changelogResource,
                                      new CompositeResourceAccessor(
                                              new ClassLoaderResourceAccessor(),
                                              new FileSystemResourceAccessor(),
                                              new ClassLoaderResourceAccessor(Thread.currentThread()
                                                                                    .getContextClassLoader())
                                      ),
                                      database
                        );

                List<HierarchicalConfiguration<ImmutableNode>> hcList =
                        lbCfg.configurationsAt("liquibase.changelog-parameters.parameter");
                for (final HierarchicalConfiguration hc : hcList) {
                    String name = hc.getString("name");
                    String value = hc.getString("value");
                    if (name != null) {
                        liquibase.setChangeLogParameter(name, value);
                    }
                }

                if (lbCfg.containsKey("dropAll")) {
                    log.info("Executing Liqubase.DropAll");
                    liquibase.dropAll();
                }

                if (lbCfg.containsKey("update")) {
                    String contexts = lbCfg.getString("update.contexts");
                    log.info("Executing Liquibase.Update, contexts={}", contexts);
                    liquibase.update(contexts);
                }

            } catch (Exception e) {
                log.error("Failed to initiate WBLiquibaseModule", e);
                throw new RuntimeException(e);
            }
        }
    }

}
