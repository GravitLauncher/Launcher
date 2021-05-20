package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportGetAllUsers;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
All-In-One provider
 */
public abstract class AuthCoreProvider implements AutoCloseable, Reconfigurable {
    public static final ProviderMap<AuthCoreProvider> providers = new ProviderMap<>("AuthCoreProvider");
    private static boolean registredProviders = false;
    private static final Logger logger = LogManager.getLogger();
    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("reject", RejectAuthCoreProvider.class);
            providers.register("mysql", MySQLCoreProvider.class);
            registredProviders = true;
        }
    }
    public abstract User getUserByUsername(String username);
    public abstract User getUserByUUID(UUID uuid);
    public abstract void verifyAuth(AuthResponse.AuthContext context) throws AuthException;
    public abstract PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password);
    public abstract void init(LaunchServer server);
    // Auth Handler methods
    protected abstract boolean updateAuth(User user, String accessToken) throws IOException;
    protected abstract boolean updateServerID(User user, String serverID) throws IOException;

    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthPasswordDetails());
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> map = defaultCommandsMap();
        map.put("checkpassword", new SubCommand("[username] [json/plain password data]", "check password") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                User user = getUserByUsername(args[0]);
                if(user == null) throw new CommandException("User not found");
                AuthRequest.AuthPasswordInterface password;
                if(args[1].startsWith("{")) {
                    password = Launcher.gsonManager.gson.fromJson(args[1], AuthRequest.AuthPasswordInterface.class);
                } else {
                    password = new AuthPlainPassword(args[1]);
                }
                PasswordVerifyReport report = verifyPassword(user, password);
                if(report.success) {
                    logger.info("Password correct");
                } else {
                    if(report.needMoreFactor) {
                        if(report.factors.size() == 1 && report.factors.get(0) == -1) {
                            logger.info("Password not correct: Required 2FA");
                        } else {
                            logger.info("Password not correct: Required more factors: {}", report.factors.toString());
                        }
                    } else {
                        logger.info("Password incorrect");
                    }
                }
            }
        });
        if(this instanceof AuthSupportGetAllUsers) {
            AuthSupportGetAllUsers instance = (AuthSupportGetAllUsers) this;
            map.put("getallusers", new SubCommand("(limit)", "print all users information") {
                @Override
                public void invoke(String... args) throws Exception {
                    int max = Integer.MAX_VALUE;
                    if(args.length > 0) max = Integer.parseInt(args[0]);
                    List<User> users = instance.getAllUsers();
                    int counter = 0;
                    for(User u : users) {
                        logger.info("User {}", u.toString());
                        counter++;
                        if(counter == max) break;
                    }
                    logger.info("Found {} users", counter);
                }
            });
        }
        return map;
    }

    public UUID checkServer(Client client, String username, String serverID) throws IOException {
        User user = getUserByUsername(username);
        if(user.getUsername().equals(username) && user.getServerId().equals(serverID)) {
            return user.getUUID();
        }
        return null;
    }

    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        User user = client.getUser();
        if(user == null) return false;
        return user.getUsername().equals(username) && user.getAccessToken().equals(accessToken) && updateServerID(user, serverID);
    }

    @Override
    public abstract void close() throws IOException;

    public static class PasswordVerifyReport {
        public final boolean success;
        public final boolean needMoreFactor;
        public final List<Integer> factors;
        public final String accessToken;
        public static final PasswordVerifyReport REQUIRED_2FA = new PasswordVerifyReport(-1);
        public static final PasswordVerifyReport FAILED = new PasswordVerifyReport(false);

        public PasswordVerifyReport(boolean success) {
            this.success = success;
            this.needMoreFactor = false;
            this.factors = List.of();
            this.accessToken = SecurityHelper.randomStringToken();
        }

        public PasswordVerifyReport(String accessToken) {
            this.success = true;
            this.needMoreFactor = false;
            this.factors = List.of();
            this.accessToken = accessToken;
        }

        public PasswordVerifyReport(int nextFactor) {
            this.success = false;
            this.needMoreFactor = true;
            this.factors = List.of(nextFactor);
            this.accessToken = null;
        }

        public PasswordVerifyReport(List<Integer> factors) {
            this.success = false;
            this.needMoreFactor = false;
            this.factors = Collections.unmodifiableList(factors);
            this.accessToken = null;
        }
    }
}
