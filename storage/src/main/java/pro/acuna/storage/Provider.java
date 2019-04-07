	package pro.acuna.storage;
	/*
	 Created by Acuna on 28.04.2018
	*/
	
	import android.app.Activity;
	import android.content.Intent;
	import android.net.Uri;
	import android.os.Bundle;
	import android.webkit.WebView;
	
	import org.json.JSONException;
	import org.json.JSONObject;
	
	import java.io.InputStream;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	
	import pro.acuna.andromeda.OS;
	import pro.acuna.andromeda.User;
	import pro.acuna.jabadaba.Files;
	import pro.acuna.jabadaba.HttpRequest;
	import pro.acuna.jabadaba.Net;
	import pro.acuna.jabadaba.Strings;
	import pro.acuna.jabadaba.exceptions.HttpRequestException;
	import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
	
	public abstract class Provider {
		
		public Storage storage;
		protected JSONObject authData = new JSONObject ();
		protected boolean delPermanent = true;
		
		public Provider () {}
		
		protected Provider (Storage storage) {
			this.storage = storage;
		}
		
		protected final String prepRemoteFile (String str) throws StorageException {
			
			str = Strings.trim ("/", str);
			
			if (!getRootDir ().equals ("") && !getRootDir ().equals ("/") && !str.equals ("")) {
				
				if (!getRootDir ().equals ("/" + str))
					str = getRootDir () + "/" + str;
				
			} else str = getRootDir () + str;
			
			return str;
			
		}
		
		protected final boolean force (String remoteFile, boolean force) throws StorageException {
			return ((!force && !isExists (remoteFile)) || force);
		}
		
		final JSONObject getProviderItems () throws StorageException {
			
			try {
				
				JSONObject data = new JSONObject ();
				
				data.put (Storage.ITEM_ICON, 0);
				data.put (Storage.ITEM_LAYOUT, 0);
				
				return setProviderItems (data);
				
			} catch (JSONException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		public InputStream getStream (String remoteFile, String link) throws StorageException {
			
			try {
				return Net.getStream (remoteFile, storage.settings.getString ("useragent"), Files.getExtension (link));
			} catch (JSONException | HttpRequestException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		public Provider auth () throws StorageException {
			return this;
		}
		
		public Map<String, Object> setDefData (Map<String, Object> data) {
			return data;
		}
		
		public String[] getDecryptedKeys () {
			return new String[0];
		}
		
		public String getAuthUrl () throws StorageException {
			return null;
		}
		
		public String getRedirectUrl () throws StorageException {
			return null;
		}
		
		public WebView setWebView (WebView webView) throws StorageException {
			return webView;
		}
		
		public void chmod (int chmod, String remoteFile) throws StorageException {}
		
		public void close () throws StorageException {}
		
		public String getRootDir () throws StorageException {
			return "";
		}
		
		protected final Storage processUrl () throws StorageException {
			return processUrl (new HashMap<String, String> (), new HashMap<String, String> ());
		}
		
		public Storage processUrl (Map<String, String> urlData, Map<String, String> redirectUrlData) throws StorageException {
			
			try {
				
				if (urlData.get (Net.URL_DOMAIN).equals (redirectUrlData.get (Net.URL_DOMAIN))) {
					
					Map<String, Object> data = Net.urlQueryDecode (urlData.get (Net.URL_ANCHOR));
					
					storage.activity.setResult (Activity.RESULT_OK, OS.toIntent (data));
					storage.activity.finish ();
					
				}
				
				return storage;
				
			} catch (JSONException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		private String nextId;
		
		protected final void setNextId (String nextId) {
			this.nextId = nextId;
		}
		
		public final String getNextId () {
			return nextId;
		}
		
		private String userId;
		
		protected final void setUserId (String id) {
			this.userId = id;
		}
		
		public final String getUserId () {
			return userId;
		}
		
		private JSONObject data = new JSONObject ();
		
		protected final void setOutputData (JSONObject data) {
			this.data = data;
		}
		
		public final JSONObject getOutputData () {
			return data;
		}
		
		public Storage onStartAuthActivity () throws StorageException {
			return storage;
		}
		
		public Storage onAuthActivityBack (Intent intent) throws StorageException {
			
			storage.activity.setResult (Activity.RESULT_CANCELED, intent);
			storage.activity.finish ();
			
			return storage;
			
		}
		
		public Intent getAuthIntent () {
			return new Intent (storage.activity, StorageActivity.class);
		}
		
		public int perPage () {
			return 30;
		}
		
		public Uri getUri (String remoteFile) throws StorageException {
			return Uri.parse (getDirectLink (remoteFile));
		}
		
		public long getTotalSize () throws StorageException {
			return 0;
		}
		
		public String setError (JSONObject result) {
			return result.toString ();
		}
		
		protected String getAppsFolderTitle () {
			return storage.context.getString (R.string.apps_folder_title);
		}
		
		protected final HttpRequest initRequest (HttpRequest request) {
			
			request.setListener (new Net.ProgressListener () {
				
				@Override
				public void onStart (long size) {
					//if (storage.listener != null) storage.listener.onStart (size);
				}
				
				@Override
				public void onProgress (final long length, final long size) {
					if (storage.listener != null) storage.listener.onProgress (length, size);
				}
				
				@Override
				public void onError (int code, String result) {
					if (storage.listener != null) storage.listener.onError (code, result);
				}
				
				@Override
				public void onFinish (int code, String result) {
					if (storage.listener != null) storage.listener.onFinish (code);
				}
				
			});
			
			return request;
			
		}
		
		abstract public Provider newInstance (Storage storager) throws StorageException;
		abstract public String getName ();
		abstract public String getDirectLink (String remoteFile) throws StorageException;
		abstract public JSONObject setProviderItems (JSONObject data) throws StorageException;
		abstract public User getUser (Bundle bundle) throws StorageException;
		
		abstract public long getSize (String remoteFile) throws StorageException;
		abstract public boolean isDir (String remoteFile) throws StorageException;
		abstract public boolean isExists (String remoteFile) throws StorageException;
		abstract public int makeDir (String remoteFile) throws StorageException;
		
		abstract public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException;
		abstract public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException;
		abstract public void delete (String remoteFile) throws StorageException;
		abstract public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException;
		abstract public boolean isAuthenticated () throws StorageException;
		
	}