# Storager
Storager is a elegant and lightweight library for clouds and local storages:

- Google Drive
- Dropbox
- SFTP
- Яндекс.Диск
- SDCard<br>
<br>

**Usage**

	Storage storage = new Storage ();
	
	JSONObject data = new JSONObject (), storagesData = new JSONObject ();
	
    data.put ("key", "DROPBOX_KEY");
    data.put ("secret", "DROPBOX_SECRET");
    data.put ("redirect_url", "DROPBOX_REDIRECT_URL");
	
    storagesData.put ("dropbox", data)
  
    data.put ("key", "YADISK_KEY");
    data.put ("secret", "YADISK_SECRET");
    data.put ("redirect_url", "YADISK_REDIRECT_URL");
	
    storagesData.put ("yadisk", data)
  
    storage.init ("yadisk", storagesData);
    
Set storage auth data<br>
<br>

    String storager.read (String remoteFile)
Read file from storage to string<br>
<br>

    List<Item> storage.list (String remoteDir)
Get list of items (files) of storage directory<br>
<br>

    boolean storage.isDir (String remoteFile)
Check if it is a directory<br>
<br>

    boolean storage.isExists (String remoteFile)
Check if file or directory exists in storage<br>
<br>

    void storage.makeDir (String remoteDir)
Create a directory in storage. Use slashes to create a subfolders (dir1/dir2)<br>
<br>

    void storage.write (Object|InputStream stream, String remoteFile)
Write a stringified `Object` or `InputStream` to file in storage<br>
<br>

    void storage.copy (File localFile, String remoteFile[, boolean force = true])
Copy file or directory in storage<br>
<br>

    void storage.copy (URL localFile, String remoteFile[, boolean force = true])
Get file from URL and put it in storage<br>
<br>

    void storager.copy (String remoteFile, File localFile)
Copy file from storage to local filesystem (download)<br>
<br>

    void storager.copy (String remoteFile, OutputStream stream)
Copy file from storage to `OutputStream`<br>
<br>

    void copy (String remoteSrcFile, String remoteDestFile)
Copy one file to another in current storage<br>
<br>

    void storager.delete (String remoteFile)
Delete file or folder from storage<br>
<br>
