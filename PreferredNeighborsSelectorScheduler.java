import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PreferredNeighborsSelectorScheduler implements Runnable {
    int id;
    private Peer peer;
    Logger LOGGER = LogManager.getLogger(EndPoint.class);

    public PreferredNeighborsSelectorScheduler(int id, Peer peer) {
        this.id = id;
        this.peer = peer;
    }

    @Override
    public void run(){
        if (Thread.currentThread().isInterrupted())
            return;
        this.peer.reselectNeighbours();
        // Log preferred neighbors
        LOGGER.info("{}: Peer {} has the preferred neighbors {}", Helper.getCurrentTime(), this.id, this.peer.getPreferredNeighbors());
        this.peer.getPreferredNeighbors();
        for (Map.Entry<Integer, EndPoint> entry : peer.getPeerEndPoints().entrySet()) {
            Integer peerId = entry.getKey();
            EndPoint endPoint = entry.getValue();
            if (peer.getPreferredNeighbors().contains(peerId)) {
                endPoint.sendMessage(Constants.MessageType.UNCHOKE);
            } else if (peer.getOptimisticNeighbor().get() == peerId) {
                continue;
            } else {
                endPoint.sendMessage(Constants.MessageType.CHOKE);
            }
        }
    }
}
