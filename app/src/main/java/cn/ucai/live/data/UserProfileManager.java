package cn.ucai.live.data;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.ucai.live.data.model.IUserModel;
import cn.ucai.live.data.model.OnCompleteListener;
import cn.ucai.live.data.model.UserModel;
import cn.ucai.live.utils.I;
import cn.ucai.live.utils.PreferenceManager;
import cn.ucai.live.utils.Result;
import cn.ucai.live.utils.ResultUtils;


public class UserProfileManager {
	private static final String TAG = "UserProfileManager";
	/**
	 * application context
	 */
	protected Context appContext = null;

	/**
	 * init flag: test if the sdk has been inited before, we don't need to init
	 * again
	 */
	private boolean sdkInited = false;



	private boolean isSyncingContactInfosWithServer = false;

	//自己是用的类
	private User currentAppUser;


	//// FIXME: 2017/4/1 之前数据库操作时调用过
	IUserModel userModel;

	public UserProfileManager() {
	}

	public synchronized boolean init(Context context) {
		if (sdkInited) {
			return true;
		}
		appContext=context;
		sdkInited = true;
		userModel=new UserModel();
		return true;
	}

	//重置方法,清空内存和sf里面的数据
	public synchronized void reset() {
		currentAppUser=null;
		PreferenceManager.getInstance().removeCurrentUserInfo();
	}
	public synchronized User getCurrentAppUserInfo(){
		//放到sharedPreference里
		if(currentAppUser==null){
			String username= EMClient.getInstance().getCurrentUser();
			currentAppUser=new User(username);
			String nick=getCurrentUserNick();
			currentAppUser.setMUserNick(nick!=null?nick:username);
		}
		return currentAppUser;
	}

	public boolean updateCurrentUserNickName(final String nickname) {
		//// FIXME: 2017/4/1
		//我们修改得进行网络的访问,然后返回昵称数据
		userModel.updateUserNick(appContext, EMClient.getInstance().getCurrentUser(), nickname,
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						//生成一个boolean判断,返回接收的广播类型
						boolean updatenick=false;
						//首先是得到的数据要转化,转化的要判断是否接受,接受的要判断返回值是否正常
						if(s!=null){
							Result result = ResultUtils.getResultFromJson(s, User.class);
							if(result.isRetMsg()){
								User user = (User) result.getRetData();
								if(user!=null){
									updatenick=true;
									//存在sharedP里
									setCurrentAppUserNick(user.getMUserNick());
								}
							}
						}
						//发送广播接收
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_USER_NICK)
								.putExtra(I.User.NICK,updatenick));
					}

					@Override
					public void onError(String error) {
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_USER_NICK)
								.putExtra(I.User.NICK,false));
					}
				});
		//// FIXME: 2017/4/1 改后不用了
		/*//提供的第三方的,得自己建立方法
		boolean isSuccess = ParseManager.getInstance().updateParseNickName(nickname);
		if (isSuccess) {
			//这一句表示昵称保存在sharedPreference里
			setCurrentUserNick(nickname);
		}
		return isSuccess;*/
		return false;
	}

	/*public String uploadUserAvatar(byte[] data) {
		String avatarUrl = ParseManager.getInstance().uploadParseAvatar(data);
		if (avatarUrl != null) {
			setCurrentUserAvatar(avatarUrl);
		}
		return avatarUrl;
	}*/
	public void uploadUserAvatar(File file){
		userModel.updateAvatar(appContext, EMClient.getInstance().getCurrentUser(), file,
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						boolean success=false;
						//设置布尔类型,是根据标志位判读是否发送广播
						if(s!=null){
							//先判断的的得到的结果不为空,然后转化为json,然后
							Result result=ResultUtils.getResultFromJson(s,User.class);
							if(result!=null&&result.isRetMsg()){
								//一直不是很懂这个isRetMsg是什么意思,里面都是直接返回
								User user= (User) result.getRetData();
								//也就是说这里返回的是uesr
								if(user!=null){
									success=true;
									//更改图片
									setCurrentAppUserAvatar(user.getAvatar());
								}
							}
						}
						//发送广播接收,写上你的发送码,后面也是你传的参数,
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_AVATAR)
								.putExtra(I.Avatar.UPDATE_TIME,success));
					}

					@Override
					public void onError(String error) {
						//失败也传一个广播
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_AVATAR)
								.putExtra(I.Avatar.UPDATE_TIME,false));
					}
				});
	}
	public void asyncGetCurrentAppUserInfo() {
		//异步取得本地用户数据
		userModel.loadUserInfo(appContext, EMClient.getInstance().getCurrentUser(),
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						if(s!=null){
							Result result = ResultUtils.getResultFromJson(s, User.class);
							//拿到返回的信息
							if(result!=null&&result.isRetMsg()){
								User user = (User) result.getRetData();
								Log.e(TAG,"asyncGetCurrentAppUserInfo,user="+user);
								//仿下面拿到图片和昵称
								if(user!=null) {
									//// FIXME: 2017/4/4
									currentAppUser=user;
									//f
									setCurrentAppUserNick(user.getMUserNick());
									setCurrentAppUserAvatar(user.getAvatar());
								}
							}
						}
					}

					@Override
					public void onError(String error) {

					}
				});
	}

	private void setCurrentAppUserNick(String nickname){
		getCurrentAppUserInfo().setMUserNick(nickname);
		PreferenceManager.getInstance().setCurrentUserNick(nickname);
	}
	//图像的数据是分散的，在user中新建一个对象avatar
	private void setCurrentAppUserAvatar(String avatar){
		getCurrentAppUserInfo().setAvatar(avatar);
		PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
	}

	private String getCurrentUserNick() {
		return PreferenceManager.getInstance().getCurrentUserNick();
	}

	private String getCurrentUserAvatar() {
		return PreferenceManager.getInstance().getCurrentUserAvatar();
	}


}
