import java.util.List;


public class CommonCfg
{
	int numberOfPreferredNeighbors;
	int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
	int numberOfPieces;

    public CommonCfg() {}

    public void parse(List<String> lines) {
		if(lines != null && lines.size() == 6) {
			this.numberOfPreferredNeighbors = Integer.parseInt(lines.get(0).split(" ")[1]);
			this.unchokingInterval = Integer.parseInt(lines.get(1).split(" ")[1]);
			this.optimisticUnchokingInterval = Integer.parseInt(lines.get(2).split(" ")[1]);
			this.fileName = lines.get(3).split(" ")[1];
			this.fileSize = Integer.parseInt(lines.get(4).split(" ")[1]);
			this.pieceSize = Integer.parseInt(lines.get(5).split(" ")[1]);
			
            // Calculate the number of chunks
			double fileSizeInDouble = (double) fileSize;
			double pieceSizeInDouble = (double) pieceSize;
			this.numberOfPieces = (int) Math.ceil(fileSizeInDouble / pieceSizeInDouble);
		}
	}

	public int getNumberOfPreferredNeighbors() {
        return this.numberOfPreferredNeighbors;
    }
 
    public int getUnchokingInterval() {
        return this.unchokingInterval;
    }
    
    public int getOptimisticUnchokingInterval() {
        return this.optimisticUnchokingInterval;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getFileSize() {
        return this.fileSize;
    }
   
    public int getPieceSize() {
        return this.pieceSize;
    }

	public int getNumberOfPieces() {
		return this.numberOfPieces;
	}

    @Override
    public String toString()
    {
        return "numberOfPreferredNeighbors : " + this.numberOfPreferredNeighbors + "\n" + 
            "unchokingInterval : " + this.unchokingInterval + "\n" + 
            "optimisticUnchokingInterval : " + this.optimisticUnchokingInterval + "\n" + 
            "fileName : " + this.fileName + "\n" + 
            "fileSize : " + this.fileSize + "\n" + 
            "pieceSize : " + this.pieceSize + "\n" + 
            "numberOfChunks : " + this.numberOfPieces + "\n" ;
    }
}
