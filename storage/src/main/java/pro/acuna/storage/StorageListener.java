  package pro.acuna.storage;
  /*
   Created by Acuna on 17.02.2019
  */
  
  public interface StorageListener {
    
    void onAuthSuccess () throws StorageException;
    void onProgress (long i, long total);
    void onFinish (int code);
    void onError (int code, String result);
    
  }