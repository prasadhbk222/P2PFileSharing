import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class PeerInfoCfg {

	public class PeerInfo {
		private int id;
		private String hostName;
		private int port;
		private boolean hasFile;
	
		public PeerInfo(int id, String hostName, int port, boolean hasFile) {
			this.id = id;
			this.hostName = hostName;
			this.port = port;
			this.hasFile = hasFile;
		}
	
		public int getId() {
			return id;
		}
	
		public String getHostName() {
			return hostName;
		}
	
		public int getPort() {
			return port;
		}
	
		public boolean getHasFile() {
			return hasFile;
		}
	
		@Override
		public String toString() {
			return "id: " + id + "\n" +
				"hostname: " + hostName + "\n" +
				"port: " + port + "\n" +
				"hasFile: " + hasFile + "\n";
		}
	}

    private Map<Integer, PeerInfo> peersInfo = new LinkedHashMap<>();

    public void parse(List<String> lines) {
		for(String line : lines) {
			// Parse individual peer info
			String[] lineSplit = line.split(" ");
			int id = Integer.parseInt(lineSplit[0]);
			String hostName = lineSplit[1];
			int port = Integer.parseInt(lineSplit[2]);
			boolean hasFile = Integer.parseInt(lineSplit[3]) == 1;
			
			// Create an instance of peerInfo and add to the peers
			this.peersInfo.put(id, new PeerInfo(id, hostName, port, hasFile));
		}
	}

    public Map<Integer, PeerInfo> getPeers() {
        return this.peersInfo;
    }

	public PeerInfo getPeer(int id) {
        return this.peersInfo.get(id);
    }

	@Override
    public String toString() {
		StringBuilder str = new StringBuilder();
		for(PeerInfo peerInfo: this.peersInfo.values()) {
            str.append(peerInfo);
        }
		return str.toString();
	}
}