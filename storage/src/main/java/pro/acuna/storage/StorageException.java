	package pro.acuna.storage;
	/*
	 Created by Acuna on 31.07.2017
	*/
	
	import org.json.JSONObject;
	
	public class StorageException extends Exception {
		
		private Storage storage;
		private String remoteFile;
		
		public StorageException (String mess) {
			super (mess);
		}
		
		public StorageException (Exception e) {
			super (e);
		}
		
		public StorageException (Storage storage, Exception e) {
			this (storage, e.getMessage ());
		}
		
		public StorageException (Storage storage, Exception e, String remoteFile) {
			
			this (storage, e);
			this.remoteFile = remoteFile;
			
		}
		
		public StorageException (Storage storage, JSONObject result) {
			this (storage, storage.setError (result));
		}
		
		public StorageException (Storage storage, String mess) {
			
			this (mess);
			this.storage = storage;
			
		}
		
		public String getRemoteFile () {
			return remoteFile;
		}
		
		public String getType () {
			return storage.getName ();
		}
		
		public String getTitle () {
			
			try {
				
				JSONObject data = storage.getProviderItems ();
				return data.optString (Storage.ITEM_TITLE);
				
			} catch (StorageException e) {
				return null;
			}
			
		}
		
		@Override
		public Exception getCause () {
			return (Exception) super.getCause ();
		}
		
	}