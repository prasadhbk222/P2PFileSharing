import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class EndPoint implements Runnable {
    int peer1Id;
    Peer peer1;
    int peer2Id;
    Socket socket;
    ExecutorService executorService;
    ScheduledExecutorService scheduler;
    InputStream inputStream;
    OutputStream outputStream;
    Bitfield bitfield;
    BitSet peerBitfield;
    FilePieces filePieces;
    boolean handshakeInitiated = false;
    boolean choke = true;
    Logger LOGGER = LogManager.getLogger(EndPoint.class);

    public EndPoint(int peer1Id, Peer peer1, Socket socket, ExecutorService executorService, ScheduledExecutorService scheduler, Bitfield bitfield, FilePieces filePieces) throws IOException {
        this.peer1Id = peer1Id;
        this.peer1 = peer1;
        this.peer2Id = peer1Id;
        this.socket = socket;
        this.executorService = executorService;
        this.scheduler = scheduler;
        this.bitfield = bitfield;
        this.filePieces = filePieces;
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
    }

    public EndPoint(int peer1Id, Peer peer1, int peer2Id, Socket socket, ExecutorService executorService, ScheduledExecutorService scheduler, Bitfield bitfield, FilePieces filePieces) throws IOException {
        this.peer1Id = peer1Id;
        this.peer1 = peer1;
        this.peer2Id = peer2Id;
        this.socket = socket;
        this.executorService = executorService;
        this.scheduler = scheduler;
        this.bitfield = bitfield;
        this.filePieces = filePieces;
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
    }

    public Socket getSocket() {
        return this.socket;
    }

    ///////////////
    // Handshake //
    ///////////////
    private void sendHandshake() throws Exception {
        this.executorService.execute(new MessageSender(this.outputStream, Helper.getHandshakeMessage(this.peer1Id)));
    }

    private void receiveHandshake() throws Exception {
        byte[] response = inputStream.readNBytes(Constants.HM_LENGTH);
        String responseHeader = new String(Arrays.copyOfRange(response, Constants.HM_HEADER_START, Constants.HM_HEADER_START + Constants.HM_HEADER_FIELD), StandardCharsets.UTF_8);
        int peer2Id = Helper.byteArrToInt(Arrays.copyOfRange(response, Constants.HM_PEER_ID_START, Constants.HM_PEER_ID_START + Constants.HM_PEER_ID_FIELD));
        // Check if the handshake response message has correct header
        if (!responseHeader.equals(Constants.HM_HEADER)) {
            // Invalid hanshake response message header
            throw new IllegalArgumentException(String.format("Peer %d received invalid handshake message header (%s) from %d", responseHeader, peer2Id));
        }
        if (this.handshakeInitiated) {
            // Check if the received handshake message has correct peer id
            if (peer2Id != this.peer2Id) {
                throw new IllegalArgumentException(String.format("Peer %d received invalid peer id (%d) in the handshake response", peer1Id, peer2Id));
            }
            LOGGER.info("{}: Peer {} makes a connection to Peer {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        } else {
            // Update peer2Id as received
            this.peer2Id = peer2Id;
            sendHandshake();
            LOGGER.info("{}: Peer {} is connected from Peer {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        }
    }

    public void sendMessage(Constants.MessageType messageType) {
        try {
            this.executorService.execute(
                new MessageSender(this.socket.getOutputStream(), Helper.getMessage(messageType, new byte[0]))
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //////////////
    // Bitfield //
    //////////////
    private void sendBitfield() {
        try {
            this.bitfield.readLock();
            byte[] bitfield = this.bitfield.getBitfield().toByteArray();
            this.executorService.execute(new MessageSender(this.outputStream, Helper.getMessage(Constants.MessageType.BITFIELD, bitfield)));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.bitfield.readUnlock();
        }
    }

    private void receiveBitfield(int messageLength) throws IOException {
        BitSet peerBitfield = BitSet.valueOf(inputStream.readNBytes(messageLength));
        this.peerBitfield = peerBitfield;
        this.peer1.addOrUpdateBitfield(this.peer2Id, this.peerBitfield);
        // Send interested message
        if (this.bitfield.isInterested(this.peerBitfield)) {
            sendInterested();
        }
    }

    ////////////////
    // Interested //
    ////////////////
    private void sendInterested() {
        this.executorService.execute(new MessageSender(this.outputStream, Helper.getMessage(Constants.MessageType.INTERESTED, null)));
    }

    private void receiveInterested() {
        LOGGER.info("{}: Peer {} received the 'interested' message from {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        this.peer1.addInterestedPeer(peer2Id);
    }

    ////////////////////
    // Not Interested //
    ////////////////////
    private void sendNotInterested() {
        this.executorService.execute(new MessageSender(this.outputStream, Helper.getMessage(Constants.MessageType.NOT_INTERESTED, null)));
    }

    private void receiveNotInterested() {
        LOGGER.info("{}: Peer {} received the 'not interested' message from {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        this.peer1.removeInterestedPeer(peer2Id);
    }

    ///////////
    // Choke //
    ///////////
    private void receiveChoke() {
        LOGGER.info("{}: Peer {} is choked by {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        this.choke = true;
    }

    /////////////
    // Unchoke //
    /////////////
    private void receiveUnchoke() {
        LOGGER.info("{}: Peer {} is unchoked by {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        this.choke = false;
        sendRequest();
    }

    /////////////
    // Request //
    /////////////
    private void sendRequest() {
        // Send request only if the node is unchoked
        if (!this.choke) {
            int nextInterestedPieceIndex = bitfield.getNextInterestedPieceIndex(peerBitfield);
            if (nextInterestedPieceIndex != -1) {
                bitfield.addToRequestedPieces(nextInterestedPieceIndex);
                executorService.execute(new MessageSender(this.outputStream, Helper.getMessage(Constants.MessageType.REQUEST, Helper.intToByteArr(nextInterestedPieceIndex))));
            } else {
                sendNotInterested();
            }
        }
    }

    private void receiveRequest(int messageLength) throws IOException {
        // Accept the piece only if the peer is unchoked
        if (this.peer1.isUnchoked(this.peer2Id)) {
            // Construct and send piece message
            int pieceIndex = Helper.byteArrToInt(inputStream.readNBytes(messageLength));
            byte[] pieceByteDataArr = this.filePieces.getFilePiece(pieceIndex);
            byte[] pieceMessage = Helper.getPieceMessage(pieceIndex, pieceByteDataArr);
            executorService.execute(new MessageSender(this.outputStream, pieceMessage));
        }
    }

    //////////
    // Have //
    //////////
    private void broadcastHave(int pieceIndex) throws IOException {
        for (EndPoint endPoint : this.peer1.getPeerEndPoints().values()) {
            this.executorService.execute(new MessageSender(endPoint.outputStream, Helper.getMessage(Constants.MessageType.HAVE, Helper.intToByteArr(pieceIndex))));
        }
    }

    private void receiveHave(int messageLength) throws IOException {
        int pieceIndex = Helper.byteArrToInt(inputStream.readNBytes(messageLength));
        LOGGER.info("{}: Peer {} received the 'have' message from {}", Helper.getCurrentTime(), this.peer1Id, this.peer2Id);
        this.peerBitfield.set(pieceIndex);
        // Check if the node is interested in the peer data
        if(this.bitfield.isInterested(this.peerBitfield)) {
            this.executorService.execute(new MessageSender(this.outputStream, Helper.getMessage(Constants.MessageType.INTERESTED, null)));
        }
    }

    ///////////
    // Piece //
    ///////////
    private void receivePiece(int messageLength) throws IOException {
        int pieceIndex = Helper.byteArrToInt(inputStream.readNBytes(Constants.PM_PIECE_INDEX_FIELD));
        byte[] pieceByteArray = inputStream.readNBytes(messageLength - Constants.PM_PIECE_INDEX_FIELD);
        LOGGER.info("{}: Peer {} has downloaded the piece {} from {}", Helper.getCurrentTime(), this.peer1Id, pieceIndex, this.peer2Id);
        this.filePieces.saveFilePiece(pieceIndex, pieceByteArray);
        bitfield.setReceivedPieceIndex(pieceIndex);
        peer1.incrementDownloadRate(this.peer2Id);
        broadcastHave(pieceIndex);
        // Check if the node is anymore interested in the peer data
        if (!bitfield.isInterested(peerBitfield)) {
            sendNotInterested();
        }
        // Check if the node has downloaded rhe complete file
        if (bitfield.receivedAllPieces()) {
            this.filePieces.joinPiecesintoFile();
            LOGGER.info("{}: Peer {} has downloaded the complete file", Helper.getCurrentTime(), this.peer1Id);
            broadcastCompleted();
        } else {
            sendRequest();
        }
    }

    ///////////////
    // Completed //
    ///////////////
    private void broadcastCompleted() throws IOException {
        for (EndPoint endPoint : this.peer1.getPeerEndPoints().values()) {
            executorService.execute(new MessageSender(endPoint.outputStream, Helper.getMessage(Constants.MessageType.COMPLETED, null)));
        }
        if (peer1.allPeersComplete()) {
            this.filePieces.deletePiecesDir();
            this.executorService.shutdownNow();
            this.scheduler.shutdown();
            peer1.getPeerServer().getServerSocket().close();
            peer1.closeSocket(peer2Id);
        }
    }

    private void receiveCompleted() throws IOException {
        this.peer1.addCompletedPeer(peer2Id);
        if (peer1.allPeersComplete()) {
            this.filePieces.deletePiecesDir();
            this.executorService.shutdownNow();
            this.scheduler.shutdown();
            peer1.getPeerServer().getServerSocket().close();
            peer1.closeSocket(peer2Id);
        }
    }

    @Override
    public void run() {
        // Initiate handshake only if both the peerIds of the endpoint are known
        if (this.peer1Id != this.peer2Id) {
            try {
                sendHandshake();
                this.handshakeInitiated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Receive handshake
        try {
            receiveHandshake();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Send bitfield message
        sendBitfield();
        // Add peer endpoint
        peer1.addPeerEndPoint(peer2Id, this);
        // Keep listening for other messages
        try {
            while (true) {      
                byte[] messageHeaders = inputStream.readNBytes(5);
                if (messageHeaders.length > 0) {
                    int messageLength = Helper.byteArrToInt(Arrays.copyOfRange(messageHeaders, Constants.AM_MESSAGE_LENGTH_START, Constants.AM_MESSAGE_LENGTH_START + Constants.AM_MESSAGE_LENGTH_FIELD));
                    Constants.MessageType messageType = Constants.MessageType.getByValue((int) messageHeaders[Constants.AM_MESSAGE_TYPE_START]);
                    if (messageType != null) {
                        switch (messageType) {
                            case CHOKE:
                                receiveChoke();
                                break;
                            case UNCHOKE:
                                receiveUnchoke();
                                break;
                            case INTERESTED:
                                receiveInterested();
                                break;
                            case NOT_INTERESTED:
                                receiveNotInterested();
                                break;
                            case HAVE:
                                receiveHave(messageLength);
                                break;
                            case BITFIELD:
                                receiveBitfield(messageLength);
                                break;
                            case REQUEST:
                                receiveRequest(messageLength);
                                break;
                            case PIECE:
                                receivePiece(messageLength);
                                break;
                            case COMPLETED:
                                receiveCompleted();
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}
