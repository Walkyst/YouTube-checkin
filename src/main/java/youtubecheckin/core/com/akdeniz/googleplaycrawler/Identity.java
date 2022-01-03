package youtubecheckin.core.com.akdeniz.googleplaycrawler;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import youtubecheckin.core.com.akdeniz.googleplaycrawler.misc.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

/**
 * ClientLogin implementation.
 * 
 * @author patrick
 * 
 */
class Identity {

	private static final String CONTINUE_URL = "https://accounts.google.com/signin/continue";
	private static final String LOGIN_URL = "https://android.clients.google.com/auth";
	private static final String PUBKEY = "AAAAgMom/1a/v0lblO2Ubrt60J2gcuXSljGFQXgcyZWveWLEwo6prwgi3iJIZdodyhKZQrNWp5nKJ3srRXcUW+F1BD3baEVGcmEgqaLZUNBjm057pKRI16kB0YppeGx5qIQ5QjKzsR8ETQbKLNWgRY0QRNVz34kMJR3P/LgHax/6rmf5AAAAAwEAAQ==";
	private static final String MARKET = "androidmarket";

	private String firstName;
	private String lastName;
	private String email;
	private String services;
	private String authToken;
	private String aas_et;
	private String continueUrl;

	private Identity() {
	}

	/**
	 * User's first name
	 * 
	 * @return the first name, retrieved from Play
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * User's last name
	 * 
	 * @return the lastname, retrieved from Play
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * User's current email address
	 * 
	 * @return the email address, retrieved from Play.
	 */
	public String getEmail() {
		return email;
	}

	public String getAas_et() {
		return aas_et;
	}

	public String getServices() {
		return services;
	}

	public String getContinueUrl() {
		return continueUrl;
	}

	/**
	 * List the services, the user is clear for.
	 * 
	 * @return list of service names. Potentially empty, never null
	 */
	public List<String> listServices() {
		ArrayList<String> ret = new ArrayList<String>();
		if (services != null) {
			String[] tmp = services.split(" *, *");
			for (String s : tmp) {
				ret.add(s);
			}
		}
		return ret;
	}

	/**
	 * Get the session cookie
	 * 
	 * @return a token that can be used for getting access.
	 */
	public String getAuthToken() {
		return authToken;
	}

