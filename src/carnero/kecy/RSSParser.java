package carnero.kecy;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import java.io.InputStream;
import java.util.ArrayList;
import org.xml.sax.Attributes;

public class RSSParser {
	public static String namespaceMedia = "http://search.yahoo.com/mrss";
	public static String namespaceAtom = "http://www.w3.org/2005/Atom";

	private String host = "";
	private String path = "";
	private ArrayList<Image> imgList = new ArrayList<Image>();
	private Image img = null;

	public RSSParser(String hostIn, String pathIn) {
		host = hostIn;
		path = pathIn;
	}

	public ArrayList<Image> parse(InputStream inputStream) {
		final RootElement rss = new RootElement("rss");
		final Element channel = rss.getChild("channel");
		final Element item = channel.getChild("item");

		// channel
		channel.setStartElementListener(new StartElementListener() {
			public void start(Attributes attrs) {

			}
		});

		channel.setEndElementListener(new EndElementListener() {
			public void end() {

			}
		});

		// item
		item.setStartElementListener(new StartElementListener() {
			public void start(Attributes arg0) {
				img = new Image();
			}
		});

		item.setEndElementListener(new EndElementListener() {
			public void end() {
				if (img.source != null) {
					imgList.add(img);
				}
			}
		});

		// image - title
		item.getChild("title").setEndTextElementListener(new EndTextElementListener() {
			public void end(String text) {
				try {
					img.title = text.trim();
				} catch (Exception e) {
					// nothing
				}
			}
		});

		// image - title
		item.getChild("link").setEndTextElementListener(new EndTextElementListener() {
			public void end(String text) {
				try {
					img.link = "http://" + host + "/" + text.trim();
				} catch (Exception e) {
					// nothing
				}
			}
		});

		// image - guid
		item.getChild("guid").setEndTextElementListener(new EndTextElementListener() {
			public void end(String text) {
				try {
					img.guid = text.trim();
				} catch (Exception e) {
					// nothing
				}
			}
		});

		// image - thumbnail
		item.getChild(namespaceMedia, "thumbnail").setStartElementListener(new StartElementListener() {
			public void start(Attributes attributes) {
				if (attributes.getIndex("url") > -1) {
					img.thumbnail = "http://" + host + "/" + attributes.getValue("url").trim();
				}
			}
		});

		// image - source
		item.getChild(namespaceMedia, "content").setStartElementListener(new StartElementListener() {
			public void start(Attributes attributes) {
				if (attributes.getIndex("url") > -1) {
					img.source = "http://" + host + "/" + attributes.getValue("url").trim();
				}
				if (attributes.getIndex("type") > -1) {
					img.mime = attributes.getValue("type").trim();
				}
			}
		});

		try {
			Xml.parse(inputStream, Xml.Encoding.UTF_8, rss.getContentHandler());
		} catch (Exception e) {
			Log.e(Base.tag, "Cannot parse RSS file");
		}

		return imgList;
	}
}
