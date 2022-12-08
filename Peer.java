import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;


public class Peer {
    private final int id;
    private final CommonCfg commonCfg;
    private final PeerInfoCfg peerInfoCfg;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;
    private final FilePieces filePieces;
    private final Bitfield bitfield;
    private final Map<Integer, BitSet> peerBitfields = new ConcurrentHashMap<>();
    private final PeerServer peerServer;
    private final PeerClient peerClient;
    private final Map<Integer, EndPoint> peerEndPoints = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> downloadRateMap = new ConcurrentHashMap<>();
    private final Set<Integer> interestedPeers = ConcurrentHashMap.newKeySet();
    private final Set<Integer> preferredNeighbors = ConcurrentHashMap.newKeySet();
    private final AtomicInteger optimisticNeighbor = new AtomicInteger(-1);
    private final Set<Integer> completedPeers = new HashSet<>();

    public Peer(int id, CommonCfg commoncfg, PeerInfoCfg peerInfoCfg, ExecutorService executorService,
            ScheduledExecutorService scheduler) {
        this.id = id;
        this.commonCfg = commoncfg;
        this.peerInfoCfg = peerInfoCfg;
        this.executorService = executorService;
        this.scheduler = scheduler;
        this.filePieces = new FilePieces(this.id, this.commonCfg);
        // If the peer has file, set all values in the bitfield
        // (of size equal to the number of pieces) to True.
        // Also, break the file into pieces
        BitSet bitfield = new BitSet(this.commonCfg.getNumberOfPieces());
        if (this.peerInfoCfg.getPeer(this.id).getHasFile()) {
            bitfield.set(0, commoncfg.getNumberOfPieces());
            this.filePieces.splitFileintoPieces();
        }
        this.bitfield = new Bitfield(bitfield, commoncfg);
        // Start peer server and client
        this.peerServer = new PeerServer();
        this.executorService.execute(this.peerServer);
        this.peerClient = new PeerClient();
        this.executorService.execute(this.peerClient);
        for (PeerInfoCfg.PeerInfo peerInfo : this.peerInfoCfg.getPeers().values()) {
            if(peerInfo.getHasFile()) {
                this.completedPeers.add(peerInfo.getId());
            }
        }
    }

    public Bitfield getBitfield() {
        return this.bitfield;
    }

    public Map<Integer, BitSet> getPeerBitfields() {
        return this.peerBitfields;
    }

    public Peer.PeerServer getPeerServer() {
        return this.peerServer;
    }
    
    public Map<Integer, EndPoint> getPeerEndPoints() {
        return this.peerEndPoints;
    }
    
    public EndPoint getPeerEndPoint(int peerId) {
        return peerEndPoints.get(peerId);
    }

    public void addPeerEndPoint(int peerId, EndPoint endPoint) {
        this.peerEndPoints.put(peerId, endPoint);
        this.downloadRateMap.put(peerId, 0);
    }

    public void addOrUpdateBitfield(int peerId, BitSet bitfield) {
        this.peerBitfields.put(peerId, bitfield);
    }

    public Set<Integer> getInterestedPeers() {
        return this.interestedPeers;
    }

    public void addInterestedPeer(int peerId) {
        this.interestedPeers.add(peerId);
    }

    public void removeInterestedPeer(int peerId) {
        this.interestedPeers.remove(peerId);
    }

    public void incrementDownloadRate(int peerId) {
        this.downloadRateMap.put(peerId, this.downloadRateMap.get(peerId) + 1);
    }

    public Set<Integer> getPreferredNeighbors() {
        return this.preferredNeighbors;
    }

    public AtomicInteger getOptimisticNeighbor() {
        return this.optimisticNeighbor;
    }

    public void setOptimisticNeighbor(int optimisticNeighbor) {
        this.optimisticNeighbor.set(optimisticNeighbor);
    }

    public List<Integer> getPeersSortedByDownloadRate() {
        List<Map.Entry<Integer, Integer>> sortedDownloadRateMap = new ArrayList<>(downloadRateMap.entrySet());
        sortedDownloadRateMap.sort(Map.Entry.comparingByValue());
        List<Integer> sortedPeers = new ArrayList<>();
        for(Map.Entry<Integer, Integer> entry : sortedDownloadRateMap) {
            sortedPeers.add(entry.getKey());
        }
        return sortedPeers;
    }

    public void reselectNeighbours() {
        // Sort peers by decreasing order of their download rates
        List<Integer> peersAccordingToDownloadRate = getPeersSortedByDownloadRate();
        // Reset unchoked peer's list and download rates
        this.preferredNeighbors.clear();
        for (int peerId : downloadRateMap.keySet()){
            this.downloadRateMap.put(peerId, 0);
        }
        // Select numberOfPreferredNeighbors with highest download rates
        // and which are interested in peer's data
        int count = 0;
        int i = 0;
        while (count < commonCfg.getNumberOfPreferredNeighbors() && i < this.interestedPeers.size()) {
            int currentPeer = peersAccordingToDownloadRate.get(i);
            if (this.interestedPeers.contains(currentPeer)) {
                this.preferredNeighbors.add(currentPeer);
                count++;
            }
            i++;
        }
    }

    public boolean isUnchoked(int peerId) {
        return preferredNeighbors.contains(peerId) || optimisticNeighbor.get() == peerId;
    }

    public Set<Integer> getCompletedPeers() {
        return this.completedPeers;
    }

    public void addCompletedPeer(int peerId) {
        this.completedPeers.add(peerId);
    }

    public boolean allPeersComplete() {
        Set<Integer> peerIds = this.peerInfoCfg.getPeers().keySet();
        // Check if the current peer has received all the pieces
        if (bitfield.receivedAllPieces()) {
            peerIds.remove(id);
        }
        // Check if all the remaining peers have received the file
        peerIds.removeAll(completedPeers);
        return peerIds.size() == 0;
    }

    public void closeSocket(int peerId) throws IOException {
        peerEndPoints.get(peerId).getSocket().close();
    }

    // Peer server
    public class PeerServer implements Runnable {
        ServerSocket serverSocket;

        public PeerServer() {
            try {
                // Create server socket to listen for incoming connection requests
                PeerInfoCfg.PeerInfo currentPeerInfo = Peer.this.peerInfoCfg.getPeer(id);
                String hostName = currentPeerInfo.getHostName();
                int port = currentPeerInfo.getPort();
                this.serverSocket = new ServerSocket(port, 50, InetAddress.getByName(hostName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public ServerSocket getServerSocket() {
            return this.serverSocket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    Socket socket = serverSocket.accept();
                    Peer.this.executorService.execute(new EndPoint(id, Peer.this, socket, Peer.this.executorService, Peer.this.scheduler, Peer.this.bitfield, Peer.this.filePieces));
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
            
        }
    }

    // Peer client
    public class PeerClient implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            for (PeerInfoCfg.PeerInfo peerInfo : Peer.this.peerInfoCfg.getPeers().values()) {
                if (peerInfo.getId() == id) {
                    break;
                }
                try {
                    Socket socket = null;
                    while (socket == null) {
                        socket = new Socket(peerInfo.getHostName(), peerInfo.getPort());
                    }
                    Peer.this.executorService.execute(new EndPoint(id, Peer.this, peerInfo.getId(), socket, Peer.this.executorService, Peer.this.scheduler, Peer.this.bitfield, Peer.this.filePieces));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
