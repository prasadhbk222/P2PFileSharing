import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class PeerProcess {
    private static void createRequiredDirStructure(PeerInfoCfg.PeerInfo peerInfo, String workingDir, String inputFileName) throws Exception {
        // Create peer directory, if not present
        try {
            String peerDirPath = Paths.get(Constants.WORKING_DIR, String.format("peer_%d", peerInfo.getId())).toString();
            Files.createDirectories(Paths.get(peerDirPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Copy input file to the peer directory, if the peer has file
        if(peerInfo.getHasFile()) {
            try {
                Path source = Paths.get(Constants.WORKING_DIR, inputFileName);
                Path target = Paths.get(Constants.WORKING_DIR, String.format("peer_%d", peerInfo.getId()), inputFileName);
                Files.copy(source, target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Read the current peer id
        int id = Integer.parseInt(args[0]);

        // Object to read the file
        ReadFile readFileObject = new ReadFile();

        // Read Common.cfg
        List<String> commonCfgLines = readFileObject.read(Constants.COMMON_CONFIG_FILE_NAME);
        CommonCfg commonCfg = new CommonCfg();
        commonCfg.parse(commonCfgLines);

        // Read PeerInfo.cfg
        List<String> peerInfoLines = readFileObject.read(Constants.PEER_INFO_CONFIG_FILE_NAME);
        PeerInfoCfg peerInfoCfg = new PeerInfoCfg();
        peerInfoCfg.parse(peerInfoLines);

        // Create folder structure
        try {
            createRequiredDirStructure(peerInfoCfg.getPeer(id), Constants.WORKING_DIR, commonCfg.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

        Peer peer = new Peer(id, commonCfg, peerInfoCfg, executorService, scheduler);
        scheduler.scheduleAtFixedRate(new PreferredNeighborsSelectorScheduler(id, peer), 0L, commonCfg.getUnchokingInterval(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new OptimisticallyUnchokedNeighborSelectorScheduler(id, peer, peerInfoCfg), 0L, commonCfg.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new RequestedPiecesScheduler(peer), 0L, 30, TimeUnit.SECONDS);
    }
}