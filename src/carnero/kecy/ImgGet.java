package carnero.kecy;

import android.app.Activity;
import android.util.Log;
import android.text.Html;
import android.view.Display;
import android.view.WindowManager;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class ImgGet implements Html.ImageGetter {
	private Activity activity = null;
	private int width = -1;
	private int height = -1;
	private float pixelDensity = 1f;
	private BitmapFactory.Options bfOptions = new BitmapFactory.Options();

	public ImgGet(Activity activityIn, int widthIn, int heightIn) {
		activity = activityIn;
		width = widthIn;
		height = heightIn;

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		pixelDensity = metrics.density;

		bfOptions.inTempStorage = new byte[16*1024];
	}

	@Override
	public BitmapDrawable getDrawable(String url) {
		Bitmap imagePre = null;
		String fileName = null;

		if (url == null || url.length() == 0) {
			return null;
		}

		final String[] urlParts = url.split("\\.");
		String urlExt = null;
		if (urlParts.length > 1) {
			urlExt = "." + urlParts[(urlParts.length - 1)];
			if (urlExt.length() > 5) {
				urlExt = "";
			}
		} else {
			urlExt = "";
		}

		fileName = Base.getStorage() + Base.md5(url) + urlExt;

		File dir = null;
		dir = new File(Base.getStorage());
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		dir = null;

		try {
            final File file = new File(fileName);
            if (file.exists() == true) {
				final long imageSize = file.length();

				// large images will be downscaled on input to save memory
				if (imageSize > (6 * 1024 * 1024)) {
					bfOptions.inSampleSize = 48;
				} else if (imageSize > (4 * 1024 * 1024)) {
					bfOptions.inSampleSize = 16;
				} else if (imageSize > (2 * 1024 * 1024)) {
					bfOptions.inSampleSize = 10;
				} else if (imageSize > (1 * 1024 * 1024)) {
					bfOptions.inSampleSize = 6;
				} else if (imageSize > (0.5 * 1024 * 1024)) {
					bfOptions.inSampleSize = 2;
				}

                if (file.lastModified() > ((new Date()).getTime() - (24 * 60 * 60 * 1000))) {
                    imagePre = BitmapFactory.decodeFile(fileName, bfOptions);
                }
			}
		} catch (Exception e) {
			Log.w(Base.tag, "ImgGet.getDrawable (reading cache): " + e.toString());
		}

		if (imagePre == null) {
			Uri uri = null;
			HttpClient client = null;
			HttpGet getMethod = null;
			HttpResponse httpResponse = null;
			HttpEntity entity = null;
			BufferedHttpEntity bufferedEntity = null;

			try {
				// check if uri is absolute or not, if not attach geocaching.com hostname and scheme
				uri = Uri.parse(url);

				if (uri.isAbsolute() == false) {
					url = "http://kecy.roumen.cz" + url;
				}
			} catch (Exception e) {
				Log.e(Base.tag, "ImgGet.getDrawable (parse URL): " + e.toString());
			}

			if (uri != null) {
				for (int i = 0; i < 2; i ++) {
					if (i > 0) Log.w(Base.tag, "ImgGet.getDrawable: Failed to download data, retrying. Attempt #" + (i + 1));

					try {
						client = new DefaultHttpClient();
						getMethod = new HttpGet(url);
						httpResponse = client.execute(getMethod);
						entity = httpResponse.getEntity();
						bufferedEntity = new BufferedHttpEntity(entity);

						final long imageSize = bufferedEntity.getContentLength();

						// large images will be downscaled on input to save memory
						if (imageSize > (6 * 1024 * 1024)) {
							bfOptions.inSampleSize = 48;
						} else if (imageSize > (4 * 1024 * 1024)) {
							bfOptions.inSampleSize = 16;
						} else if (imageSize > (2 * 1024 * 1024)) {
							bfOptions.inSampleSize = 10;
						} else if (imageSize > (1 * 1024 * 1024)) {
							bfOptions.inSampleSize = 6;
						} else if (imageSize > (0.5 * 1024 * 1024)) {
							bfOptions.inSampleSize = 2;
						}
						
						Log.i(Base.tag, "[" + entity.getContentLength() + "B] Downloading image " + url);

						if (bufferedEntity != null) imagePre = BitmapFactory.decodeStream(bufferedEntity.getContent(), null, bfOptions);
						if (imagePre != null) break;
					} catch (Exception e) {
						Log.e(Base.tag, "ImgGet.getDrawable (downloading from web): " + e.toString());
					}
				}
			}

			try {
				// save to memory/SD cache
				if (bufferedEntity != null) {
					final InputStream is = (InputStream)bufferedEntity.getContent();
					final FileOutputStream fos = new FileOutputStream(fileName);
					try {
						final byte[] buffer = new byte[4096];
						int l;
						while ((l = is.read(buffer)) != -1) fos.write(buffer, 0, l);
					} catch (IOException e) {
						Log.e(Base.tag, "ImgGet.getDrawable (saving to cache): " + e.toString());
					} finally {
						is.close();
						fos.flush();
						fos.close();
					}
				}
			} catch (Exception e) {
				Log.e(Base.tag, "ImgGet.getDrawable (saving to cache): " + e.toString());
			}

			entity = null;
			bufferedEntity = null;
		}

		if (imagePre == null) {
			Log.d(Base.tag, "ImgGet.getDrawable: Failed to obtain image");

			return null;
		}

		Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		final int imgWidth = imagePre.getWidth();
		final int imgHeight = imagePre.getHeight();
		int maxWidth = display.getWidth() - 25;
		int maxHeight = display.getHeight() - 25;
		if (width > 0 && height > 0) {
			maxWidth = width;
			maxHeight = height;
			/*
			maxWidth = (int)(width * pixelDensity);
			maxHeight = (int)(height * pixelDensity);
			 */
		}
		int widthFinal = imgWidth;
		int heightFinal = imgHeight;
		double ratio = 1.0d;

		if (imgWidth > maxWidth || imgHeight > maxHeight) {
			if ((maxWidth / imgWidth) > (maxHeight / imgHeight)) {
				ratio = (double)maxHeight / (double)imgHeight;
			} else {
				ratio = (double)maxWidth / (double)imgWidth;
			}

			widthFinal = (int)Math.ceil(imgWidth * ratio);
			heightFinal = (int)Math.ceil(imgHeight * ratio);

			try {
				imagePre = Bitmap.createScaledBitmap(imagePre, widthFinal, heightFinal, true);
			} catch (Exception e) {
				Log.d(Base.tag, "ImgGet.getDrawable: Failed to scale image");
				return null;
			}
		}

		final BitmapDrawable image = new BitmapDrawable(imagePre);
		image.setBounds(new Rect(0, 0, width, height));

		return image;
	}
}
