package org.salesforce.gate.SalesGate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SFGate {
	// Credentials
		private final String USERNAME;
		private final String PASSWORD;
		private final String LOGINURL;
		private static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
		private final String CLIENTID;
		private final String CLIENTSECRET;
		
		// Necessary Headers
		private static final Header PRETTYPRINTHEADER = new BasicHeader("X-PrettyPrint", "1");
		private static Header OAUTHHEADER = null;
		
		// Tokens and Others
		private String LOGININSTANCEURL = null;
		private String LOGINACCESSTOKEN = null;
		private HttpPost CONNECTION = null;
		
		public SFGate(String clientId, String clientSecret, String username, String password,String loginUrl) {
			this.USERNAME = username;
			this.PASSWORD = password;
			this.LOGINURL = loginUrl;
			this.CLIENTID = clientId;
			this.CLIENTSECRET = clientSecret;
		}
		
		public String getUsername(){
			return USERNAME;
		}
		
		public String getLoginUrl(){
			return LOGINURL;
		}
		
		/*
		 *  Connection to login.salesforce.com and authenticating using the
		 *  1) Client Id
		 *  2) Client Secret
		 *  3) Username 
		 *  4) Password 
		 */
		
		private void connect() throws ParseException, IOException {

			HttpClient httpclient = HttpClientBuilder.create().build();

			// Assemble the login request URL
			String loginURL = LOGINURL +
					GRANTSERVICE +
					"&client_id=" + CLIENTID +
					"&client_secret=" + CLIENTSECRET +
					"&username=" + USERNAME +
					"&password=" + PASSWORD;

			// Login requests must be POSTs
			HttpPost httpPost = new HttpPost(loginURL);
			CONNECTION = httpPost;
			HttpResponse response = null;

			try {

				// Execute the login POST request
				response = httpclient.execute(httpPost);
			} catch (ClientProtocolException cpException) {
				cpException.printStackTrace();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}

			// Verify response is HTTP OK
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				System.out.println("Error authenticating to Force.com: "+statusCode);

				// Error is in EntityUtils.toString(response.getEntity())
				System.out.println(EntityUtils.toString(response.getEntity()));
				return;
			}

			String getResult = null;
			try {
				getResult = EntityUtils.toString(response.getEntity());
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
			JSONObject jsonObject = null;
			try {
				
				// Getting the login Access Token and Login instance URL
				jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
				LOGINACCESSTOKEN = jsonObject.getString("access_token");
				LOGININSTANCEURL = jsonObject.getString("instance_url");
				OAUTHHEADER = new BasicHeader("Authorization", "OAuth " + LOGINACCESSTOKEN);
			} catch (JSONException jsonException) {
				jsonException.printStackTrace();
			}
			System.out.println(response.getStatusLine());
			System.out.println("Successful login");
			System.out.println("  instance URL: "+LOGININSTANCEURL);
			System.out.println("  access token/session ID: "+LOGINACCESSTOKEN);
			
		}
		
		/*
		 * login instance URL: https://ap4.salesforce.com
		 * full URL: https://ap4.salesforce.com/services/data/v37.0/sobjects/Account/
		 * + Authorization as Header
		 * + user details as body
		 */
		public String createUser(String firstName, String lastName, String email, String alias, String communityNickname, String timeZoneSidKey,
				String localeSidKey, String emailEncodingKey, String profileId, String languageLocalKey, String userName) {

			String accountId = null;
			String userUri = LOGININSTANCEURL + "/services/data/v37.0/sobjects/User/";
			System.out.println(userUri);
			Header oauthHeader = new BasicHeader("Authorization", "OAuth " + LOGINACCESSTOKEN);
			try {

				// Create the JSON object containing the new account details.
				JSONObject user = new JSONObject();
				user.put("FirstName", firstName);
				user.put("LastName", lastName);
				user.put("Email", email);
				user.put("Alias", alias);
				user.put("CommunityNickname", communityNickname);
				user.put("TimeZoneSidKey", timeZoneSidKey);
				user.put("LocaleSidKey", localeSidKey);
				user.put("EmailEncodingKey", emailEncodingKey);
				user.put("ProfileId", profileId);
				user.put("LanguageLocaleKey", languageLocalKey);
				user.put("Username", userName);
				

				System.out.println("JSON for account to be inserted:\n" + user.toString(1));

				// Construct the objects needed for the request
				HttpClient httpClient = HttpClientBuilder.create().build();

				HttpPost httpPost = new HttpPost(userUri);
				httpPost.addHeader(oauthHeader);
				httpPost.addHeader(PRETTYPRINTHEADER);

				// The message we are going to post
				StringEntity body = new StringEntity(user.toString(1));
				body.setContentType("application/json");
				httpPost.setEntity(body);

				// Make the request
				HttpResponse response = httpClient.execute(httpPost);

				// Process the results
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_CREATED) {
					String response_string = EntityUtils.toString(response.getEntity());
					JSONObject json = new JSONObject(response_string);

					// Store the retrieved user id
					accountId = json.getString("id");
					Boolean result=json.getBoolean("success");
					System.out.println("New User id from response: " + accountId);
					System.out.println("Success: " + result); 
				} else {
					String response_string = EntityUtils.toString(response.getEntity());
					System.out.println(response_string);
					System.out.println(response.getStatusLine().getReasonPhrase());
					System.out.println("Insertion unsuccessful. Status code returned is " + statusCode);
				}
			} catch (JSONException e) {
				System.out.println("Issue creating JSON or processing results");
				e.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
			return accountId;
		}
		
		// Querying the user using userName
		public String queryUser(String userName) {
			String id = "";
			try {

				//Set up the HTTP objects needed to make the request.
				HttpClient httpClient = HttpClientBuilder.create().build();
				userName = userName.replaceAll("\\s","+");
				userName = '\'' + userName + '\'';
				String querybase = LOGININSTANCEURL + "/services/data/v37.0/query?q=";
				String queryadd = "SELECT+id,Username+FROM+User+WHERE+Username="+ userName; 
				String finalquery = querybase + queryadd;
				System.out.println("Query URL: " + finalquery);
				HttpGet httpGet = new HttpGet(finalquery);
				System.out.println("oauthHeader: " + OAUTHHEADER);
				httpGet.addHeader(OAUTHHEADER);
				httpGet.addHeader(PRETTYPRINTHEADER);

				// Make the request.
				HttpResponse response = httpClient.execute(httpGet);

				// Process the result
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 200) {
					String response_string = EntityUtils.toString(response.getEntity());
					try {
						JSONObject json = new JSONObject(response_string);
						System.out.println("JSON result of Query:\n" + json.toString(1));
						JSONArray j = json.getJSONArray("records");
						for (int i = 0; i < j.length(); i++){
							String retrievedUserId = json.getJSONArray("records").getJSONObject(i).getString("Id");
							String retrievedUserName = json.getJSONArray("records").getJSONObject(i).getString("Username");
							id = retrievedUserId;
							System.out.println("User record is: " + i + ". " + retrievedUserId + " " + retrievedUserName);
						}
					} catch (JSONException je) {
						je.printStackTrace();
					}
				} else {
					System.out.println("Query was unsuccessful. Status code returned is " + statusCode);
					System.out.println("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
					System.out.println(getBody(response.getEntity().getContent()));
					System.exit(-1);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
			return id;
		}
		
		// Deactivate User using REST HttpPatch.
	    public void deactivateUser(String userId) {
	       
	        String deactivateURI = LOGININSTANCEURL + "/services/data/v37.0/sobjects/User/" + userId;
	        try {
	            //Create the JSON object containing the updated user isActive
	            JSONObject user = new JSONObject();
	            user.put("IsActive", false);
	            System.out.println("JSON for deactivation of user record:\n" + user.toString(1));
	 
	            //Set up the objects necessary to make the request.
	            //DefaultHttpClient httpClient = new DefaultHttpClient();
	            HttpClient httpClient = HttpClientBuilder.create().build();
	 
	            HttpPatch httpPatch = new HttpPatch(deactivateURI);
	            httpPatch.addHeader(OAUTHHEADER);
	            httpPatch.addHeader(PRETTYPRINTHEADER);
	            StringEntity body = new StringEntity(user.toString(1));
	            body.setContentType("application/json");
	            httpPatch.setEntity(body);
	 
	            //Make the request
	            HttpResponse response = httpClient.execute(httpPatch);
	 
	            //Process the response
	            int statusCode = response.getStatusLine().getStatusCode();
	            if (statusCode == 204) {
	                System.out.println("deactivated the user successfully.");
	            } else {
	                System.out.println("user deactivation is not successful. Status code is " + statusCode);
	            }
	        } catch (JSONException e) {
	            System.out.println("Issue creating JSON or processing results");
	            e.printStackTrace();
	        } catch (IOException ioe) {
	            ioe.printStackTrace();
	        } catch (NullPointerException npe) {
	            npe.printStackTrace();
	        }
	    }
	    
	    // Reading the input line by line and putting into result string
		private static String getBody(InputStream inputStream) {

			String result = "";
			try {
				BufferedReader in = new BufferedReader(
						new InputStreamReader(inputStream)
						);
				String inputLine;
				while ( (inputLine = in.readLine() ) != null ) {
					result += inputLine;
					result += "\n";
				}
				in.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return result;
		}

		// Closing the Connection after all the operation
		@Override
		protected void finalize() throws Throwable {

			super.finalize();
			// Release connection
			CONNECTION.releaseConnection();
		}
}
