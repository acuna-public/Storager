	package pro.acuna.storage.providers;
	
	import android.os.Bundle;
	
	import org.json.JSONArray;
	import org.json.JSONException;
	import org.json.JSONObject;
	
	import java.io.InputStream;
	import java.io.UnsupportedEncodingException;
	import java.util.ArrayList;
	import java.util.LinkedHashMap;
	import java.util.List;
	import java.util.Map;
	
	import pro.acuna.andromeda.User;
	import pro.acuna.jabadaba.HttpRequest;
	import pro.acuna.jabadaba.Int;
	import pro.acuna.jabadaba.Net;
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
			
			try {
				this.storage.settings.put ("useragent", "api-explorer-client");
			} catch (JSONException e) {
				throw new StorageException (storage, e);
			}
			
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
		public User getUser (Bundle bundle) throws StorageException {
			
			try {
				
				token = bundle.getString ("access_token");
				HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/users/get_current_account");
				
				request.send ((JSONObject) null);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (!output.has ("error_summary")) {
					
					JSONObject user = output.getJSONObject ("name");
					
					return new User ().setName (output.getString ("email"))
														.setFullName (user.getString ("display_name"))
														.setAvatar (output.getString ("profile_photo_url"));
					
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
				
				if (token == null) token = storage.utils.getPref ("access_token");
				
				request.setUserAgent (storage.settings.getString ("useragent"));
				request.setHeader ("Authorization", "Bearer " + token);
				request.isJSON (true);
				
				return request;
				
			} catch (JSONException | HttpRequestException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		@Override
		public boolean isAuthenticated () throws StorageException {
			return storage.utils.has ("access_token");
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
		
		private JSONArray list (String remoteDir) throws StorageException, OutOfMemoryException {
			
			try {
				
				HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/list_folder");
				
				JSONObject data = new JSONObject ();
				
				if (remoteDir.equals ("/")) remoteDir = "";
				
				data.put ("path", remoteDir);
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
				throw new StorageException (storage, e, remoteDir);
			}
			
		}
		
		@Override
		public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
			
			List<Item> files = new ArrayList<> ();
			String fullDir = prepRemoteFile (remoteDir);
			
			try {
				
				JSONArray items = list (fullDir);
				
				for (int i = 0; i < Int.size (items); ++i) {
					
					JSONObject data = items.getJSONObject (i);
					
					Item item = storage.toItem (data.getString ("path_display"))
														 .isDir (data.getString (".tag").equals ("folder"))
														 .isImage (data.has ("media_info") && data.getJSONObject ("media_info").getJSONObject ("metadata").getString (".tag").equals ("photo"));
					
					if (item.show (mode)) files.add (item);
					
				}
				
			} catch (JSONException e) {
				throw new StorageException (storage, e, remoteDir);
			}
			
			return files;
			
		}
		
		@Override
		public long getSize (String remoteFile) throws StorageException {
			return 0; // TODO
		}
		
		private JSONObject dropboxAttr (String remoteFile) throws StorageException {
			
			try {
				
				HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/get_metadata");
				
				JSONObject data = new JSONObject ();
				
				if (remoteFile.equals ("/")) remoteFile = "";
				
				data.put ("path", remoteFile);
				data.put ("include_media_info", false);
				data.put ("include_deleted", false);
				data.put ("include_has_explicit_shared_members", false);
				
				request.send (data);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (!output.has ("error_summary"))
					return output;
				else
					throw new StorageException (storage, setError (output));
				
			} catch (JSONException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			} catch (HttpRequestException e) {
				return new JSONObject ();
			}
			
		}
		
		@Override
		public boolean isDir (String remoteFile) throws StorageException {
			
			try {
				
				JSONObject metadata = dropboxAttr (remoteFile);
				return (metadata.getString (".tag").equals ("folder"));
				
			} catch (JSONException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public boolean isExists (String remoteFile) throws StorageException {
			
			try {
				return dropboxAttr (remoteFile).has (".tag");
			} catch (StorageException e) {
				return false;
			}
			
		}
		
		@Override
		public int makeDir (String remoteFile) throws StorageException {
			
			try {
				
				if (!this.isExists (remoteFile)) {
					
					HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/create_folder_v2");
					
					JSONObject data = new JSONObject ();
					
					data.put ("path", remoteFile);
					data.put ("autorename", false);
					
					request.send (data);
					
					JSONObject output = new JSONObject (request.getContent ());
					
					if (!output.has ("error_summary"))
						return 1;
					else
						throw new StorageException (storage, output.toString ());
					
				} else return 2;
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			try {
				
				HttpRequest request = request (HttpRequest.METHOD_POST, "https://content.dropboxapi.com/2/files/upload");
				
				JSONObject data = new JSONObject ();
				
				data.put ("path", remoteFile);
				data.put ("autorename", false);
				data.put ("mute", false);
				
				JSONObject mode = new JSONObject ();
				mode.put (".tag", "overwrite");
				
				data.put ("mode", mode);
				
				request.setHeader ("Dropbox-API-Arg", data);
				request.setContentType ("application/octet-stream");
				
				request.send (stream);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (output.has ("error_summary"))
					throw new StorageException (storage, setError (output));
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
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
		public String getDirectLink (String remoteFile) throws StorageException {
			
			try {
				
				HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/get_temporary_link");
				
				JSONObject data = new JSONObject ();
				
				data.put ("path", remoteFile);
				
				request.send (data);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (output.has ("error_summary"))
					throw new StorageException (storage, setError (output));
				else
					return output.getString ("link");
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void delete (String remoteFile) throws StorageException {
			
			try {
				
				if (this.isDir (remoteFile)) {
					
					JSONArray items = list (remoteFile);
					
					for (int i = 0; i < Int.size (items); ++i) {
						
						JSONObject data = items.getJSONObject (i);
						String name = data.getString ("path_display");
						
						if (!data.getString (".tag").equals ("folder"))
							deleteFile (name);
						else
							delete (name);
						
					}
					
				}
				
				deleteFile (remoteFile);
				
			} catch (JSONException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		private void deleteFile (String remoteFile) throws StorageException {
			
			try {
				
				if (this.isExists (remoteFile)) {
					
					HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/delete_v2");
					
					JSONObject data = new JSONObject ();
					
					data.put ("path", remoteFile);
					
					request.send (data);
					
					JSONObject output = new JSONObject (request.getContent ());
					
					if (output.has ("error_summary"))
						throw new StorageException (storage, setError (output));
					
				}
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException {
			
			try {
				
				//this.makeDir (remoteDestFile);
				
				HttpRequest request = request (HttpRequest.METHOD_POST, API_URL + "/files/copy_v2");
				
				JSONObject data = new JSONObject ();
				
				data.put ("from_path", remoteSrcFile);
				data.put ("to_path", remoteDestFile);
				data.put ("autorename", false);
				
				request.send (data);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (output.has ("error_summary"))
					throw new StorageException (storage, setError (output));
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteSrcFile);
			}
			
		}
		
	}