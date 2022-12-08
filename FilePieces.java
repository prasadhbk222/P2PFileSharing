import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class FilePieces {
    int id;
    CommonCfg commonConfig;
    String fileName;
    int fileSize;
    int pieceSize;
    int numberOfPieces;
    String piecesDirPath;
    String filePath;

    public FilePieces(int id, CommonCfg commonCfg) {
        this.id = id;
        this.commonConfig = commonCfg;
        this.fileName = this.commonConfig.getFileName();
        this.fileSize = this.commonConfig.getFileSize();
        this.pieceSize = this.commonConfig.getPieceSize();
        this.numberOfPieces = this.commonConfig.getNumberOfPieces();
        
        this.piecesDirPath = Paths.get(Constants.WORKING_DIR, String.format("peer_%d", this.id), "temp").toString();
        try {
            Files.createDirectories(Paths.get(this.piecesDirPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.filePath = Paths.get(Constants.WORKING_DIR, String.format("peer_%d", this.id), this.commonConfig.getFileName()).toString();
    }

    public void splitFileintoPieces() {
		try {
            FileInputStream fileInputStream = new FileInputStream(this.filePath);
            int pieceStart = 0;
            for (int pieceIndex = 0; pieceIndex < this.numberOfPieces; pieceIndex++) {
                int newPieceStart = pieceStart + this.pieceSize;
                int pieceLength = this.pieceSize;
                // In case of the last piece, adjust the newPieceStart and pieceLength
                if (this.fileSize < newPieceStart) {
                    newPieceStart = this.fileSize;
                    pieceLength = this.fileSize - pieceStart;
                }
                byte[] pieceByteArray = new byte[pieceLength];
                fileInputStream.read(pieceByteArray);
                String piecePath = Paths.get(this.piecesDirPath, String.format("%s_%d", this.fileName, pieceIndex)).toString();
                FileOutputStream fileOutputStream = new FileOutputStream(piecePath);
                fileOutputStream.write(pieceByteArray);
                fileOutputStream.close();
                pieceStart = newPieceStart;
            }
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getFilePiece(int pieceIndex) throws IOException {
        String piecePath = Paths.get(this.piecesDirPath, String.format("%s_%d", this.fileName, pieceIndex)).toString();
        FileInputStream fileInputStream = new FileInputStream(piecePath);
        int pieceLength = (int) fileInputStream.getChannel().size();
        byte[] pieceByteArray = new byte[pieceLength];
        fileInputStream.read(pieceByteArray);
        fileInputStream.close();
        return pieceByteArray;
    }

    public void saveFilePiece(int pieceIndex, byte[] pieceByteArray) throws IOException {
        String piecePath = Paths.get(this.piecesDirPath, String.format("%s_%d", this.fileName, pieceIndex)).toString();
        FileOutputStream fileOutputStream = new FileOutputStream(piecePath);
        fileOutputStream.write(pieceByteArray);
        fileOutputStream.close();
    }

    public void joinPiecesintoFile() throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(this.filePath);
		File[] splitFiles = new File[numberOfPieces];
		for(int pieceIndex = 0; pieceIndex < numberOfPieces; pieceIndex++) {
            String piecePath = Paths.get(this.piecesDirPath, String.format("%s_%d", this.fileName, pieceIndex)).toString();
			splitFiles[pieceIndex] = new File(piecePath);
		}
		for(int pieceIndex = 0; pieceIndex < numberOfPieces; pieceIndex++) {
			FileInputStream fileInputStream = new FileInputStream(splitFiles[pieceIndex]);
			int chunkFileLength = (int)splitFiles[pieceIndex].length();
			byte[] readChunkFile = new byte[chunkFileLength];
			fileInputStream.read(readChunkFile);
			fileOutputStream.write(readChunkFile);
			fileInputStream.close();
		}
		fileOutputStream.close();
	}

    public void deletePiecesDir() throws IOException {
        try {
            if (Files.exists(Paths.get(piecesDirPath))) {
                Helper.deleteDirectory(piecesDirPath);
                Files.delete(Paths.get(piecesDirPath));
            }
        } catch (Exception e) {
            // 
        }
    }
}
