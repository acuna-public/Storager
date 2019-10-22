  package pro.acuna.storage;
  /*
   Created by Acuna on 10.05.2017
  */
  
  import android.app.Activity;
  import android.app.AlertDialog;
  import android.content.Context;
  import android.content.Intent;
  import android.graphics.Bitmap;
  import android.net.Uri;
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
  import pro.acuna.andromeda.Prefs;
  import pro.acuna.jabadaba.Arrays;
  import pro.acuna.jabadaba.Files;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.Streams;
  import pro.acuna.jabadaba.Strings;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.providers.Dropbox;
  import pro.acuna.storage.providers.GDrive;
  import pro.acuna.storage.providers.SDCard;
  import pro.acuna.storage.providers.SFTP;
  import pro.acuna.storage.providers.YandexDisk;
  
  public final class Storage {
    
    private static final String NAME = "storage";
    public static final String VERSION = BuildConfig.VERSION_NAME;
    
    public Activity activity;
    public Context context;
    
    private boolean refreshToken = true, auth = true;
    public JSONObject settings = new JSONObject ();
    private int accountId = -1, providerId = 0;
    private Prefs prefs;
    
    protected String type;
    
    public Provider provider;
    
    private List<Provider> providers = new ArrayList<> ();
    
    public boolean wiFiOnly = true;
    public JSONObject data = new JSONObject ();
    
    public static final String ITEM_TITLE = "title";
    public static final String ITEM_ICON = "icon";
    public static final String ITEM_LAYOUT = "layout";
    
    public static final String USER_NAME = "name";
    public static final String USER_FULLNAME = "fullname";
    public static final String USER_AVATAR = "avatar";
    
    private static final String PREF_ACCOUNTS = "accounts";
    private static final String PREF_PROVIDER_ID = "provider_id";
    
    protected Storage () {}
    
    public Storage (Context context) {
      this (context, -1);
    }
    
    public Storage (Context context, Bundle bundle) {
      this (context, bundle.getInt ("id"));
    }
    
    public Storage (Context context, int accountId) {
      this (context, accountId, NAME);
    }
    
    public Storage (Context context, int accountId, String prefsName) {
      
      this.context = context;
      this.accountId = accountId;
      
      if (context instanceof Activity)
        activity = (Activity) context;
      
      addProvider (new SDCard ());
      addProvider (new SFTP ());
      addProvider (new Dropbox ());
      addProvider (new GDrive ());
      addProvider (new YandexDisk ());
      
      prefs = new Prefs (context, prefsName);
      
    }
    
    public Storage setActivity (Activity activity) {
      
      this.activity = activity;
      return this;
      
    }
    
    public final String getName () {
      return provider.getName ();
    }
    
    public final boolean isAuthenticated () throws StorageException {
      return provider.isAuthenticated ();
    }
    
    public final JSONObject setProviderItems (JSONObject data) throws StorageException {
      return (Arrays.contains (type, items) ? items.get (type) : provider.setProviderItems (data));
    }
    
    private Map<String, JSONObject> items = new HashMap<> ();
    
    public final Storage setProviderItem (String type, JSONObject data) {
      
      items.put (type, data);
      return this;
      
    }
    
    public final Map<String, Object> getDefData () throws StorageException {
      
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
    
    public final List<Provider> getProviders () {
      return providers;
    }
    
    public final class ProviderItem {
      
      private String name, title, icon;
      
      public final String getTitle () {
        return title;
      }
      
      public final String getName () {
        return name;
      }
      
    }
    
    public final class ProvidersData {
      
      private List<ProviderItem> items = new ArrayList<> ();
      public final String[] names, titles;
      
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
      
      public final List<ProviderItem> getItems () {
        return items;
      }
      
    }
    
    public final ProvidersData getProvidersData () throws StorageException {
      return new ProvidersData ();
    }
    
    public final Storage addProvider (Provider provider) {
      
      providers.add (provider);
      return this;
      
    }
    
    public final Storage addProviders (List<Provider> list) {
      
      providers = new ArrayList<> ();
      
      for (Provider provider : list)
        addProvider (provider);
      
      return this;
      
    }
    
    private int getAccountId () {
      return prefs.get (type + "_account", accountId);
    }
    
    public final Storage setAccountId (int id) {
      
      prefs.set (type + "_account", id);
      this.accountId = id;
      
      return this;
      
    }
    
    private void setAccountId () throws StorageException {
      
      if (accountId == -1) accountId = getAccountId ();
      provider = provider.newInstance (this);
      
    }
    
    public final JSONObject defaultData () throws StorageException {
      
      try {
        
        setAccountId ();
        
        Map<String, Object> defData = getDefData ();
        
        JSONObject data = new JSONObject ();
        
        for (String key : defData.keySet ()) {
          
          Object obj = defData.get (key);
          
          if (obj instanceof Integer)
            data.put (key, prefs.get (prefKey (key), (int) obj));
          else
            data.put (key, prefs.get (prefKey (key), obj.toString ()));
          
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
    
    public final Storage init (JSONArray data) throws StorageException {
      
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
    
    public final Storage init (String type, Bundle bundle) throws StorageException {
      
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
    
    public final Storage init (JSONObject data) throws StorageException {
      
      try {
        
        JSONObject values = new JSONObject ();
        values.put (data.getString ("type"), data);
        
        return init (data.getString ("type"), values);
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final Storage init (String type) throws StorageException {
          
          /*items.put (TYPE_S3, new Object[] { "Amazon S3" });
          items.put (TYPE_ONEDRIVE, new Object[] { "OneDrive" });
          items.put (TYPE_MEGA, new Object[] { "Mega" });
          
          keys.put (TYPE_S3, new String[] { "key", "secret", "bucket", "region" });*/
      
      this.type = type;
      
      provider = getProvider (type);
      
      if (provider != null) {
        
        JSONObject data2 = data.optJSONObject (type);
        if (data2 == null) data2 = new JSONObject ();
        
        settings = Arrays.extend (data2, getDefData ());
        
        setAccountId ();
        
      } else throw new StorageException ("Provider is null");
      
      return this;
      
    }
    
    public final Storage makeAuth (boolean auth) {
      
      this.auth = auth;
      return this;
      
    }
    
    public final Storage init (String type, JSONObject data) throws StorageException { // Главный инициализатор
      
      try {
        
        this.data = data;
        
        init (type);
        
        if (auth) provider = provider.auth ();
        
        if (refreshToken && Int.size (provider.authData) > 0) { // Обновляем токен
          
          JSONArray keys = provider.authData.names ();
          
          for (int i = 0; i < Int.size (keys); ++i) {
            
            String key = keys.getString (i);
            putPref (key, provider.authData.get (key));
            
          }
          
          applyPrefs ();
          
          refreshToken = false;
          
        }
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
      return this;
      
    }
    
    void applyPrefs () {
      prefs.apply ();
    }
    
    public final String getPref (String key) {
      return prefs.get (prefKey (key), "");
    }
    
    public final String getPref (int id, String key) {
      return prefs.get (prefKey (id, key), "");
    }
    
    private Object getPref (String key, Map<String, Object> data) {
      
      Object value = data.get (key);
      
      if (value instanceof Integer)
        return prefs.get (getPref (key), (int) value);
      else
        return prefs.get (getPref (key), value.toString ());
      
    }
    
    public final boolean hasPref (String key) {
      return (!getPref (key).equals (""));
    }
    
    public final String toString () {
      return getName ();
    }
    
    public final Storage setWifiOnly (boolean wiFiOnly) {
      
      this.wiFiOnly = wiFiOnly;
      return this;
      
    }
    
    public StorageListener listener;
    
    public final Storage setListener (StorageListener listener) {
      
      this.listener = listener;
      return this;
      
    }
    
    public static final String CLOSE_IF_SUCCESS = "close_if_success";
    
    public final void startAuthActivityAndFinish () throws StorageException {
      startAuthActivity ();
    }
    
    private Intent intent;
    
    public final Storage setAuthIntent (Intent authIntent) {
      
      this.intent = authIntent;
      return this;
      
    }
    
    public final void startAuthActivity () throws StorageException {
      
      if (intent == null) intent = new Intent (activity, StorageActivity.class);
      startAuthActivity (intent);
      
    }
    
    public final void startAuthActivity (Intent intent) throws StorageException {
      
      try {
        
        intent.putExtra ("type", type);
        intent.putExtra ("id", accountId);
        intent.putExtra ("wifi_only", wiFiOnly);
        
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
    
    public final Storage onStartAuthActivity () throws StorageException {
      return provider.onStartAuthActivity ();
    }
    
    public final Storage onAuthActivityBack (int keyCode) throws StorageException {
      return onAuthActivityBack (new Intent (), keyCode);
    }
    
    public final Storage onAuthActivityBack (Intent intent, int keyCode) throws StorageException {
      
      if (keyCode == KeyEvent.KEYCODE_BACK)
        provider.onAuthActivityBack (intent);
      
      return this;
      
    }
    
    public final void processUrl () throws StorageException {
      provider.processUrl ();
    }
    
    public final void setPrefs (JSONObject userData, Bundle bundle) throws StorageException {
      
      try {
        setPrefs (userData, pro.acuna.andromeda.Arrays.toJSONObject (bundle));
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final void setPrefs (JSONObject userData, JSONObject data) throws StorageException {
      
      JSONArray keys = data.names ();
      
      try {
        
        addUser (userData);
        
        for (int i = 0; i < Int.size (keys); ++i) {
          
          String key = keys.getString (i);
          putPref (key, data.get (key));
          
        }
        
        applyPrefs ();
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
      if (listener != null) listener.onAuthSuccess ();
      
    }
    
    private String prefKey (int id, String key) {
      return type + "_" + id + "_" + key;
    }
    
    private String prefKey (String key) {
      return prefKey (accountId, key);
    }
    
    void putPref (String key, Object value) {
      prefs.put (prefKey (key), value);
    }
    
    public final void onActivityResult (int requestCode, int resultCode, Intent intent) throws StorageException {
      
      if (intent != null && requestCode == 200) {
        
        Bundle bundle = intent.getExtras ();
        
        if (resultCode == Activity.RESULT_OK) {
          
          try {
            
            JSONObject userData = provider.getUser (bundle);
            
            if (Int.size (userData) > 0) {
              
              addUser (userData);
              
              if (bundle != null)
                for (String key : bundle.keySet ())
                  putPref (key, bundle.get (key));
              
              if (listener != null) listener.onAuthSuccess ();
              
            }
            
            prefs.put (PREF_PROVIDER_ID, findProviderId ());
            
            applyPrefs ();
            
          } catch (JSONException e) {
            throw new StorageException (this, e);
          }
          
        } else if (bundle != null && resultCode != Activity.RESULT_CANCELED)
          throw new StorageException (bundle.getString ("error") + (bundle.getString ("error_description") != null ? ": " + bundle.getString ("error_description") : "")); // TODO
        
      }
      
    }
    
    private final void addUser (JSONObject userData) throws JSONException, StorageException {
      
      JSONObject accounts = getAccounts ();
      JSONArray users = new JSONArray (), getUsers = new JSONArray ();
      
      if (accounts.has (type)) {
        
        getUsers = accounts.getJSONArray (type);
        
        for (int i = 0; i < Int.size (getUsers); ++i)
          users.put (getUsers.getJSONObject (i).getString (USER_NAME));
        
      }
      
      if (userData != null) {
        
        if (!Arrays.contains (userData.getString (USER_NAME), users)) {
  
          accountId = Int.size (getUsers);
          
          getUsers.put (accountId, userData);
          
          accounts.put (type, getUsers);
          
          prefs.put (PREF_ACCOUNTS, accounts.toString ());
          setAccountId (accountId);
          
        }
        
      }
      
    }
    
    public final JSONObject getAccounts () throws StorageException {
      
      try {
        return prefs.get (PREF_ACCOUNTS, new JSONObject ());
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final JSONArray getAccounts (String type) throws StorageException {
      
      try {
        
        JSONObject accounts = getAccounts ();
        return (accounts.has (type) ? accounts.getJSONArray (type) : new JSONArray ());
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final JSONObject getAccount () throws StorageException {
      
      try {
        
        JSONArray accounts = getAccounts (type);
        return (Int.size (accounts) > 0 ? accounts.getJSONObject (getAccountId ()) : new JSONObject ());
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final int getAccountId (String user) throws StorageException {
      
      try {
        
        JSONArray account = getAccounts (type);
        
        for (int i = 0; i < Int.size (account); ++i) {
          
          if (account.getJSONObject (i).getString (USER_NAME).equals (user))
            return i;
          
        }
        
        return -1;
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final JSONObject getProviderItems () throws StorageException {
      return provider.getProviderItems ();
    }
    
    public final void chmod (Item item, int chmod) throws StorageException {
      provider.chmod (item, chmod);
    }
    
    public final View onDialogView (AlertDialog.Builder builder, View view, JSONObject item) throws StorageException {
      
      try {
        
        JSONArray keys = item.names ();
        
        for (int i = 0; i < Int.size (keys); ++i) {
          
          String key = keys.getString (i);
          
          EditText textView = view.findViewById (item.getInt (key));
          textView.setText (getPref (key, getDefData ()).toString ());
          
        }
        
        return view;
        
      } catch (JSONException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final URL getDirectLink (String remoteFile) throws StorageException, OutOfMemoryException {
      return getItem (remoteFile).getDirectLink ();
    }
    
    public final InputStream getStream (String remoteFile) throws StorageException, OutOfMemoryException {
      return getStream (remoteFile, "");
    }
    
    public final InputStream getStream (String remoteFile, URL url) throws StorageException, OutOfMemoryException {
      return getStream (remoteFile, Files.getExtension (url.toString ()));
    }
    
    public final InputStream getStream (String remoteFile, String type) throws StorageException, OutOfMemoryException {
      return getItem (remoteFile).getStream (type);
    }
    
    public final List<Item> list () throws StorageException, OutOfMemoryException {
      return list ("");
    }
    
    public final List<Item> list (String remoteDir) throws StorageException, OutOfMemoryException {
      return list (getItem (remoteDir));
    }
    
    public final List<Item> list (Item item) throws StorageException, OutOfMemoryException {
      return list (item, 0);
    }
    
    public final List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
      return list (getItem (remoteDir), mode);
    }
    
    public final List<Item> list (Item item, int mode) throws StorageException, OutOfMemoryException {
      return provider.list (item, mode);
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
    
    public final long getSize (String remoteFile) throws StorageException {
      return getItem (remoteFile).getSize ();
    }
    
    public final boolean isDir (String remoteFile) throws StorageException {
      return getItem (remoteFile).isDir ();
    }
    
    public final boolean isExists (String remoteFile) throws StorageException {
      return getItem (remoteFile).isExists ();
    }
    
    public final int makeDir (String remoteFile) throws StorageException {
      return makeDir (getItem (remoteFile));
    }
    
    public final int makeDir (Item item) throws StorageException {
      return provider.makeDir (item);
    }
    
    public final void check () throws StorageException, OutOfMemoryException {
      put (getName (), "." + NAME, true, false);
    }
    
    public final Item put (List<?> items, Item item) throws StorageException, OutOfMemoryException {
      return put (items, item.getShortFile ());
    }
    
    public final Item put (List<?> items, String remoteFile) throws StorageException, OutOfMemoryException {
      return put (Arrays.implode (items), remoteFile);
    }
    
    public final Item put (Object text, Item item) throws StorageException, OutOfMemoryException {
      return put (text, item.getShortFile ());
    }
    
    public final Item put (Object text, String remoteFile) throws StorageException, OutOfMemoryException {
      return put (text, remoteFile, true);
    }
    
    public final Item put (Object text, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return put (text, remoteFile, force, true);
    }
    
    public final Item put (Object text, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      return put (Streams.toInputStream (text), remoteFile, force, makeDir, data);
    }
    
    public final Item copy (File localFile) throws StorageException, OutOfMemoryException {
      return copy (localFile, localFile.getAbsolutePath ());
    }
    
    public final Item copy (File localFile, String remoteFile) throws StorageException, OutOfMemoryException {
      return copy (localFile, remoteFile, true);
    }
    
    public final Item copy (File localFile, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return copy (getItem (localFile.getAbsolutePath ()), remoteFile, force);
    }
    
    private Item copy (Item item, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return put (item, remoteFile, force, true);
    }
    
    public final Item copy (URL url, Item item) throws StorageException, OutOfMemoryException {
      return copy (url, item.getShortFile ());
    }
    
    public final Item copy (URL url, String remoteFile) throws StorageException, OutOfMemoryException {
      return copy (url, remoteFile, true);
    }
    
    public final Item copy (URL url, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return copy (url, remoteFile, force, true);
    }
    
    private class StreamItem extends Item {
      
      private StreamItem (Storage storage, String... item) {
        super (storage, item);
      }
      
      @Override
      public boolean isExists () throws StorageException {
        return true;
      }
      
      @Override
      public boolean isDir () throws StorageException {
        return false;
      }
      
      @Override
      public URL getDirectLink () throws StorageException, OutOfMemoryException {
        return directUrl;
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        return new JSONObject ();
      }
      
    }
    
    public final Item copy (URL url, String remoteFile, boolean force, boolean makeDir) throws StorageException, OutOfMemoryException {
      return put (new StreamItem (this, remoteFile).setDirectLink (url), remoteFile, force, makeDir);
    }
    
    public final Item put (InputStream stream) throws StorageException, OutOfMemoryException {
      return put (stream, "");
    }
    
    public final Item put (InputStream stream, String remoteFile) throws StorageException, OutOfMemoryException {
      return put (stream, remoteFile, true);
    }
    
    public final Item put (InputStream stream, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return put (stream, remoteFile, force, true);
    }
    
    public final Item put (InputStream stream, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      return put (getItem ().setStream (stream), remoteFile, force, makeDir, data);
    }
    
    public final Item put (Item item) throws StorageException, OutOfMemoryException {
      return put (item, "");
    }
    
    public final Item put (Item item, String remoteFile) throws StorageException, OutOfMemoryException {
      return put (item, remoteFile, true);
    }
    
    public final Item put (Item item, String remoteFile, boolean force) throws StorageException, OutOfMemoryException {
      return put (item, remoteFile, force, true);
    }
    
    public final Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      return provider.put (item, remoteFile, force, makeDir, data);
    }
    
    private Net.ProgressListener streamListener;
    
    public final Storage setListener (Net.ProgressListener listener) {
      
      streamListener = listener;
      return this;
      
    }
    
    public final void copy (String remoteFile, File localFile) throws StorageException, OutOfMemoryException {
      
      try {
        
        Files.makeDir (Files.getPath (localFile));
        copy (remoteFile, new FileOutputStream (localFile));
        
      } catch (IOException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final void copy (String remoteFile, OutputStream outputStream) throws StorageException, OutOfMemoryException {
      
      try {
        Net.download (getStream (remoteFile), outputStream, streamListener, getSize (remoteFile));
      } catch (IOException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final String read (String remoteFile) throws StorageException, OutOfMemoryException {
      
      try {
        return Strings.toString (getStream (remoteFile));
      } catch (IOException e) {
        throw new StorageException (this, e);
      }
      
    }
    
    public final void delete (String remoteFile) throws StorageException {
      delete (getItem (remoteFile));
    }
    
    public final void delete (Item item) throws StorageException {
      provider.delete (item);
    }
    
    public final void copy (String remoteSrcFile, String remoteDestFile) throws StorageException, OutOfMemoryException {
      
      List<Item> from = new ArrayList<> ();
      
      from.add (getItem (remoteSrcFile));
      
      List<Item> to = new ArrayList<> ();
      
      to.add (getItem (remoteDestFile));
      
      copy (from, to);
      
    }
    
    public final void copy (Item remoteSrcFile, String remoteDestFile) throws StorageException, OutOfMemoryException {
      copy (remoteSrcFile, getItem (remoteDestFile));
    }
    
    public final void copy (Item remoteSrcFile, Item remoteDestFile) throws StorageException, OutOfMemoryException {
      provider.copy (remoteSrcFile, remoteDestFile);
    }
    
    public final void copy (List<?> from, List<?> to) throws StorageException, OutOfMemoryException {
      
      List<Item> from2 = new ArrayList<> ();
      List<Item> to2 = new ArrayList<> ();
      
      for (int i = 0; i < Int.size (from); ++i) {
        
        Object obj = from.get (i);
        
        if (obj instanceof Item) {
          
          from2.add ((Item) obj);
          to2.add ((Item) to.get (i));
          
        } else {
          
          from2.add (getItem (obj.toString ()));
          to2.add (getItem (to.get (i).toString ()));
          
        }
        
      }
      
      provider.copy (from2, to2);
      
    }
    
    public final void move (String remoteSrcFile, String remoteDestFile) throws StorageException, OutOfMemoryException {
      
      List<Item> from = new ArrayList<> ();
      
      from.add (getItem (remoteSrcFile));
      
      List<Item> to = new ArrayList<> ();
      
      to.add (getItem (remoteDestFile));
      
      move (from, to);
      
    }
    
    public final void move (Item remoteSrcFile, String remoteDestFile) throws StorageException, OutOfMemoryException {
      move (remoteSrcFile, getItem (remoteDestFile));
    }
    
    public final void move (Item remoteSrcFile, Item remoteDestFile) throws StorageException, OutOfMemoryException {
      provider.move (remoteSrcFile, remoteDestFile);
    }
    
    public final void move (List<?> from, List<?> to) throws StorageException, OutOfMemoryException {
      
      List<Item> from2 = new ArrayList<> ();
      List<Item> to2 = new ArrayList<> ();
      
      for (int i = 0; i < Int.size (from); ++i) {
        
        Object obj = from.get (i);
        
        if (obj instanceof Item) {
          
          from2.add ((Item) obj);
          to2.add ((Item) to.get (i));
          
        } else {
          
          from2.add (getItem (obj.toString ()));
          to2.add (getItem (to.get (i).toString ()));
          
        }
        
      }
      
      provider.move (from2, to2);
      
    }
    
    public final String getAuthUrl () throws StorageException {
      return provider.getAuthUrl ();
    }
    
    public final String getRedirectUrl () throws StorageException {
      return provider.getRedirectUrl ();
    }
    
    public final WebView setWebView () throws StorageException {
      return setWebView (new WebView (activity));
    }
    
    public final WebView setWebView (WebView webView) throws StorageException {
      return provider.setWebView (webView);
    }
    
    public final Item getItem (String... file) {
      return provider.getItem (file);
    }
    
    public final void close () throws StorageException {
      provider.close ();
    }
    
    public final long getTotalSize () throws StorageException { // TODO
      return provider.getTotalSize ();
    }
    
    public final String test (Provider provider, String file, String file2) throws StorageException, OutOfMemoryException {
      return test (provider, file, file2, true);
    }
    
    public final String test (Provider provider, String file, String file2, boolean delete) throws StorageException, OutOfMemoryException {
      return test (provider, file, file2, delete, "");
    }
    
    public final Map<String, Object> getCookies () {
      return provider.getCookies ();
    }
    
    public final String test (Provider provider, String file, String file2, boolean delete, String output) throws StorageException, OutOfMemoryException {
      
      try {
        
        this.provider = provider;
        
        if (data.has (getName ())) {
          
          init (getName (), data);
          
          JSONObject item = setProviderItems (new JSONObject ());
          
          output += item.getString (ITEM_TITLE) + "\n";
          output += "\n";
          
          copy (new File (file));
          put (type, "123/456/ttt.txt");
          copy (file, new File (file2));
          
          output += "list:\n\n";
          output += Arrays.implode (list ("", 0)) + "\n\n";
          
          output += "read: " + read ("123/456/ttt.txt") + "\n\n";
          
          if (delete) {
            
            if (isExists (file2))
              delete (file2);
            //else
            //  throw new StorageException (type + ": File " + prepRemoteFile (file2) + " not found");
            
            //if (isDir ("123"))
            delete ("123");
            //else
            //  throw new StorageException (type + ": Folder " + prepRemoteFile ("123") + " not found");
            
          }
          
          provider.close ();
          
        }
        
      } catch (JSONException e) {
        throw new StorageException (e);
      }
      
      return output;
      
    }
    
    public final Storage loadUrl (final TextView textView, View button) {
      
      if (Device.isConnected (context)) {
        
        if (Device.isOnline (context, wiFiOnly)) {
          
          try {
            
            activity.setContentView (pro.acuna.andromeda.Net.loadURL (setWebView (), getAuthUrl (), true, new pro.acuna.andromeda.Net.WebViewListener () {
              
              @Override
              public final void onPageStarted (WebView view, Uri uri, Bitmap favicon) {}
              
              @Override
              public final boolean onPageLoading (WebView view, Uri uri) {
                return true;
              }
              
              @Override
              public final void onPageFinished (WebView view, Uri uri) {
                
                try {
                  
                  Map<String, String> urlData = Net.parseUrl (uri.toString ());
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
        public final void onClick (View view) {
          loadUrl (textView, button);
        }
        
      });
      
    }
    
    public final String setError (Object result) {
      
      try {
        
        if (result instanceof JSONObject)
          return provider.setError ((JSONObject) result);
        else
          return String.valueOf (result);
        
      } catch (JSONException e) {
        return e.toString ();
      }
      
    }
    
    public Item setDir (String... path) {
      return getItem (path).isDir (true);
    }
    
    public final String getUseragent () throws StorageException {
      
      try {
        return settings.getString ("useragent");
      } catch (JSONException e) {
        throw new StorageException (e);
      }
      
    }
    
    private int findProviderId () {
      
      for (int i = 0; i < Int.size (providers); ++i)
        if (providers.get (i).getName ().equals (type))
          return i;
      
      return providerId;
      
    }
    
    public final void setProviderId (int id) {
      prefs.set (PREF_PROVIDER_ID, id);
    }
    
    public final int getProviderId () {
      return prefs.get (PREF_PROVIDER_ID, providerId);
    }
    
  }