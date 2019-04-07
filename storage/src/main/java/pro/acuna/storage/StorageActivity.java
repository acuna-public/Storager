	package pro.acuna.storage;
	/*
	 Created by Acuna on 25.07.2017
	*/
	
	import android.app.Activity;
	import android.os.Bundle;
	import android.view.KeyEvent;
	import android.widget.TextView;
	
	public class StorageActivity extends Activity {
		
		private Storage storage;
		
		private TextView textView;
		
		@Override
		protected void onCreate (Bundle savedInstanceState) {
			
			super.onCreate (savedInstanceState);
			
			setContentView (R.layout.content);
			
			textView = findViewById (R.id.text);
			textView.setText (R.string.loading);
			
			Bundle bundle = getIntent ().getExtras ();
			String type = bundle.getString ("type");
			
			try {
				
				storage = new Storage (this, bundle);
				storage.init (type, bundle);
				
				storage = storage.onStartAuthActivity ();
				storage.loadUrl (textView, findViewById (R.id.button));
				
			} catch (StorageException e) {
				textView.setText (e.getMessage ());
			}
			
		}
		
		@Override
		public boolean onKeyDown (int keyCode, KeyEvent event) {
			
			try {
				storage = storage.onAuthActivityBack (keyCode);
			} catch (StorageException e) {
				textView.setText (e.getMessage ());
			}
			
			return super.onKeyDown (keyCode, event);
			
		}
		
	}