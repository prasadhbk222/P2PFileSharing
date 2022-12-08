import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.BitSet;

public class Bitfield {
    private final BitSet bitfield;
    private final CommonCfg commonCfg;
    private final int numberOfPieces;
    private final Set<Integer> requestedPieces = ConcurrentHashMap.newKeySet();
    private final DelayQueue<PieceIndex> requestedPiecesQueue = new DelayQueue<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public Bitfield(BitSet bitfield, CommonCfg commonCfg) {
        this.bitfield = bitfield;
        this.commonCfg = commonCfg;
        this.numberOfPieces = this.commonCfg.getNumberOfPieces();
    }

    public BitSet getBitfield() {
        return this.bitfield;
    }

    public boolean isInterested(BitSet peerBitField) {
        try {
            this.readLock.lock();
            return getNextInterestedPieceIndex(peerBitField) != -1;
        } finally {
            this.readLock.unlock();
        }
    }

    public int getNextInterestedPieceIndex(BitSet peerBitField) {
        for (int i = peerBitField.nextSetBit(0); i != -1; i = peerBitField.nextSetBit(i + 1)) {
            // If the piece is present / already requested
            if (requestedPieces.contains(i)) {
                continue;
            }
            // If the peer has an interesting piece
            if (!bitfield.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public void addToRequestedPieces(int pieceIndex) {
        this.requestedPieces.add(pieceIndex);
        this.requestedPiecesQueue.add(new PieceIndex(pieceIndex));
    }

    public void setReceivedPieceIndex(int pieceIndex) {
        try {
            this.writeLock.lock();
            bitfield.set(pieceIndex);
            requestedPieces.remove(pieceIndex);
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean receivedAllPieces() {
        int nextClearIndex = bitfield.nextClearBit(0);
        return nextClearIndex == -1 || nextClearIndex >= numberOfPieces;
    }

    public DelayQueue<PieceIndex> getDelayQueue() {
        return this.requestedPiecesQueue;
    }

    public void removeTimedOutPieceIndex(int pieceIndex) {
        this.requestedPieces.remove(pieceIndex);
    }

    public void readLock() {
        this.readLock.lock();
    }

    public void readUnlock() {
        this.readLock.unlock();
    }
}
