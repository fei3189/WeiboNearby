package info.caq9;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Weibo web version login process. References:
 * http://3352580.blog.51cto.com/3342580/1205051 ,
 * http://blog.csdn.net/htw2012/article/details/12911075
 * 
 * @author CAQ9
 * 
 */
public class WeiboLogin {
	static final boolean DEBUG = false;

	static final String JSFILE = "data/ssoencoder.js";
	static final String SSOLOGINJS = "ssologin.js(v1.4.11)";
	static final JSONParser JSONPARSER = new JSONParser();

	static CookieStore cookieStore;
	static final CloseableHttpClient HTTPCLIENT;

	static String ENCODERSCRIPT = null;
	static {
		cookieStore = new BasicCookieStore();
		HTTPCLIENT = HttpClients.custom().setDefaultCookieStore(cookieStore)
				.build();
		try {
			if (DEBUG)
				System.out.println("Reading JavaScript file from " + JSFILE
						+ " ...");
			Scanner scanner = new Scanner(new File(JSFILE));
			scanner.useDelimiter("\\Z");
			ENCODERSCRIPT = scanner.next();
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String navigator = "var navigator = {'appName':'Netscape', 'userAgent':'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:27.0) Gecko/20100101 Firefox/27.0', 'appVersion':'5.0 (X11)'};";
		String location = "var location = {'protocol':'http'};";
		ENCODERSCRIPT = navigator + "\n" + location + "\n" + ENCODERSCRIPT;
		if (DEBUG)
			System.out.println("Encoder script loaded.");
	}

	static JSONObject loginObject;

	/**
	 * Get the current Linux time stamp
	 * 
	 * @return Timestamp in milliseconds
	 */
	static long getTimestamp() {
		return new Date().getTime();
	}

	/**
	 * Get the content of a page with the given request. Cookies are updated in
	 * this step.
	 * 
	 * @param request
	 *            An HttpGet or HttpPost object
	 * @param encoding
	 *            If null, use default "UTF-8"
	 * @return null if some exception occurred
	 */
	static String getContent(HttpUriRequest request, String encoding) {
		try {
			HttpResponse response = HTTPCLIENT.execute(request);
			Header[] cookieHeaders = response.getHeaders("Set-Cookie");
			if (cookieHeaders != null)
				for (Header cookieHeader : cookieHeaders) {
					String setCookie = cookieHeader.getValue();
					if (DEBUG)
						System.out.println("Set-Cookie: " + setCookie);
					String domain = null, path = null, expires = null;
					BasicClientCookie cookie = new BasicClientCookie("", "");
					for (String cookieStr : setCookie.split(";")) {
						cookieStr = cookieStr.trim();
						if (cookieStr.indexOf('=') <= 0)
							continue;
						String[] fields = cookieStr.split("=");
						if (fields[0].equals("domain")) {
							domain = fields[1];
						} else if (fields[0].equals("path")) {
							path = fields[1];
						} else if (fields[0].equals("expires")) {
							expires = fields[1];
						} else {
							cookie = new BasicClientCookie(fields[0], fields[1]);
						}
					}
					if (domain != null)
						cookie.setDomain(domain);
					if (path != null)
						cookie.setPath(path);
					if (expires != null)
						try {
							cookie.setExpiryDate(new SimpleDateFormat(
									"EEE, dd-MMM-yy kk:mm:ss z").parse(expires));
						} catch (java.text.ParseException e) {
							if (DEBUG)
								e.printStackTrace();
						}
					cookieStore.addCookie(cookie);
				}
			return EntityUtils.toString(response.getEntity(),
					(encoding == null ? "UTF-8" : encoding));
		} catch (org.apache.http.ParseException e) {
			if (DEBUG)
				e.printStackTrace();
		} catch (ClientProtocolException e) {
			if (DEBUG)
				e.printStackTrace();
		} catch (IOException e) {
			if (DEBUG)
				e.printStackTrace();
		}
		return null;
	}

	/**
	 * Use Sina's RSA2 algorithm to encrypt the password, by executing the
	 * JavaScript code.
	 * 
	 * @param password
	 * @param pubKey
	 * @param servertime
	 * @param nonce
	 * @return
	 * @throws ScriptException
	 */
	static String generatePassword(String password, String pubKey,
			String servertime, String nonce) throws ScriptException {
		String rsa = "var RSAKey = new sinaSSOEncoder.RSAKey();RSAKey.setPublic('"
				+ pubKey
				+ "', '10001');password = RSAKey.encrypt(['"
				+ servertime
				+ "', '"
				+ nonce
				+ "'].join('\\t') + '\\n' + '"
				+ password + "');";
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		return engine.eval(ENCODERSCRIPT + "\n" + rsa + "\npassword;")
				.toString();
	}

	/**
	 * Try to log in with the given username and password, update and store the
	 * cookies, so that later web requests can get correct (after-login)
	 * contents.
	 * 
	 * @param username
	 *            Weibo username (full email address).
	 * @param password
	 *            Corresponding Weibo password.
	 * @return A boolean denoting whether the login is successful.
	 */
	public static boolean updateCookie(String username, String password) {
		loginObject = null;

		/*
		 * First step, visit the pre-login page to get login parameters. This
		 * page URI is discovered when visiting the homepage of weibo.com.
		 */
		String callback = null;
		try {
			if (DEBUG)
				System.out.println("Pre-login...");
			callback = getContent(
					new HttpGet(new URIBuilder()
							.setScheme("http")
							.setHost("login.sina.com.cn")
							.setPath("/sso/prelogin.php")
							.setParameter("entry", "weibo")
							.setParameter("callback",
									"sinaSSOController.preloginCallBack")
							.setParameter("su", "")
							.setParameter("rsakt", "mod")
							.setParameter("client", SSOLOGINJS)
							// 20140228
							.setParameter("_", WeiboLogin.getTimestamp() + "")
							.build()), "UTF-8");
		} catch (URISyntaxException e) {
			if (DEBUG)
				e.printStackTrace();
		}
		// Check if something wrong during the request-response
		if (callback == null)
			return false;

		if (DEBUG)
			System.out.println("prelogin parameters: " + callback);

		// callback example:
		// sinaSSOController.preloginCallBack({"retcode":0,"servertime":1393621276,"pcid":"hk-3c02f4c5772fe8ee5b543dca4b219a0daf0d","nonce":"RQAFZ9","pubkey":"EB2A38568661887FA180BDDB5CABD5F21C7BFD59C090CB2D245A87AC253062882729293E5506350508E7F9AA3BB77F4333231490F915F6D63C55FE2F08A49B353F444AD3993CACC02DB784ABBB8E42A9B1BBFFFB38BE18D78E87A0E41B9B8F73A928EE0CCEE1F6739884B9777E4FE9E88A1BBE495927AC4A799B3181D6442443","rsakv":"1330428213","exectime":2})

		JSONObject callbackObj = null;
		try {
			String jsonStr = callback.substring(callback.indexOf('{'),
					callback.lastIndexOf('}') + 1).trim();
			if (DEBUG)
				System.out.println("JSON String: " + jsonStr);
			callbackObj = (JSONObject) JSONPARSER.parse(jsonStr);
		} catch (ParseException e) {
			if (DEBUG)
				e.printStackTrace();
		}
		// Check if something wrong during parsing
		if (callbackObj == null)
			return false;

		if (DEBUG)
			System.out.println("Prelogin object parsed.");

		/* Second step, send a lot of parameters to login */
		HttpPost httppost = new HttpPost(
				"http://login.sina.com.cn/sso/login.php?client=" + SSOLOGINJS);

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("service", "miniblog"));
		formparams.add(new BasicNameValuePair("entry", "weibo"));
		formparams.add(new BasicNameValuePair("encoding", "UTF-8"));
		formparams.add(new BasicNameValuePair("gateway", "1"));
		formparams.add(new BasicNameValuePair("savestate", "0"));
		formparams.add(new BasicNameValuePair("useticket", "1"));
		formparams.add(new BasicNameValuePair("servertime", callbackObj.get(
				"servertime").toString()));
		formparams.add(new BasicNameValuePair("nonce", callbackObj.get("nonce")
				.toString()));
		formparams.add(new BasicNameValuePair("pwencode", "rsa2"));
		formparams
				.add(new BasicNameValuePair(
						"url",
						"http://www.weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
		formparams.add(new BasicNameValuePair("returntype", "META"));
		formparams.add(new BasicNameValuePair("from", ""));
		formparams.add(new BasicNameValuePair("pagerefer", ""));
		formparams.add(new BasicNameValuePair("vsnf", "1"));
		int prelt = new Random().nextInt(900) + 100;
		formparams.add(new BasicNameValuePair("prelt", prelt + ""));
		formparams.add(new BasicNameValuePair("rsakv", callbackObj.get("rsakv")
				.toString()));

		String sp = null;
		try {
			sp = generatePassword(password, callbackObj.get("pubkey")
					.toString(), callbackObj.get("servertime").toString(),
					callbackObj.get("nonce").toString());
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		if (sp == null)
			return false;
		formparams.add(new BasicNameValuePair("sp", sp));

		String encodedUsername = null;
		try {
			encodedUsername = URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			encodedUsername = username;
			// e.printStackTrace();
		}
		formparams.add(new BasicNameValuePair("su", new String(Base64
				.encodeBase64(encodedUsername.getBytes()))));
		if (DEBUG)
			System.out.println("Post parameters: " + formparams);

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
				Consts.UTF_8);
		httppost.setEntity(entity);
		callback = getContent(httppost, "GBK");
		if (callback == null)
			return false;

		if (DEBUG)
			System.out.println("Pubkey received.");

		/* Third step, redirect to the correct page */
		String newLocation = callback.substring(
				callback.lastIndexOf("location.replace"),
				callback.lastIndexOf(';') + 1);
		newLocation = newLocation.substring(newLocation.indexOf('\'') + 1,
				newLocation.lastIndexOf('\''));
		if (DEBUG)
			System.out.println("New location: " + newLocation);

		if (DEBUG)
			System.out.println("Login...");
		// Send a GET request to this new location to get parameters and
		// cookies.
		callback = getContent(new HttpGet(newLocation), "UTF-8");

		// Failed login: <html><head><script
		// language='javascript'>parent.sinaSSOController.feedBackUrlCallBack({"result":false,"errno":"80","reason":"\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u5bc6\u7801"});</script></head><body></body></html>

		// Successful login: <html><head><script
		// language='javascript'>parent.sinaSSOController.feedBackUrlCallBack({"result":true,"userinfo":{"uniqueid":"1982631825","userid":null,"displayname":null,"userdomain":"?wvr=5&lf=reg"},"redirect":"http:\/\/weibo.com\/oguide\/recommend?guide=old"});</script></head><body></body></html>

		callbackObj = null;
		try {
			String jsonStr = callback.substring(callback.indexOf('{'),
					callback.lastIndexOf('}') + 1).trim();
			if (DEBUG)
				System.out.println("JSON String: " + jsonStr);
			callbackObj = (JSONObject) JSONPARSER.parse(jsonStr);
		} catch (ParseException e) {
			if (DEBUG)
				e.printStackTrace();
		}
		// Check if something wrong during parsing
		if (callbackObj == null)
			return false;

		boolean result = (boolean) callbackObj.get("result");

		if (DEBUG)
			System.out.println("Login " + (result ? "" : "un") + "successful: "
					+ callbackObj.toJSONString());
		loginObject = callbackObj;

		return result;
	}

	public static void main(String[] args) throws URISyntaxException,
			ScriptException, org.apache.http.ParseException,
			ClientProtocolException, IOException {
		// before login, this content is a login page.
//		System.out.println(getContent(new HttpGet(
//				"http://www.weibo.com/p/100101120_40/home"), "UTF-8"));

		if (WeiboLogin.updateCookie("yourname", "yourpasswd")) {
			BufferedWriter writer = new BufferedWriter(new FileWriter("sample.html"));
			// after login, this content contains Weibo data
			writer.append((getContent(new HttpGet(
					"http://www.weibo.com/p/100101116.331695_39.996668/home"), "UTF-8")));
			writer.close();
		}
	}
}
