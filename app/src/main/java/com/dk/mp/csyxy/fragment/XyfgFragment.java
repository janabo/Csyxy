package com.dk.mp.csyxy.fragment;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.volley.VolleyError;
import com.dk.mp.core.http.HttpUtil;
import com.dk.mp.core.http.request.HttpListener;
import com.dk.mp.core.ui.BaseFragment;
import com.dk.mp.core.util.DeviceUtil;
import com.dk.mp.core.widget.ErrorLayout;
import com.dk.mp.csyxy.R;
import com.dk.mp.csyxy.ui.xyfg.entity.SceneryEntity;
import com.dk.mp.csyxy.ui.xyfg.view.SceneryItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by cobb on 2017/6/24.
 */

public class XyfgFragment extends BaseFragment {

    private List<SceneryEntity> list;
    private ScrollView scroll;

    private ErrorLayout errorLayout;
    LinearLayout rootview;

    @Override
    protected int getLayoutId() {
        return R.layout.app_scenery;
    }

    @Override
    protected void initWidget(View root) {
        super.initWidget(root);
        rootview = findView(R.id.rootview);
        scroll = findView(R.id.sceneryScrollView);
        errorLayout = findView(R.id.error_layout);

//        if(DeviceUtil.checkNet()){
//            initDatas();
//        }else{
//           errorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
//        }

//        BroadcastUtil.registerReceiver(getContext(), mRefreshBroadcastReceiver, new String[]{"checknetwork_true","checknetwork_false"});
    }

//    private BroadcastReceiver mRefreshBroadcastReceiver = new BroadcastReceiver() {
//        @SuppressLint("NewApi") @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action.equals("checknetwork_true")) {
//                initDatas();
//            }
////            if(action.equals("checknetwork_false")){
////                mError.setErrorType(ErrorLayout.NETWORK_ERROR);
////            }
//
//        }
//    };

    private void initDatas(){
        errorLayout.setErrorType(ErrorLayout.LOADDATA);
        scroll.removeAllViews();

        HttpUtil.getInstance().postJsonObjectRequest("apps/xyfg/getTypeList", null, new HttpListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result)  {
                try {
                    if (result.getInt("code") != 200) {
                        errorLayout.setErrorType(ErrorLayout.DATAFAIL);
                    }else{
                        errorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
                        String json =  result.getJSONArray("data").toString();
                        list = new Gson().fromJson(json,new TypeToken<List<SceneryEntity>>(){}.getType());
                        if (list.size() > 0){
                            LinearLayout layout = SceneryItem.getViews(getActivity(), list);
                            scroll.addView(layout);

                        }else {
                            errorLayout.setErrorType(ErrorLayout.NODATA);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    error();
                }
            }
            @Override
            public void onError(VolleyError error) {
                error();
            }
        });
    }

    @Override
    public void onFirstUserVisible() {
        super.onFirstUserVisible();
        getData();
    }

    @Override
    public void onUserVisible() {
        super.onUserVisible();
        getData();
    }

    /**
     * 获取数据
     */
    public void getData(){
        if(DeviceUtil.checkNet()){
            initDatas();
        }else{
            if(list != null && list.size()>0){
//                SnackBarUtil.showShort(rootview,getResources().getString(R.string.net_no2));
                return;
            }else {
//                errorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
                errorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
            }
        }
    }

    /**
     * 加载失败处理信息
     */
    public void error(){
        if(list != null && list.size()>0){
//            SnackBarUtil.showShort(rootview,getResources().getString(R.string.data_fail));
            return;
        }else {
            errorLayout.setErrorType(ErrorLayout.DATAFAIL);
        }
    }

}
