package videoplayer.stromdzh.com.myalivideoplayer;

import android.app.Application;
import android.content.Context;

/**
 * author : dzh .
 * date   : 2018/1/26
 * desc   : 
 */
public    class MyApplication extends Application {

    public static Context mContxt=new MyApplication();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static Application  getApplication(){
        return (Application) mContxt;
    }
}
