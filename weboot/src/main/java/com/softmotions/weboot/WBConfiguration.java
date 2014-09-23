package com.softmotions.weboot;


import com.softmotions.commons.io.DirUtils;
import com.softmotions.commons.io.Loader;
import com.softmotions.weboot.lifecycle.Dispose;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBConfiguration {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected XMLConfiguration xcfg;

    protected ServletContext servletContext;

    protected File tmpdir;

    protected volatile File sessionTmpDir;

    protected WBConfiguration() {
    }

    public void load(String location, ServletContext sctx) {
        this.servletContext = sctx;
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        try {
            xcfg = new XMLConfiguration(cfgUrl);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        String dir = xcfg.getString("tmpdir");
        if (StringUtils.isBlank(dir)) {
            dir = System.getProperty("java.io.tmpdir");
        }
        tmpdir = new File(dir);
        log.info("Using TMP dir: " + tmpdir.getAbsolutePath());
        try {
            DirUtils.ensureDir(tmpdir, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * System-wide tmp dir.
     */
    public File getTmpdir() {
        return tmpdir;
    }

    /**
     * Session tmp dir.
     */
    public File getSessionTmpdir() {
        if (sessionTmpDir != null) {
            return sessionTmpDir;
        }
        synchronized (WBConfiguration.class) {
            if (sessionTmpDir == null) {
                try {
                    sessionTmpDir = Files.createTempDirectory("weboot-").toFile();
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
        return sessionTmpDir;
    }

    public XMLConfiguration xcfg() {
        return xcfg;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String substitutePath(String path) {
        if (path == null) {
            return null;
        }
        String webappPath = getServletContext().getRealPath("/");
        if (webappPath != null) {
            if (webappPath.endsWith("/")) {
                webappPath = webappPath.substring(0, webappPath.length() - 1);
            }
            path = path.replace("{webapp}", webappPath);
        }
        path = path.replace("{cwd}", System.getProperty("user.dir"))
                .replace("{home}", System.getProperty("user.home"))
                .replace("{tmp}", getTmpdir().getAbsolutePath());

        if (path.contains("{newtmp}")) {
            path = path.replace("{newtmp}", getSessionTmpdir().getAbsolutePath());
        }
        return path;
    }


    @Dispose(order = 1)
    public void dispose() {
        synchronized (WBConfiguration.class) {
            if (sessionTmpDir != null) {
                try {
                    FileUtils.deleteDirectory(sessionTmpDir);
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
    }


    public abstract String getEnvironmentType();

    public abstract String getDBEnvironmentType();
}
