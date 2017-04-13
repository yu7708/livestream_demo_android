package cn.ucai.live;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMError;
import com.hyphenate.EMGroupChangeListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMessage.ChatType;
import com.hyphenate.chat.EMMessage.Status;
import com.hyphenate.chat.EMMessage.Type;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.controller.EaseUI.EaseEmojiconInfoProvider;
import com.hyphenate.easeui.controller.EaseUI.EaseSettingsProvider;
import com.hyphenate.easeui.controller.EaseUI.EaseUserProfileProvider;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseEmojiconGroupEntity;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.model.EaseNotifier.EaseNotificationInfoProvider;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.ucai.live.data.UserProfileManager;
import cn.ucai.live.data.model.IUserModel;
import cn.ucai.live.data.model.OnCompleteListener;
import cn.ucai.live.data.model.UserModel;
import cn.ucai.live.ui.activity.MainActivity;
import cn.ucai.live.utils.PreferenceManager;
import cn.ucai.live.utils.Result;
import cn.ucai.live.utils.ResultUtils;


public class LiveHelper {

    protected static final String TAG = "SuperWeChatDemoHelper";

	private EaseUI easeUI;


	private UserProfileManager userProManager;

	private static LiveHelper instance;

	private LiveModel demoModel = null;

	private String username;

    private Context appContext;

    private IUserModel userModel;

    private LocalBroadcastManager broadcastManager;


	private LiveHelper() {
	}

	public synchronized static LiveHelper getInstance() {
		if (instance == null) {
			instance = new LiveHelper();
		}
		return instance;
	}

	/**
	 * init helper
	 *
	 * @param context
	 *            application context
	 */
	public void init(Context context) {
        userModel = new UserModel();
        demoModel = new LiveModel(context);
	    //use default options if options is null
		if (EaseUI.getInstance().init(context, null)) {
		    appContext = context;

		    //debug mode, you'd better set it to false, if you want release your App officially.
		    EMClient.getInstance().setDebugMode(true);
		    //get easeui instance
		    easeUI = EaseUI.getInstance();
		    //to set user's profile and avatar
		    setEaseUIProviders();
			//initialize preference manager
			PreferenceManager.init(context);
			//initialize profile manager
			getUserProfileManager().init(context);

            setGlobalListeners();
			broadcastManager = LocalBroadcastManager.getInstance(appContext);
		}
	}




    protected void setEaseUIProviders() {
        // set profile provider if you want easeUI to handle avatar and nickname
        //如果你想easeui处理头像和昵称设置配置文件提供程序
        easeUI.setUserProfileProvider(new EaseUserProfileProvider() {

            @Override
            public EaseUser getUser(String username) {
                return getUserInfo(username);
            }

            @Override
            public User getAppUser(String username) {
                return getAppUserInfo(username);
            }
        });
    }
    EMConnectionListener connectionListener;
    /**
     * set global listener
     */
    protected void setGlobalListeners(){
        // create the global connection listener
        connectionListener = new EMConnectionListener() {
            @Override
            public void onDisconnected(int error) {
                EMLog.d("global listener", "onDisconnect" + error);
                if (error == EMError.USER_REMOVED) {
                    onUserException(LiveConstants.ACCOUNT_REMOVED);
                } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                    onUserException(LiveConstants.ACCOUNT_CONFLICT);
                } else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
                    onUserException(LiveConstants.ACCOUNT_FORBIDDEN);
                }
            }

