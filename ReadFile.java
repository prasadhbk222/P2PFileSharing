import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFile {
    public ReadFile() {}

    public List<String> read(String filePath)
	{
		List<String> fileContent = new ArrayList<>();
		if(filePath != null && filePath.length() > 0)
		{
            String fullFilePath = Constants.WORKING_DIR + File.separator + filePath;
			try
			{
				FileReader fileReader = new FileReader(fullFilePath);
				BufferedReader bufferReader = new BufferedReader(fileReader);
				String currentLine = "";
				while((currentLine = bufferReader.readLine()) != null) {
					fileContent.add(currentLine);
				}
				bufferReader.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}

		}	
		return fileContent;
	}
}