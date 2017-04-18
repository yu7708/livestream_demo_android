package cn.ucai.live.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.data.model.LiveRoom;
import cn.ucai.live.data.model11.Gift;
import cn.ucai.live.data.model11.Result;
import cn.ucai.live.data.restapi.LiveException;
import cn.ucai.live.data.restapi.model.ResponseModule;
import cn.ucai.live.ui.GridMarginDecoration;

import com.bumptech.glide.Glide;
import cn.ucai.live.R;
import cn.ucai.live.ThreadPoolManager;
import cn.ucai.live.data.restapi.ApiManager;

import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMPageResult;
import com.hyphenate.exceptions.HyphenateException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
//// FIXME: 2017/4/18 主播主界面
public class LiveListFragment extends Fragment {
    private static final String TAG = "LiveListFragment";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar loadmorePB;

    private static final int pageSize = 20;
    private String cursor;
    private boolean hasMoreData;
    private boolean isLoading;
    private final List<LiveRoom> liveRoomList = new ArrayList<>();
    private PhotoAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadmorePB = (ProgressBar) getView().findViewById(R.id.pb_load_more);
        recyclerView = (RecyclerView) getView().findViewById(R.id.recycleview);
        final GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new GridMarginDecoration(3));

        swipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.refresh_layout);
        showLiveList(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                showLiveList(false);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_IDLE
                        && hasMoreData
                        && !isLoading
                        && glm.findLastVisibleItemPosition() == glm.getItemCount() -1){
                    showLiveList(true);
                }
            }
        });

    }


    private void showLiveList(final boolean isLoadMore){
        //// FIXME: 2017/4/18 这里定义一个自己的方法,是为了暂且屏蔽掉环信的
        if (getLiveRoom()){

            return;
        }
        //--->
        if(!isLoadMore)
            swipeRefreshLayout.setRefreshing(true);
        else
            loadmorePB.setVisibility(View.VISIBLE);
        isLoading = true;
        //// FIXME: 2017/4/14
        //loadGiftList();

        //--->

        ThreadPoolManager.getInstance().executeTask(new ThreadPoolManager.Task<ResponseModule<List<LiveRoom>>>() {
            @Override public ResponseModule<List<LiveRoom>> onRequest() throws HyphenateException {
                if(!isLoadMore){
                    cursor = null;
                }
                return ApiManager.get().getLivingRoomList(pageSize, cursor);
            }

            @Override public void onSuccess(ResponseModule<List<LiveRoom>> listResponseModule) {
                hideLoadingView(isLoadMore);
                List<LiveRoom> returnList = listResponseModule.data;
                if(returnList.size() < pageSize){
                    hasMoreData = false;
                    cursor = null;
                }else{
                    hasMoreData = true;
                    cursor = listResponseModule.cursor;
                }

                if(!isLoadMore) {
                    liveRoomList.clear();
                }
                liveRoomList.addAll(returnList);
                if(adapter == null){
                    adapter = new PhotoAdapter(getActivity(), liveRoomList);
                    recyclerView.setAdapter(adapter);
                }else{
                    adapter.notifyDataSetChanged();
                }

            }

            @Override public void onError(HyphenateException exception) {
                hideLoadingView(isLoadMore);
            }
        });
    }

    private boolean getLiveRoom() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //从SuperWechat里拿出的房间的3个方法
                final EMPageResult<EMChatRoom> result;
                try {
                    int pageCount = -1;
                    result = EMClient.getInstance().chatroomManager()
                            .fetchPublicChatRoomsFromServer(0, pageSize);
                    //get chat room list
                    final List<EMChatRoom> chatRooms = result.getData();
                    pageCount = result.getPageCount();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "run: chatRooms="+chatRooms );
                            if(chatRooms!=null&&chatRooms.size()>0){
                                Log.e(TAG, "run: chatRooms.size="+chatRooms.size() );
                                swipeRefreshLayout.setRefreshing(false);
                                liveRoomList.clear();
                                for(EMChatRoom room:chatRooms){
                                    Log.e(TAG, "run: room="+room.getName() );
                                    LiveRoom liveRoom = showLive2List(room);
                                    if(liveRoom!=null){
                                        liveRoomList.add(liveRoom);
                                    }
                                }
                                if(adapter==null){
                                    adapter = new PhotoAdapter(getActivity(), liveRoomList);
                                    recyclerView.setAdapter(adapter);
                                }else{
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });

                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

    private LiveRoom showLive2List(EMChatRoom room) {
        LiveRoom liveroom=null;
        if(room!=null){
            liveroom=new LiveRoom();
            liveroom.setId(room.getOwner());
            liveroom.setChatroomId(room.getId());
            String s="#live201612#";
            //添加一个保护,不然总是下标越界,
            if(room.getName().indexOf(s)>0) {
                int i = room.getName().indexOf(s);
                String name1 = room.getName().substring(0, i + 1);
                Log.e(TAG, "showLive2List: name1===" + name1);
                String name2 = room.getName().substring(i + s.length());
                Log.e(TAG, "showLive2List: name2===" + name2);
                // liveroom.setName(room.getName());
                liveroom.setName(name1);
                liveroom.setCover("https://a1.easemob.com/i/superwechat201612.chatfiles/" + name2);
            }else{
                liveroom.setName(room.getName());
            }
            liveroom.setDescription(room.getDescription());
            liveroom.setAnchorId(room.getId());
            liveroom.setAudienceNum(room.getMemberCount());
        }
        return liveroom;
    }


    private void loadGiftList() {
      /*  ThreadPoolManager.getInstance().executeTask(new ThreadPoolManager.Task<Result<List<Gift>>>() {
            @Override
            public Result<List<Gift>> onRequest() throws HyphenateException {
                return (Result<List<Gift>>) ApiManager.get().getAllGifts();
                //这个放在这里面写的意义何在?
                //而且显示为类型转换异常,arraylist不能转化为reuslt
                //java.lang.ClassCastException: java.util.ArrayList cannot be cast to cn.ucai.live.data.model11.Result
            }

            @Override
            public void onSuccess(Result<List<Gift>> listResult) {
                Log.e(TAG, "onSuccess: listResult="+listResult);
                if(listResult!=null&&listResult.isRetMsg()){
                    List<Gift> list=listResult.getRetData();
                    if(list!=null){
                        Log.e(TAG, "onSuccess: list.size()="+list.size());
                        for (Gift g:list){
                            Log.e(TAG, "onSuccess: g="+g);
                        }
                    }
                }
            }

            @Override
            public void onError(HyphenateException exception) {

            }
        });*/
      //// FIXME: 2017/4/15 这就是原来实验用的,现在写了数据库就删掉
       /* new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Gift> allGifts = ApiManager.get().getAllGifts();
                    if(allGifts!=null){
                        Log.e(TAG, "onSuccess: list.size()="+allGifts.size());
                        for (Gift g:allGifts){
                            Log.e(TAG, "onSuccess: g="+g);
                        }
                    }
                } catch (LiveException e) {
                    e.printStackTrace();
                }
            }
        }).start();*/
    }

    private void hideLoadingView(boolean isLoadMore){
        isLoading = false;
        if(!isLoadMore)
            swipeRefreshLayout.setRefreshing(false);
        else
            loadmorePB.setVisibility(View.INVISIBLE);
    }

    static class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        private final List<LiveRoom> liveRoomList;
        private final Context context;

        public PhotoAdapter(Context context, List<LiveRoom> liveRoomList){
            this.liveRoomList = liveRoomList;
            this.context = context;
        }
        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final PhotoViewHolder holder = new PhotoViewHolder(LayoutInflater.from(context).
                    inflate(R.layout.layout_livelist_item, parent, false));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = holder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;
                    context.startActivity(new Intent(context, LiveAudienceActivity.class)
                            .putExtra("liveroom", liveRoomList.get(position)));
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(PhotoViewHolder holder, int position) {
            LiveRoom liveRoom = liveRoomList.get(position);
            holder.anchor.setText(liveRoom.getName());
            holder.audienceNum.setText(liveRoom.getAudienceNum() + "人");
            Glide.with(context)
                    .load(liveRoomList.get(position).getCover())
                    .placeholder(R.color.placeholder)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return liveRoomList.size();
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.photo)
        ImageView imageView;
        @BindView(R.id.author)
        TextView anchor;
        @BindView(R.id.audience_num) TextView audienceNum;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