            @Override
            public void onConnected() {

            }
        };

        //register connection listener
        EMClient.getInstance().addConnectionListener(connectionListener);
    }

    void showToast(final String message) {
        Message msg = Message.obtain(handler, 0, message);
        handler.sendMessage(msg);
    }

    protected android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = (String)msg.obj;
            Toast.makeText(appContext, str, Toast.LENGTH_LONG).show();
        }
    };

    /**
     * user met some exception: conflict, removed or forbidden
     */
    protected void onUserException(String exception){
        EMLog.e(TAG, "onUserException: " + exception);
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(exception, true);
        appContext.startActivity(intent);
    }

	private EaseUser getUserInfo(String username){
		// To get instance of EaseUser, here we get it from the user list in memory
		// You'd better cache it if you get it from your server
        //得到easeuser实例，这里我们把它从内存中的用户列表
        //你最好缓存它，如果你从你的服务器
        EaseUser user = null;

        // if user is not in your contacts, set inital letter for him/her
        if(user == null){
            user = new EaseUser(username);
            EaseCommonUtils.setUserInitialLetter(user);
        }
        return user;
	}
    //// FIXME: 2017/3/31 //设置个人界面的图像和昵称
    private User getAppUserInfo(String username){
        // To get instance of EaseUser, here we get it from the user list in memory
        // You'd better cache it if you get it from your server
        //得到easeuser实例，这里我们把它从内存中的用户列表
        //你最好缓存它，如果你从你的服务器
        User user = null;
        if(username.equals(EMClient.getInstance().getCurrentUser()))
            return getUserProfileManager().getCurrentAppUserInfo();

        // if user is not in your contacts, set inital letter for him/
        //如果用户不在您的联系人，设置初始的信给他/她
        if(user == null){
            user = new User(username);
            EaseCommonUtils.setAppUserInitialLetter(user);
        }
        return user;
    }
    //// FIXME: 2017/4/13 这里以后将注释

    /**
	 * if ever logged in
	 *
	 * @return
	 */
	public boolean isLoggedIn() {
		return EMClient.getInstance().isLoggedInBefore();
	}

	/**
	 * logout
	 *
	 * @param unbindDeviceToken
	 *            whether you need unbind your device token
	 * @param callback
	 *            callback
	 */
	public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
		//endCall();
		Log.d(TAG, "logout: " + unbindDeviceToken);
		EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

			@Override
			public void onSuccess() {
				Log.d(TAG, "logout: onSuccess");
			    reset();
				if (callback != null) {
					callback.onSuccess();
				}

			}

			@Override
			public void onProgress(int progress, String status) {
				if (callback != null) {
					callback.onProgress(progress, status);
				}
			}

			@Override
			public void onError(int code, String error) {
				Log.d(TAG, "logout: onSuccess");
                reset();
				if (callback != null) {
					callback.onError(code, error);
				}
			}
		});
	}

	/**
	 * get instance of EaseNotifier
	 * @return
	 */
	public EaseNotifier getNotifier(){
	    return easeUI.getNotifier();
	}

	/*public SuperWeChatModel getModel(){
        return (SuperWeChatModel) demoModel;
    }*/
    public LiveModel getModel(){
        return (LiveModel) demoModel;
    }
    /*
	*//**
	 * update contact list
	 *
	 * @param aContactList
	 *//*
	public void setContactList(Map<String, EaseUser> aContactList) {
		if(aContactList == null){
		    if (contactList != null) {
		        contactList.clear();
		    }
			return;
		}

		contactList = aContactList;
	}

	*//**
     * save single contact
     *//*
    public void saveContact(EaseUser user){
    	contactList.put(user.getUsername(), user);
    	demoModel.saveContact(user);
    }

    *//**
     * get contact list
     *
     * @return
     *//*
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            contactList = demoModel.getContactList();
        }

        // return a empty non-null object to avoid app crash
        if(contactList == null){
        	return new Hashtable<String, EaseUser>();
        }

        return contactList;
    }
*/
    /**
     * set current username
     * @param username
     */
    public void setCurrentUserName(String username){
    	this.username = username;
    	demoModel.setCurrentUserName(username);
    }

    /**
     * get current user's id
     */
    public String getCurrentUsernName(){
    	if(username == null){
    		username = demoModel.getCurrentUsernName();
    	}
    	return username;
    }

	/*public void setRobotList(Map<String, RobotUser> robotList) {
		this.robotList = robotList;
	}*/

	/*public Map<String, RobotUser> getRobotList() {
		if (isLoggedIn() && robotList == null) {
			robotList = demoModel.getRobotList();
		}
		return robotList;
	}*/
