## Storager
Storager is a lightweight but very powerful library for clouds and local storages for Android
<br>

**Supported storages**

- [x] Google Drive
- [x] Dropbox
- [x] Яндекс.Диск
- [x] SFTP
- [x] SDCard
<br>

**Advantages**

- Simple to use
- Lightweight
- Doesn't use any side libraries, only native Java `HttpURLConnection` to access the clouds services REST APIs
<br>

**Install**

Add the following dependency in `dependencies` section of your module `build.gradle` file:

    api 'pro.acuna:storager:2.3'
<br>

**Usage**

~~~java
Storage storage = new Storage ();

JSONObject data = new JSONObject (), storagesData = new JSONObject ();

data.put ("key", "DROPBOX_KEY");
data.put ("secret", "DROPBOX_SECRET");
data.put ("redirect_url", "DROPBOX_REDIRECT_URL");

storagesData.put ("dropbox", data);

data = new JSONObject ();

data.put ("key", "YADISK_KEY");
data.put ("secret", "YADISK_SECRET");
data.put ("redirect_url", "YADISK_REDIRECT_URL");

storagesData.put ("yadisk", data);

storage.init ("dropbox", storagesData);
~~~
    
Set storage auth data<br>
<br>

    String storage.read (String remoteFile)
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

    void storage.copy (File localFile, String remoteFile)
    void storage.copy (File localFile, String remoteFile, boolean force)
Copy file or directory in storage<br>
<br>

    void storage.copy (URL localFile, String remoteFile)
    void storage.copy (URL localFile, String remoteFile, boolean force)
Get file from URL and put it in storage<br>
<br>

    void storage.copy (String remoteFile, File localFile)
Copy file from storage to local filesystem (download)<br>
<br>

    void storage.copy (String remoteFile, OutputStream stream)
Copy file from storage to `OutputStream`<br>
<br>

    void storage.copy (String remoteSrcFile, String remoteDestFile)
Copy one file to another in current storage<br>
<br>

    void storage.delete (String remoteFile)
Delete file or folder from storage<br>
<br>
