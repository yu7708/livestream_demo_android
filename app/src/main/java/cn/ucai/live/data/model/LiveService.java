package cn.ucai.live.data.model;

import java.util.List;

import cn.ucai.live.data.model11.Gift;
import cn.ucai.live.utils.I;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Administrator on 2017/4/14.
 */

public interface LiveService {
    //// FIXME: 2017/4/14 先提供调用的方法尾址
    @GET("live/getAllGifts")
    Call<String> getAllGifts();

    @GET("findUserByUserName")
    Call<String> loadUserInfo(@Query(I.User.USER_NAME) String uesrname);

    @GET("live/createChatRoom")
    Call<String> createChatRoom(
            @Query("auth")String auth,
            @Query("name")String name,
            @Query("description")String description,
            @Query("owner")String owner,
            @Query("maxusers")int maxuser,
            @Query("members")String members
    );
}
