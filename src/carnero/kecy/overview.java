package carnero.kecy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class overview extends Activity {
	private Activity activity = null;
	private Base base = null;
	private LayoutInflater inflater = null;
	private ArrayList<Image> images = null;
	private LinearLayout imagesLayout = null;
	private LinearLayout duopackLayout = null;
	private ImageView imageView = null;
	private TextView imageTitle = null;
	private int maxThreads = 5;
	private ArrayList<loadImageThread> threads = new ArrayList<loadImageThread>();
	private int threadsRunning = 0;

	private Handler loadHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (images.size() > 0) {
				imagesLayout = (LinearLayout)findViewById(R.id.images);
				imagesLayout.removeAllViewsInLayout();

				boolean first = true;
				for (Image img : images) {
					if (first == true) {
						if (duopackLayout != null) {
							imagesLayout.addView(duopackLayout, imagesLayout.getChildCount());
						}
						duopackLayout = (LinearLayout)inflater.inflate(R.layout.duopack, null);
					}

					if (first == true) {
						first = false;

						imageView = (ImageView)duopackLayout.findViewById(R.id.image_left);
						imageTitle = (TextView)duopackLayout.findViewById(R.id.image_left_title);
					} else {
						first = true;

						imageView = (ImageView)duopackLayout.findViewById(R.id.image_right);
						imageTitle = (TextView)duopackLayout.findViewById(R.id.image_right_title);
					}

					String title = img.title;
					String ext = null;
					final String[] titleParts = title.split("\\.");
					if (titleParts.length > 1) {
						ext = titleParts[(titleParts.length - 1)];
					}
					if (ext != null && (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("gif"))) {
						title = title.replaceAll("." + ext + "$", "");
					}

					imageTitle.setText(title);
					imageView.setOnClickListener(new imageDetail(img));

					loadImageHandler imgHandler = new loadImageHandler(imageView);
					loadImageThread imgThread = new loadImageThread(img.thumbnail, imgHandler);

					threads.add(imgThread);
					runNextThread();
				}

				imagesLayout.addView(duopackLayout, imagesLayout.getChildCount());
			}

			(findViewById(R.id.actionbar_progress)).setVisibility(View.GONE);
		}
	};

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

		activity = this;
		base = new Base();
		inflater = getLayoutInflater();

		setContentView(R.layout.overview);

		(findViewById(R.id.actionbar_progress)).setVisibility(View.VISIBLE);
		loadKecy kecyLoader = new loadKecy(Base.kecy, Base.feed, loadHandler);
		kecyLoader.start();
    }

	private class loadKecy extends Thread {
		private String host = null;
		private String path = null;
		private Handler handler = null;

		public loadKecy(String hostIn, String pathIn, Handler handlerIn) {
			host = hostIn;
			path = pathIn;
			handler = handlerIn;
		}

		@Override
		public void run() {
			if (host == null || path == null) {
				return;
			}

			images = base.getKecy(host, path);

			handler.sendEmptyMessage(1);
		}
	}

	private class loadImageThread extends Thread {
		private String url = null;
		private Handler handler = null;

		public loadImageThread(String urlIn, Handler handlerIn) {
			url = urlIn;
			handler = handlerIn;
		}

		@Override
		public void run() {
			threadsRunning ++;
			threads.remove(this);

			BitmapDrawable image = null;
			try {
				ImgGet imgGetter = new ImgGet(activity, 100, 100);

				image = imgGetter.getDrawable(url);
				Message message = handler.obtainMessage(0, image);
				handler.sendMessage(message);
			} catch (Exception e) {
				// nothing
			}
		}
	}

	private class loadImageHandler extends Handler {
		ImageView view = null;

		public loadImageHandler(ImageView viewIn) {
			view = viewIn;
		}

		@Override
		public void handleMessage(Message message) {
			threadsRunning --;

			runNextThread();

			BitmapDrawable image = (BitmapDrawable) message.obj;
			if (image != null) {
				view.setImageDrawable(image);
			}
		}
	}

	private void runNextThread() {
		if (threadsRunning < maxThreads) {
			ArrayList<loadImageThread> threadsTmp = (ArrayList<loadImageThread>)threads.clone();

			for (loadImageThread thread : threadsTmp) {
				if (thread.isAlive() == false) {
					thread.start();
					
					return;
				}
			}
		}
	}

	private class imageDetail implements View.OnClickListener {
		Image img = null;

		public imageDetail(Image imgIn) {
			img = imgIn;
		}

		public void onClick(View view) {
			Intent detailIntent = new Intent(activity, detail.class);
			detailIntent.putExtra("source", img.source);

			startActivity(detailIntent);
		}
	}
}