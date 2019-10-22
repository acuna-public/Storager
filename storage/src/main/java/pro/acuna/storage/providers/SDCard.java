  package pro.acuna.storage.providers;
  
  import android.graphics.BitmapFactory;
  import android.os.Bundle;
  
  import org.json.JSONException;
  import org.json.JSONObject;
  
  import java.io.File;
  import java.io.FileInputStream;
  import java.io.IOException;
  import java.io.InputStream;
  import java.net.MalformedURLException;
  import java.net.URL;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;

  import pro.acuna.andromeda.Graphic;
  import pro.acuna.andromeda.Log;
  import pro.acuna.andromeda.OS;
  import pro.acuna.jabadaba.Files;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.Item;
  import pro.acuna.storage.Provider;
  import pro.acuna.storage.Storage;
  import pro.acuna.storage.StorageException;
  
  public class SDCard extends Provider {
    
    public SDCard () {}
    
    private SDCard (Storage storager) throws StorageException {
      super (storager);
    }
    
    @Override
    public Provider newInstance (Storage storager) throws StorageException {
      return new SDCard (storager);
    }
    
    @Override
    public String getName () {
      return "sdcard";
    }
    
    @Override
    public boolean isAuthenticated () throws StorageException {
      return true;
    }
    
    @Override
    public JSONObject getUser (Bundle bundle) throws StorageException {
      
      try {
        
        JSONObject userData = new JSONObject ();
        
        userData.put (Storage.USER_NAME, storage.getProviderItems ().getString (Storage.ITEM_TITLE));
        
        return userData;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public JSONObject setProviderItems (JSONObject data) throws StorageException {
      
      try {
        
        data.put (Storage.ITEM_TITLE, "SDCard");
        data.put (Storage.ITEM_LAYOUT, -1);
        
        return data;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public Map<String, Object> setDefData (Map<String, Object> data) {
      
      data.put ("folder", OS.getExternalFilesDir (storage.context));
      data.put ("useragent", "Storage");
      
      return data;
      
    }
    
    @Override
    public String getRootDir () throws StorageException {
      
      try {
        return storage.settings.getString ("folder");
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private File destFile (Item item) {
      return new File (item.toString ());
    }
    
    private class FileItem extends Item {
      
      private File file;
      
      private FileItem (Storage storage, String... remoteFile) {
        
        super (storage, remoteFile);
        file = destFile (this);
        
      }
      
      @Override
      public boolean isExists () throws StorageException {
        return file.exists ();
      }
      
      @Override
      public boolean isDir () throws StorageException {
        return file.isDirectory ();
      }
      
      @Override
      public long getSize () throws StorageException {
        return Int.size (file);
      }
      
      @Override
      public int getWidth () throws StorageException {
        
        try {
          return getInfo ().getInt ("width");
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public int getHeight () throws StorageException {
        
        try {
          return getInfo ().getInt ("height");
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        
        try {
          
          if (info == null) {
            
            info = new JSONObject ();
            
            BitmapFactory.Options options = Graphic.getInfo (file);
            
            info.put ("width", options.outWidth);
            info.put ("height", options.outHeight);
            
          }
          
        } catch (JSONException e) {
          throw new StorageException (storage, e);
        }
        
        return info;
        
      }
      
      @Override
      public URL getDirectLink () throws StorageException {
        
        try {
          
          if (directUrl == null) directUrl = file.toURI ().toURL ();
          return directUrl;
          
        } catch (MalformedURLException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public InputStream getStream (String link) throws StorageException {
        
        try {
          return (stream == null ? new FileInputStream (file) : stream);
        } catch (IOException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
    }
    
    @Override
    public Item getItem (String... remoteFile) {
      return new FileItem (storage, remoteFile);
    }
    
    @Override
    public List<Item> list (Item item, int mode) throws StorageException {
      
      String[] files = destFile (item).list ();
      
      List<Item> output = new ArrayList<> ();
      
      if (files != null) {
        
        for (String file : files) {
          
          Item item2 = getItem (item.getShortFile (), file);
          if (item2.show (mode)) output.add (item2);
          
        }
        
      } else throw new StorageException (storage, item + ": Not found or access denied", item);
      
      return output;
      
    }
    
    @Override
    public int makeDir (Item item) throws StorageException {
      
      try {
        return Files.makeDir (destFile (item));
      } catch (IOException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      
      Item remoteItem = getItem (remoteFile);
      
      try {
        if (force (remoteItem, force)) Files.copy (item.getStream (), destFile (remoteItem));
      } catch (IOException e) {
        throw new StorageException (storage, e, item);
      }
      
      return remoteItem;
      
    }
    
    @Override
    public void delete (Item item) throws StorageException {
      
      try {
        Files.delete (destFile (item));
      } catch (IOException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void copy (List<Item> from, List<Item> to) throws StorageException {
      
      for (int i = 0; i < Int.size (from); ++i) {
        
        try {
          Files.copy (from.get (i).toString (), to.get (i).toString ());
        } catch (IOException e) {
          throw new StorageException (storage, e, from.get (0));
        }
        
      }
      
    }
    
    @Override
    public void move (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException {
      
      copy (from, to);
      
      for (int i = 0; i < Int.size (from); ++i)
        delete (from.get (i));
      
    }
    
  }