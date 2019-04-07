	package pro.acuna.storage.providers;
	/*
	 Created by Acuna on 15.05.2018
	*/
	
	import android.net.Uri;
	import android.os.Bundle;
	
	import com.jcraft.jsch.ChannelSftp;
	import com.jcraft.jsch.JSch;
	import com.jcraft.jsch.JSchException;
	import com.jcraft.jsch.Session;
	import com.jcraft.jsch.SftpATTRS;
	import com.jcraft.jsch.SftpException;
	
	import pro.acuna.andromeda.User;
	import pro.acuna.jabadaba.Arrays;
	import pro.acuna.jabadaba.Files;
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
		public User getUser (Bundle bundle) throws StorageException {
			
			try {
				return new User ().setName (storage.settings.getString (USER));
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
		
		@Override
		public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
			
			List<Item> files = new ArrayList<> ();
			String fullDir = prepRemoteFile (remoteDir);
			
			try {
				
				if (this.isDir (fullDir)) {
					
					Vector entries = channel.ls (fullDir);
					
					for (int i = 0; i < Int.size (entries); ++i) {
						
						ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) entries.get (i);
						
						String name = entry.getFilename ();
						
						if (!name.equals (".") && !name.equals ("..")) {
							
							Item item = storage.toItem (remoteDir, name)
																 .isDir (entry.getAttrs ().isDir ())
																 .setDirectLink (entry.toString ());
							
							if (item.show (mode)) files.add (item);
							
						}
						
					}
					
				}
				
			} catch (SftpException e) {
				throw new StorageException (storage, e);
			}
			
			return files;
			
		}
		
		private SftpATTRS getAttrs (String remoteFile) throws SftpException {
			return channel.lstat (remoteFile);
		}
		
		@Override
		public long getSize (String remoteFile) throws StorageException {
			
			try {
				return getAttrs (remoteFile).getSize ();
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public boolean isDir (String remoteFile) throws StorageException {
			
			try {
				return getAttrs (remoteFile).isDir ();
			} catch (SftpException e) {
				
				if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
					throw new StorageException (storage, e, remoteFile); // Вообще ничего не смогли получить
				
			}
			
			return false;
			
		}
		
		@Override
		public boolean isExists (String remoteFile) throws StorageException {
			
			try {
				return (getAttrs (remoteFile) != null);
			} catch (SftpException e) {
				
				if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE)
					throw new StorageException (storage, e, remoteFile);
				else
					return false;
				
			}
			
		}
		
		@Override
		public void chmod (int chmod, String remoteFile) throws StorageException {
			
			try {
				channel.chmod (chmod, remoteFile);
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public int makeDir (String remoteFile) throws StorageException {
			
			try {
				
				if (!this.isExists (remoteFile)) {
					
					List<String> parts = Arrays.explode ("/", remoteFile);
					
					String path = "";
					
					for (int i = 0; i < Int.size (parts); ++i) {
						
						if (i > 0) path += "/";
						path += parts.get (i);
						
						if (!this.isDir (path))
							channel.mkdir (path);
						
					}
					
					return 1;
					
				} else return 2;
				
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			try {
				
				if (force (remoteFile, force)) {
					
					if (makeDir) this.makeDir (Files.getPath (remoteFile));
					channel.put (stream, remoteFile, ChannelSftp.OVERWRITE);
					
				}
				
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void close () throws StorageException {
			
			if (session != null) session.disconnect ();
			if (channel != null) channel.quit ();
			
		}
		
		@Override
		public InputStream getStream (String remoteFile, String link) throws StorageException {
			
			try {
				
				OutputStream outputStream = new ByteArrayOutputStream ();
				channel.get (remoteFile, outputStream);
				
				return Streams.toInputStream (outputStream);
				
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void delete (String remoteFile) throws StorageException {
			
			try {
				
				if (this.isDir (remoteFile)) {
					
					Vector entries = channel.ls (remoteFile);
					
					for (int i = 0; i < Int.size (entries); ++i) {
						
						ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) entries.get (i);
						
						String name = entry.getFilename ();
						
						if (!name.equals (".") && !name.equals ("..")) {
							
							name = remoteFile + "/" + name;
							
							if (!this.isDir (name))
								deleteFile (name);
							else
								delete (name);
							
						}
						
					}
					
					channel.rmdir (remoteFile);
					
				} else deleteFile (remoteFile);
				
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		private void deleteFile (String remoteFile) throws StorageException {
			
			try {
				channel.rm (remoteFile);
			} catch (SftpException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException {
			
			try {
				
				ChannelSftp jSchChannelWrite = (ChannelSftp) session.openChannel (getName ());
				jSchChannelWrite.connect ();
				
				PipedInputStream in = new PipedInputStream (2048);
				PipedOutputStream out = new PipedOutputStream (in);
				
				channel.get (remoteSrcFile, out);
				jSchChannelWrite.put (in, remoteDestFile);
				
			} catch (SftpException | JSchException | IOException e) {
				throw new StorageException (storage, e, remoteSrcFile);
			}
			
		}
		
		@Override
		public Uri getUri (String remoteFile) throws StorageException {
			
			try {
				return Uri.parse (getName () + "://" + storage.settings.getString (SERVER) + "/" + remoteFile);
			} catch (JSONException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public String getDirectLink (String remoteFile) throws StorageException {
			return remoteFile;
		}
		
	}