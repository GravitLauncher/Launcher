package pro.gravit.launchserver.command.service;

import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.SubCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public TokenCommand(LaunchServer server) {
        super(server);
        this.childCommands.put("info", new SubCommand("[token]", "print token info") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                var parser = Jwts.parser().verifyWith(server.keyAgreementManager.ecdsaPublicKey).build();
                var claims = parser.parseSignedClaims(args[0]);
                logger.info("Token: {}", claims.getPayload());
            }
        });
        this.childCommands.put("server", new SubCommand("[profileName] (authId) (public only)", "generate new server token") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                AuthProviderPair pair = args.length > 1 ? server.config.getAuthProviderPair(args[1]) : server.config.getAuthProviderPair();
                boolean publicOnly = args.length <= 2 || Boolean.parseBoolean(args[2]);
                ProfilesProvider.UncompletedProfile profile = null;
                for (var p : server.config.profilesProvider.getProfiles(null)) {
                    if (p.getName().equals(args[0]) || p.getUuid().toString().equals(args[0])) {
                        profile = p;
                        break;
                    }
                }
                if (profile == null) {
                    logger.warn("Profile {} not found", args[0]);
                }
                if (pair == null) {
                    logger.error("AuthId {} not found", args[1]);
                    return;
                }
                String token = server.authManager.newCheckServerToken(profile != null ? profile.getUuid().toString() : args[0], pair.name, publicOnly);
                logger.info("Server token {} authId {}: {}", args[0], pair.name, token);
            }

            @Override
            public List<Candidate> complete(List<String> words, int wordIndex, String word) {
                return switch (wordIndex) {
                    case 0 -> server.config.profilesProvider.getProfiles(null).stream()
                            .map(ProfilesProvider.UncompletedProfile::getName)
                            .filter(name -> name != null && name.startsWith(word))
                            .map(Candidate::new)
                            .collect(Collectors.toList());
                    case 1 -> server.config.auth.keySet().stream()
                            .filter(name -> name.startsWith(word))
                            .map(Candidate::new)
                            .collect(Collectors.toList());
                    case 2 -> Stream.of("false", "true")
                            .filter(s -> s.startsWith(word))
                            .map(Candidate::new)
                            .collect(Collectors.toList());
                    default -> new ArrayList<>();
                };
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
