package org.salesforce.gate.SalesGate;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;

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
		static Header OAUTHHEADER = null;
		
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
}
