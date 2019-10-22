  package pro.acuna.storage;
  /*
   Created by Acuna on 18.12.2018
  */
  
  import org.json.JSONObject;

  import java.io.InputStream;
  import java.net.URL;
  import java.util.List;

  import pro.acuna.jabadaba.Arrays;
  import pro.acuna.jabadaba.Files;
  import pro.acuna.jabadaba.Net;
  import pro.acuna.jabadaba.exceptions.HttpRequestException;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  
  public abstract class Item {
    
    public Storage storage;
    
    private int width = -1, height = -1;
    private long size = -1;
    private String id;
    public URL url, directUrl;
    protected Boolean isDir;
    private boolean isImage;
    public InputStream stream;
    
    private String file = "", shortFile;
    public JSONObject info;
    
    public Item () {}
    
    public Item (Storage storage, String... file) {
      
      this.storage = storage;
      this.file = Arrays.implode ("/", file);
      
      shortFile = this.file;
      
      try {
        this.file = storage.provider.prepRemoteFile (this.file);
      } catch (StorageException e) {
        // empty
      }
      
    }
    
    public abstract boolean isExists () throws StorageException;
    public abstract boolean isDir () throws StorageException;
    public abstract URL getDirectLink () throws StorageException, OutOfMemoryException;
    public abstract JSONObject getInfo () throws StorageException;
    
    public final Item setSize (long size) {
      
      this.size = size;
      return this;
      
    }
    
    public long getSize () throws StorageException {
      return size;
    }
    
    public final Item isDir (boolean isDir) {
      
      this.isDir = isDir;
      return this;
      
    }
    
    public final Item isImage (boolean isImage) {
      
      this.isImage = isImage;
      return this;
      
    }
    
    public boolean isImage () throws StorageException {
      return isImage;
    }
    
    public final Item setDirectLink (URL url) {
      
      this.directUrl = url;
      return this;
      
    }
    
    public final Item setWidth (int size) {
      
      this.width = size;
      return this;
      
    }
    
    public final Item setURL (URL url) {
      
      this.url = url;
      return this;
      
    }
    
    public URL getURL () {
      return url;
    }
    
    public final Item setHeight (int size) {
      
      this.height = size;
      return this;
      
    }
    
    public int getWidth () throws StorageException {
      return width;
    }
    
    public int getHeight () throws StorageException {
      return height;
    }
    
    public final Item setId (String id) {
      
      this.id = id;
      return this;
      
    }
    
    public String getId () {
      return id;
    }
    
    public final boolean show (int mode) throws StorageException {
      
      return (
             (mode == 1 && !isDir ()) || // Только файлы
             (mode == 2 &&  isDir ()) || // Только папки
              mode == 0 // Все
      );
      
    }
    
    public final List<Item> list () throws StorageException, OutOfMemoryException {
      return list (0);
    }
    
    public final List<Item> list (int type) throws StorageException, OutOfMemoryException {
      return storage.list (shortFile, type);
    }
    
    public final Item setFile (String file) {
      
      this.file = file;
      return this;
      
    }
    
    public final String getFile () {
      return file;
    }
    
    public final String getShortFile () {
      return shortFile;
    }
    
    public final String getPath () {
      return Files.getPath (file);
    }
    
    public final String getName () {
      return Files.getName (file, true);
    }
    
    public String toString () {
      return getFile ();
    }
    
    public final Item getParent () {
      return storage.setDir (Files.getPath (file));
    }
    
    public InputStream getThumbnail () throws StorageException, OutOfMemoryException {
      return getStream ();
    }
    
    final Item setStream (InputStream stream) {
      
      this.stream = stream;
      return this;
      
    }
    
    public final InputStream getStream () throws StorageException, OutOfMemoryException {
      return getStream (Files.getExtension (file));
    }
    
    public InputStream getStream (String type) throws StorageException, OutOfMemoryException {
      
      try {
        
        if (stream == null)
          stream = Net.getStream (getDirectLink (), storage.getUseragent (), type);
        
        return stream;
        
      } catch (HttpRequestException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
  }