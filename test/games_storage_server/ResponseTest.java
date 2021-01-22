package games_storage_server;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.my.gamesdataserver.dbengineclasses.DataBaseMethods;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class ResponseTest {
	
	private static final String host = "http://localhost:3636";
	private static final String gameName = "Match 3 Game";
	private static final String ownerEmail = "owner@example.com";
	private static String gameHmac = "";
	private static final String playerFacebookId = "8373351478";
	private static String playerId = "";
	private static String prefix = "";
	
	private Pattern successfullyGameResponsePattern = Pattern.compile("^\\{\"game_name\":\""+gameName+"\",\"api_key\":\"[a-zA-Z0-9]{24}\",\"api_secret\":\"[a-zA-Z0-9]{45}\",\"status\":\"success\"\\}$");
	private Pattern successfullyPlayerAuthorizationPattern = Pattern.compile("^\\{\"playerId\":\"[a-z0-9]{8}\\-[a-z0-9]{8}\"\\}$");
	
	private Connection createDatabaseConnection(String connectionUrl, String username, String password) throws SQLException {
		return (Connection) DriverManager.getConnection(connectionUrl, username, password);
	}
	
	private String readFileContent(String path) {
		File file = new File(path);
		FileInputStream fis = null;
		String result = null;
		try {
			fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			result = new String(data);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(fis != null) fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private void initTestDatabase(Statement statement, /*String dbName,*/ String[] queries) throws SQLException {
		for (int i = 0; i < queries.length; i++)
			statement.executeUpdate(queries[i].trim());
	}
	
	private void addTestDataToDatabase(Statement statement) throws SQLException {
		statement.executeUpdate("CREATE TABLE `"+prefix+"test_table` (\r\n"
				+ "    `id` int(11) NOT NULL AUTO_INCREMENT,\r\n"
				+ "    `playerId` varchar(17) NOT NULL,\r\n"
				+ "    `test_int` int(11) NOT NULL,\r\n"
				+ "    `test_float` DECIMAL(5,2) NOT NULL,\r\n"
				+ "    `test_string` varchar(30) NOT NULL,\r\n"
				+ "    PRIMARY KEY (`id`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
		
		statement.executeUpdate("INSERT INTO `"+prefix+"levels` (`id`, `playerId`, `level`, `score`, `stars`) VALUES\r\n"
				+ "(1, 'emfvv9zi-4wwb3gjc', 1, 0, 0),\r\n"
				+ "(2, 'lshheyeq-iacaqsvc', 1, 650, 2),\r\n"
				+ "(3, 'lshheyeq-iacaqsvc', 2, 0, 0),\r\n"
				+ "(4, 'yh6cgoqh-2sstxb6r', 1, 730, 2),\r\n"
				+ "(5, 'yh6cgoqh-2sstxb6r', 2, 0, 0),\r\n"
				+ "(6, 'mgw8vrhu-bp3lu9yk', 1, 730, 2),\r\n"
				+ "(7, 'mgw8vrhu-bp3lu9yk', 2, 0, 0),\r\n"
				+ "(8, 'weoh98bf-vjyziipb', 1, 920, 2),\r\n"
				+ "(9, 'weoh98bf-vjyziipb', 2, 0, 0),\r\n"
				+ "(10, 'qig0qa8a-w4m3spwa', 1, 680, 2),\r\n"
				+ "(11, 'qig0qa8a-w4m3spwa', 2, 0, 0),\r\n"
				+ "(12, 'emfvv9zi-4wwb3gjc', 2, 222, 222);");
		
		statement.executeUpdate("INSERT INTO `"+prefix+"players` (`playerId`, `facebookId`) VALUES\r\n"
				+ "('emfvv9zi-4wwb3gjc', '3294251159'),\r\n"
				+ "('lshheyeq-iacaqsvc', '6186682720'),\r\n"
				+ "('yh6cgoqh-2sstxb6r', '3006020705'),\r\n"
				+ "('mgw8vrhu-bp3lu9yk', '3998823149'),\r\n"
				+ "('weoh98bf-vjyziipb', '4979193465'),\r\n"
				+ "('qig0qa8a-w4m3spwa', '1464368749');");
	}
	
	private String createGame(String gameName, String clientEmail, String match3, String sendMail) throws IOException {
		 return new HttpClient(host+"/system/register_game").doPost(String.format("game_name=%s&email=%s&match3=%s&send_mail=%s", gameName, clientEmail, match3, sendMail)).getResponseBody();
	}
	
	private void testGameCreation() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
		String response = createGame(gameName, ownerEmail, "Yes", "No");
		System.out.println("Game creation response: "+response);
		assertTrue("Game creation: ", successfullyGameResponsePattern.matcher(response).find());
		JSONObject json = new JSONObject(response);
		String apiKey = json.getString("api_key");
		String apiSecret = json.getString("api_secret");
		
		prefix = DataBaseMethods.generateTablePrefix(gameName, apiKey);
		gameHmac = Base64.getEncoder().encodeToString(hmac(apiSecret.getBytes(), apiKey.getBytes()));
	}
	
	private String playerAuthorization(String facebookId) throws InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException {
		String url = "/player/authorization";
		return new HttpClient(host+url)
					.addHeader("Authorization", computeRequestHmac(gameHmac, url)+":"+gameHmac)
					.doPost("facebookId="+facebookId).getResponseBody();
	}
	
	private void testPlayerAuthorization() throws InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException, JSONException {
		String response = playerAuthorization(playerFacebookId);
		System.out.println("Player authorization response: "+response);
		
		assertTrue("Player authorization: ", successfullyPlayerAuthorizationPattern.matcher(response).find());
		
		playerId = new JSONObject(response).getString("playerId");
	}
	
	private String apiRequest(String url, String body) throws InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException {
		return new HttpClient(host+url)
					.addHeader("Authorization", computeRequestHmac(gameHmac, url)+":"+gameHmac)
					.addHeader("Player_id", playerId)
					.doPost(body).getResponseBody();
	}
	
	private void testApiRequest(String jsonQuery, String url, String expectedValue) throws JSONException, InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException {
		String response = apiRequest(url, jsonQuery);
		System.out.println("\""+url+"\" response: "+response);
		assertEquals("Api request \""+url+"\" fail:", expectedValue, response.trim());
	}
	
	private void testApiRequestArray(JSONArray queries, String command, String expectedValue) throws JSONException, InvalidKeyException, MalformedURLException, NoSuchAlgorithmException, IOException {
		for (int i = 0; i < queries.length(); i++) {
			String jsonQuery = queries.getJSONObject(i).toString();
			testApiRequest(jsonQuery, command, expectedValue);
		}
	}
	
	@Test
	public void test() {
		Connection databaseConnection = null;
		HttpURLConnection serverConnnection = null;
		try {
			databaseConnection = createDatabaseConnection("jdbc:mysql://localhost:3306?useSSL=false", "root", "1234567890");
			Statement statement = (Statement) databaseConnection.createStatement();
			
			String sqlFileContent = readFileContent(".\\test\\testResources\\test_sql");
			
			if(sqlFileContent == null) {
				fail("Fail: Error in sql file read");
				return;
			}
			
			initTestDatabase(statement, sqlFileContent.split(";"));
			testGameCreation();
			
			if(prefix == null || prefix.length() <= 0 || gameHmac == null || gameHmac.length() <= 0) {
				fail("Game creation fail");
				return;
			}
			
			addTestDataToDatabase(statement);
			
			testPlayerAuthorization();
			
			if(playerId.length() <= 0) {
				fail("Player authorization fail");
				return;
			}
			
			String jsonQueriesFileContent = readFileContent(".\\test\\testResources\\test_json_queries");
			
			if(jsonQueriesFileContent == null) {
				fail("Fail: Error in json file read");
				return;
			}
			
			JSONObject jsonQueries = new JSONObject(jsonQueriesFileContent);
			
			testApiRequestArray(jsonQueries.getJSONArray("insert_queries"), "/api/insert", "{\"Success\":\"Insert completed successfully\"}");
			
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(0).toString(), "/api/select", "[{\"test_float\":7.55,\"test_int\":10,\"test_string\":\"entri1\"},{\"test_float\":9.90,\"test_int\":100,\"test_string\":\"entri2\"},{\"test_float\":8.00,\"test_int\":500,\"test_string\":\"entri3\"},{\"test_float\":10.55,\"test_int\":10,\"test_string\":\"entri4\"},{\"test_float\":99.90,\"test_int\":100,\"test_string\":\"entri5\"},{\"test_float\":10.00,\"test_int\":500,\"test_string\":\"entri6\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(1).toString(), "/api/select", "[{\"test_int\":10,\"test_string\":\"entri1\"},{\"test_int\":100,\"test_string\":\"entri2\"},{\"test_int\":500,\"test_string\":\"entri3\"},{\"test_int\":10,\"test_string\":\"entri4\"},{\"test_int\":100,\"test_string\":\"entri5\"},{\"test_int\":500,\"test_string\":\"entri6\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(2).toString(), "/api/select", "[{\"test_int\":100,\"test_string\":\"entri2\"},{\"test_int\":100,\"test_string\":\"entri5\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(3).toString(), "/api/select", "[{\"test_int\":10,\"test_string\":\"entri1\"},{\"test_int\":500,\"test_string\":\"entri3\"},{\"test_int\":10,\"test_string\":\"entri4\"},{\"test_int\":500,\"test_string\":\"entri6\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(4).toString(), "/api/select", "[{\"test_int\":10,\"test_string\":\"entri1\"},{\"test_int\":10,\"test_string\":\"entri4\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(5).toString(), "/api/select", "[{\"test_int\":10,\"test_string\":\"entri4\"}]");
			testApiRequest(jsonQueries.getJSONArray("select_queries").getJSONObject(6).toString(), "/api/select", "[{\"test_int\":100,\"test_string\":\"entri2\"}]");

			testApiRequestArray(jsonQueries.getJSONArray("update_queries"), "/api/update", "{\"Success\":\"Update completed successfully\"}");
			
			testApiRequest("[{name:\"boost1\",count:2},{name:\"boost2\",count:4},{name:\"boost3\",count:6}]", "/game/boosts", "{udpated : 0, inserted: 3}");	
			testApiRequest("[{name:\"boost1\",count:3},{name:\"boost2\",count:4},{name:\"boost3\",count:7},{name:\"boost4\",count:1},{name:\"boost5\",count:1}]", "/game/boosts", "{udpated : 3, inserted: 2}");
			
			testApiRequest("[{level:1,score:200,stars:3},{level:2,score:350,stars:3},{level:3,score:500,stars:3}]", "/game/levels", "{udpated : 0, inserted: 3}");
			testApiRequest("[{level:3,score:590,stars:3},{level:4,score:600,stars:3}]", "/game/levels", "{udpated : 1, inserted: 1}");
			
			testApiRequest("", "/game/leaderboard", "[{\"facebookId\":\"8373351478\",\"max_lvl\":4},{\"facebookId\":\"1464368749\",\"max_lvl\":2},{\"facebookId\":\"3006020705\",\"max_lvl\":2},{\"facebookId\":\"3294251159\",\"max_lvl\":2},{\"facebookId\":\"3998823149\",\"max_lvl\":2},{\"facebookId\":\"4979193465\",\"max_lvl\":2},{\"facebookId\":\"6186682720\",\"max_lvl\":2}]");
			
			testApiRequest("{\"level\":2,\"f_ids\":[\"3294251159\",\"6186682720\"]}", "/game/playerprogress", "[{\"facebookId\":\"3294251159\",\"level\":2,\"score\":222,\"stars\":222},{\"facebookId\":\"6186682720\",\"level\":2,\"score\":0,\"stars\":0}]");
		
			testApiRequest("[\"3006020705\",\"3998823149\"]", "/game/maxplayerprogress", "[{\"facebookId\":\"3006020705\",\"level\":2,\"score\":0,\"stars\":0},{\"facebookId\":\"3998823149\",\"level\":2,\"score\":0,\"stars\":0}]");
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			fail();
		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			fail();
		} finally {
			if(serverConnnection != null) serverConnnection.disconnect();
			try {
				if(databaseConnection != null) databaseConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private byte[] hmac(byte[] key, byte[] message) throws InvalidKeyException, NoSuchAlgorithmException {
		Mac mac = Mac.getInstance("HmacSHA256");
	    SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
	    mac.init(secretKeySpec);
	    return mac.doFinal(message);
	}
	
	private String computeRequestHmac(String gameHmac, String url) throws InvalidKeyException, NoSuchAlgorithmException {
		return Base64.getEncoder().encodeToString(hmac(gameHmac.getBytes(), (gameHmac.substring(0, 8)+url).getBytes()));
	}
}
