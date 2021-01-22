package responsetest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HttpClient {
	private HttpURLConnection serverConnection;
	private StringBuilder response;
	
	public HttpClient(String url) throws MalformedURLException, IOException {
		serverConnection = (HttpURLConnection) new URL(url).openConnection();
	}
	
	public HttpClient doPost(String body) throws IOException {
		serverConnection.setRequestMethod("POST");
		serverConnection.setDoOutput(true);
		serverConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		serverConnection.setRequestProperty("Content-Length", Integer.toString(body.getBytes().length));
		DataOutputStream outputStream = new DataOutputStream(serverConnection.getOutputStream());
		outputStream.writeBytes(body);
		outputStream.flush();
		outputStream.close();
		
		InputStream inputStream = serverConnection.getResponseCode() > 400?serverConnection.getErrorStream():serverConnection.getInputStream();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		response = new StringBuilder();
		String line;
	    while ((line = reader.readLine()) != null) {
	      response.append(line);
	      response.append("\r\n");
	    }
	    
	    reader.close();
	    
	    return this;
	}
	
	public HttpClient addHeader(String name, String value) {
		serverConnection.setRequestProperty(name, value);
		return this;
	}
	
	public String getResponseBody() {
		return response.toString();
	}
}
