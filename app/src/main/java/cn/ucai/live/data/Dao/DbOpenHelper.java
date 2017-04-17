package cn.ucai.live.data.Dao;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.ucai.live.LiveApplication;

/**
 * Created by Administrator on 2017/4/15.
 */

public class DbOpenHelper extends SQLiteOpenHelper{
    //先创建版本号,方便改动后,升级,不然程序会崩溃
    private static final int DATABASE_VERSION = 1;
    //然后建立个实例的常量
    private static DbOpenHelper instance;
    //创建表
    private static final String GIFT_TABLE_CREATE = "CREATE TABLE "
            + GiftDao.GIFT_TABLE_NAME + " ("
            + GiftDao.GIFT_COLUMN_NAME + " TEXT, "
            + GiftDao.GIFT_COLUMN_URL + " TEXT, "
            + GiftDao.GIFT_COLUMN_PRICE + " INTEGER, "
            + GiftDao.GIFT_COLUMN_ID + " INTEGER PRIMARY KEY);";

    //提供全参的样子
    private DbOpenHelper(Context context){
        super(context,getUserDatabaseName(),null,DATABASE_VERSION);
    }

    //实例化instance
    public static DbOpenHelper getInstance(Context context){
        if(instance==null){
            //返回的第二个参数我不是很懂
            instance=new DbOpenHelper(context.getApplicationContext());
        }
        return instance;
    }
    //返回从Application中得到的包名
    private static String getUserDatabaseName() {
        return LiveApplication.getInstance().getPackageName() + "_demo.db";
    }
    public DbOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DbOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //这一句就是SQLiteDatabase执行数据库的创建
        db.execSQL(GIFT_TABLE_CREATE);
    }
    //升级版本,这是创建自带的,给后续开发用的
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    //然后就是关闭数据库
    public void closeDB(){
        if(instance!=null){
            //// FIXME: 2017/4/15 数据库操作
            //这是数据库的一个操作,现在也不能细看,先留下标记
            SQLiteDatabase db=instance.getWritableDatabase();
            db.close();
            instance=null;
        }
    }
}
