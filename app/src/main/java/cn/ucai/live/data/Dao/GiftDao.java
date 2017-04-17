package cn.ucai.live.data.Dao;

import java.util.List;
import java.util.Map;

import cn.ucai.live.data.model11.Gift;

/**
 * Created by Administrator on 2017/4/15.
 */

public class GiftDao {
    public static final String GIFT_TABLE_NAME = "t_superwechat_gift";
    public static final String GIFT_COLUMN_ID = "m_gift_id";
    public static final String GIFT_COLUMN_NAME = "m_gift_name";
    public static final String GIFT_COLUMN_URL = "m_gift_url";
    public static final String GIFT_COLUMN_PRICE = "m_gift_price";
    public GiftDao(){

    }
    //拿到在DbManager中定义的礼物列表
    public void saveAppGiftList(List<Gift> giftList) {
        DbManager.getInstance().saveAppGiftList(giftList);
    }
    public Map<Integer, Gift> getAppGiftList() {
        return DbManager.getInstance().getAppGiftList();
    }

}
