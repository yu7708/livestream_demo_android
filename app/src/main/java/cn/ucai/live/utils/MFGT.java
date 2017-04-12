package cn.ucai.live.utils;

import android.app.Activity;
import android.content.Intent;

import cn.ucai.live.R;
import cn.ucai.live.ui.activity.MainActivity;

/**
 * Created by Administrator on 2017/3/16.
 */

public class MFGT {
    public static void finish(Activity activity){
        activity.finish();
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void startActivity(Activity activity,Class cls){
        activity.startActivity(new Intent(activity,cls));
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void startActivity(Activity activity,Intent intent){
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void gotoMain(Activity activity){
        startActivity(activity, MainActivity.class);
    }
    public static void startActivityForResult(Activity activity,Intent intent,int requestCode){
        activity.startActivityForResult(intent,requestCode);
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }

}
