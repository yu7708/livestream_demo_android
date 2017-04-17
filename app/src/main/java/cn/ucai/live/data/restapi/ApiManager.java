package cn.ucai.live.data.restapi;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import cn.ucai.live.LiveApplication;
import cn.ucai.live.data.model.LiveRoom;
import cn.ucai.live.data.model.LiveService;
import cn.ucai.live.data.model.User;
import cn.ucai.live.data.model11.Gift;
import cn.ucai.live.data.restapi.model.LiveStatusModule;
import cn.ucai.live.data.restapi.model.ResponseModule;
import cn.ucai.live.data.restapi.model.StatisticsType;
import com.hyphenate.chat.EMClient;
import java.io.IOException;
import java.util.List;

import cn.ucai.live.utils.I;
import cn.ucai.live.utils.L;
import cn.ucai.live.utils.Result;
import cn.ucai.live.utils.ResultUtils;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Query;

/**
 * Created by wei on 2017/2/14.
 */

public class ApiManager {
    private static final String TAG = "ApiManager";
    private String appkey;
    private ApiService apiService;
    private LiveService liveService;
    private static  ApiManager instance;

    private ApiManager(){
        //拼接,apikey,都拼接好后创建,apiService
        try {
            ApplicationInfo appInfo = LiveApplication.getInstance().getPackageManager().getApplicationInfo(
                    LiveApplication.getInstance().getPackageName(), PackageManager.GET_META_DATA);
            appkey = appInfo.metaData.getString("EASEMOB_APPKEY");
            appkey = appkey.replace("#","/");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("must set the easemob appkey");
        }

        //HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        //httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new RequestInterceptor())
                //.addInterceptor(httpLoggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://a1.easemob.com/"+appkey+"/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        apiService = retrofit.create(ApiService.class);
        //// FIXME: 2017/4/14 仿照上面的写请求的固定前半部分,并且导入自己写的类
        Retrofit liveRetrofit=new Retrofit.Builder()
                .baseUrl(I.SERVER_ROOT)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(httpClient)
                .build();
        liveService =liveRetrofit.create(LiveService.class);
        //--->
    }


    static class RequestInterceptor implements Interceptor {

