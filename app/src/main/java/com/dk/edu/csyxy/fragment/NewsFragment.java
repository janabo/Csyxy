package com.dk.edu.csyxy.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.android.volley.VolleyError;
import com.dk.edu.core.entity.SlideNews;
import com.dk.edu.core.http.HttpUtil;
import com.dk.edu.core.http.request.HttpListener;
import com.dk.edu.core.ui.BaseFragment;
import com.dk.edu.core.util.DeviceUtil;
import com.dk.edu.core.view.RecycleViewDivider;
import com.dk.edu.csyxy.R;
import com.dk.edu.csyxy.adapter.NewsAdapter;
import com.dk.edu.csyxy.entity.News;
import com.dk.edu.csyxy.entity.PageMsg;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻模块
 * 作者：janabo on 2017/6/13 16:39
 */
public class NewsFragment extends BaseFragment{
    SwipeRefreshLayout mRefresh;
    RecyclerView mRecyclerView;
    NewsAdapter nAdapter;
    List<News> news = new ArrayList<>();
    List<SlideNews> slideNewses = new ArrayList<>();
    boolean isLoading;
    int pageNo = 1;
    int totalPages = 1;
    private String mType;
    boolean isRefreshing = false;
    Gson gson = new Gson();

    @Override
    protected int getLayoutId() {
        return R.layout.mp_news;
    }

    public static NewsFragment newInstance(String type) {
        Bundle args = new Bundle();
        args.putString("mType",type);
        NewsFragment fragment = new NewsFragment();
        fragment.setArguments(args);
        return fragment;
    }

//    @Override
//    public void onFirstUserVisible() {
//        super.onFirstUserVisible();
//        initMyData();
//    }


    @Override
    protected void initWidget(View root) {
        super.initWidget(root);
        initMyData();
    }

    protected void initMyData() {
        mRefresh = findView(R.id.swipe_refresh);
        mRecyclerView = findView(R.id.rv_listview);

        mType = getArguments().getString("mType");
        news.clear();
        news.add(new News(1));
        nAdapter = new NewsAdapter(getContext(),news,mType,slideNewses);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new RecycleViewDivider(getContext(), GridLayoutManager.HORIZONTAL, 1, Color.rgb(201, 201, 201)));//添加分割线
        mRecyclerView.setAdapter(nAdapter);

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pageNo = 1;
                news.clear();
                news.add(new News(1));
                getList();
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisibleItemPosition = manager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition + 1 == nAdapter.getItemCount()) {
                    Log.d("test", "loading executed");
                    isRefreshing = mRefresh.isRefreshing();
                    if (isRefreshing) {
//                        nAdapter.notifyItemRemoved(nAdapter.getItemCount());
                        return;
                    }
                    if(totalPages<=pageNo){
//                        nAdapter.notifyItemRemoved(nAdapter.getItemCount());
                        return;
                    }
                    if (!isLoading) {
                        isLoading = true;
                        pageNo++;
//                        loadMore();
                        news.clear();
                        news.add(new News(1));
                        getList();
                        Log.d("test", "load more completed");
                        isLoading = false;
                    }
                }
            }
        });

        //recycleview刷新时clear后加载新的item会报错
//        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (isRefreshing) {
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        });

        mRefresh.setRefreshing(true);
        getList();

    }

    public void getList(){
        if (DeviceUtil.checkNet()){
            Map<String, Object> map = new HashMap<>();
//            map.put("type",mType);
//            map.put("pageNo",pageNo);
            HttpUtil.getInstance().postJsonObjectRequest("apps/tabs/news?type="+mType+"&pageNo="+pageNo, map, new HttpListener<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        if(result.getInt("code") == 200){
                            JSONObject jo = result.getJSONObject("data");
                            //轮播新闻
                            List<SlideNews> slides = gson.fromJson(jo.getJSONArray("slide").toString(),new TypeToken<ArrayList<SlideNews>>(){}.getType());
                            slideNewses.clear();
                            if(slides!=null){
                                slideNewses.addAll(slides);
                            }
                            //列表新闻
                            PageMsg<News> pageMsg = gson.fromJson(jo.getJSONObject("news").toString(),new TypeToken<PageMsg<News>>(){}.getType());
                            totalPages = pageMsg.getTotalPages();
                            if(pageMsg.getList() != null && pageMsg.getList().size()>0) {
                                news.addAll(pageMsg.getList());
                                nAdapter.notifyDataSetChanged();
                                //停止刷新
                                mRefresh.setRefreshing(false);
                                //RecyclerView滑动到第一个
//                                mRecyclerView.scrollToPosition(0);
                            }else{
                                mRefresh.setRefreshing(false);
                                news.add(new News(2));
                                nAdapter.notifyDataSetChanged();
                            }
                        }else{
                            mRefresh.setRefreshing(false);
                            news.add(new News(3));
                            nAdapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mRefresh.setRefreshing(false);
                        nAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(VolleyError error) {
                    news.add(new News(3));
                    nAdapter.notifyDataSetChanged();
                    mRefresh.setRefreshing(false);
                }
            });
        }else {
            news.add(new News(4));
            nAdapter.notifyDataSetChanged();
            mRefresh.setRefreshing(false);
        }
    }

}
