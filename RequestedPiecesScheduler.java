import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;

import java.util.BitSet;
import java.util.Map;;

public class RequestedPiecesScheduler implements Runnable{
    private Peer peer;
    private final Bitfield bitfield;

    public RequestedPiecesScheduler(Peer peer) {
        this.peer = peer;
        this.bitfield = peer.getBitfield();
    }

    @Override
    public void run() {
        try{
            if (Thread.currentThread().isInterrupted())
                return;

            DelayQueue<PieceIndex> piecesRequested = bitfield.getDelayQueue();
            PieceIndex expiredPieceIndex = piecesRequested.poll();
            while(Objects.nonNull(expiredPieceIndex)){
                bitfield.removeTimedOutPieceIndex(expiredPieceIndex.getIndex());
                for (Map.Entry<Integer, BitSet> entry : peer.getPeerBitfields().entrySet()){
                    BitSet bitset = entry.getValue();
                    if (bitset.get(expiredPieceIndex.getIndex())){
                        EndPoint ep = peer.getPeerEndPoint(entry.getKey());
                        ep.sendMessage(Constants.MessageType.INTERESTED);
                    }
                }
                expiredPieceIndex = piecesRequested.poll();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        
    }

}
