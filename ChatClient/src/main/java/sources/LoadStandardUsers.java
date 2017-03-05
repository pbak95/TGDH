package sources;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class LoadStandardUsers {
	public static void main(String[] args) {
		for (int i = 0; i < 3; i++) {
			ProcessBuilder pb = new ProcessBuilder("java", "-jar", "D:\\pkry projekt\\Client.jar", "localhost", "2222");
			pb.directory(new File("D:\\pkry projekt\\"));
			try {
				Process p = pb.start();
				LogStreamReaderandWriter lsr = new LogStreamReaderandWriter(p.getInputStream(), p.getOutputStream());
				Thread thread = new Thread(lsr, "LogStreamReader");
				thread.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

class LogStreamReaderandWriter implements Runnable {

	private BufferedReader reader;
//	private BufferedWriter writer;

	public LogStreamReaderandWriter(InputStream is, OutputStream os) {
		this.reader = new BufferedReader(new InputStreamReader(is));
	//	this.writer = new BufferedWriter(new OutputStreamWriter(os));
	}

	public void run() {
		try {
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
