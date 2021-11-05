package pro.gravit.launchserver.command.service;

import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.SubCommand;

public class TokenCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();
    public TokenCommand(LaunchServer server) {
        super(server);
        this.childCommands.put("info", new SubCommand("[token]", "print token info") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                var parser = Jwts.parserBuilder().setSigningKey(server.keyAgreementManager.ecdsaPublicKey).build();
                var claims = parser.parseClaimsJws(args[0]);
                logger.info("Token: {}", claims.getBody());
            }
        });
        this.childCommands.put("server", new SubCommand("[profileName] (authId)", "generate new server token") {
            @Override
            public void invoke(String... args) throws Exception {
                AuthProviderPair pair = args.length > 1 ? server.config.getAuthProviderPair(args[1]) : server.config.getAuthProviderPair();
                ClientProfile profile = null;
                for(ClientProfile p : server.getProfiles()) {
                    if(p.getTitle().equals(args[0]) || p.getUUID().toString().equals(args[0])) {
                        profile = p;
                        break;
                    }
                }
                if(profile == null) {
                    logger.warn("Profile {} not found", args[0]);
                }
                if(pair == null) {
                    logger.error("AuthId {} not found", args[1]);
                    return;
                }
                String token = server.authManager.newCheckServerToken(profile != null ? profile.getUUID().toString() : args[0], pair.name);
                logger.info("Server token {} authId {}: {}", args[0], pair.name, token);
            }
        });
    }

    @Override
    public String getArgsDescription() {
        return "[server/info/token name] [args]";
    }

    @Override
    public String getUsageDescription() {
        return "jwt management";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}
