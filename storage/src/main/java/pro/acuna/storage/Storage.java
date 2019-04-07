	package pro.acuna.storage;
	/*
	 Created by Acuna on 10.05.2017
	*/
	
	import android.app.Activity;
	import android.app.AlertDialog;
	import android.content.Context;
	import android.content.Intent;
	import android.graphics.Bitmap;
	import android.os.Bundle;
	import android.view.KeyEvent;
	import android.view.View;
	import android.webkit.WebView;
	import android.widget.EditText;
	import android.widget.TextView;
	
	import org.json.JSONArray;
	import org.json.JSONException;
	import org.json.JSONObject;
	
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileOutputStream;
	import java.io.IOException;
	import java.io.InputStream;
	import java.io.OutputStream;
	import java.net.MalformedURLException;
	import java.net.URL;
	import java.util.ArrayList;
	import java.util.LinkedHashMap;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	
	import pro.acuna.andromeda.Device;
	import pro.acuna.andromeda.OS;
	import pro.acuna.andromeda.User;
	import pro.acuna.jabadaba.Arrays;
	import pro.acuna.jabadaba.Files;
	import pro.acuna.jabadaba.HttpRequest;
	import pro.acuna.jabadaba.Int;
	import pro.acuna.jabadaba.Net;
	import pro.acuna.jabadaba.Streams;
	import pro.acuna.jabadaba.Strings;
	import pro.acuna.jabadaba.exceptions.HttpRequestException;
	import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
	import pro.acuna.storage.providers.Dropbox;
	import pro.acuna.storage.providers.GDrive;
	import pro.acuna.storage.providers.SDCard;
	import pro.acuna.storage.providers.SFTP;
	import pro.acuna.storage.providers.YandexDisk;
	
	public class Storage {
		
		private static final String NAME = "storage";
		public static final String VERSION = BuildConfig.VERSION_NAME;
		
		public Activity activity;
		public Context context;
		
		private boolean refreshToken = true, auth = true;
		public JSONObject settings = new JSONObject ();
		int id = 0;
		public Prefs utils;
		public pro.acuna.andromeda.Prefs prefs;
		
		protected String type;
		String prefsName;
		
		public Provider provider;
		
		private List<Provider> providers = new ArrayList<> ();
		
		public boolean wiFiOnly = true;
		public JSONObject data = new JSONObject ();
		
		public static final String ITEM_TITLE = "title";
		public static final String ITEM_ICON = "icon";
		public static final String ITEM_LAYOUT = "layout";
		
		protected Storage () {}
		
		public Storage (Context context) {
			this (context, 0);
		}
		
		public Storage (Context context, Bundle bundle) {
			this (context, bundle.getInt ("id"));
		}
		
		public Storage (Context context, int id) {
			this (context, id, NAME);
		}
		
		public Storage (Context context, int id, String prefsName) {
			
			this.context = context;
			this.id = id;
			this.prefsName = prefsName;
			
			if (context instanceof Activity)
				activity = (Activity) context;
			
			addProvider (new SDCard ());
			addProvider (new SFTP ());
			addProvider (new Dropbox ());
			addProvider (new GDrive ());
			addProvider (new YandexDisk ());
			
			utils = new Prefs (this);
			prefs = utils.loadPrefs ();
			
		}
		
		public String getName () {
			return provider.getName ();
		}
		
		public boolean isAuthenticated () throws StorageException {
			return provider.isAuthenticated ();
		}
		
		public JSONObject setProviderItems (JSONObject data) throws StorageException {
			return (Arrays.contains (type, items) ? items.get (type) : provider.setProviderItems (data));
		}
		
		private Map<String, JSONObject> items = new HashMap<> ();
		
		public Storage setProviderItem (String type, JSONObject data) {
			
			items.put (type, data);
			return this;
			
		}
		
		public Map<String, Object> getDefData () throws StorageException {
			
			Map<String, Object> data = new LinkedHashMap<> ();
			
			data.put ("type", type);
			
			provider = provider.newInstance (this);
			
			return provider.setDefData (data);
			
		}
		
		private Provider getProvider (String name) {
			
			for (Provider provider : providers)
				if (provider.getName ().equals (name))
					return provider;
			
			return null;
			
		}
		
		public List<Provider> getProviders () {
			return providers;
		}
		
		public class ProviderItem {
			
			private String name, title, icon;
			
			public String getTitle () {
				return title;
			}
			
			public String getName () {
				return name;
			}
			
		}
		
		public class ProvidersData {
			
			private List<ProviderItem> items = new ArrayList<> ();
			public String[] names, titles;
			
			ProvidersData () throws StorageException {
				
				names = new String[Int.size (providers)];
				titles = new String[Int.size (providers)];
				
				int i = 0;
				
				for (Provider provider : providers) {
					
					try {
						
						JSONObject item = provider.setProviderItems (new JSONObject ());
						
						ProviderItem item2 = new ProviderItem ();
						
						item2.name = names[i] = provider.getName ();
						item2.title = titles[i] = item.getString (ITEM_TITLE);
						
						items.add (item2);
						
						++i;
						
					} catch (JSONException e) {
						throw new StorageException (e);
					}
					
				}
				
			}
			
			public List<ProviderItem> getItems () {
				return items;
			}
			
		}
		
		public ProvidersData getProvidersData () throws StorageException {
			return new ProvidersData ();
		}
		
		public Storage addProvider (Provider provider) {
			
			providers.add (provider);
			return this;
			
		}
		
		public Storage addProviders (List<Provider> list) {
			
			providers = new ArrayList<> ();
			
			for (Provider provider : list)
				addProvider (provider);
			
			return this;
			
		}
		
		private void getId () throws StorageException {
			
			if (id == 0) id = prefs.getInt (type + "_account", 1);
			provider = provider.newInstance (this);
			
		}
		
		public JSONObject defaultData () throws StorageException {
			
			try {
				
				getId ();
				
				Map<String, Object> defData = getDefData ();
				
				JSONObject data = new JSONObject ();
				
				for (String key : defData.keySet ()) {
					
					Object obj = defData.get (key);
					
					if (obj instanceof Integer)
						data.put (key, utils.getPref (key, (int) obj));
					else
						data.put (key, utils.getPref (key, obj.toString ()));
					
				}
				
				return data;
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		private JSONObject defaultData (JSONArray data) throws StorageException {
			
			try {
				
				JSONObject data2 = new JSONObject ();
				
				int i = 0;
				
				for (String key : getDefData ().keySet ()) {
					
					data2.put (key, data.get (i));
					++i;
					
				}
				
				return data2;
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public Storage init (JSONArray data) throws StorageException {
			
			try {
				
				this.type = data.getString (0);
				provider = getProvider (type);
				
				JSONObject output = new JSONObject ();
				output.put (type, defaultData (data));
				
				init (type, output);
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
			return this;
			
		}
		
		public Storage init (String type, Bundle bundle) throws StorageException {
			
			makeAuth (false);
			
			wiFiOnly = bundle.getBoolean ("wifi_only", wiFiOnly);
			
			JSONObject data = new JSONObject ();
			
			try {
				data.put (type, pro.acuna.andromeda.Arrays.toJSONObject (bundle));
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
			init (type, data);
			
			return this;
			
		}
		
		public Storage init (JSONObject data) throws StorageException {
			
			try {
				
				JSONObject values = new JSONObject ();
				values.put (data.getString ("type"), data);
				
				return init (data.getString ("type"), values);
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public Storage init (String type) throws StorageException {
			
			/*items.put (TYPE_S3, new Object[] { "Amazon S3" });
			items.put (TYPE_ONEDRIVE, new Object[] { "OneDrive" });
			items.put (TYPE_MEGA, new Object[] { "Mega" });
			
			keys.put (TYPE_S3, new String[] { "key", "secret", "bucket", "region" });*/
			
			this.type = type;
			
			provider = getProvider (type);
			
			JSONObject data2 = data.optJSONObject (type);
			if (data2 == null) data2 = new JSONObject ();
			
			settings = Arrays.extend (data2, getDefData ());
			
			getId ();
			
			return this;
			
		}
		
		public Storage makeAuth (boolean auth) {
			
			this.auth = auth;
			return this;
			
		}
		
		public Storage init (String type, JSONObject data) throws StorageException { // Главный инициализатор
			
			try {
				
				this.data = data;
				
				init (type);
				
				if (auth) provider = provider.auth ();
				
				if (refreshToken && Int.size (provider.authData) > 0) { // Обновляем токен
					
					JSONArray keys = provider.authData.names ();
					
					for (int i = 0; i < Int.size (keys); ++i) {
						
						String key = keys.getString (i);
						utils.setPref (key, provider.authData.get (key));
						
					}
					
					prefs.editor.apply ();
					
					prefs = utils.loadPrefs (); // TODO
					
					refreshToken = false;
					
				}
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
			return this;
			
		}
		
		private Object getPref (String key) throws StorageException {
			return utils.getPref (key, getDefData ());
		}
		
		public String toString () {
			return getName ();
		}
		
		public Storage setWifiOnly (boolean wiFiOnly) {
			
			this.wiFiOnly = wiFiOnly;
			return this;
			
		}
		
		public User getUser (Bundle bundle) throws StorageException {
			return provider.getUser (bundle);
		}
		
		public StorageListener listener;
		
		public Storage setListener (StorageListener listener) {
			
			this.listener = listener;
			return this;
			
		}
		
		public static final String CLOSE_IF_SUCCESS = "close_if_success";
		
		public void startAuthActivityAndFinish () throws StorageException {
			
			Map<String, Object> data = new HashMap<> ();
			
			data.put (CLOSE_IF_SUCCESS, true);
			
			startAuthActivity (data);
			
		}
		
		public void startAuthActivity () throws StorageException {
			startAuthActivity (new HashMap<String, Object> ());
		}
		
		public void startAuthActivity (Map<String, Object> data) throws StorageException {
			
			try {
				
				Intent intent = provider.getAuthIntent ();
				
				intent.putExtra ("type", type);
				intent.putExtra ("id", id);
				intent.putExtra ("wifi_only", wiFiOnly);
				
				intent = OS.toIntent (intent, data);
				
				JSONArray keys = settings.names ();
				
				for (int i = 0; i < Int.size (keys); ++i) {
					
					String key = keys.getString (i);
					intent.putExtra (key, settings.getString (key));
					
				}
				
				activity.startActivityForResult (intent, 200);
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public Storage onStartAuthActivity () throws StorageException {
			return provider.onStartAuthActivity ();
		}
		
		public Storage onAuthActivityBack (Intent intent) throws StorageException {
			return provider.onAuthActivityBack (intent);
		}
		
		public Storage onAuthActivityBack (int keyCode) throws StorageException {
			return onAuthActivityBack (new Intent (), keyCode);
		}
		
		public Storage onAuthActivityBack (Intent intent, int keyCode) throws StorageException {
			
			switch (keyCode) {
				
				case KeyEvent.KEYCODE_BACK:
					return onAuthActivityBack (intent);
				
			}
			
			return this;
			
		}
		
		public void setPrefs (String user, Bundle bundle) throws StorageException {
			
			try {
				setPrefs (user, pro.acuna.andromeda.Arrays.toJSONObject (bundle));
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public void setPrefs (String user, JSONObject data) throws StorageException {
			
			JSONArray keys = data.names ();
			
			try {
				
				addUser (user);
				
				for (int i = 0; i < Int.size (keys); ++i) {
					
					String key = keys.getString (i);
					Object value = getDefData ().get (key);
					
					if (value instanceof Integer)
						utils.setPref (key, data.getInt (key));
					else
						utils.setPref (key, data.get (key));
					
				}
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
			utils.apply ();
			
			if (listener != null) listener.onAuthSuccess ();
			
		}
		
		public Map<String, String> onActivityResult (int requestCode, int resultCode, Intent intent) throws StorageException {
			
			Map<String, String> output = new HashMap<> ();
			
			if (intent != null && requestCode == 200) {
				
				final Bundle bundle = intent.getExtras ();
				
				if (bundle != null && resultCode == Activity.RESULT_OK) {
					
					try {
						
						User user = getUser (bundle);
						addUser (user.getName ());
						
						for (String key : bundle.keySet ())
							utils.setPref (key, bundle.getString (key));
						
						prefs.apply ();
						
						if (listener != null) listener.onAuthSuccess ();
						
					} catch (JSONException e) {
						throw new StorageException (this, e);
					}
					
				} else if (resultCode != Activity.RESULT_CANCELED)
					throw new StorageException (bundle.getString ("error") + (bundle.getString ("error_description") != null ? ": " + bundle.getString ("error_description") : ""));
				
			}
			
			return output;
			
		}
		
		private void addUser (String user) throws JSONException, StorageException {
			
			JSONObject accounts = getAccounts ();
			JSONArray users = new JSONArray ();
			
			if (accounts.has (type))
				users = accounts.getJSONArray (type);
			
			int id = Int.size (users);
			
			if (!Arrays.contains (user, users)) {
				
				users.put (user);
				++id;
				
				accounts.put (type, users);
				
				prefs.put ("accounts", accounts.toString ());
				prefs.put (type + "_account", id);
				
				this.id = id;
				
			}
			
		}
		
		public JSONObject getAccounts () throws StorageException {
			
			try {
				return prefs.get ("accounts", new JSONObject ());
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public JSONObject getProviderItems () throws StorageException {
			return provider.getProviderItems ();
		}
		
		public String getRootDir () throws StorageException {
			return provider.getRootDir ();
		}
		
		public void chmod (int chmod, String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			provider.chmod (chmod, remoteFile);
			
		}
		
		public final View onDialogView (AlertDialog.Builder builder, View view, JSONObject item) throws StorageException {
			
			try {
				
				JSONArray keys = item.names ();
				
				for (int i = 0; i < Int.size (keys); ++i) {
					
					String key = keys.getString (i);
					
					EditText textView = view.findViewById (item.getInt (key));
					textView.setText (getPref (key).toString ());
					
				}
				
				return view;
				
			} catch (JSONException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public URL getDirectLink (String remoteFile) throws StorageException {
			
			try {
				
				remoteFile = provider.prepRemoteFile (remoteFile);
				return new URL (provider.getDirectLink (remoteFile));
				
			} catch (MalformedURLException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public InputStream getInputStream (String remoteFile) throws StorageException {
			return getInputStream (remoteFile, "");
		}
		
		public InputStream getInputStream (String remoteFile, String link) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			return provider.getStream (provider.getDirectLink (remoteFile), link);
			
		}
		
		public final List<Item> list () throws StorageException, OutOfMemoryException {
			return list ("");
		}
		
		public final List<Item> list (String remoteDir) throws StorageException, OutOfMemoryException {
			return list (remoteDir, 0);
		}
		
		public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
			return provider.list (remoteDir, mode);
		}
		
		public final List<Item> sizeList (String remoteDir, int size) throws StorageException, OutOfMemoryException {
			return sizeList (remoteDir, size, new ArrayList<Item> ());
		}
		
		public final List<Item> sizeList (String remoteFile, int size, List<Item> output) throws StorageException, OutOfMemoryException {
			
			for (Item file : list (remoteFile)) {
				
				if (isDir (file.getFile ()))
					sizeList (file.getFile (), size, output);
				else if (getSize (file.getFile ()) == size)
					output.add (file);
				
			}
			
			return output;
			
		}
		
		public long getSize (String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			return provider.getSize (remoteFile);
			
		}
		
		public boolean isDir (String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			return provider.isDir (remoteFile);
			
		}
		
		public boolean isExists (String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			return provider.isExists (remoteFile);
			
		}
		
		public int makeDir (String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			return provider.makeDir (remoteFile);
			
		}
		
		public final void check () throws StorageException {
			write (getName (), "." + NAME, true, false);
		}
		
		public final void write (Object text, String remoteFile) throws StorageException {
			write (text, remoteFile, true);
		}
		
		public final void write (Object text, String remoteFile, boolean force) throws StorageException {
			write (text, remoteFile, force, true);
		}
		
		public final void write (Object text, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			write (Streams.toInputStream (text), remoteFile, force, makeDir);
		}
		
		public final void copy (File localFile) throws StorageException {
			copy (localFile, localFile.getAbsolutePath ());
		}
		
		public final void copy (File localFile, String remoteFile) throws StorageException {
			copy (localFile, remoteFile, true);
		}
		
		public final void copy (File localFile, String remoteFile, boolean force) throws StorageException {
			
			try {
				copy (localFile, remoteFile, force, localFile.getAbsolutePath ());
			} catch (IOException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		private void copy (File localFile, String remoteFile, boolean force, String root) throws StorageException, IOException {
			
			if (localFile.isDirectory ()) {
				
				File[] files = localFile.listFiles ();
				
				for (File file : files)
					copy (file, Strings.trimStart (root, file), force, root);
				
			} else write (new FileInputStream (localFile), remoteFile, force, true);
			
		}
		
		public void copy (URL url, String remoteFile) throws StorageException {
			copy (url, remoteFile, true, true);
		}
		
		public void copy (URL url, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			try {
				
				HttpRequest request = new HttpRequest (HttpRequest.METHOD_GET, url, settings.optString ("useragent"));
				write (request.getInputStream (), remoteFile, force, makeDir);
				
			} catch (HttpRequestException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public final void write (InputStream stream, String remoteFile) throws StorageException {
			write (stream, remoteFile, true);
		}
		
		public final void write (InputStream stream, String remoteFile, boolean force) throws StorageException {
			write (stream, remoteFile, force, true);
		}
		
		public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			provider.write (stream, remoteFile, force, makeDir);
			
		}
		
		private Net.ProgressListener streamListener;
		
		public Storage setListener (Net.ProgressListener listener) {
			
			streamListener = listener;
			return this;
			
		}
		
		public void copy (String remoteFile, File localFile) throws StorageException {
			
			try {
				
				Files.makeDir (Files.getPath (localFile));
				
				copy (remoteFile, new FileOutputStream (localFile));
				
			} catch (IOException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public void copy (String remoteFile, OutputStream outputStream) throws StorageException {
			
			try {
				Net.download (getInputStream (remoteFile), outputStream, streamListener, getSize (remoteFile));
			} catch (IOException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public final String read (String remoteFile) throws StorageException, OutOfMemoryException {
			
			try {
				return Strings.toString (getInputStream (remoteFile));
			} catch (IOException e) {
				throw new StorageException (this, e);
			}
			
		}
		
		public void delete (String remoteFile) throws StorageException {
			
			remoteFile = provider.prepRemoteFile (remoteFile);
			provider.delete (remoteFile);
			
		}
		
		public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException {
			
			remoteSrcFile = provider.prepRemoteFile (remoteSrcFile);
			remoteDestFile = provider.prepRemoteFile (remoteDestFile);
			
			provider.copy (remoteSrcFile, remoteDestFile);
			
		}
		
		public String getAuthUrl () throws StorageException {
			return provider.getAuthUrl ();
		}
		
		public String getRedirectUrl () throws StorageException {
			return provider.getRedirectUrl ();
		}
		
		public final WebView setWebView () throws StorageException {
			
			WebView webView = new WebView (activity);
			return setWebView (webView);
			
		}
		
		public WebView setWebView (WebView webView) throws StorageException {
			return provider.setWebView (webView);
		}
		
		public final Item toItem (String... item) {
			return new Item (this, Arrays.implode ("/", item));
		}
		
		public void close () throws StorageException {
			provider.close ();
		}
		
		public long getTotalSize () throws StorageException {
			return provider.getTotalSize ();
		}
		
		public String test (Provider provider, String file, String file2) throws StorageException, OutOfMemoryException {
			return test (provider, file, file2, true);
		}
		
		public String test (Provider provider, String file, String file2, boolean delete) throws StorageException, OutOfMemoryException {
			return test (provider, file, file2, delete, "");
		}
		
		public String test (Provider provider, String file, String file2, boolean delete, String output) throws StorageException, OutOfMemoryException {
			
			try {
				
				this.provider = provider;
				
				if (data.has (getName ())) {
					
					init (getName (), data);
					
					JSONObject item = setProviderItems (new JSONObject ());
					
					output += item.getString (ITEM_TITLE) + "\n";
					output += "\n";
					
					copy (new File (file));
					write (type, "123/456/ttt.txt");
					copy (file, new File (file2));
					
					output += "list:\n\n";
					output += Arrays.implode (list ("", 0)) + "\n\n";
					
					output += "read: " + read ("123/456/ttt.txt") + "\n\n";
					
					if (delete) {
						
						if (isExists (file2))
							delete (file2);
						//else
						//	throw new StorageException (type + ": File " + provider.prepRemoteFile (file2) + " not found");
						
						//if (isDir ("123"))
							delete ("123");
						//else
						//	throw new StorageException (type + ": Folder " + provider.prepRemoteFile ("123") + " not found");
						
					}
					
					provider.close ();
					
				}
				
			} catch (JSONException e) {
				throw new StorageException (e);
			}
			
			return output;
			
		}
		
		public Storage loadUrl (final TextView textView, View button) {
			
			if (Device.isConnected (context)) {
				
				if (Device.isOnline (context, wiFiOnly)) {
					
					try {
						
						activity.setContentView (pro.acuna.andromeda.Net.loadURL (setWebView (), getAuthUrl (), true, new pro.acuna.andromeda.Net.WebViewListener () {
							
							@Override
							public void onPageStarted (WebView view, String url, Bitmap favicon) {}
							
							@Override
							public void onPageFinished (WebView view, String url) {
								
								try {
									
									Map<String, String> urlData = Net.parseUrl (url);
									Map<String, String> redirectUrlData = Net.parseUrl (getRedirectUrl ());
									
									provider.processUrl (urlData, redirectUrlData);
									
								} catch (StorageException | MalformedURLException e) {
									textView.setText (e.getMessage ());
								}
								
							}
							
						}));
						
					} catch (StorageException e) {
						textView.setText (e.getMessage ());
					}
					
				} else noInternet (textView, button, R.string.alert_error_only_wifi);
				
			} else noInternet (textView, button, R.string.alert_error_nointernet);
			
			return this;
			
		}
		
		private void noInternet (final TextView textView, final View button, int title) {
			
			textView.setText (title);
			
			button.setVisibility (View.VISIBLE);
			
			button.setOnClickListener (new View.OnClickListener () {
				
				@Override
				public void onClick (View view) {
					loadUrl (textView, button);
				}
				
			});
			
		}
		
		public String setError (Object result) {
			
			if (result instanceof JSONObject)
				return provider.setError ((JSONObject) result);
			else
				return String.valueOf (result);
			
		}
		
	}