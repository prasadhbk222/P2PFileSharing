import java.nio.file.Paths;
import java.util.*;

public class StartRemotePeers {
    private static void copyFiles(String username, PeerInfoCfg.PeerInfo peerInfo, String workingDir, String inputFileName) throws Exception {
        // Make working directory if not present
        String[] makeWorkingDirCommand = new String[] {"sh", "-c", String.format("ssh %s@%s mkdir -p %s", username, peerInfo.getHostName(), workingDir)};
        Process makeWorkingDirProcess = Runtime.getRuntime().exec(makeWorkingDirCommand);
        makeWorkingDirProcess.waitFor();
        
        // Copy .java files
        String[] copyJavaFilesCommand = new String[] {"sh", "-c", String.format("scp *.java %s@%s:%s", username, peerInfo.getHostName(), workingDir)};
        Process copyJavaFilesProcess = Runtime.getRuntime().exec(copyJavaFilesCommand);
        copyJavaFilesProcess.waitFor();

        // Copy lib files
        String[] copyLibFilesCommand = new String[] {"sh", "-c", String.format("scp -r lib/ %s@%s:%s", username, peerInfo.getHostName(), workingDir)};
        Process copyLibFilesProcess = Runtime.getRuntime().exec(copyLibFilesCommand);
        copyLibFilesProcess.waitFor();

        // Copy xml files
        String[] copyXmlFilesCommand = new String[] {"sh", "-c", String.format("scp -r *.xml %s@%s:%s", username, peerInfo.getHostName(), workingDir)};
        Process copyXmlFilesProcess = Runtime.getRuntime().exec(copyXmlFilesCommand);
        copyXmlFilesProcess.waitFor();
        
        // Copy .cfg files
        String[] copyCfgFilesCommand = new String[] {"sh", "-c", String.format("scp *.cfg %s@%s:%s", username, peerInfo.getHostName(), workingDir)};
        Process copyCfgFilesProcess = Runtime.getRuntime().exec(copyCfgFilesCommand);
        copyCfgFilesProcess.waitFor();

        // Copy input file
        String[] copyinputFileCommand = new String[] {"sh", "-c", String.format("scp %s %s@%s:%s", inputFileName, username, peerInfo.getHostName(), workingDir)};
        Process copyInputFileProcess = Runtime.getRuntime().exec(copyinputFileCommand);
        copyInputFileProcess.waitFor();

        // Copy run.sh file
        String[] copyRunFileCommand = new String[] {"sh", "-c", String.format("scp run.sh %s@%s:%s", username, peerInfo.getHostName(), workingDir)};
        Process copyRunFileProcess = Runtime.getRuntime().exec(copyRunFileCommand);
        copyRunFileProcess.waitFor();
    }

    private static void startPeerProcess(String username, PeerInfoCfg.PeerInfo peerInfo, String workingDir) throws Exception {
        // Give executable permissions
        String[] giveExecPermissionsCommand = new String[] {"sh", "-c", String.format("ssh %s@%s chmod 777 %s", username, peerInfo.getHostName(), Paths.get(workingDir, "run.sh").toString())};
        Process giveExecPermissionsProcess = Runtime.getRuntime().exec(giveExecPermissionsCommand);
        giveExecPermissionsProcess.waitFor();

        // Run peer process
        String[] startPeerProcessCommand = new String[] {"sh", "-c", String.format("ssh %s@%s %s %s %d", username, peerInfo.getHostName(), Paths.get(workingDir, "run.sh").toString(), workingDir, peerInfo.getId())};
        Process startPeerProcessProcess = Runtime.getRuntime().exec(startPeerProcessCommand);
        startPeerProcessProcess.waitFor();
    }

	public static void main(String[] args) {
        // Read command line arguments
        String username = System.getProperty("username");
        String workingDir = System.getProperty("workingDir");
        String inputFileName = System.getProperty("inputFileName");

        // Object to read the file
        ReadFile readFileObject = new ReadFile();

        // Read PeerInfo.cfg
        List<String> peerInfoLines = readFileObject.read(Constants.PEER_INFO_CONFIG_FILE_NAME);
        PeerInfoCfg peerInfoCfg = new PeerInfoCfg();
        peerInfoCfg.parse(peerInfoLines);
        Map<Integer, PeerInfoCfg.PeerInfo> peersInfo = peerInfoCfg.getPeers();
        
        // Loop over peerInfo to start peer process
        try {
            for (PeerInfoCfg.PeerInfo peerInfo : peersInfo.values()) {
                copyFiles(username, peerInfo, workingDir, inputFileName);
                startPeerProcess(username, peerInfo, workingDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
