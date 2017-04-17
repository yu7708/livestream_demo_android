package cn.ucai.live.data.Dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import cn.ucai.live.LiveApplication;
import cn.ucai.live.data.model11.Gift;

/**
 * Created by Administrator on 2017/4/15.
 */

public class DbManager {
    //管理数据库
    static private DbManager dbMgr = new DbManager();
    private DbOpenHelper dbHelper;
    //从application中拿到实例
    private DbManager(){
        dbHelper = DbOpenHelper.getInstance(LiveApplication.getInstance());
    }
    //这一个是加锁,用于实行单例的
    public static synchronized DbManager getInstance(){
        if(dbMgr == null){
            dbMgr = new DbManager();
        }
        return dbMgr;
    }
    //保存礼物列表和得到礼物列表
    /**
     * save gift list
     *
     * @param giftList
     */
    synchronized public void saveAppGiftList(List<Gift> giftList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(GiftDao.GIFT_TABLE_NAME, null, null);
            for (Gift gift:giftList) {
                ContentValues values = new ContentValues();
                if(gift.getId() != null)
                    values.put(GiftDao.GIFT_COLUMN_ID, gift.getId());
                if(gift.getGname() != null)
                    values.put(GiftDao.GIFT_COLUMN_NAME, gift.getGname());
                if(gift.getGurl() != null)
                    values.put(GiftDao.GIFT_COLUMN_URL, gift.getGurl());
                if(gift.getGprice() != null)
                    values.put(GiftDao.GIFT_COLUMN_PRICE, gift.getGprice());
                db.replace(GiftDao.GIFT_TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get gift list
     *
     * @return
     */
    synchronized public Map<Integer, Gift> getAppGiftList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<Integer, Gift> gifts = new Hashtable<>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + GiftDao.GIFT_TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                Gift gift = new Gift();
                gift.setId(cursor.getInt(cursor.getColumnIndex(GiftDao.GIFT_COLUMN_ID)));
                gift.setGprice(cursor.getInt(cursor.getColumnIndex(GiftDao.GIFT_COLUMN_PRICE)));
                gift.setGname(cursor.getString(cursor.getColumnIndex(GiftDao.GIFT_COLUMN_NAME)));
                gift.setGurl(cursor.getString(cursor.getColumnIndex(GiftDao.GIFT_COLUMN_URL)));
                gifts.put(gift.getId(),gift);
            }
            cursor.close();
        }
        return gifts;
    }
    synchronized public void closeDB(){
        if(dbHelper != null){
            dbHelper.closeDB();
        }
        dbMgr = null;
        }
}
