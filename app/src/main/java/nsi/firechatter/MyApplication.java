package nsi.firechatter;

import android.app.Application;

import com.twitter.sdk.android.core.Twitter;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Twitter.initialize(this);
    }

}
