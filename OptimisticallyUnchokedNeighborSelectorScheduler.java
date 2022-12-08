import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class OptimisticallyUnchokedNeighborSelectorScheduler implements Runnable{
    int id;
    Peer peer;
    PeerInfoCfg peerInfoCfg;
    Random random;
    Logger LOGGER = LogManager.getLogger(EndPoint.class);

    public OptimisticallyUnchokedNeighborSelectorScheduler(int id, Peer peer, PeerInfoCfg peerInfoCfg) {
        this.id = id;
        this.peer = peer;
        this.peerInfoCfg = peerInfoCfg;
        random = new Random();
    }

    @Override
    public void run(){
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        // Find the list of choked peers
        List<Integer> chokedPeers = new ArrayList<>();
        for (int id : this.peerInfoCfg.getPeers().keySet()){
            if ((!peer.getPreferredNeighbors().contains(id)) && (peer.getInterestedPeers().contains(id)))
                chokedPeers.add(id);
        }
        // Select one choked peer randomly as optimistically unchoked neighbor
        try {
            if(chokedPeers.size() > 0) {
                int optimisticNeighbor = chokedPeers.get(random.nextInt(chokedPeers.size()));
                peer.setOptimisticNeighbor(optimisticNeighbor);
                LOGGER.info("{}: Peer {} has the optimistically unchoked neighbor {}", Helper.getCurrentTime(), this.id, optimisticNeighbor);
                EndPoint endPoint = peer.getPeerEndPoint(optimisticNeighbor);
                endPoint.sendMessage(Constants.MessageType.UNCHOKE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
