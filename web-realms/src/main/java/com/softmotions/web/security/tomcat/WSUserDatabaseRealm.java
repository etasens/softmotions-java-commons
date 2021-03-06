package com.softmotions.web.security.tomcat;

import com.softmotions.web.security.WSRole;
import com.softmotions.web.security.WSUser;
import com.softmotions.web.security.WSUserDatabase;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Based on {@link org.apache.catalina.realm.UserDatabaseRealm}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WSUserDatabaseRealm extends RealmBase {

    /**
     * The <code>UserDatabase</code> we will use to authenticate users
     * and identify associated roles.
     */
    private volatile WSUserDatabase database;

    /**
     * Descriptive information about this Realm implementation.
     */
    private static final String info = "com.softmotions.web.security.tomcat.WSUserDatabaseRealm/1.0";

    /**
     * Descriptive information about this Realm implementation.
     */
    private static final String name = "WSUserDatabaseRealm";

    /**
     * The global JNDI name of the <code>UserDatabase</code> resource
     * we will be utilizing.
     */
    private String resourceName = "WSUserDatabase";

    /**
     * Use context local database.
     */
    private boolean localDatabase;


    public String getInfo() {
        return info;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isLocalDatabase() {
        return localDatabase;
    }

    public void setLocalDatabase(boolean localDatabase) {
        this.localDatabase = localDatabase;
    }

    public void setDatabase(WSUserDatabase database) {
        this.database = database;
    }

    public WSUserDatabaseRealm() {
    }

    public WSUserDatabaseRealm(WSUserDatabase database) {
        this.database = database;
    }

    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>. This implementation returns <code>true</code>
     * if the <code>User</code> has the role, or if any <code>Group</code>
     * that the <code>User</code> is a member of has the role.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role      Security role to be checked
     */
    @Override
    public boolean hasRole(Wrapper wrapper, Principal principal, String role) {
        // Check for a role alias defined in a <security-role-ref> element
        if (wrapper != null) {
            String realRole = wrapper.findSecurityReference(role);
            if (realRole != null)
                role = realRole;
        }
        if (principal instanceof GenericPrincipal) {
            GenericPrincipal gp = (GenericPrincipal) principal;
            if (gp.getUserPrincipal() instanceof WSUser) {
                principal = gp.getUserPrincipal();
            }
        }
        if (!(principal instanceof WSUser)) {
            //Play nice with SSO and mixed Realms
            return super.hasRole(null, principal, role);
        }
        if ("*".equals(role)) {
            return true;
        } else if (role == null) {
            return false;
        }
        WSUser user = (WSUser) principal;
        WSRole dbrole = getDatabase().findRole(role);
        return (dbrole != null && user.isHasAnyRole(dbrole.getName()));
    }

    protected String getName() {
        return name;
    }
    
    /**
     * Return the password associated with the given principal's user name.
     */
    @Override
    protected String getPassword(String username) {
        WSUser user = getDatabase().findUser(username);
        if (user == null) {
            return null;
        }
        return user.getPassword();
    }

    /**
     * Return the Principal associated with the given user name.
     */
    @Override
    protected Principal getPrincipal(String username) {
        WSUser user = getDatabase().findUser(username);
        if (user == null) {
            return null;
        }
        List<String> roles = new ArrayList<>();
        Iterator<WSRole> uroles = user.getRoles();
        while (uroles.hasNext()) {
            WSRole role = uroles.next();
            roles.add(role.getName());
        }
        return new GenericPrincipal(username, user.getPassword(), roles, user);
    }

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @throws org.apache.catalina.LifecycleException if this component detects a fatal error
     *                                                that prevents this component from being used
     */
    @Override
    protected void startInternal() throws LifecycleException {
        super.startInternal();
    }

    private WSUserDatabase getDatabase() {
        if (database != null) {
            return database;
        }
        synchronized (this) {
            if (database != null) {
                return database;
            }
            try {
                Context context = null;
                if (localDatabase) {
                    Context initCtx = new InitialContext();
                    context = (Context) initCtx.lookup("java:comp/env");
                    database = (WSUserDatabase) context.lookup(resourceName);
                } else {
                    context = getServer().getGlobalNamingContext();
                    database = (WSUserDatabase) context.lookup(resourceName);
                }
            } catch (Throwable e) {
                containerLog.error(sm.getString("wsUserDatabaseRealm.lookup"), e);
                database = null;
            }
            if (database == null) {
                throw new RuntimeException(sm.getString("wsUserDatabaseRealm.noDatabase", resourceName));
            }
            return database;
        }
    }

    /**
     * Gracefully terminate the active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that needs to be reported
     */
    @Override
    protected void stopInternal() throws LifecycleException {
        // Perform normal superclass finalization
        super.stopInternal();
        if (resourceName != null) {
            // Release reference to our user database
            database = null;
        }
    }
}
