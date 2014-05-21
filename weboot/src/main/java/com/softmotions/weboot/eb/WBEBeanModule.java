package com.softmotions.weboot.eb;

import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import com.softmotions.weboot.WBConfiguration;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

/**
 * Ebean module integration.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBEBeanModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBEBeanModule.class);

    protected void configure() {
        bind(EbeanServer.class).toProvider(EbeanProvider.class).in(Singleton.class);
        bind(EbeanInitializer.class).asEagerSingleton();
    }

    public static class EbeanProvider implements Provider<EbeanServer> {

        @Inject
        Injector injector;

        @Inject
        WBConfiguration cfg;

        @Inject(optional = true)
        Provider<DataSource> dsProvider;

        public EbeanServer get() {
            ServerConfig scfg = new ServerConfig();
            SubnodeConfiguration ebeanCfg = cfg.impl().configurationAt("ebean");
            String propsStr = cfg.impl().getString("ebean");
            if (ebeanCfg.getBoolean("[@useGuiceProvidedDatasource]", false)) {
                DataSource ds = dsProvider != null ? dsProvider.get() : null;
                if (ds == null) {
                    throw new RuntimeException("No Guice bound DataSource.class");
                }
                log.info("Bound guice provoded datasource: " + ds);
                scfg.setDataSource(ds);
            }
            if (propsStr != null) {
                Properties cprops = new Properties();
                try {
                    cprops.load(new StringReader(propsStr));
                    BeanUtils.populate(scfg, (Map) cprops);
                } catch (IllegalAccessException | InvocationTargetException | IOException e) {
                    String msg = "Failed to load <ebean> properties";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
            if (ebeanCfg.getBoolean("[@scanGuiceEntities]", false)) {
                for (final Binding<?> b : injector.getBindings().values()) {
                    final Type type = b.getKey().getTypeLiteral().getType();
                    if (type instanceof Class) {
                        if (AnnotationResolver.getClassWithAnnotation((Class<?>) type, Entity.class) != null) {
                            log.info("Register EBean entity: " + ((Class<?>) type).getName());
                            scfg.addClass((Class<?>) type);
                        }
                    }
                }
            }

            try {
                //Хак ебена
                log.info("Fixing ebean shutdown manager");
                ShutdownManager.touch();
                Class<ShutdownManager> smClass = ShutdownManager.class;
                Method deregister = smClass.getDeclaredMethod("deregister");
                deregister.setAccessible(true);
                deregister.invoke(null);
            } catch (Exception e) {
                log.error("Failed to fix shutdown manager", e);
            }

            log.info("Creating EbeanServer instance. " +
                     "Name: " + scfg.getName() +
                     " Register: " + scfg.isRegister() +
                     " Default: " + scfg.isDefaultServer());
            return EbeanServerFactory.create(scfg);
        }
    }

    public static class EbeanInitializer extends Thread {

        @Start(order = 10)
        public void startup() {
        }

        @Dispose(order = 10)
        public void shutdown() {
        }
    }

    private static class AnnotationResolver {

        public static Class getClassWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
            if (clazz.isAnnotationPresent(annotation)) {
                return clazz;
            }
            for (Class intf : clazz.getInterfaces()) {
                if (intf.isAnnotationPresent(annotation)) {
                    return intf;
                }
            }
            Class superClass = clazz.getSuperclass();
            //noinspection ObjectEquality
            if (superClass != Object.class && superClass != null) {
                //noinspection TailRecursion
                return getClassWithAnnotation(superClass, annotation);
            }
            return null;
        }
    }
}