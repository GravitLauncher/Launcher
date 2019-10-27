package pro.gravit.launcher.client;

import java.security.Permission;

public class ClientSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm)
    {
        String permName = perm.getName();
        if(permName == null) return;
        if (permName.startsWith("exitVM"))
        {
            Class<?>[] classContexts = getClassContext();
            String callingClass = classContexts.length > 3 ? classContexts[4].getName() : "none";
            if (!(callingClass.startsWith("pro.gravit.")))
            {
                throw new ExitTrappedException();
            }
        }
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
    }

    public static class ExitTrappedException extends SecurityException {
		private static final long serialVersionUID = 6929785890434102330L;
    }
}
