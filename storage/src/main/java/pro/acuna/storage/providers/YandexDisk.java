  package pro.acuna.storage.providers;
  
  import android.os.Bundle;
  
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;

  import java.io.UnsupportedEncodingException;
  import java.net.MalformedURLException;
  import java.net.URL;
  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;
  
  import pro.acuna.jabadaba.Arrays;
  import pro.acuna.jabadaba.HttpRequest;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.exceptions.HttpRequestException;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.Item;
  import pro.acuna.storage.Provider;
  import pro.acuna.storage.Storage;
  import pro.acuna.storage.StorageException;
  
  public class YandexDisk extends Provider {
    
    private String API_URL = "https://cloud-api.yandex.net/v1/disk";
    
    public YandexDisk () {}
    
    private YandexDisk (Storage storage) throws StorageException {
      
      super (storage);
      setUseragent ("api-explorer-client");
      
    }
    
    @Override
    public Provider newInstance (Storage storage) throws StorageException {
      return new YandexDisk (storage);
    }
    
    @Override
    public Provider auth () throws StorageException {
      
      try {
        
        String refToken = storage.getPref ("refresh_token");
        
        if (refToken != null && !refToken.equals ("")) {
          
          HttpRequest request = new HttpRequest (HttpRequest.METHOD_POST, "https://oauth.yandex.ru/token");
          
          Map<String, Object> data = new LinkedHashMap<> ();
          
          data.put ("client_id", storage.settings.getString ("client_id"));
          data.put ("client_secret", storage.settings.getString ("client_secret"));
          data.put ("grant_type", "refresh_token");
          data.put ("refresh_token", refToken);
          
          request.send (data);
          
          authData = new JSONObject (request.getContent ());
          
        }
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
      return this;
      
    }
    
    @Override
    public String getName () {
      return "yadisk";
    }
    
    @Override
    public JSONObject getUser (Bundle bundle) throws StorageException {
      
      try {
        
        Map<String, Object> data = new LinkedHashMap<> ();
        
        data.put ("format", "json");
        data.put ("oauth_token", bundle.getString ("access_token"));
        
        HttpRequest request = new HttpRequest (HttpRequest.METHOD_GET, "https://login.yandex.ru/info", data);
        
        String content = request.getContent ();
        
        if (!content.equals ("")) {
          
          JSONObject result = new JSONObject (content);
          
          if (result.has ("login")) {
            
            JSONObject userData = new JSONObject ();
            
            userData.put (Storage.USER_NAME, result.getString ("login"));
            
            return userData;
            
          } else throw new StorageException (storage, result.toString ());
          
        } else throw new StorageException (storage, content);
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public JSONObject setProviderItems (JSONObject data) throws StorageException {
      
      try {
        
        data.put (Storage.ITEM_TITLE, "Яндекс.Диск");
        
        return data;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String getAuthUrl () throws StorageException {
      
      try {
        
        Map<String, Object> data = new LinkedHashMap<> ();
        
        data.put ("client_id", storage.settings.getString ("client_id"));
        data.put ("response_type", "token");
        data.put ("force_confirm", true);
        
        return "https://oauth.yandex.ru/authorize" + Net.urlQueryEncode (data);
        
      } catch (JSONException | UnsupportedEncodingException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String getRedirectUrl () throws StorageException {
      
      try {
        return storage.settings.getString ("redirect_url");
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String getRootDir () throws StorageException {
      return "app:";
    }
    
    private class FileItem extends Item {
      
      private FileItem (Storage storage, String... remoteFile) {
        super (storage, remoteFile);
      }
      
      @Override
      public boolean isExists () throws StorageException {
        return (Int.size (getInfo ()) > 0);
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        
        try {
          
          if (getShortFile ().equals (getRootDir ())) setFile (getFile () + "/");
          
          Map<String, Object> params = new LinkedHashMap<> ();
          
          params.put ("path", getFile ());
          
          HttpRequest request = request (HttpRequest.METHOD_GET, API_URL + "/resources", params);
          
          JSONObject output = new JSONObject (request.getContent ());
          
          if (output.has ("resource_id") || output.has ("public_id"))
            return output;
          else
            return new JSONObject ();
          
        } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public long getSize () throws StorageException {
        
        try {
          
          long size = 0;
          JSONObject data = getInfo ();
          
          if (data.has ("_embedded")) {
            
            JSONArray items = data.getJSONArray ("_embedded");
            
            for (int i = 0; i < Int.size (items); ++i) {
              
              JSONObject item = items.getJSONObject (i);
              size += item.getLong ("size");
              
            }
            
          } else size = data.getLong ("size");
          
          return size;
          
        } catch (JSONException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public boolean isDir () throws StorageException {
        
        try {
          
          if (isDir == null) {
            
            JSONObject data = getInfo ();
            isDir = (Int.size (data) > 0 && data.getString ("type").equals ("dir"));
            
          }
          
          return isDir;
          
        } catch (JSONException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public boolean isImage () throws StorageException {
        return false;
      }
      
      @Override
      public URL getDirectLink () throws StorageException, OutOfMemoryException {
        
        try {
          
          if (directUrl == null) {
            
            Map<String, Object> params = new LinkedHashMap<> ();
            
            params.put ("path", getFile ());
  
            directUrl = new URL (_getDirectLink ("resources/download", params));
            
          }
          
          return directUrl;
          
        } catch (MalformedURLException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
    }
    
    @Override
    public Item getItem (String... remoteFile) {
      return new FileItem (storage, remoteFile);
    }
    
    @Override
    public List<Item> list (Item item, int mode) throws StorageException, OutOfMemoryException {
      
      List<Item> files = new ArrayList<> ();
      
      try {
        
        JSONArray items = item.getInfo ().getJSONObject ("_embedded").getJSONArray ("items");
        
        for (int i = 0; i < Int.size (items); ++i) {
          
          JSONObject file = items.getJSONObject (i);
          
          item = storage.getItem (item.toString (), file.getString ("name"))
                        .isDir (file.getString ("type").equals ("dir"));
					
					if (!item.isDir ()) item.setDirectLink (new URL (file.getString ("href")));;
          
          if (item.show (mode)) files.add (item);
          
        }
        
      } catch (MalformedURLException | JSONException e) {
        throw new StorageException (storage, e, item);
      }
      
      return files;
      
    }
    
    @Override
    public int makeDir (Item item) throws StorageException {
      
      try {
        
        if (!item.isExists ()) {
          
          List<String> parts = Arrays.explode ("/", item.getShortFile ());
          
          String path = "";
          
          for (int i = 0; i < (Int.size (parts) - 1); ++i) {
            
            path += parts.get (i);
            if (i < Int.size (parts) - 1) path += "/";
            
            item = getItem (path);
            
            if (!item.isDir ()) {
              
              Map<String, Object> params = new LinkedHashMap<> ();
              
              params.put ("path", item.getShortFile ());
              
              HttpRequest request = request (HttpRequest.METHOD_PUT, API_URL + "/resources", params);
              
              if (request.getCode () != 201)
                throw new StorageException (storage, setError (request.getContent ()));
              
            }
            
          }
          
          return 1;
          
        } else return 2;
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private String setError (String result) throws JSONException {
      return setError (new JSONObject (result));
    }
    
    @Override
    public String setError (JSONObject result) throws JSONException {
      return result.getString ("description");
    }
    
    @Override
    public Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      
      try {
        
        Item remoteItem = getItem (remoteFile);
        
        if (force (remoteItem, force)) {
          
          if (makeDir) makeDir (remoteItem);
          
          Map<String, Object> params = new LinkedHashMap<> ();
          
          params.put ("path", remoteItem);
          params.put ("overwrite", force);
          
          String link = _getDirectLink ("resources/upload", params);
          HttpRequest request = request (HttpRequest.METHOD_PUT, link, new LinkedHashMap<String, Object> ());
          
          request.send (item.getStream ());
          
          if (request.getCode () != 201)
            throw new StorageException (storage, request.getMessageCode ());
          
          //yadiskApi.publish (remoteFile);
          
        }
        
        return remoteItem;
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void delete (Item item) throws StorageException {
      
      try {
        
        Map<String, Object> params = new LinkedHashMap<> ();
        
        params.put ("path", item);
        params.put ("permanently", delPermanent);
        
        HttpRequest request = request (HttpRequest.METHOD_DELETE, API_URL + "/resources", params);
        
        if (request.getCode () != 202 && request.getCode () != 204)
          throw new StorageException (storage, request.getCode () + " " + request.getMessage ());
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void copy (List<Item> from, List<Item> to) throws StorageException {
      // TODO
    }
    
    @Override
    public void move (List<Item> from, List<Item> to) throws StorageException {
      // TODO
    }
    
    @Override
    public boolean isAuthenticated () throws StorageException {
      return storage.hasPref ("access_token");
    }
    
    private HttpRequest request (String method, String url, Map<String, Object> params) throws StorageException {
      
      try {
        
        HttpRequest request = initRequest (new HttpRequest (method, url, params));
        
        request.setHeader ("Authorization", "OAuth " + storage.getPref ("access_token"));
        request.setUserAgent (storage.getUseragent ());
        
        return request;
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private String _getDirectLink (String url, Map<String, Object> params) throws StorageException, OutOfMemoryException {
      
      try {
        
        HttpRequest request = request (HttpRequest.METHOD_GET, API_URL + "/" + url, params);
        
        JSONObject result = new JSONObject (request.getContent ());
        
        if (request.isOK ())
          return result.getString ("href");
        else
          throw new StorageException (storage, setError (result));
        
      } catch (HttpRequestException | JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
  }