  package pro.acuna.storage.providers;
  /*
   Created by Acuna on 15.05.2018
  */
  
  import android.os.Bundle;
  
  import com.jcraft.jsch.ChannelSftp;
  import com.jcraft.jsch.JSch;
  import com.jcraft.jsch.JSchException;
  import com.jcraft.jsch.Session;
  import com.jcraft.jsch.SftpATTRS;
  import com.jcraft.jsch.SftpException;
  
  import pro.acuna.jabadaba.Arrays;
  import pro.acuna.jabadaba.Int;
  import pro.acuna.jabadaba.Streams;
  import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
  import pro.acuna.storage.Item;
  import pro.acuna.storage.Provider;
  import pro.acuna.storage.R;
  import pro.acuna.storage.Storage;
  import pro.acuna.storage.StorageException;
  
  import org.json.JSONException;
  import org.json.JSONObject;
  
  import java.io.ByteArrayOutputStream;
  import java.io.IOException;
  import java.io.InputStream;
  import java.io.OutputStream;
  import java.io.PipedInputStream;
  import java.io.PipedOutputStream;
  import java.net.MalformedURLException;
  import java.net.URL;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;
  import java.util.Properties;
  import java.util.Vector;
  
  public class SFTP extends Provider {
    
    private Session session;
    private ChannelSftp channel;
    
    final public static String SERVER = "server";
    final public static String PORT = "port";
    final public static String USER = "username";
    final public static String PASSWORD = "password";
    final public static String PATH = "remote_root_path";
    final public static String TIMEOUT = "timeout";
    
    public SFTP () {}
    
    private SFTP (Storage storager) throws StorageException {
      super (storager);
    }
    
    @Override
    public Provider newInstance (Storage storager) throws StorageException {
      return new SFTP (storager);
    }
    
    @Override
    public String getName () {
      return "sftp";
    }
    
    @Override
    public JSONObject getUser (Bundle bundle) throws StorageException {
      
      try {
        
        JSONObject userData = new JSONObject ();
        
        userData.put (Storage.USER_NAME, storage.settings.getString (USER));
        
        return userData;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public Provider auth () throws StorageException {
      
      try {
        
        if (Int.size (storage.settings) > 0) {
          
          Map<String, Object> defData = storage.getDefData ();
          
          if ((storage.settings.getInt (PORT) <= 0) && (int) defData.get (PORT) > 0)
            storage.settings.put (PORT, (int) defData.get (PORT));
          
          session = new JSch ().getSession (storage.settings.getString (USER), storage.settings.getString (SERVER), storage.settings.getInt (PORT));
          
          if (storage.settings.getString (PASSWORD) != null && !storage.settings.getString (PASSWORD).equals (""))
            session.setPassword (storage.settings.getString (PASSWORD));
          
          Properties properties = new Properties ();
          
          properties.put ("StrictHostKeyChecking", "no");
          properties.put ("PreferredAuthentications", "password");
          
          session.setConfig (properties);
          
          storage.settings.put (TIMEOUT, Int.correct (storage.settings.getInt (TIMEOUT), (int) defData.get (TIMEOUT)));
          
          session.connect (storage.settings.getInt (TIMEOUT) * 1000);
          
          if (storage.settings.getString (PATH) == null || storage.settings.getString (PATH).equals (""))
            storage.settings.put (PATH, "/");
          
          channel = (ChannelSftp) session.openChannel ("sftp");
          channel.connect ();
          
        }
        
        return this;
        
      } catch (JSONException | JSchException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public boolean isAuthenticated () throws StorageException {
      return channel.isConnected ();
    }
    
    @Override
    public JSONObject setProviderItems (JSONObject data) throws StorageException {
      
      try {
        
        data.put (Storage.ITEM_TITLE, "SFTP");
        data.put (Storage.ITEM_LAYOUT, R.layout.dialog_ftp);
        
        return data;
        
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    @Override
    public Map<String, Object> setDefData (Map<String, Object> data) {
      
      data.put (SERVER, "");
      data.put (PORT, 22);
      data.put (USER, "");
      data.put (PASSWORD, "");
      data.put (PATH, "");
      data.put (TIMEOUT, 10);
      
      return data;
      
    }
    
    @Override
    public String[] getDecryptedKeys () {
      return new String[] { USER };
    }
    
    @Override
    public String getRootDir () throws StorageException {
      
      try {
        return storage.settings.getString (PATH);
      } catch (JSONException e) {
        throw new StorageException (storage, e);
      }
      
    }
    
    private class File extends Item {
      
      private File (Storage storage, String... remoteFile) {
        super (storage, remoteFile);
      }
      
      @Override
      public boolean isExists () throws StorageException {
        
        try {
          return (getAttrs (getFile ()) != null);
        } catch (SftpException e) {
          
          if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
            throw new StorageException (storage, e, this);
          else
            return false;
          
        }
        
      }
      
      @Override
      public JSONObject getInfo () throws StorageException {
        return new JSONObject ();
      }
      
      @Override
      public long getSize () throws StorageException {
        
        try {
          return getAttrs (getFile ()).getSize ();
        } catch (SftpException e) {
          throw new StorageException (storage, e, this);
        }
        
      }
      
      @Override
      public boolean isDir () throws StorageException {
        
        try {
          if (isDir == null) isDir = getAttrs (getFile ()).isDir ();
        } catch (SftpException e) {
          
          if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
            throw new StorageException (storage, e, this); // Вообще ничего не смогли получить
          
        }
        
        return isDir;
        
      }
      
      @Override
      public boolean isImage () throws StorageException {
        return false;
      }
      
      @Override
      public URL getDirectLink () throws StorageException {
        
        try {
          
          if (directUrl == null) directUrl = new URL (getFile ()); // TODO
          return directUrl;
          
        } catch (MalformedURLException e) {
          throw new StorageException (storage, e);
        }
        
      }
      
      @Override
      public InputStream getStream (String type) throws StorageException {
        
        try {
          
          OutputStream outputStream = new ByteArrayOutputStream ();
          channel.get (getFile (), outputStream);
          
          return Streams.toInputStream (outputStream);
          
        } catch (SftpException e) {
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
        
        if (item.isDir ()) {
          
          Vector entries = channel.ls (item.getFile ());
          
          for (int i = 0; i < Int.size (entries); ++i) {
            
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) entries.get (i);
            
            String name = entry.getFilename ();
            
            if (!name.equals (".") && !name.equals ("..")) {
              
              Item item2 = getItem (item.toString (), name)
																 .isDir (entry.getAttrs ().isDir ())
																 .setDirectLink (new URL (entry.toString ()));
              
              if (item2.show (mode)) files.add (item2);
              
            }
            
          }
          
        }
        
      } catch (SftpException | MalformedURLException e) {
        throw new StorageException (storage, e, item);
      }
      
      return files;
      
    }
    
    private SftpATTRS getAttrs (String remoteFile) throws SftpException {
      return channel.lstat (remoteFile);
    }
    
    @Override
    public void chmod (Item item, int chmod) throws StorageException {
      
      try {
        channel.chmod (chmod, item.toString ());
      } catch (SftpException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public int makeDir (Item item) throws StorageException {
      
      try {
        
        if (!item.isExists ()) {
          
          List<String> parts = Arrays.explode ("/", item.getFile ());
          
          String path = "";
          
          for (int i = 0; i < Int.size (parts); ++i) {
            
            if (i > 0) path += "/";
            path += parts.get (i);
            
            if (!getItem (path).isDir ())
              channel.mkdir (path);
            
          }
          
          return 1;
          
        } else return 2;
        
      } catch (SftpException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public Item put (Item item, String remoteFile, boolean force, boolean makeDir, Object... data) throws StorageException, OutOfMemoryException {
      
      try {
        
        Item remoteItem = getItem (remoteFile);
        
        if (force (remoteItem, force)) {
          
          if (makeDir) makeDir (getItem (remoteItem.getPath ()));
          channel.put (item.getStream (), remoteFile, ChannelSftp.OVERWRITE);
          
        }
        
        return remoteItem;
        
      } catch (SftpException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void close () throws StorageException {
      
      if (session != null) session.disconnect ();
      if (channel != null) channel.quit ();
      
    }
    
    @Override
    public void delete (Item item) throws StorageException {
      
      try {
        
        if (item.isDir ()) {
          
          Vector entries = channel.ls (item.getFile ());
          
          for (int i = 0; i < Int.size (entries); ++i) {
            
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) entries.get (i);
            
            String name = entry.getFilename ();
            
            if (!name.equals (".") && !name.equals ("..")) {
              
              Item item2 = getItem (item.getFile (), name);
              
              if (!item2.isDir ())
                deleteFile (item2);
              else
                delete (item2);
              
            }
            
          }
          
          channel.rmdir (item.getFile ());
          
        } else deleteFile (item);
        
      } catch (SftpException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    private void deleteFile (Item item) throws StorageException {
      
      try {
        channel.rm (item.getFile ());
      } catch (SftpException e) {
        throw new StorageException (storage, e, item);
      }
      
    }
    
    @Override
    public void copy (List<Item> from, List<Item> to) throws StorageException {
      
      try {
        
        ChannelSftp wChannel = (ChannelSftp) session.openChannel (getName ());
        wChannel.connect ();
        
        for (int i = 0; i < Int.size (from); ++i) {
          
          PipedInputStream in = new PipedInputStream (2048);
          PipedOutputStream out = new PipedOutputStream (in);
          
          channel.get (from.get (i).toString (), out);
          wChannel.put (in, to.get (i).toString ());
          
        }
        
      } catch (SftpException | JSchException | IOException e) {
        throw new StorageException (storage, e, from.get (0));
      }
      
    }
    
    @Override
    public void move (List<Item> from, List<Item> to) throws StorageException, OutOfMemoryException {
      //TODO
    }
    
  }