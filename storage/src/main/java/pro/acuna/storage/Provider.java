  package pro.acuna.storage;
  /*
   Created by Acuna on 28.04.2018
  */
  
  import android.app.Activity;
  import android.content.Intent;
  import android.os.Bundle;
  import android.webkit.WebView;
  
  import org.json.JSONException;
  import org.json.JSONObject;

  import java.util.ArrayList;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  
  import pro.acuna.andromeda.AppsService;
  import pro.acuna.andromeda.OS;
  import pro.acuna.jabadaba.HttpRequest;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.Strings;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  
  public abstract class Provider {
    
    protected static final String KEY_COOKIES = "cookies";
    
    public Storage storage;
    protected JSONObject authData = new JSONObject ();
    protected boolean delPermanent = true;
    
    public Provider () {}
    
    protected Provider (Storage storage) {
      this.storage = storage;
    }
    
    public abstract Provider newInstance (Storage storage) throws StorageException;
    public abstract Item getItem (String... item);
    
    public abstract String getName ();
    public abstract JSONObject setProviderItems (JSONObject data) throws StorageException;
    public abstract JSONObject getUser (Bundle bundle) throws StorageException;
    public abstract boolean isAuthenticated () throws StorageException;
    
    public abstract int makeDir (Item item) throws StorageException;
    public abstract List<Item> list (Item item, int mode) throws StorageException, OutOfMemoryException;
    public abstract Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException;
    public abstract void delete (Item item) throws StorageException;
    public abstract void copy (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException;
    public abstract void move (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException;
    
    final String prepRemoteFile (String str) throws StorageException {
      
      str = Strings.trim ("/", str);
      
      if (!getRootDir ().equals ("") && !getRootDir ().equals ("/") && !str.equals ("")) {
  
        if (!getRootDir ().equals ("/" + str))
          str = getRootDir () + "/" + str;
  
      } else str = getRootDir () + str;
      
      return str;
      
    }
    
    protected final boolean force (Item item, boolean force) throws StorageException {
      return ((!force && !item.isExists ()) || force);
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
    
    public void chmod (Item item, int chmod) throws StorageException {}
    
    public void close () throws StorageException {}
    
    public String getRootDir () throws StorageException {
      return "";
    }
    
    final void processUrl () throws StorageException {
      processUrl (new HashMap<String, String> (), new HashMap<String, String> ());
    }
    
    public void processUrl (Map<String, String> urlData, Map<String, String> redirectUrlData) throws StorageException {
      
      try {
        
        if (urlData.get (Net.URL_DOMAIN).equals (redirectUrlData.get (Net.URL_DOMAIN))) {
          
          Map<String, Object> data = Net.urlQueryDecode (urlData.get (Net.URL_ANCHOR));
          
          storage.activity.setResult (Activity.RESULT_OK, OS.toIntent (data));
          storage.activity.finish ();
          
        }
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    public Storage onStartAuthActivity () throws StorageException {
      return storage;
    }
    
    public void onAuthActivityBack (Intent intent) throws StorageException {
      
      storage.activity.setResult (AppsService.STATUS_ACTIVITY_FINISH, intent);
      storage.activity.finish ();
      
    }
    
    public long getTotalSize () throws StorageException {
      return 0;
    }
    
    public String setError (JSONObject result) throws JSONException {
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
    
    protected final void setCookies (String cookies) {
      
      storage.putPref (KEY_COOKIES, cookies);
      storage.applyPrefs ();
      
    }
    
    protected final Map<String, Object> getCookies () {
      return Net.explodeCookies (storage.getPref (KEY_COOKIES));
    }
    
    protected final Provider setUseragent (String useragent) throws StorageException {
      
      try {
        
        storage.settings.put ("useragent", useragent);
        return this;
        
      } catch (JSONException e) {
        throw new StorageException (e);
      }
      
    }
    
    public Pagination pagination = new Pagination ();
    
    public static class Pagination {
      
      public int perPage () {
        return 30;
      }
      
      private String nextId;
      
      public final Pagination setNextId (String nextId) {
        
        this.nextId = nextId;
        return this;
        
      }
      
      public final String getNextId () {
        return nextId;
      }
      
      private String userId;
      
      public final Pagination setUserId (String id) {
        
        this.userId = id;
        return this;
        
      }
      
      public final String getUserId () {
        return userId;
      }
      
      private JSONObject data = new JSONObject ();
      
      public final Pagination setOutputData (JSONObject data) {
        
        this.data = data;
        return this;
        
      }
      
      public final JSONObject getOutputData () {
        return data;
      }
      
    }
    
    public void copy (Item remoteSrcFile, Item remoteDestFile) throws StorageException, OutOfMemoryException {
      
      List<Item> from = new ArrayList<> ();
      
      from.add (remoteSrcFile);
      
      List<Item> to = new ArrayList<> ();
      
      to.add (remoteDestFile);
      
      copy (from, to);
      
    }
    
    public void move (Item remoteSrcFile, Item remoteDestFile) throws StorageException, OutOfMemoryException {
      
      List<Item> from = new ArrayList<> ();
      
      from.add (remoteSrcFile);
      
      List<Item> to = new ArrayList<> ();
      
      to.add (remoteDestFile);
      
      move (from, to);
      
    }
    
  }