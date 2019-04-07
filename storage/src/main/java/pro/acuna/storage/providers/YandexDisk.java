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
	import pro.acuna.jabadaba.Arrays;
	import pro.acuna.jabadaba.HttpRequest;
	import pro.acuna.jabadaba.Int;
	import pro.acuna.jabadaba.Net;
	import pro.acuna.jabadaba.exceptions.HttpRequestException;
	import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
	import pro.acuna.storage.Item;
	import pro.acuna.storage.Provider;
	import pro.acuna.storage.Storage;
	import pro.acuna.storage.StorageException;
	
	public class YandexDisk extends Provider {
		
		private String API_URL = "https://cloud-api.yandex.net/v1/disk";
		
		public YandexDisk () {}
		
		private YandexDisk (Storage storage) throws StorageException {
			
			super (storage);
			
			try {
				this.storage.settings.put ("useragent", "api-explorer-client");
			} catch (JSONException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		@Override
		public Provider newInstance (Storage storage) throws StorageException {
			return new YandexDisk (storage);
		}
		
		@Override
		public Provider auth () throws StorageException {
			
			try {
				
				String refToken = storage.utils.getPref ("refresh_token");
				
				if (refToken != null && !refToken.equals ("")) {
					
					HttpRequest request = new HttpRequest (HttpRequest.METHOD_POST, "https://oauth.yandex.ru/token");
					
					Map<String, Object> data = new LinkedHashMap<> ();
					
					data.put ("client_id", storage.settings.getString ("client_id"));
					data.put ("client_secret", storage.settings.getString ("client_secret"));
					data.put ("grant_type", "refresh_token");
					data.put ("refresh_token", refToken);
					
					request.send (data);
					
					authData = new JSONObject (request.getContent ());
					
				}
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e);
			}
			
			return this;
			
		}
		
		@Override
		public String getName () {
			return "yadisk";
		}
		
		@Override
		public User getUser (Bundle bundle) throws StorageException {
			
			try {
				
				Map<String, Object> data = new LinkedHashMap<> ();
				
				data.put ("format", "json");
				data.put ("oauth_token", bundle.getString ("access_token"));
				
				HttpRequest request = new HttpRequest (HttpRequest.METHOD_GET, "https://login.yandex.ru/info", data);
				
				String content = request.getContent ();
				
				if (!content.equals ("")) {
					
					JSONObject result = new JSONObject (content);
					
					if (result.has ("login"))
						return new User ().setName (result.getString ("login"));
					else
						throw new StorageException (storage, result.toString ());
					
				} else throw new StorageException (storage, content);
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		@Override
		public JSONObject setProviderItems (JSONObject data) throws StorageException {
			
			try {
				
				data.put (Storage.ITEM_TITLE, "Яндекс.Диск");
				
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
				data.put ("response_type", "token");
				data.put ("force_confirm", true);
				
				return "https://oauth.yandex.ru/authorize" + Net.urlQueryEncode (data);
				
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
			return "app:";
		}
		
		@Override
		public List<Item> list (String remoteDir, int mode) throws StorageException, OutOfMemoryException {
			
			List<Item> files = new ArrayList<> ();
			
			try {
				
				String fullDir = prepRemoteFile (remoteDir);
				JSONArray items = getAttrs (fullDir).getJSONObject ("_embedded").getJSONArray ("items");
				
				for (int i = 0; i < Int.size (items); ++i) {
					
					JSONObject data = items.getJSONObject (i);
					
					Item item = storage.toItem (remoteDir, data.getString ("name"))
														 .isDir (data.getString ("type").equals ("dir"));
					
					if (!item.isDir ()) item.setDirectLink (data.getString ("href"));
					
					if (item.show (mode)) files.add (item);
					
				}
				
			} catch (HttpRequestException | JSONException e) {
				throw new StorageException (storage, e, remoteDir);
			}
			
			return files;
			
		}
		
		@Override
		public long getSize (String remoteFile) throws StorageException {
			
			try {
				
				long size = 0;
				JSONObject data = getAttrs (remoteFile);
				
				if (data.has ("_embedded")) {
					
					JSONArray items = data.getJSONArray ("_embedded");
					
					for (int i = 0; i < Int.size (items); ++i) {
						
						JSONObject item = items.getJSONObject (i);
						size += item.getLong ("size");
						
					}
					
				} else size = data.getLong ("size");
				
				return size;
				
			} catch (HttpRequestException | JSONException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		private JSONObject getAttrs (String remoteFile) throws StorageException, HttpRequestException {
			
			try {
				
				if (remoteFile.equals (getRootDir ())) remoteFile += "/";
				
				Map<String, Object> params = new LinkedHashMap<> ();
				
				params.put ("path", remoteFile);
				
				HttpRequest request = request (HttpRequest.METHOD_GET, API_URL + "/resources", params);
				
				JSONObject output = new JSONObject (request.getContent ());
				
				if (output.has ("resource_id") || output.has ("public_id"))
					return output;
				else
					return new JSONObject ();
				
			} catch (JSONException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public boolean isDir (String remoteFile) throws StorageException {
			
			try {
				
				JSONObject data = getAttrs (remoteFile);
				return (Int.size (data) > 0 && data.getString ("type").equals ("dir"));
				
			} catch (JSONException e) {
				throw new StorageException (storage, e, remoteFile);
			} catch (HttpRequestException e) {
				return false;
			}
			
		}
		
		@Override
		public boolean isExists (String remoteFile) throws StorageException {
			
			try {
				return (Int.size (getAttrs (remoteFile)) > 0);
			} catch (HttpRequestException e) {
				return false;
			}
			
		}
		
		@Override
		public int makeDir (String remoteFile) throws StorageException {
			
			try {
				
				if (!this.isExists (remoteFile)) {
					
					List<String> parts = Arrays.explode ("/", remoteFile);
					
					String path = "";
					
					for (int i = 0; i < (Int.size (parts) - 1); ++i) {
						
						path += parts.get (i);
						if (i < Int.size (parts) - 1) path += "/";
						
						if (!this.isDir (path)) {
							
							Map<String, Object> params = new LinkedHashMap<> ();
							
							params.put ("path", path);
							
							HttpRequest request = request (HttpRequest.METHOD_PUT, API_URL + "/resources", params);
							
							if (request.getCode () != 201)
								throw new StorageException (storage, error (request.getContent ()));
							
						}
						
					}
					
					return 1;
					
				} else return 2;
				
			} catch (JSONException | HttpRequestException | OutOfMemoryException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		private String error (String body) throws JSONException {
			return error (new JSONObject (body));
		}
		
		private String error (JSONObject body) throws JSONException {
			return body.getString ("description");
		}
		
		@Override
		public void write (InputStream stream, String remoteFile, boolean force, boolean makeDir) throws StorageException {
			
			try {
				
				if (force (remoteFile, force)) {
					
					if (makeDir) this.makeDir (remoteFile);
					
					Map<String, Object> params = new LinkedHashMap<> ();
					
					params.put ("path", remoteFile);
					params.put ("overwrite", force);
					
					String link = getDirectLink ("resources/upload", params);
					HttpRequest request = request (HttpRequest.METHOD_PUT, link, new LinkedHashMap<String, Object> ());
					
					request.send (stream);
					
					if (request.getCode () != 201)
						throw new StorageException (storage, request.getMessageCode ());
					
					//yadiskApi.publish (remoteFile);
					
				}
				
			} catch (HttpRequestException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void delete (String remoteFile) throws StorageException {
			
			try {
				
				Map<String, Object> params = new LinkedHashMap<> ();
				
				params.put ("path", remoteFile);
				params.put ("permanently", delPermanent);
				
				HttpRequest request = request (HttpRequest.METHOD_DELETE, API_URL + "/resources", params);
				
				if (request.getCode () != 202 && request.getCode () != 204)
					throw new StorageException (storage, request.getCode () + " " + request.getMessage ());
				
			} catch (HttpRequestException e) {
				throw new StorageException (storage, e, remoteFile);
			}
			
		}
		
		@Override
		public void copy (String remoteSrcFile, String remoteDestFile) throws StorageException { // TODO
			
		}
		
		@Override
		public boolean isAuthenticated () throws StorageException {
			return storage.utils.has ("access_token");
		}
		
		private HttpRequest request (String method, String url, Map<String, Object> params) throws StorageException {
			
			try {
				
				HttpRequest request = initRequest (new HttpRequest (method, url, params));
				
				request.setHeader ("Authorization", "OAuth " + storage.utils.getPref ("access_token"));
				request.setUserAgent (storage.settings.getString ("useragent"));
				
				return request;
				
			} catch (JSONException | HttpRequestException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
		@Override
		public String getDirectLink (String remoteFile) throws StorageException {
			
			Map<String, Object> params = new LinkedHashMap<> ();
			
			params.put ("path", remoteFile);
			
			return getDirectLink ("resources/download", params);
			
		}
		
		private String getDirectLink (String url, Map<String, Object> params) throws StorageException {
			
			try {
				
				HttpRequest request = request (HttpRequest.METHOD_GET, API_URL + "/" + url, params);
				
				JSONObject result = new JSONObject (request.getContent ());
				
				if (request.isOK ())
					return result.getString ("href");
				else
					throw new StorageException (storage, error (result));
				
			} catch (HttpRequestException | JSONException | OutOfMemoryException e) {
				throw new StorageException (storage, e);
			}
			
		}
		
	}