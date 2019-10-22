  package pro.acuna.storage.providers;
  
  import android.os.Bundle;
  
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  
  import java.io.IOException;
  import java.io.InputStream;
  import java.io.UnsupportedEncodingException;
  import java.net.MalformedURLException;
  import java.net.URL;
  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;

  import pro.acuna.jabadaba.HttpRequest;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.Strings;
  import pro.acuna.jabadaba.exceptions.HttpRequestException;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.Item;
  import pro.acuna.storage.Provider;
  import pro.acuna.storage.Storage;
  import pro.acuna.storage.StorageException;
  
  public class Dropbox extends Provider {
    
    private static final String API_URL = "https://api.dropboxapi.com/2";
    
    public Dropbox () {}
    
    private Dropbox (Storage storager) throws StorageException {
      
      super (storager);
      setUseragent ("api-explorer-client");
      
    }
    
    @Override
    public Provider newInstance (Storage storager) throws StorageException {
      return new Dropbox (storager);
    }
    
    @Override
    public String getName () {
      return "dropbox";
    }
    
    private String token;
    
    @Override
    public JSONObject getUser (Bundle bundle) throws StorageException {
      
      try {
        
        token = bundle.getString ("access_token");
        HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/users/get_current_account");
        
        request.send ((JSONObject) null);
        
        JSONObject output = new JSONObject (request.getContent ());
        
        if (!output.has ("error_summary")) {
          
          JSONObject user = output.getJSONObject ("name");
          
          JSONObject userData = new JSONObject ();
          
          userData.put (Storage.USER_NAME, output.getString ("email"));
          userData.put (Storage.USER_FULLNAME, user.getString ("display_name"));
          userData.put (Storage.USER_AVATAR, output.getString ("profile_photo_url"));
          
          return userData;
          
        } else throw new StorageException (storage, setError (output));
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private HttpRequest request (String method, String url) throws StorageException {
      return request (method, url, new LinkedHashMap<String, Object> ());
    }
    
    private HttpRequest request (String method, String url, Map<String, Object> params) throws StorageException {
      
      try {
        
        HttpRequest request = initRequest (new HttpRequest (method, url, params));
        
        if (token == null) token = storage.getPref ("access_token");
        
        request.setUserAgent (storage.getUseragent ());
        request.setHeader ("Authorization", "Bearer " + token);
        request.isJSON (true);
        
        return request;
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public boolean isAuthenticated () throws StorageException {
      return storage.hasPref ("access_token");
    }
    
    @Override
    public JSONObject setProviderItems (JSONObject data) throws StorageException {
      
      try {
        
        data.put (Storage.ITEM_TITLE, "Dropbox");
        
        return data;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String getAuthUrl () throws StorageException {
      
      try {
        
        Map<String, Object> data = new LinkedHashMap<> ();
        
        data.put ("response_type", "token");
        data.put ("client_id", storage.settings.getString ("key"));
        data.put ("redirect_uri", storage.settings.getString ("redirect_url"));
        data.put ("force_reapprove", true);
        
        return "https://www.dropbox.com/oauth2/authorize" + Net.urlQueryEncode (data);
        
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
      return "/";
    }
    
    private JSONArray list (Item item) throws StorageException, OutOfMemoryException {
      
      try {
        
        HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/list_folder");
        
        JSONObject data = new JSONObject ();
        
        if (item.toString ().equals ("/")) item.setFile ("");
        
        data.put ("path", item);
        data.put ("recursive", false);
        data.put ("include_media_info", true);
        data.put ("include_deleted", false);
        data.put ("include_has_explicit_shared_members", false);
        data.put ("include_mounted_folders", true);
        
        request.send (data);
        
        JSONObject output = new JSONObject (request.getContent ());
        
        if (output.has ("error_summary"))
          throw new StorageException (storage, setError (output));
        else
          return output.getJSONArray ("entries");
        
      } catch (JSONException | HttpRequestException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private class File extends Item {
      
      private File (Storage storage, String... items) {
        super (storage, items);
      }
      
      @Override
      public boolean isExists () throws StorageException {
        
        try {
          return getInfo ().has (".tag");
        } catch (StorageException e) {
          return false;
        }
        
      }
      
      @Override
      public long getSize () throws StorageException {
        
        try {
          return getInfo ().getLong ("size");
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      private JSONObject dimen;
      
      private JSONObject getDimen () throws StorageException, JSONException {
        
        if (dimen == null) dimen = getInfo ().getJSONObject ("media_info").getJSONObject ("metadata").getJSONObject ("dimensions");
        return dimen;
        
      }
      
      @Override
      public int getWidth () throws StorageException {
        
        try {
          return getDimen ().getInt ("width");
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public int getHeight () throws StorageException {
        
        try {
          return getDimen ().getInt ("height");
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public boolean isDir () throws StorageException {
        
        try {
          
          if (isDir == null) {
            
            try {
              isDir = getInfo ().getString (".tag").equals ("folder");
            } catch (StorageException e) {
              isDir = false;
            }
            
          }
          
          return isDir;
          
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        
        try {
          
          if (info == null) {
            
            HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/get_metadata");
            
            JSONObject data = new JSONObject ();
            
            if (toString ().equals ("/")) setFile ("");
            
            data.put ("path", getFile ());
            data.put ("include_media_info", true);
            data.put ("include_deleted", false);
            data.put ("include_has_explicit_shared_members", false);
            
            request.send (data);
            
            info = new JSONObject (request.getContent ());
            
            if (info.has ("error_summary"))
              throw new StorageException (storage, setError (info));
            
          }
          
          return info;
          
        } catch (JSONException | OutOfMemoryException | HttpRequestException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public URL getDirectLink () throws StorageException, OutOfMemoryException {
        
        try {
          
          if (directUrl == null) {
            
            HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/get_temporary_link");
            
            JSONObject data = new JSONObject ();
            
            data.put ("path", getFile ());
            
            request.send (data);
            
            String content = request.getContent ();
            
            if (content.startsWith ("Error"))
              throw new StorageException (storage, content, this);
            else
              directUrl = new URL (new JSONObject (content).getString ("link"));
            
          }
          
          return directUrl;
          
        } catch (JSONException | HttpRequestException | MalformedURLException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public InputStream getThumbnail () throws StorageException, OutOfMemoryException {
        
        try {
          
          HttpRequest request = request (HttpRequest.METHOD_POST, "https://content.dropboxapi.com/2/files/get_thumbnail");
          
          JSONObject data = new JSONObject ();
          
          data.put ("path", getFile ());
          data.put ("format", "jpeg");
          data.put ("size", "w480h320");
          data.put ("mode", "bestfit");
          
          request.setHeader ("Dropbox-API-Arg", data);
          request.setContentType ("text/plain");
          
          InputStream output = request.getInputStream ();
          
          String sOutput = "";
          if (!request.isOK ()) sOutput = Strings.toString (output);
          
          if (sOutput.startsWith ("Error"))
            throw new StorageException (storage, sOutput, this);
          else
            return output;
          
        } catch (JSONException | HttpRequestException | IOException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
    }
    
    @Override
    public Item getItem (String... remoteFile) {
      return new File (storage, remoteFile);
    }
    
    @Override
    public List<Item> list (Item item, int mode) throws StorageException, OutOfMemoryException {
      
      List<Item> files = new ArrayList<> ();
      
      try {
        
        JSONArray items = list (item);
        
        for (int i = 0; i < Int.size (items); ++i) {
          
          JSONObject data = items.getJSONObject (i);
          
          item = getItem (data.getString ("path_display"))
                   .isDir (data.getString (".tag").equals ("folder"))
                   .isImage (data.has ("media_info") && data.getJSONObject ("media_info").getJSONObject ("metadata").getString (".tag").equals ("photo"));
          
          if (item.show (mode)) files.add (item);
          
        }
        
      } catch (JSONException e) {
        throw new StorageException (storage, e, item);
      }
      
      return files;
      
    }
    
    @Override
    public int makeDir (Item item) throws StorageException {
      
      try {
        
        if (!item.isExists ()) {
          
          HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/create_folder_v2");
          
          JSONObject data = new JSONObject ();
          
          data.put ("path", item);
          data.put ("autorename", false);
          
          request.send (data);
          
          JSONObject output = new JSONObject (request.getContent ());
          
          if (!output.has ("error_summary"))
            return 1;
          else
            throw new StorageException (storage, setError (output));
          
        } else return 2;
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException {
      
      try {
        
        Item remoteItem = getItem (remoteFile);
        
        HttpRequest request = request (HttpRequest.METHOD_POST, "https://content.dropboxapi.com/2/files/upload");
        
        JSONObject data2 = new JSONObject ();
        
        data2.put ("path", remoteItem);
        data2.put ("autorename", false);
        data2.put ("mute", false);
        
        JSONObject mode = new JSONObject ();
        mode.put (".tag", "overwrite");
        
        data2.put ("mode", mode);
        
        request.setHeader ("Dropbox-API-Arg", data2);
        request.setContentType ("application/octet-stream");
        
        request.send (item.getStream ());
        
        JSONObject output = new JSONObject (request.getContent ());
        
        if (output.has ("error_summary"))
          throw new StorageException (storage, setError (output));
        
        return remoteItem;
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public String setError (JSONObject output) {
      
      try {
        
        return output.getString ("error_summary");
        //return output.toString ();
        
      } catch (JSONException e) {
        return e.getMessage ();
      }
      
    }
    
    @Override
    public void delete (Item item) throws StorageException {
      
      try {
        
        if (item.isDir ()) {
          
          JSONArray items = list (item);
          
          for (int i = 0; i < Int.size (items); ++i) {
            
            JSONObject data = items.getJSONObject (i);
            Item item2 = getItem (data.getString ("path_display"));
            
            if (!data.getString (".tag").equals ("folder"))
              deleteFile (item2);
            else
              delete (item2);
            
          }
          
        }
        
        deleteFile (item);
        
      } catch (JSONException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private void deleteFile (Item item) throws StorageException {
      
      try {
        
        if (item.isExists ()) {
          
          HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/delete_v2");
          
          JSONObject data = new JSONObject ();
          
          data.put ("path", item);
          
          request.send (data);
          
          JSONObject output = new JSONObject (request.getContent ());
          
          if (output.has ("error_summary"))
            throw new StorageException (storage, setError (output));
          
        }
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void copy (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException {
      
      try {
        move ("copy_batch_v2", new JSONObject (), from, to);
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, from.get (0));
      }
      
    }
    
    private void move (String url, JSONObject data, List<Item> from, List<Item> to) throws StorageException, JSONException, HttpRequestException, OutOfMemoryException {
      
      HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/" + url);
      
      JSONArray entries = new JSONArray ();
      
      for (int i = 0; i < Int.size (from); ++i) {
        
        JSONObject entry = new JSONObject ();
        
        Item fromItem = from.get (i), toItem = to.get (i);
        
        String path;
        
        if (toItem.isDir ())
          path = toItem.toString () + "/" + fromItem.getName ();
        else
          path = toItem.toString ();
        
        entry.put ("from_path", fromItem);
        entry.put ("to_path", path);
        
        entries.put (entry);

      }
      
      data.put ("entries", entries);
      data.put ("autorename", false);
      
      request.send (data);
      
      JSONObject output = new JSONObject (request.getContent ());
      
      if (output.has ("error_summary"))
        throw new StorageException (storage, setError (output));
      
    }
    
    @Override
    public void move (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException {
      
      try {
        
        JSONObject data = new JSONObject ();
        
        data.put ("allow_ownership_transfer", false);
        
        move ("move_batch_v2", data, from, to);
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e, from.get (0));
      }
      
    }
    
  }