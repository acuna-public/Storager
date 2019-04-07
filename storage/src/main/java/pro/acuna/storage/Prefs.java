	package pro.acuna.storage;
	/*
	 Created by Acuna on 17.05.2018
	*/
	
	import java.util.Map;
	
	import pro.acuna.andromeda.Crypto;
	import pro.acuna.jabadaba.Arrays;
	
	public class Prefs {
		
		private Storage storage;
		
		Prefs (Storage storage) {
			this.storage = storage;
		}
		
		pro.acuna.andromeda.Prefs loadPrefs () {
			return new pro.acuna.andromeda.Prefs (storage.context, storage.prefsName);
		}
		
		private String prefKey (String key) {
			return storage.type + "_" + storage.id + "_" + key;
		}
		
		void setPref (String key, int value) {
			storage.prefs.set (prefKey (key), value);
		}
		
		void setPref (String key, Object value) throws StorageException {
			
			try {
				storage.prefs.set (prefKey (key), value, !Arrays.contains (key, storage.provider.getDecryptedKeys ()));
			} catch (Crypto.EncryptException e) {
				throw new StorageException (e);
			}
			
		}
		
		public String getPref (String key) throws StorageException {
			return getPref (key, "");
		}
		
		public boolean has (String key) throws StorageException {
			return (!getPref (key).equals (""));
		}
		
		void apply () {
			storage.prefs.editor.apply ();
		}
		
		Object getPref (String key, Map<String, Object> data) throws StorageException {
			
			Object value = data.get (key);
			
			if (value instanceof Integer)
				return getPref (key, (int) value);
			else
				return getPref (key, value.toString ());
			
		}
		
		String getPref (String key, String defValue) throws StorageException {
			
			try {
				return storage.prefs.get (prefKey (key), defValue, !Arrays.contains (key, storage.provider.getDecryptedKeys ()));
			} catch (Crypto.DecryptException e) {
				throw new StorageException (e);
			}
			
		}
		
		int getPref (String key, int defValue) {
			return storage.prefs.get (prefKey (key), defValue);
		}
		
		long getPref (String key, long defValue) {
			return storage.prefs.get (prefKey (key), defValue);
		}
		
		boolean getPref (String key, boolean defValue) {
			return storage.prefs.get (prefKey (key), defValue);
		}
		
	}