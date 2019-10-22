  package pro.acuna.storage.providers;
  
  import android.app.Activity;
  import android.content.Intent;
  import android.os.Bundle;
  import android.webkit.WebView;
  
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
  
  import pro.acuna.andromeda.AsyncTaskLoading;
  import pro.acuna.jabadaba.Arrays;
  import pro.acuna.jabadaba.HttpRequest;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.exceptions.HttpRequestException;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.Item;
  import pro.acuna.storage.Provider;
  import pro.acuna.storage.R;
  import pro.acuna.storage.Storage;
  import pro.acuna.storage.StorageException;
  
  public class GDrive extends Provider {
    
    private static final String API_URL = "https://www.googleapis.com";
    
    private String[] scopes;
    private String mimeFolder = "application/vnd.google-apps.folder";
    
    public GDrive () {}
    
    private GDrive (Storage storage) throws StorageException {
      
      super (storage);
      setUseragent ("Google-API-Java-Client");
      
      scopes = new String[] {
        
        "email",
        API_URL + "/auth/drive.file",
        API_URL + "/auth/drive.metadata",
        
        };
      
    }
    
    @Override
    public Provider newInstance (Storage storage) throws StorageException {
      return new GDrive (storage);
    }
    
    @Override
    public JSONObject getUser (Bundle bundle) throws StorageException {
      
      try {
        
        Map<String, Object> params = new LinkedHashMap<> ();
        
        params.put ("access_token", bundle.getString ("access_token"));
        
        HttpRequest request = new HttpRequest (HttpRequest.METHOD_GET, API_URL + "/oauth2/v3/userinfo", params);
        request.setUserAgent (storage.getUseragent ());
        
        JSONObject data = new JSONObject (request.getContent ());
        
        if (request.isOK ()) {
          
          JSONObject userData = new JSONObject ();
          
          userData.put (Storage.USER_NAME, data.getString ("email"));
          
          return userData;
          
        } else throw new StorageException (storage, data);
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public Provider auth () throws StorageException {
      
      try {
        
        String token = storage.getPref ("refresh_token");
        
        if (!token.equals ("")) {
          
          Map<String, Object> data = new LinkedHashMap<> ();
          
          data.put ("client_id", storage.settings.getString ("client_id"));
          data.put ("client_secret", storage.settings.getString ("client_secret"));
          data.put ("grant_type", "refresh_token");
          data.put ("refresh_token", token);
          
          HttpRequest request = request (HttpRequest.METHOD_POST, "oauth2/v4/token", new LinkedHashMap<String, Object> ());
          request.send (data);
          
          authData = new JSONObject (request.getContent ());
          
        }
        
        return this;
        
      } catch (JSONException | HttpRequestException | StorageException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String getName () {
      return "gdrive";
    }
    
    @Override
    public JSONObject setProviderItems (JSONObject data) throws StorageException {
      
      try {
        
        data.put (Storage.ITEM_TITLE, "Google Drive");
        
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
        data.put ("response_type", "code");
        data.put ("scope", Arrays.implode (" ", scopes));
        data.put ("redirect_uri", getRedirectUrl ());
        data.put ("prompt", "consent");
        data.put ("access_type", "offline");
        
        return "https://accounts.google.com/o/oauth2/auth" + Net.urlQueryEncode (data);
        
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
    public WebView setWebView (WebView webView) throws StorageException {
      
      //try {
      
      //webView.getSettings ().setUserAgentString (storage.getUseragent ());
      webView.getSettings ().setUserAgentString ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"); // TODO
      
      //} catch (JSONException e) {
      //  throw new StorageException (storage, e);
      //}
      
      return webView;
      
    }
    
    @Override
    public String getRootDir () throws StorageException {
      
      try {
        return getAppsFolderTitle () + "/" + storage.settings.getString ("app_name");
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public void processUrl (final Map<String, String> urlData, final Map<String, String> redirectUrlData) throws StorageException {
      
      final List<Exception> errors = new ArrayList<> ();
      
      new AsyncTaskLoading (storage.activity, R.string.loading, new AsyncTaskLoading.AsyncTaskInterface () {
        
        @Override
        public List<Object> doInBackground (Object... params) {
          
          List<Object> result = new ArrayList<> ();
          
          try {
            
            Map<String, Object> data = Net.urlQueryDecode (urlData.get (Net.URL_QUERY));
            
            if (data != null && data.get ("code") != null) {
              
              String code = data.get ("code").toString ();
              
              data = new LinkedHashMap<> ();
              
              data.put ("code", code);
              data.put ("client_id", storage.settings.getString ("client_id"));
              data.put ("client_secret", storage.settings.getString ("client_secret"));
              data.put ("redirect_uri", getRedirectUrl ());
              data.put ("scope", Arrays.implode (" ", scopes));
              data.put ("grant_type", "authorization_code");
              
              result.add (HttpRequest.post (API_URL + "/oauth2/v4/token").send (data).getContent ());
              
            }
            
          } catch (StorageException | JSONException | HttpRequestException | OutOfMemoryException e) {
            errors.add (e);
          }
          
          return result;
          
        }
        
        @Override
        public void onPostExecute (List<Object> result) {
          
          Intent intent = new Intent ();
          
          if (Int.size (result) > 0) {
            
            if (Int.size (errors) == 0) {
              
              try {
                
                JSONObject data = new JSONObject (result.get (0).toString ());
                JSONArray keys = data.names ();
                
                for (int i = 0; i < Int.size (keys); ++i) {
                  
                  String key = keys.getString (i);
                  intent.putExtra (key, storage.setError (data.get (key)));
                  
                }
                
                storage.activity.setResult ((data.has ("error") ? 2 : Activity.RESULT_OK), intent);
                
              } catch (JSONException e) {
                
                intent.putExtra ("error", e.getMessage ());
                
                storage.activity.setResult (2, intent);
                
              }
              
            } else {
              
              intent.putExtra ("error", Arrays.implode ("\n", errors));
              
              storage.activity.setResult (2, intent);
              
            }
            
            storage.activity.finish ();
            
          }
          
        }
        
      }).execute (); // TODO
      
    }
    
    @Override
    public boolean isAuthenticated () throws StorageException {
      return storage.hasPref ("access_token");
    }
    
    private HttpRequest request (String method, String url, Map<String, Object> params) throws StorageException {
      
      try {
        
        HttpRequest request = initRequest (new HttpRequest (method, API_URL + "/" + url, params));
        
        request.setUserAgent (storage.getUseragent ());
        request.setHeader ("Authorization", storage.getPref ("token_type") + " " + storage.getPref ("access_token"));
        request.isJSON (true);
        
        return request;
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private JSONArray fileMetadata (String remoteFile, String folderId) throws StorageException {
      
      List<String> query = new ArrayList<> ();
      
      query.add ("name = '" + remoteFile + "'");
      query.add ("mimeType != '" + mimeFolder + "'");
      
      return gdriveList (query, folderId, "id, webContentLink");
      
    }
    
    private JSONArray dirMetadata (String remoteFile, String folderId) throws StorageException {
      
      List<String> query = new ArrayList<> ();
      
      query.add ("name = '" + remoteFile + "'");
      query.add ("mimeType = '" + mimeFolder + "'");
      
      return gdriveList (query, folderId, "id");
      
    }
    
    private JSONArray filesMetadata (String remoteFile, String folderId) throws StorageException {
      
      List<String> query = new ArrayList<> ();
      
      query.add ("name = '" + remoteFile + "'");
      
      return gdriveList (query, folderId, "id, name, webContentLink");
      
    }
    
    private JSONArray gdriveList (List<String> query, String folderId, String fields) throws StorageException {
      
      if (!folderId.equals ("")) query.add ("'" + folderId + "' IN parents");
      if (delPermanent) query.add ("trashed = false");
      
      try {
        
        Map<String, Object> params = new LinkedHashMap<> ();
        
        params.put ("fields", "files(" + fields + ")");
        params.put ("q", Arrays.implode (" AND ", query));
        
        HttpRequest request = request (HttpRequest.METHOD_GET, "drive/v3/files", params);
        JSONObject result = new JSONObject (request.getContent ());
        
        if (result.has ("error"))
          throw new StorageException (storage, result);
        else
          return result.getJSONArray ("files");
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private class File extends Item {
      
      private File (Storage storage, String... remoteFile) {
        super (storage, remoteFile);
      }
      
      @Override
      public boolean isExists () throws StorageException {
        
        List<String> parts = Arrays.explode ("/", getFile ());
        String folderId = _getId (parts, 1);
        
        JSONArray filesMetadata = new JSONArray ();
        
        if (!folderId.equals (""))
          filesMetadata = filesMetadata (parts.get (Arrays.endKey (parts)), folderId);
        
        return (Int.size (filesMetadata) > 0);
        
      }
      
      @Override
      public boolean isDir () throws StorageException {
        
        if (isDir == null) {
          
          List<String> parts = Arrays.explode ("/", getFile ());
          isDir = !_getId (parts, 0).equals ("");
          
        }
        
        return isDir;
        
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        
        try {
          
          if (info == null) {
            
            List<String> parts = Arrays.explode ("/", getFile ());
            String folderId = _getId (parts, 1);
            
            JSONArray filesMetadata = new JSONArray ();
            
            if (!folderId.equals (""))
              filesMetadata = fileMetadata (parts.get (Arrays.endKey (parts)), folderId);
            
            if (Int.size (filesMetadata) > 0)
              info = filesMetadata.getJSONObject (0);
            else
              throw new StorageException ("File " + getFile () + " not found on server");
            
          }
          
          return info;
          
        } catch (JSONException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public URL getDirectLink () throws StorageException {
        
        try {
          
          if (directUrl == null) directUrl = new URL (getInfo ().getString ("webContentLink"));
          return directUrl;
          
        } catch (JSONException | MalformedURLException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
    }
    
    @Override
    public Item getItem (String... remoteFile) {
      return new File (storage, remoteFile);
    }
    
    @Override
    public List<Item> list (Item item, int mode) throws StorageException, OutOfMemoryException {
      
      try {
        
        String folderId = "";
        JSONArray filesMetadata;
        
        List<String> parts = Arrays.explode ("/", item.toString ());
        
        for (String part : parts) {
          
          filesMetadata = dirMetadata (part, folderId);
          
          // Из-за откровенно индусского кода Гугла можно с легкостью создавать файлы с одинаковыми именами,
          // поэтому если найдено два одинаковых файла или папки - выбираем первый.
          
          if (Int.size (filesMetadata) > 0)
            folderId = filesMetadata.getJSONObject (0).getString ("id");
          
        }
        
        List<Item> files = new ArrayList<> ();
        
        if (!folderId.equals ("")) {
          
          filesMetadata = gdriveList (new ArrayList<String> (), folderId, "name, mimeType, webContentLink");
          
          for (int i = 0; i < Int.size (filesMetadata); ++i) {
            
            JSONObject file = filesMetadata.getJSONObject (i);
            
            Item item2 = getItem (item.toString (), file.getString ("name"))
                          .isDir (file.getString ("mimeType").equals (mimeFolder));
            
            if (!item2.isDir ()) item2.setDirectLink (new URL (file.getString ("webContentLink")));
            if (item2.show (mode)) files.add (item2);
            
          }
          
        }
        
        return files;
        
      } catch (JSONException | MalformedURLException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private String _getId (List<String> parts, int offset) throws StorageException {
      
      try {
        
        String folderId = "";
        
        for (int i = 0; i < (Int.size (parts) - offset); ++i) {
          
          JSONArray filesMetadata = dirMetadata (parts.get (i), folderId);
          
          if (Int.size (filesMetadata) > 0)
            folderId = filesMetadata.getJSONObject (0).getString ("id");
          else
            folderId = "";
          
        }
        
        return folderId;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private String gdriveMakeDir (String name, String parentId) throws StorageException {
      
      try {
        
        JSONObject data = new JSONObject ();
        
        JSONArray folders = dirMetadata (name, parentId);
        HttpRequest request;
        
        Map<String, Object> params = new LinkedHashMap<> ();
        
        String output = "";
        
        params.put ("uploadType", "multipart");
        
        //params.put ("addParents", Arrays.implode (",", addParents));
        //params.put ("removeParents", Arrays.implode (",", removeParents));
        
        if (Int.size (folders) > 0) {
          
          JSONObject folder = folders.getJSONObject (0);
          //file.setModifiedTime (Locales.date (0));
          
          params.put ("fileId", folder.getString ("id"));
          
          request = request (HttpRequest.METHOD_PATCH, "drive/v3/files/" + folder.getString ("id"), params);
          
        } else {
          
          data.put ("name", name);
          data.put ("mimeType", mimeFolder);
          
          if (!parentId.equals (""))
            data.put ("parents", Arrays.toJSONArray (parentId));
          
          request = request (HttpRequest.METHOD_POST, "drive/v3/files", params);
          
        }
        
        data.put ("fields", "id");
        
        request.send (data);
        
        output = request.getContent ();
        
        if (request.isOK ())
          return new JSONObject (output).getString ("id");
        else
          throw new StorageException (storage, output);
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private String gdriveMakeDir (Item item) throws StorageException {
      
      List<String> parts = Arrays.explode ("/", item.getShortFile ());
      
      String folderId = "";
      
      for (int i = 0; i < (Int.size (parts) - 1); ++i)
        folderId = gdriveMakeDir (parts.get (i), folderId);
      
      return folderId;
      
    }
    
    @Override
    public int makeDir (Item item) throws StorageException {
      
      if (!item.isExists ()) {
        
        gdriveMakeDir (item);
        return 1;
        
      } else return 2;
      
    }
    
    @Override
    public Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException {
      
      try {
        
        JSONObject data2 = new JSONObject ();
        String folderId = "";
        
        if (makeDir) folderId = gdriveMakeDir (item);
        
        Item remoteItem = getItem (remoteFile);
        
        JSONArray fileMetadata = fileMetadata (remoteItem.getName (), folderId);
        boolean isExists = (Int.size (fileMetadata) > 0); // TODO
        
        if ((!force && !isExists) || force) {
          
          if (isExists) {
            
            JSONObject file = fileMetadata.getJSONObject (0);
            //file.setModifiedTime (new DateTime (System.currentTimeMillis ()));
                    
                    /*gdriveApi.files ().update (file, new GDriveAPI.File (), stream)
                             .setFields ("id, modifiedTime")
                             .execute ();*/
            
          } else {
            
            data2.put ("name", remoteItem.getName ());
            
            if (!folderId.equals (""))
              data2.put ("parents", Arrays.toJSONArray (folderId));
            
            Map<String, Object> params = new LinkedHashMap<> ();
            
            params.put ("uploadType", "resumable");
            
            HttpRequest request = request (HttpRequest.METHOD_POST, "upload/drive/v3/files", params);
            
            request.setHeader ("X-Upload-Content-Type", data2.optString ("mimeType"));
            request.setHeader ("Content-Type", "application/json; charset=UTF-8");
            request.setHeader ("Accept", "application/json");
            
            request.send (data2);
            
            String output = request.getContent ();
            
            if (request.isOK ()) {
              
              String sessionUrl = request.getHeader ("Location");
              
              if (sessionUrl != null) {
                
                data2 = upload (item.getStream (), data2, sessionUrl, 0, Int.size (item.getStream ()));
                
                params = new LinkedHashMap<> ();
                
                params.put ("fileId", data2.getString ("id"));
                params.put ("transferOwnership", false);
                
                request = request (HttpRequest.METHOD_POST, "drive/v3/files/" + data2.getString ("id") + "/permissions", params);
                
                data2 = new JSONObject ();
                
                data2.put ("role", "reader");
                data2.put ("type", "anyone");
                
                request.send (data2);
                
                if (!request.isOK ())
                  throw new StorageException (storage, request.getContent ());
                
              } else throw new StorageException (storage, output);
              
            } else throw new StorageException (storage, output);
            
          }
          
        }
        
        return remoteItem;
        
      } catch (IOException | HttpRequestException | JSONException | OutOfMemoryException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private long uploadedBytes = (2000 * 1024 * 1024);
    
    private JSONObject upload (InputStream stream, JSONObject file, String sessionUrl, long chunkStart, long total) throws StorageException {
      
      JSONObject result = new JSONObject ();
      
      try {
        
        HttpRequest request = request (HttpRequest.METHOD_PUT, sessionUrl, null);
        
        if ((chunkStart + uploadedBytes) > total)
          uploadedBytes = total - chunkStart;
        
        request.setHeader ("Content-Type", file.optString ("mimeType"));
        request.setHeader ("Content-Length", uploadedBytes);
        request.setHeader ("Content-Range", "bytes " + chunkStart + "-" + (chunkStart + uploadedBytes - 1) + "/" + total);
          
          /*if (stream instanceof FileInputStream) {
            
            byte[] buffer = new byte[(int) uploadedBytes];
            
            FileInputStream fileInputStream = (FileInputStream) stream;
            fileInputStream.getChannel ().position (chunkStart);
            
            if (fileInputStream.read (buffer, 0, (int) uploadedBytes) == -1) { }
            fileInputStream.close ();
            
            OutputStream outputStream = request.getOutputStream ();
            
            outputStream.write (buffer);
            outputStream.close ();
            
          } else */
        request.send (stream);
        
        if (request.getCode () == 308) { // Продолжаем
          
          String range = request.getHeader ("Range");
          chunkStart = Long.parseLong (range.substring (range.lastIndexOf ("-") + 1, Int.size (range))) + 1;
          
          result = upload (stream, file, sessionUrl, chunkStart, total);
          
        } else if (request.isOK ())
          result = new JSONObject (request.getContent ());
        else if (result.has ("error"))
          throw new StorageException (storage, result);
        else
          throw new StorageException (storage, request.getContent ());
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
      return result;
      
    }
    
    @Override
    public void delete (Item item) throws StorageException {
      
      try {
        
        if (item.isDir ()) {
          
          String folderId = "";
          
          JSONArray filesMetadata;
          List<String> parts = Arrays.explode ("/", item.getFile ());
          
          for (int i = 0; i < Int.size (parts); ++i) {
            
            filesMetadata = dirMetadata (parts.get (i), folderId);
            deleteObject (filesMetadata.getJSONObject (0).getString ("id"));
            
          }
          
        } else deleteObject (item.getInfo ().getString ("id"));
        
      } catch (JSONException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private void deleteObject (String id) throws StorageException {
      
      try {
        
        Map<String, Object> params = new LinkedHashMap<> ();
        
        params.put ("fileId", id);
        
        HttpRequest request = request (HttpRequest.METHOD_DELETE, "drive/v3/files/" + id, params);
        
        String output = request.getContent ();
        
        if (!output.equals (""))
          throw new StorageException (storage, new JSONObject (output));
        
      } catch (JSONException | HttpRequestException | OutOfMemoryException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public String setError (JSONObject result) {
      
      try {
        return result.getJSONObject ("error").getString ("message");
      } catch (JSONException e) {
        return e.getMessage ();
      }
      
    }
    
    @Override
    public void copy (List<Item> from, List<Item> to) throws StorageException {
      // TODO
    }
    
    @Override
    public void move (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException {
      //TODO
    }
    
  }