/*
	 *//**
     * update user list to cache and database
     *
     * @param
     *//*
    public void updateContactList(List<EaseUser> contactInfoList) {
         for (EaseUser u : contactInfoList) {
            contactList.put(u.getUsername(), u);
         }
         ArrayList<EaseUser> mList = new ArrayList<EaseUser>();
         mList.addAll(contactList.values());
         demoModel.saveContactList(mList);
    }*/

	public UserProfileManager getUserProfileManager() {
		if (userProManager == null) {
			userProManager = new UserProfileManager();
		}
		return userProManager;
	}

	/*void endCall() {
		try {
			EMClient.getInstance().callManager().endCall();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

 /* public void addSyncGroupListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncGroupsListeners.contains(listener)) {
            syncGroupsListeners.add(listener);
        }
    }*/

    /*public void removeSyncGroupListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncGroupsListeners.contains(listener)) {
            syncGroupsListeners.remove(listener);
        }
    }
*/
   /* public void addSyncContactListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncContactsListeners.contains(listener)) {
            syncContactsListeners.add(listener);
        }
    }*/

  /*  public void removeSyncContactListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncContactsListeners.contains(listener)) {
            syncContactsListeners.remove(listener);
        }
    }*/

   /* public void addSyncBlackListListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (!syncBlackListListeners.contains(listener)) {
            syncBlackListListeners.add(listener);
        }
    }*/

    /*public void removeSyncBlackListListener(DataSyncListener listener) {
        if (listener == null) {
            return;
        }
        if (syncBlackListListeners.contains(listener)) {
            syncBlackListListeners.remove(listener);
        }
    }*/

	/**
    * Get group list from server
    * This method will save the sync state
    * @throws HyphenateException
    */
   /*public synchronized void asyncFetchGroupsFromServer(final EMCallBack callback){
       if(isSyncingGroupsWithServer){
           return;
       }

       isSyncingGroupsWithServer = true;

       new Thread(){
           @Override
           public void run(){
               try {
                   EMClient.getInstance().groupManager().getJoinedGroupsFromServer();

                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       isGroupsSyncedWithServer = false;
                       isSyncingGroupsWithServer = false;
                       noitifyGroupSyncListeners(false);
                       return;
                   }

                   demoModel.setGroupsSynced(true);

                   isGroupsSyncedWithServer = true;
                   isSyncingGroupsWithServer = false;

                   //notify sync group list success
                   noitifyGroupSyncListeners(true);

                   if(callback != null){
                       callback.onSuccess();
                   }
               } catch (HyphenateException e) {
                   demoModel.setGroupsSynced(false);
                   isGroupsSyncedWithServer = false;
                   isSyncingGroupsWithServer = false;
                   noitifyGroupSyncListeners(false);
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }

           }
       }.start();
   }*/

  /* public void noitifyGroupSyncListeners(boolean success){
       for (DataSyncListener listener : syncGroupsListeners) {
           listener.onSyncComplete(success);
       }
   }*/
    //// FIXME: 2017/4/7 异步获取联系人
   /* public void asyncFetchAppContactsFromServer(){
        if (isLoggedIn()) {
            userModel.loadContact(appContext, LiveHelper.getInstance().getCurrentUsernName(), new OnCompleteListener<String>() {
                @Override
                public void onSuccess(String s) {
                    if (s != null) {
                        Result result = ResultUtils.getListResultFromJson(s, User.class);
                        if (result != null && result.isRetMsg()) {
                          *//*  isContactsSyncedWithServer = true;
                            isSyncingContactsWithServer = false;
*//*
                            //notify sync success
                            notifyContactsSyncListener(true);
                            List<User> list = (List<User>) result.getRetData();
                            //// FIXME: 2017/4/7 拿的下面异步获取数组后的保存到数据库里的方法
                            Map<String, User> userlist = new HashMap<String, User>();
                            for (User user : list) {
                                EaseCommonUtils.setAppUserInitialLetter(user);
                                userlist.put(user.getMUserName(), user);
                            }
                            // save the contact list to cache
                            //保存联系人列表到缓存
                            getAppContactList().clear();
                            getAppContactList().putAll(userlist);
                            // save the contact list to database
                            //保存联系人列表到数据库
                         *//*   UserDao dao = new UserDao(appContext);
                            List<User> users = new ArrayList<User>(userlist.values());
                            dao.saveAppContactList(users);*//*
                            //还得设置退出刷新,不然,读取的还是第一个登录的帐号的列表
                            //同类中有个logout方法,里面有个reset()方法.
                        }
                    }
                }

                @Override
                public void onError(String error) {

                }
            });
        } else {
            reset();
          *//*  isContactsSyncedWithServer = false;
            isSyncingContactsWithServer = false;*//*
            notifyContactsSyncListener(false);
        }
    }
    //从服务器异步读取联系人
 *//*  public void asyncFetchContactsFromServer(final EMValueCallBack<List<String>> callback){
       if(isSyncingContactsWithServer){
           return;
       }

       isSyncingContactsWithServer = true;*//*
       //// FIXME: 2017/4/7 这里是环信的自带的获取一组联系人的方法,我们这里借用
       //仿照他的做法,先判断登录没有
       //asyncFetchAppContactsFromServer();
       //---
       new Thread(){
           @Override
           public void run(){
               List<String> usernames = null;
               try {
                   usernames = EMClient.getInstance().contactManager().getAllContactsFromServer();
                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       //判读,提出标志位
                     *//*  isContactsSyncedWithServer = false;
                       isSyncingContactsWithServer = false;*//*
                       notifyContactsSyncListener(false);
                       return;
                   }

                   Map<String, EaseUser> userlist = new HashMap<String, EaseUser>();
                   for (String username : usernames) {
                       EaseUser user = new EaseUser(username);
                       EaseCommonUtils.setUserInitialLetter(user);
                       userlist.put(username, user);
                   }
                   // save the contact list to cache
                   //保存联系人列表到缓存
             *//*      getContactList().clear();
                   getContactList().putAll(userlist);*//*
                    // save the contact list to database
                   //保存联系人列表到数据库
               *//*    UserDao dao = new UserDao(appContext);
                   List<EaseUser> users = new ArrayList<EaseUser>(userlist.values());
                   dao.saveContactList(users);*//*

                  // demoModel.setContactSynced(true);
                   EMLog.d(TAG, "set contact syn status to true");
*//*

                   isContactsSyncedWithServer = true;
                   isSyncingContactsWithServer = false;
*//*

                   //notify sync success
                 //  notifyContactsSyncListener(true);
                   //拉取加载联系人从服务器上
                   //getUserProfileManager()实际就是UserProfileManager的引用常量,多加了判断减少多余的new实例化
                   //getUserProfileManager().asyncFetchContactInfosFromServer(usernames,new EMValueCallBack<List<EaseUser>>() {

                       @Override
                       public void onSuccess(List<EaseUser> uList) {
                           updateContactList(uList);
                           getUserProfileManager().notifyContactInfosSyncListener(true);
                       }

                       @Override
                       public void onError(int error, String errorMsg) {
                       }
                   });
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (HyphenateException e) {
                   demoModel.setContactSynced(false);
                   isContactsSyncedWithServer = false;
                   isSyncingContactsWithServer = false;
                   notifyContactsSyncListener(false);
                   e.printStackTrace();
                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }

           }
       }.start();
   }*/

  /* public void notifyContactsSyncListener(boolean success){
       for (DataSyncListener listener : syncContactsListeners) {
           listener.onSyncComplete(success);
       }
   }*/

  /* public void asyncFetchBlackListFromServer(final EMValueCallBack<List<String>> callback){

       if(isSyncingBlackListWithServer){
           return;
       }

       isSyncingBlackListWithServer = true;

       new Thread(){
           @Override
           public void run(){
               try {
                   List<String> usernames = EMClient.getInstance().contactManager().getBlackListFromServer();

                   // in case that logout already before server returns, we should return immediately
                   if(!isLoggedIn()){
                       isBlackListSyncedWithServer = false;
                       isSyncingBlackListWithServer = false;
                       notifyBlackListSyncListener(false);
                       return;
                   }

                   demoModel.setBlacklistSynced(true);

                   isBlackListSyncedWithServer = true;
                   isSyncingBlackListWithServer = false;

                   notifyBlackListSyncListener(true);
                   if(callback != null){
                       callback.onSuccess(usernames);
                   }
               } catch (HyphenateException e) {
                   demoModel.setBlacklistSynced(false);

                   isBlackListSyncedWithServer = false;
                   isSyncingBlackListWithServer = true;
                   e.printStackTrace();

                   if(callback != null){
                       callback.onError(e.getErrorCode(), e.toString());
                   }
               }

           }
       }.start();
   }*/

	/*public void notifyBlackListSyncListener(boolean success){
        for (DataSyncListener listener : syncBlackListListeners) {
            listener.onSyncComplete(success);
        }
    }

    public boolean isSyncingGroupsWithServer() {
        return isSyncingGroupsWithServer;
    }

    public boolean isSyncingContactsWithServer() {
        return isSyncingContactsWithServer;
    }

    public boolean isSyncingBlackListWithServer() {
        return isSyncingBlackListWithServer;
    }

    public boolean isGroupsSyncedWithServer() {
        return isGroupsSyncedWithServer;
    }

    public boolean isContactsSyncedWithServer() {
        return isContactsSyncedWithServer;
    }

    public boolean isBlackListSyncedWithServer() {
        return isBlackListSyncedWithServer;
    }
*/
    synchronized void reset(){
       /* isSyncingGroupsWithServer = false;
        isSyncingContactsWithServer = false;
        isSyncingBlackListWithServer = false;

        demoModel.setGroupsSynced(false);
        demoModel.setContactSynced(false);
        demoModel.setBlacklistSynced(false);

        isGroupsSyncedWithServer = false;
        isContactsSyncedWithServer = false;
        isBlackListSyncedWithServer = false;

        isGroupAndContactListenerRegisted = false;

        setContactList(null);*/
        //// FIXME: 2017/4/8 后加的清空联系人
        //etAppContactList(null);
        //---
       // setRobotList(null);
        getUserProfileManager().reset();
        //SuperWeChatDBManager.getInstance().closeDB();
    }

    public void pushActivity(Activity activity) {
        easeUI.pushActivity(activity);
    }

    public void popActivity(Activity activity) {
        easeUI.popActivity(activity);
    }

    //// FIXME: 2017/3/30
    /**
     * update contact list
     *
     * @param aContactList
     */
  /*  public void setAppContactList(Map<String, User> aContactList) {
        if(aContactList == null){
            if (appContactList != null) {
                appContactList.clear();
            }
            return;
        }

        appContactList = aContactList;
    }

    *//**
     * save single contact
     *//*
    public void saveAppContact(User user){
        //这样会报空指针
        //appContactList.put(user.getMUserName(), user);
        getAppContactList().put(user.getMUserName(),user);
        demoModel.saveAppContact(user);
    }

    *//**
     * get contact list
     *
     * @return
     *//*
    public Map<String, User> getAppContactList() {
        Log.e(TAG, "getAppContactList: ....." );
        if (isLoggedIn() && appContactList == null) {
            Log.e(TAG, "getAppContactList: getAppContactList到数据库取得数据" );
            appContactList = demoModel.getAppContactList();
        }

        // return a empty non-null object to avoid app crash
        if(appContactList == null){
            Log.e(TAG, "getAppContactList: ....appContactList="+appContactList.size());
            Log.e(TAG, "getAppContactList: ....appContactList.containsKey" );
            return new Hashtable<String, User>();
        }

        return appContactList;
    }
    *//**
     * update user list to cache and database
     *
     * @param contactInfoList
     *//*
    public void updateAppContactList(List<User> contactInfoList) {
        for (User u : contactInfoList) {
            appContactList.put(u.getMUserName(), u);
        }
        ArrayList<User> mList = new ArrayList<User>();
        mList.addAll(appContactList.values());
        demoModel.saveAppContactList(mList);
    }*/
}
