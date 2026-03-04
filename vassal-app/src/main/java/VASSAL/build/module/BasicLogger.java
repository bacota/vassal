import java.io.File;
import java.io.IOException;

public class BasicLogger {
    // other existing methods

    public void saveLogFile(File outputFile) throws IOException {
        // your existing logic to save the log file

        // After saving the log file, clean it
        LogCleaner.cleanLogFile(outputFile);
    }
}