	/**
	 * Sing into Play
	 * 
	 * @param client httpclient
	 * @param uid email
	 * @param pwd password
	 * @return new instance
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static Identity signIn(HttpClient client, String uid, String pwd) throws IOException, URISyntaxException, InterruptedException {
		Locale loc = Locale.getDefault();
		String epwd;
		try {
			epwd = encryptString(uid + "\u0000" + pwd);
		}
		catch (Exception e) {
			// Should not happen unless the user is in a country with an embargo on
			// cryptography. In which case, we are screwed anyway.
			throw new RuntimeException("Could not encrypt password", e);
		}

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("Email", uid));
		params.add(new BasicNameValuePair("EncryptedPasswd", epwd));
		params.add(new BasicNameValuePair("service", MARKET));
		params.add(new BasicNameValuePair("add_account", "1"));
		params.add(new BasicNameValuePair("sdk_version", "16"));
		params.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
		params.add(new BasicNameValuePair("hasPermission", "1"));
		params.add(new BasicNameValuePair("source", "android"));
		params.add(new BasicNameValuePair("app", "com.android.vending"));
		if (loc != null) {
			params.add(new BasicNameValuePair("device_country", loc.getLanguage()));
			params.add(new BasicNameValuePair("lang", loc.getLanguage()));
		}

		Map<String, String> map = doPost(client, params);
		Identity ret = new Identity();
		System.out.println("signIn request -> " + map);
		String tok = map.get("Token");
		ret.continueUrl = map.get("continueUrl");
		ret.firstName = map.get("firstName");
		ret.lastName = map.get("lastName");
		ret.email = map.get("Email");
		ret.services = map.get("services");
		ret.authToken = map.get("Auth");
		ret.aas_et = tok;

		if (tok != null && tok.length() > 0) {
			// Since mid Oct/2017, "Token" must be sent back if account details
			// (first-, lastname, services) are requested by "add_account".
			// Otherwise the "Auth" cookie will have a short TTL.
			params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("Authorization", "GoogleLogin auth="
					+ map.get("Auth")));
			params.add(new BasicNameValuePair("Token", tok));
			params.add(new BasicNameValuePair("token_request_options", "CAA4AQ=="));
			params.add(new BasicNameValuePair("service", MARKET));
			params.add(new BasicNameValuePair("accountType", "HOSTED_OR_GOOGLE"));
			params.add(new BasicNameValuePair("app", "com.android.vending"));
			map = doPost(client, params);
			ret.authToken = map.get("Auth");
		}
		return ret;
	}

	private static Map<String, String> doPost(HttpClient client, List<NameValuePair> params) throws IOException, URISyntaxException, InterruptedException {
		HttpPost httppost = new HttpPost(LOGIN_URL);
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		HttpResponse response = client.execute(httppost);
		Map<String, String> map = convertToMapLayout(EntityUtils.toString(response.getEntity()));
		if (response.getStatusLine().getStatusCode() == 200) {
			return map;
		}

		if (map.containsKey("Error")) {
			if (map.get("Error").contains("NeedsBrowser") && map.get("Url").length() > 0) {
				List<NameValuePair> pairs = new URIBuilder(map.get("Url")).getQueryParams();
				for (NameValuePair pair : pairs) {
					if (pair.getName().contains("plt")) {
						URI uri = new URIBuilder(CONTINUE_URL)
										.addParameter("nojavascript", "1")
										.addParameter("bgresponse", "js_disabled")
										.addParameter("continue", "https://accounts.google.com/o/android/auth?hl=en_en&xoauth_display_name=Android+Login+Service&source=Android+Login")
										.addParameter("sarp", "1")
										.addParameter("scc", "1")
										.addParameter("plt", pair.getValue())
										.build();
						map.put("continueUrl", uri.toString());
						return map;
					}
				}
			} else {
				throw new ClientProtocolException(map.get("Error"));
			}
		}

		throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
	}

	public static Map<String, String> convertToMapLayout(String response) {
		Map<String, String> map = new HashMap<>();
		StringTokenizer st = new StringTokenizer(response, "\n\r");
		while (st.hasMoreTokens()) {
			String[] keyValue = st.nextToken().split("=", 2);
			if (keyValue.length >= 2) {
				map.put(keyValue[0], keyValue[1]);
			}
		}
		return map;
	}

	private static String encryptString(String str)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, UnsupportedEncodingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

		int i = 0;

		byte[] obj = new byte[5];
		Key createKeyFromString = createKeyFromString(PUBKEY, obj);
		if (createKeyFromString == null) {
			return null;
		}

		Cipher instance = Cipher.getInstance("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");
		byte[] bytes = str.getBytes("UTF-8");
		int length = ((bytes.length - 1) / 86) + 1;
		byte[] obj2 = new byte[(length * 133)];
		while (i < length) {
			instance.init(1, createKeyFromString);
			byte[] doFinal = instance.doFinal(bytes, i * 86,
					i == length + -1 ? bytes.length - (i * 86) : 86);
			System.arraycopy(obj, 0, obj2, i * 133, obj.length);
			System
					.arraycopy(doFinal, 0, obj2, (i * 133) + obj.length, doFinal.length);
			i++;
		}
		return Base64.encodeToString(obj2, 10);
	}

	private static PublicKey createKeyFromString(String str, byte[] bArr)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] decode = Base64.decode(str, 0);
		int readInt = readInt(decode, 0);
		byte[] obj = new byte[readInt];
		System.arraycopy(decode, 4, obj, 0, readInt);
		BigInteger bigInteger = new BigInteger(1, obj);
		int readInt2 = readInt(decode, readInt + 4);
		byte[] obj2 = new byte[readInt2];
		System.arraycopy(decode, readInt + 8, obj2, 0, readInt2);
		BigInteger bigInteger2 = new BigInteger(1, obj2);
		decode = MessageDigest.getInstance("SHA-1").digest(decode);
		bArr[0] = (byte) 0;
		System.arraycopy(decode, 0, bArr, 1, 4);
		return KeyFactory.getInstance("RSA").generatePublic(
				new RSAPublicKeySpec(bigInteger, bigInteger2));

	}

	private static int readInt(byte[] bArr, int i) {
		return (((((bArr[i] & 255) << 24) | 0) | ((bArr[i + 1] & 255) << 16)) | ((bArr[i + 2] & 255) << 8))
				| (bArr[i + 3] & 255);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(firstName);
		sb.append(' ');
		sb.append(lastName);
		sb.append('<');
		sb.append(email);
		sb.append('>');
		sb.append('\t');
		sb.append(services);
		sb.append('\t');
		sb.append(authToken);
		return sb.toString();
	}

}
