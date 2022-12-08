import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.time.LocalDateTime;


public class Helper {
    public static byte[] getHandshakeMessage(int id) {
        byte[] message = new byte[Constants.HM_HEADER_FIELD + Constants.HM_ZERO_BITS_FIELD + Constants.HM_PEER_ID_FIELD];
        int counter = mergeArr2WithArr1(message, Constants.HM_HEADER.getBytes(), 0);
        for (int i=0; i<10; i++) {
            message[counter++] = 0;
        }
        counter = mergeArr2WithArr1(message, intToByteArr(id), counter);
        return message;
    }

    public static byte[] getMessage(Constants.MessageType messageType, byte[] messagePayload) {
        int messageLength = messagePayload != null ? messagePayload.length : 0;
        byte[] message = new byte[Constants.AM_MESSAGE_LENGTH_FIELD + Constants.AM_MESSAGE_TYPE_FIELD + messageLength];
        int counter = mergeArr2WithArr1(message, intToByteArr(messageLength), 0);
        message[counter++] = (byte) messageType.getValue();
        if (messageLength > 0) {
            mergeArr2WithArr1(message, messagePayload, counter);
        }
        return message;
    }

    private static byte[] getPieceMessagePayload(int pieceIndex, byte[] pieceByteDataArr) {
        byte[] pieceMessagePayload = new byte[Constants.PM_PIECE_INDEX_FIELD + pieceByteDataArr.length];
        int counter = mergeArr2WithArr1(pieceMessagePayload, intToByteArr(pieceIndex), 0);
        mergeArr2WithArr1(pieceMessagePayload, pieceByteDataArr, counter);
        return pieceMessagePayload;
    }

    public static byte[] getPieceMessage(int pieceIndex, byte[] pieceByteDataArr) {
        byte[] pieceMessagePayload = getPieceMessagePayload(pieceIndex, pieceByteDataArr);
        byte[] pieceMessage = getMessage(Constants.MessageType.PIECE, pieceMessagePayload);
        return pieceMessage;
    }

    public static byte[] intToByteArr(int num) {
        return ByteBuffer.allocate(4).putInt(num).array();
    }

    public static int byteArrToInt(byte[] byteArr) {
        return ByteBuffer.wrap(byteArr).getInt();
    }

    public static String getCurrentTime() {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
    }

    public static int mergeArr2WithArr1(byte[] arr1, byte[] arr2, int startIndex) {
        for (byte byteData : arr2) {
            arr1[startIndex++] = byteData;
        }
        return startIndex;
    }

    public static void deleteDirectory(String path) throws IOException
    {
        Files
        .walk(Paths.get(path))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }
}