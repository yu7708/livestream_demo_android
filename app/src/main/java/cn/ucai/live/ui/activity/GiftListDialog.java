package cn.ucai.live.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;
import cn.ucai.live.data.model11.Gift;
import cn.ucai.live.utils.I;
import cn.ucai.live.utils.Utils;

/**
 * Created by wei on 2016/7/25.
 */
public class GiftListDialog extends DialogFragment {
    private static final String TAG = "RoomUserDetailsDialog";
    Unbinder unbinder;

    EaseImageView ivFragmentRoomUserDetailsAvatar;
    @BindView(R.id.rv_gift)
    RecyclerView rvGift;
    @BindView(R.id.tv_my_bill)
    TextView tvMyBill;
    @BindView(R.id.tv_recharge)
    TextView tvRecharge;

    List<Gift> giftList;
    GridLayoutManager gm;
    GiftAdapter adapter;

  /*  public static GiftListDialog newInstance(String username, LiveRoom liveRoom) {
        GiftListDialog dialog = new GiftListDialog();
        Bundle args = new Bundle();
        args.putString("username", username);
        args.putSerializable("liveRoom", liveRoom);
        dialog.setArguments(args);
        return dialog;
    }*/

    public static GiftListDialog newInstance() {
        //提供实例化，是为了不必每一个都new？
        GiftListDialog dialog = new GiftListDialog();
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gift_list, container, false);
        unbinder = ButterKnife.bind(this, view);
        //显示需要布局管理，recycleView里面要设置布局和adapter，设置自适应，显示dialog
        gm = new GridLayoutManager(getContext(), I.GIFT_COLUMN_COUNT);
        rvGift.setLayoutManager(gm);
        rvGift.setHasFixedSize(true);
        customDialog();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //拿到列表
        giftList = LiveHelper.getInstance().getGiftList();
        Log.e(TAG, "onActivityCreated,giftList.size=" + giftList.size());
        if(giftList.size()>0){
            if(adapter==null){
                adapter=new GiftAdapter(getContext(),giftList);
                rvGift.setAdapter(adapter);
            }else{
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void customDialog() {
        getDialog().setCanceledOnTouchOutside(true);
        Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wlp);
    }



    private void showToast(final String toast) {
        Utils.showToast(getActivity(), toast);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
    //提供一个接口，可以供外面调用
    private View.OnClickListener eventListener;

    public void setGiftEventListener(View.OnClickListener eventListener) {
        this.eventListener = eventListener;
    }


    class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.GiftViewHolder> {
        Context context;
        List<Gift> mList;

        public GiftAdapter(Context context, List<Gift> mList) {
            this.context = context;
            this.mList = mList;
        }

        @Override
        public GiftAdapter.GiftViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //这里加载布局，实例化holder
            View layout = View.inflate(context, R.layout.item_gift, null);
            return new GiftViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(GiftAdapter.GiftViewHolder holder, int position) {
            //绑定就是找到控件和设置控件的数据
            holder.bind(mList.get(position));
        }

        @Override
        public int getItemCount() {
            //返回列表的大小
            return mList.size();
        }

        class GiftViewHolder extends RecyclerView.ViewHolder{
            @BindView(R.id.ivGiftThumb)
            ImageView ivGiftThumb;
            @BindView(R.id.tvGiftName)
            TextView tvGiftName;
            @BindView(R.id.tvGiftPrice)
            TextView tvGiftPrice;
            @BindView(R.id.layout_gift)
            LinearLayout layoutGift;

            GiftViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
            public void bind(Gift gift){
                EaseUserUtils.setAvatar(context,gift.getGurl(),ivGiftThumb);
                tvGiftName.setText(gift.getGname());
                tvGiftPrice.setText(gift.getGprice()+"");
                //viewholder自带的一个属性，好像就是点击就相当于点击这个对象
                //// FIXME: 2017/4/19 要设置礼物的点击事件还要知道是哪个礼物，多少钱反馈出去。
                //原来在SuperWeChat里写过
                itemView.setTag(gift.getId());
                itemView.setOnClickListener(eventListener);
            }
        }
    }
}
