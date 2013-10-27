package com.sensoria.workbench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class RestClient {
	String strKey;
	String strFeed;
	String strStream;
	
	public RestClient(){
		//For TestFeed from tutorial
		this.strKey = "6fc8993281064f8dc80afdb19d8795b7";
		this.strFeed = "6bced640e03992c89bced843b9439236";
		this.strStream = "pressure1";
	}
	
	public int httpPull(){
		try {
				//URL url = new URL("http://localhost:8080/RESTfulExample/json/product/get");
				//tstClient.httpGet("http://api-m2x.att.com/v1/feeds/cee72cc686706df9965ce8abc821caff/streams/load_1m/values");
				String urlStr = "http://api-m2x.att.com/v1/feeds/" + strFeed + "/streams/" + strStream;
			    URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "application/json");
				conn.setRequestProperty("X-M2X-KEY", this.strKey);
		 
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
				}
		 
				BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
		 
				String output;
				String result = "";
				System.out.println("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					result += output;
					System.out.println(output);
				}		
				//System.out.println(result);
				//JSONObject jsonObj = new JSONObject(output);
				String[] tokens = result.split(",");
				String[] resTokens = tokens[2].split(":");
				resTokens = resTokens[1].split("\"");
				
				//System.out.println("value:" + resTokens[1]);
				conn.disconnect();
				return Integer.parseInt(resTokens[1]);
				
		} catch (MalformedURLException e) {		 
				e.printStackTrace();	
				return 0;
		} catch (IOException e) {		 
				e.printStackTrace();
				return 0;				
		} 
	}
	
	public void httpPush(int value){
		  try {
				//URL url = new URL("http://localhost:8080/RESTfulExample/json/product/post");
				String urlStr = "http://api-m2x.att.com/v1/feeds/" + strFeed + "/streams/" + strStream;
			    URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("PUT");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("X-M2X-KEY", this.strKey);
		 
				//String input = "{\"qty\":100,\"name\":\"iPad 4\"}"; //  the json string format
				String input = "{\"value\":" + String.valueOf(value) + "}";
				System.out.println("myinput:" + input);
		 
				OutputStream os = conn.getOutputStream();
				os.write(input.getBytes());
				os.flush();
		 
				if ((conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) &&
					(conn.getResponseCode() != 204)) {
					throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
				}
		 
				BufferedReader br = new BufferedReader(new InputStreamReader(
						(conn.getInputStream())));
		 
//				String output;
//				System.out.println("Output from Server .... \n");
//				while ((output = br.readLine()) != null) {
//					System.out.println(output);
//				}		 
				conn.disconnect();
				
			  } catch (MalformedURLException e) {		 
				e.printStackTrace();		 
			  } catch (IOException e) {		 
				e.printStackTrace();
			 }		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RestClient tstClient = new RestClient();
		int val = tstClient.httpPull();
		System.out.println(val);
		//strValue = "20";
		//strJson = "{\"value\":\"iPad 4\"}";
		//for(int i = 1; i <= 100; i++)
			//tstClient.httpPush(0);
	}

}
