package carnero.kecy;

import android.os.Environment;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Base {
	public static String kecy = "kecy.roumen.cz";
	public static String feed =  "/roumingPiclensRSS.php";
	public static String tag = "kecy";

	public ArrayList<Image> getKecy(String host, String path) {
		ArrayList<Image> images = null;

		int httpCode = -1;

		URLConnection uc = null;
		HttpURLConnection connection = null;
		Integer timeout = 30000;

		for (int i = 0; i < 5; i++) {
			if (i > 0) {
				Log.w(Base.tag, "Failed to download data, retrying. Attempt #" + (i + 1));
			}

			timeout = 30000 + (i * 10000);

			try {
				// GET
				final URL u = new URL("http://" + host + path);
				uc = u.openConnection();

				uc.setRequestProperty("Host", host);
				uc.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
				uc.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
				uc.setRequestProperty("Accept-Language", "en-US");
				uc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.86 Safari/533.4");
				uc.setRequestProperty("Connection", "keep-alive");
				uc.setRequestProperty("Keep-Alive", "300");

				connection = (HttpURLConnection) uc;
				connection.setReadTimeout(timeout);
				connection.setRequestMethod("GET");
				connection.setDoInput(true);
				connection.setDoOutput(false);

				final String encoding = connection.getContentEncoding();
				InputStream ins;

				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
					ins = new GZIPInputStream(connection.getInputStream());
				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
					ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
				} else {
					ins = connection.getInputStream();
				}

				httpCode = connection.getResponseCode();

				if (connection != null) {
					Log.i(Base.tag, " Downloading server response (" + httpCode + ", " + connection.getResponseMessage() + ") " + "http://" + host + path);
				} else {
					Log.i(Base.tag, " Failed to download server response (" + httpCode + ") " + "http://" + host + path);
				}

				
				RSSParser parser = new RSSParser(host, path);
				images = parser.parse(ins);

				connection.disconnect();
				ins.close();
			} catch (IOException e) {
				Log.e(Base.tag, "getKecy.IOException: " + e.toString());
			} catch (Exception e) {
				Log.e(Base.tag, "getKecy: " + e.toString());
			}

			if (images != null) {
				break;
			}
		}

		return images;
	}

	public static String getStorage() {
		String storage = null;

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			storage = Environment.getExternalStorageDirectory() + "/.roumen/";
		} else {
			storage = Environment.getDataDirectory() + "/data/carnero.kecy/images/";
		}

		return storage;
	}

	public static String md5(String text) {
		String hashed = "";

		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(text.getBytes(), 0, text.length());
			hashed = new BigInteger(1, digest.digest()).toString(16);
		} catch (Exception e) {
			// nothing
		}

		return hashed;
	}
}
