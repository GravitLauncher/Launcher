package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.request.uuid.BatchProfileByUsernameRequest;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.Arrays;
import java.util.UUID;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
    private static final long BUSY_WAIT_MS = VerifyHelper.verifyLong(
            Long.parseLong(System.getProperty("launcher.com.mojang.authlib.busyWait", Long.toString(100L))),
            VerifyHelper.L_NOT_NEGATIVE, "launcher.com.mojang.authlib.busyWait can't be < 0");
    private static final long ERROR_BUSY_WAIT_MS = VerifyHelper.verifyLong(
            Long.parseLong(System.getProperty("launcher.com.mojang.authlib.errorBusyWait", Long.toString(500L))),
            VerifyHelper.L_NOT_NEGATIVE, "launcher.com.mojang.authlib.errorBusyWait can't be < 0");

    public YggdrasilGameProfileRepository() {
        LogHelper.debug("Patched GameProfileRepository created");
    }

    private static void busyWait(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void findProfilesByNames(String[] usernames, Agent agent, ProfileLookupCallback callback) {
        int offset = 0;
        while (offset < usernames.length) {
            String[] sliceUsernames = Arrays.copyOfRange(usernames, offset, Math.min(offset + 128, usernames.length));
            offset += 128;

            // Batch Username-To-UUID request
            PlayerProfile[] sliceProfiles;
            try {
                sliceProfiles = new BatchProfileByUsernameRequest(sliceUsernames).request().playerProfiles;
            } catch (Exception e) {
                boolean debug = LogHelper.isDebugEnabled();
                for (String username : sliceUsernames) {
                    if (debug) {
                        LogHelper.debug("Couldn't find profile '%s': %s", username, e);
                    }
                    callback.onProfileLookupFailed(new GameProfile((UUID) null, username), e);
                }

                // Busy wait, like in standard com.mojang.authlib
                busyWait(ERROR_BUSY_WAIT_MS);
                continue;
            }

            // Request succeeded!
            int len = sliceProfiles.length;
            boolean debug = len > 0 && LogHelper.isDebugEnabled();
            for (int i = 0; i < len; i++) {
                PlayerProfile pp = sliceProfiles[i];
                if (pp == null) {
                    String username = sliceUsernames[i];
                    if (debug) {
                        LogHelper.debug("Couldn't find profile '%s'", username);
                    }
                    callback.onProfileLookupFailed(new GameProfile((UUID) null, username), new ProfileNotFoundException("Server did not find the requested profile"));
                    continue;
                }

                // Report as looked up
                if (debug) {
                    LogHelper.debug("Successfully looked up profile '%s'", pp.username);
                }
                callback.onProfileLookupSucceeded(YggdrasilMinecraftSessionService.toGameProfile(pp));
            }

            // Busy wait, like in standard com.mojang.authlib
            busyWait(BUSY_WAIT_MS);
        }
    }
}
