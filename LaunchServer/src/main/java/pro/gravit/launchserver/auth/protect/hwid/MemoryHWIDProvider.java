package pro.gravit.launchserver.auth.protect.hwid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryHWIDProvider extends HWIDProvider implements Reconfigurable {
    private transient final Logger logger = LogManager.getLogger();
    public double warningSpoofingLevel = -1.0;
    public double criticalCompareLevel = 1.0;
    public transient Set<MemoryHWIDEntity> db = ConcurrentHashMap.newKeySet();

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("hardwarelist", new SubCommand() {
            @Override
            public void invoke(String... args) {
                for (MemoryHWIDEntity e : db) {
                    printHardwareInfo(e.hardware);
                    logger.info("ID {} banned {}", e.id, e.banned ? "true" : "false");
                    logger.info("PublicKey Hash: {}", SecurityHelper.toHex(SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA1, e.publicKey)));
                }
            }
        });
        commands.put("hardwareban", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                long id = Long.parseLong(args[0]);
                for (MemoryHWIDEntity e : db) {
                    if (e.id == id) {
                        e.banned = true;
                        logger.info("HardwareID {} banned", e.id);
                    }
                }
            }
        });
        return commands;
    }

    @Override
    public HardwareReportRequest.HardwareInfo findHardwareInfoByPublicKey(byte[] publicKey, Client client) throws HWIDException {
        for (MemoryHWIDEntity e : db) {
            if (Arrays.equals(e.publicKey, publicKey)) {
                if (e.banned) throw new HWIDException("You HWID banned");
                return e.hardware;
            }
        }
        return null;
    }

    @Override
    public void createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) {
        db.add(new MemoryHWIDEntity(hardwareInfo, publicKey));
    }

    @Override
    public boolean addPublicKeyToHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, Client client) throws HWIDException {
        boolean isAlreadyWarning = false;
        for (MemoryHWIDEntity e : db) {
            HardwareInfoCompareResult result = compareHardwareInfo(e.hardware, hardwareInfo);
            if (warningSpoofingLevel > 0 && result.firstSpoofingLevel > warningSpoofingLevel && !isAlreadyWarning) {
                logger.warn("HardwareInfo spoofing level too high: {}", result.firstSpoofingLevel);
                isAlreadyWarning = true;
            }
            if (result.compareLevel > criticalCompareLevel) {
                logger.debug("HardwareInfo publicKey change: compareLevel {}", result.compareLevel);
                if (e.banned) throw new HWIDException("You HWID banned");
                e.publicKey = publicKey;
                return true;
            }
        }
        return false;
    }

    static class MemoryHWIDEntity {
        public HardwareReportRequest.HardwareInfo hardware;
        public byte[] publicKey;
        public boolean banned;
        public long id;

        public MemoryHWIDEntity(HardwareReportRequest.HardwareInfo hardware, byte[] publicKey) {
            this.hardware = hardware;
            this.publicKey = publicKey;
            this.id = SecurityHelper.newRandom().nextLong();
        }
    }
}
