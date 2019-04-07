	package pro.acuna.storage;
	/*
	 Created by Acuna on 18.12.2018
	*/
	
	import java.net.MalformedURLException;
	import java.net.URL;
	import java.util.List;
	
	import pro.acuna.jabadaba.Files;
	import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
	
	public class Item {
		
		public Storage storage;
		private String file;
		
		public Item (Storage storage, String file) {
			
			this.storage = storage;
			this.file = file;
			
		}
		
		private boolean isDir = false;
		
		public Item isDir (boolean isDir) {
			
			this.isDir = isDir;
			return this;
			
		}
		
		public boolean isDir () {
			return isDir;
		}
		
		private Boolean isImage;
		
		public Item isImage (boolean isImage) {
			
			this.isImage = isImage;
			return this;
			
		}
		
		public boolean isImage () {
			
			if (isImage == null) isImage = Files.isImageByExt (file);
			return isImage;
			
		}
		
		public final boolean show (int mode) {
			
			return (
						 (mode == 1 && !isDir ()) || // Только файлы
						 (mode == 2 &&	isDir ()) || // Только папки
							mode == 0 // Все
			);
			
		}
		
		public List<Item> list () throws StorageException, OutOfMemoryException {
			return list (0);
		}
		
		public List<Item> list (int type) throws StorageException, OutOfMemoryException {
			return storage.list (file, type);
		}
		
		private long size = -1;
		
		protected Item setSize (long size) {
			
			this.size = size;
			return this;
			
		}
		
		public long getSize () throws StorageException {
			
			if (size < 0) size = storage.getSize (file);
			return size;
			
		}
		
		public String getFile () {
			return file;
		}
		
		private String id;
		
		public Item setId (String id) {
			
			this.id = id;
			return this;
			
		}
		
		public String getId () {
			return id;
		}
		
		public String getPath () {
			
			try {
				return storage.provider.prepRemoteFile (file);
			} catch (StorageException e) {
				return null;
			}
			
		}
		
		public String toString () {
			return getFile ();
		}
		
		public Item getParent () {
			return storage.toItem (Files.getPath (file)).isDir (true);
		}
		
		public String read () throws StorageException, OutOfMemoryException {
			return storage.read (file);
		}
		
		private URL link;
		
		public Item setDirectLink (String link) throws StorageException {
			
			try {
				this.link = new URL (link);
			} catch (MalformedURLException e) {
				throw new StorageException (storage, e);
			}
			
			return this;
			
		}
		
		public URL getDirectLink () throws StorageException {
			
			if (link == null) link = storage.getDirectLink (file);
			return link;
			
		}
		
		private int width, height;
		
		public Item setWidth (int size) {
			
			this.width = size;
			return this;
			
		}
		
		public int getWidth () {
			return width;
		}
		
		public Item setHeight (int size) {
			
			this.height = size;
			return this;
			
		}
		
		public int getHeight () {
			return height;
		}
		
	}