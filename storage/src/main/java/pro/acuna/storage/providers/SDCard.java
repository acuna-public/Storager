	package pro.acuna.storage.providers;
	
	import android.os.Bundle;
	
	import org.json.JSONException;
	import org.json.JSONObject;
	
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.IOException;
	import java.io.InputStream;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.Map;
	
	import pro.acuna.andromeda.OS;
	import pro.acuna.andromeda.User;
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
		public User getUser (Bundle bundle) throws StorageException {
			
			try {
				return new User ().setName (storage.getProviderItems ().getString (Storage.ITEM_TITLE));
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
		
		private File destFile (String file) {
			return new File (file);
		}
		
		@Override
		public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
			
			String fullDir = prepRemoteFile (remoteDir);
			String[] files = destFile (fullDir).list ();
			
			List<Item> output = new ArrayList<> ();
			
			if (files != null) {
				
				for (String file : files) {
					
					Item item = storage.toItem (remoteDir, file)
														 .isDir (destFile (file).isDirectory ())
														 .setDirectLink (prepRemoteFile (file));
					
					if (item.show (mode)) output.add (item);
					
				}
				
			}
			
			return output;
			
		}
		
		@Override
		public long getSize (String remoteFile) throws StorageException {
			return Int.size (destFile (remoteFile));
		}
		
		@Override
		public boolean isDir (String remoteFile) throws StorageException {
			return destFile (remoteFile).isDirectory ();
		}
		
		@Override
		public boolean isExists (String remoteFile) throws StorageException {
			return destFile (remoteFile).exists ();
		}
		
		@Override
		public int makeDir (String remoteFile) throws StorageException {
			
			try {
				return Files.makeDir (destFile (remoteFile));
			} catch (IOException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			try {
				
				if (force (remoteFile, force))
					Files.copy (stream, destFile (remoteFile));
				
			} catch (IOException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void delete (String remoteFile) throws StorageException {
			
			try {
				Files.delete (destFile (remoteFile));
			} catch (IOException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException {
			
			//this.copy (destFile (remoteSrcFile), remoteDestFile, true);
			delete (remoteSrcFile);
			
		}
		
		@Override
		public InputStream getStream (String remoteFile, String link) throws StorageException {
			
			try {
				return new FileInputStream (destFile (remoteFile));
			} catch (IOException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public String getDirectLink (String remoteFile) throws StorageException {
			return remoteFile;
		}
		
	}