        @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + EMClient.getInstance().getAccessToken())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(original.method(), original.body())
                    .build();
            okhttp3.Response response =  chain.proceed(request);
            return response;
        }
    }

    public static ApiManager get(){
        if(instance == null){
            instance = new ApiManager();
        }
        return instance;
    }
    //// FIXME: 2017/4/14 然后就是调用,这样大概就是让外面的方法调用的比较少
    public List<Gift> getAllGifts() throws LiveException {
        Call<String> call = liveService.getAllGifts();
        Result<List<Gift>> result = handleResponseCallToResultList(call, Gift.class);
        if(result!=null&&result.isRetMsg()){
            return result.getRetData();
        }
return null;
       /* allGifts.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
            //这里原来的String result现在从response中拿,其他的还是和以前一样
                String s = response.body();
                Result result = ResultUtils.getResultFromJson(s, Gift.class);
                if(result!=null&&result.isRetMsg()){
                    List<Gift> list = (List<Gift>) result.getRetData();
                    for(Gift gift:list){
                        L.e(TAG,"gift:"+gift);
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                L.e(TAG,"onFailure="+t.toString());
            }
        });*/
    }
    //// FIXME: 2017/4/14 异步加载,同步获取
    public com.hyphenate.easeui.domain.User loadUserInfo(String username) throws IOException, LiveException {
        com.hyphenate.easeui.domain.User user=null;
        Call<String> call = liveService.loadUserInfo(username);
        //上面的enqueue是异步操作,execute是同步的方法
        //异步就是两个不同的事同一个人同一时间做
        //同步就是两件事顺着做下来,比如你撕开方便面再吃
        /*Response<String> response = call.execute();//抛出异常
        String body = response.body();
        //同步和异步只是多转了一次
        Result result = ResultUtils.getResultFromJson(body, User.class);
        if(result!=null&&result.isRetMsg()){
            user= (com.hyphenate.easeui.domain.User) result.getRetData();
        }
        return user;*/
        Result<com.hyphenate.easeui.domain.User> result = handleResponseCallToResult(call, com.hyphenate.easeui.domain.User.class);
       if(result!=null&&result.isRetMsg()){
            result.getRetData();
       }
        return null;
    }
    //------>
    //// FIXME: 2017/4/14 然后觉的别人的写法好,我们就看着能不能自己仿写
    private <T> Result<T>handleResponseCallToResult(Call<String> responseCall,Class<T> clazz) throws LiveException{
        try {
            Response<String> response = responseCall.execute();
            if(!response.isSuccessful()){
                throw new LiveException(response.code(), response.errorBody().string());
            }
            String body = response.body();
            //同步和异步只是多转了一次
           // Result result = ResultUtils.getResultFromJson(body, User.class);
           // if(result!=null&&result.isRetMsg()){
             //   user= (com.hyphenate.easeui.domain.User) result.getRetData();
          // }
           //return response;
            return ResultUtils.getResultFromJson(body, clazz);
        } catch (IOException e) {
            throw new LiveException(e.getMessage());
        }
    }
    //还写一个list的
    private <T> Result<List<T>>handleResponseCallToResultList(Call<String> responseCall,Class<T> clazz) throws LiveException{
        try {
            Response<String> response = responseCall.execute();
            if(!response.isSuccessful()){
                throw new LiveException(response.code(), response.errorBody().string());
            }
            String body = response.body();
            //同步和异步只是多转了一次
            // Result result = ResultUtils.getResultFromJson(body, User.class);
            // if(result!=null&&result.isRetMsg()){
            //   user= (com.hyphenate.easeui.domain.User) result.getRetData();
            // }
            //return response;
            return ResultUtils.getListResultFromJson(body, clazz);
        } catch (IOException e) {
            throw new LiveException(e.getMessage());
        }
    }
    //
    //// FIXME: 2017/4/15 自己写的创建聊天室
    public String createLiveRoom(String auth, String name, String description, String owner,
                                 int maxuser, String members) throws IOException {
        //这一个是传参数调用,下面2个参数的是给外面用的调用
        Call<String> call = liveService.createChatRoom(auth, name, description, owner, maxuser, members);
       //将这个调用,执行,然后执行的就是原来的请求返回的String类型的result

            Response<String> response = call.execute();
            //这一步的返回就相当于原来的调用了,方法是不变的,加深是原来没看到的暴露出的越多
             return ResultUtils.getEMResultFromJson(response.body());
    }
    public String createLiveRoom(String name,String description) throws IOException {
        //这个调用的上面的方法,所以也是一个String的类型
        Log.e(TAG, "createLiveRoom: name="+name+",description="+description );
        return createLiveRoom("1IFgE",name,description,EMClient.getInstance().getCurrentUser(),
                300,EMClient.getInstance().getCurrentUser());
    }

    //f------>
    public LiveRoom createLiveRoom(String name, String description, String coverUrl) throws LiveException, IOException {
        return createLiveRoomWithRequest(name, description, coverUrl, null);
    }

    public LiveRoom createLiveRoom(String name, String description, String coverUrl, String liveRoomId) throws LiveException, IOException {
        return createLiveRoomWithRequest(name, description, coverUrl, liveRoomId);
    }

    private LiveRoom createLiveRoomWithRequest(String name, String description, String coverUrl, String liveRoomId) throws LiveException, IOException {
        LiveRoom liveRoom = new LiveRoom();
        liveRoom.setName(name);
        liveRoom.setDescription(description);
        liveRoom.setAnchorId(EMClient.getInstance().getCurrentUser());
        liveRoom.setCover(coverUrl);
        //// FIXME: 2017/4/15 在此调用自己写的创建聊天室的方法,判断返回的id,仿照下面的方法写
        String id = createLiveRoom(name, description);
        L.e(TAG,"id="+id);
        if(id!=null){
            liveRoom.setId(id);
            liveRoom.setChatroomId(id);
        }else{
            //这个id是传过来的id
            liveRoom.setId(liveRoomId);
        }
        //f---->
        //// FIXME: 2017/4/15 上面这一部分是仿写,把原来的注释
   /*     Call<ResponseModule<LiveRoom>> responseCall;
        if(liveRoomId != null){
            responseCall = apiService.createLiveShow(liveRoomId, liveRoom);

        }else {
            responseCall = apiService.createLiveRoom(liveRoom);
        }
        //先拿到数据在创建
        ResponseModule<LiveRoom> response = handleResponseCall(responseCall).body();
        LiveRoom room = response.data;
        if(room.getId() != null) {
            liveRoom.setId(room.getId());
        }else {
            liveRoom.setId(liveRoomId);
        }
        //设置房间号
        liveRoom.setChatroomId(room.getChatroomId());
        //liveRoom.setAudienceNum(1);
        //推流和拉流,这应该是直播的根本把,这个技术是怎么做的呢,
        liveRoom.setLivePullUrl(room.getLivePullUrl());
        liveRoom.setLivePushUrl(room.getLivePushUrl());*/
        return liveRoom;
    }

    public void updateLiveRoomCover(String roomId, String coverUrl) throws LiveException {
        JSONObject jobj = new JSONObject();
        JSONObject picObj = new JSONObject();
        try {
            picObj.put("cover_picture_url", coverUrl);
            jobj.put("liveroom", picObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Call<ResponseModule> responseCall = apiService.updateLiveRoom(roomId, jsonToRequestBody(jobj.toString()));
        handleResponseCall(responseCall);
    }



    //public void joinLiveRoom(String roomId, String userId) throws LiveException {
    //    JSONObject jobj = new JSONObject();
    //    String[] arr = new String[]{userId};
    //    JSONArray jarr = new JSONArray(Arrays.asList(arr));
    //    try {
    //        jobj.put("usernames", jarr);
    //    } catch (JSONException e) {
    //        e.printStackTrace();
    //    }
    //    handleResponseCall(apiService.joinLiveRoom(roomId, jsonToRequestBody(jobj.toString())));
    //}



    //public void updateLiveRoom(LiveRoom liveRoom) throws LiveException {
    //    Call respCall = apiService.updateLiveRoom(liveRoom.getId(), liveRoom);
    //    handleResponseCall(respCall);
    //}

    public LiveStatusModule.LiveStatus getLiveRoomStatus(String roomId) throws LiveException {
        Call<ResponseModule<LiveStatusModule>> respCall = apiService.getStatus(roomId);
        return handleResponseCall(respCall).body().data.status;
    }

    public void terminateLiveRoom(String roomId) throws LiveException {
        LiveStatusModule module = new LiveStatusModule();
        module.status = LiveStatusModule.LiveStatus.completed;
        handleResponseCall(apiService.updateStatus(roomId, module));
    }

    //public void closeLiveRoom(String roomId) throws LiveException {
    //    Call respCall = apiService.closeLiveRoom(roomId);
    //    handleResponseCall(respCall);
    //}

    public List<LiveRoom> getLiveRoomList(int pageNum, int pageSize) throws LiveException {
        Call<ResponseModule<List<LiveRoom>>> respCall = apiService.getLiveRoomList(pageNum, pageSize);

        ResponseModule<List<LiveRoom>> response = handleResponseCall(respCall).body();
        return response.data;
    }

    public ResponseModule<List<LiveRoom>> getLivingRoomList(int limit, String cursor) throws LiveException {
        Call<ResponseModule<List<LiveRoom>>> respCall = apiService.getLivingRoomList(limit, cursor);

        ResponseModule<List<LiveRoom>> response = handleResponseCall(respCall).body();

        return response;
    }

    public LiveRoom getLiveRoomDetails(String roomId) throws LiveException {
        return handleResponseCall(apiService.getLiveRoomDetails(roomId)).body().data;
    }

    public List<String> getAssociatedRooms(String userId) throws LiveException {
        ResponseModule<List<String>> response = handleResponseCall(apiService.getAssociatedRoom(userId)).body();
        return response.data;
    }

    //public void grantLiveRoomAdmin(String roomId, String adminId) throws LiveException {
    //    GrantAdminModule module = new GrantAdminModule();
    //    module.newAdmin = adminId;
    //    handleResponseCall(apiService.grantAdmin(roomId, module));
    //}
    //
    //public void revokeLiveRoomAdmin(String roomId, String adminId) throws LiveException {
    //    handleResponseCall(apiService.revokeAdmin(roomId, adminId));
    //}
    //
    //public void grantLiveRoomAnchor(String roomId, String anchorId) throws LiveException {
    //    handleResponseCall(apiService.grantAnchor(roomId, anchorId));
    //}
    //
    //public void revokeLiveRoomAnchor(String roomId, String anchorId) throws LiveException {
    //    handleResponseCall(apiService.revokeAdmin(roomId, anchorId));
    //}
    //
    //public void kickLiveRoomMember(String roomId, String memberId) throws LiveException {
    //    handleResponseCall(apiService.kickMember(roomId, memberId));
    //}

    public void postStatistics(StatisticsType type, String roomId, int count) throws LiveException {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("type", type);
            jobj.put("count", count);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handleResponseCall(apiService.postStatistics(roomId, jsonToRequestBody(jobj.toString())));
    }

    public void postStatistics(StatisticsType type, String roomId, String username) throws LiveException {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("type", type);
            jobj.put("count", username);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handleResponseCall(apiService.postStatistics(roomId, jsonToRequestBody(jobj.toString())));
    }

    private <T> Response<T>handleResponseCall(Call<T> responseCall) throws LiveException{
        try {
            Log.e(TAG, "handleResponseCall: responseCall="+responseCall.toString() );
            Response<T> response = responseCall.execute();
            Log.e(TAG, "handleResponseCall: response="+response);
            if(!response.isSuccessful()){
                throw new LiveException(response.code(), response.errorBody().string());
            }
            return response;
        } catch (IOException e) {
            throw new LiveException(e.getMessage());
        }
    }

    private RequestBody jsonToRequestBody(String jsonStr){
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonStr);
    }
}
