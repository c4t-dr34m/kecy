package carnero.kecy;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class detail extends Activity {
	private Activity activity = null;
	private Base base = null;
	private String source = null;
	private ImageView imageView = null;

	@Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

		activity = this;
		base = new Base();
		
		setContentView(R.layout.detail);

		// init view
		imageView = (ImageView)findViewById(R.id.image);
		
		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			source = extras.getString("source");
		}

		loadImageHandler imgHandler = new loadImageHandler(imageView);
		loadImageThread imgThread = new loadImageThread(source, imgHandler);
		imgThread.start();
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
			BitmapDrawable image = null;
			try {
				ImgGet imgGetter = new ImgGet(activity, -1, -1);

				image = imgGetter.getDrawable(source);
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
			BitmapDrawable image = (BitmapDrawable) message.obj;
			if (image != null) {
				view.setImageDrawable(image);
			}
		}
	}
}
