package cn.ucai.live.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.R;
import cn.ucai.live.data.model.IUserModel;
import cn.ucai.live.data.model.OnCompleteListener;
import cn.ucai.live.data.model.UserModel;
import cn.ucai.live.utils.I;
import cn.ucai.live.utils.Result;
import cn.ucai.live.utils.ResultUtils;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    @BindView(R.id.email)
    EditText username;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.usernick)
    EditText usernick;
    @BindView(R.id.confirm_password)
    EditText confirmPassword;
    ProgressDialog pd;
    IUserModel mModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mModel=new UserModel();
        setListener();
    }

    private void setListener() {
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkIn()){
                    showDialog();
                    mModel.register(RegisterActivity.this,
                            username.getText().toString(),
                            usernick.getText().toString(),
                            password.getText().toString(),
                            new OnCompleteListener<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    boolean sucess=false;
                                    if(s!=null){
                                        Result result = ResultUtils.getResultFromJson(s, User.class);
                                        //这里还要考虑注册失败的问题
                                        if(result!=null){
                                           if(result.isRetMsg()){
                                               sucess=true;
                                               EMRegister();
                                           }else if(result.getRetCode()== I.MSG_REGISTER_USERNAME_EXISTS){
                                              showToast(R.string.User_already_exists+"");
                                           }else{
                                               showToast(R.string.Registration_failed+"");
                                           }
                                        }
                                    }
                                    if(!sucess){
                                        pd.dismiss();
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    pd.dismiss();
                                }
                            });
                }
            }
        });
    }
    private void unRegister(){
        mModel.unregister(RegisterActivity.this, username.getText().toString(),
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.e(TAG,"result:"+result);
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
    }
    private void EMRegister(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EMClient.getInstance().createAccount(username.getText().toString(), password.getText().toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            showToast("注册成功");
                            //// FIXME: 2017/4/12 注册成功,跳转至登录
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                } catch (final HyphenateException e) {
                    //环信上传之前自己上传服务器,环信上传失败后才注册
                    unRegister();
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.dismiss();
                            showLongToast("注册失败：" + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    private void showDialog(){
        pd = new ProgressDialog(RegisterActivity.this);
        pd.setMessage("正在注册...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();
    }
    private boolean checkIn(){
        if (TextUtils.isEmpty(username.getText()) || TextUtils.isEmpty(password.getText())) {
            showToast("用户名和密码不能为空");
            return false;
        }
        if(TextUtils.isEmpty(usernick.getText())){
            showToast("昵称不能为空");
            return false;
        }
        if(TextUtils.isEmpty(confirmPassword.getText())){
            showToast("确认密码不能为空");
            return false;
        }
        if(!password.getText().toString().equals(confirmPassword.getText().toString())){
            showToast("两段密码不一致");
            return false;
        }
        return true;
    }
}
