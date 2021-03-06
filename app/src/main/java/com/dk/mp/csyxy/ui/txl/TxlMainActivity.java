package com.dk.mp.csyxy.ui.txl;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.dk.mp.core.entity.Department;
import com.dk.mp.core.entity.Jbxx;
import com.dk.mp.core.entity.LoginMsg;
import com.dk.mp.core.entity.XbPersons;
import com.dk.mp.core.http.HttpUtil;
import com.dk.mp.core.http.request.HttpListener;
import com.dk.mp.core.ui.BaseFragment;
import com.dk.mp.core.ui.MyActivity;
import com.dk.mp.core.util.BroadcastUtil;
import com.dk.mp.core.util.CoreSharedPreferencesHelper;
import com.dk.mp.core.util.DeviceUtil;
import com.dk.mp.core.util.SnackBarUtil;
import com.dk.mp.core.view.RecycleViewDivider;
import com.dk.mp.core.widget.ErrorLayout;
import com.dk.mp.csyxy.R;
import com.dk.mp.csyxy.adapter.TxlAdapter;
import com.dk.mp.csyxy.db.RealmHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * 通讯录
 * 作者：janabo on 2017/3/16 09:59
 */
public class TxlMainActivity extends MyActivity implements EasyPermissions.PermissionCallbacks{
    FloatingActionButton fab;
    RelativeLayout mdialog;//提示框
    ErrorLayout error_layout;//加载
    RecyclerView mRecyclerView;//星标同事
    RecyclerView mBmRecyclerView;//所有部门
    LinearLayout xbts_lin;//
    LinearLayout bm_lin;
    TxlAdapter mAdapter;
    TxlAdapter bAdapter;
    List<Jbxx> mList = new ArrayList<>();//星标同事
    List<Department> mData = new ArrayList<>();//所有部门

    private RealmHelper mRealmHelper;
    private boolean isxb = true;
    String uid = "guest";
    private CoreSharedPreferencesHelper preference;

    @Override
    protected int getLayoutID() {
        return R.layout.app_yellowpage;
    }

    @Override
    protected void initialize() {
        super.initialize();

        startExprotPhonesByPermissions();
        mRealmHelper = new RealmHelper(mContext);
        error_layout.setErrorType(ErrorLayout.LOADDATA);
        LoginMsg loginMsg = getSharedPreferences().getLoginMsg();
        if(loginMsg!=null){
            uid = loginMsg.getUid();
        }
        getXb(uid);
        if(DeviceUtil.checkNet()) {//判断是否有网络
            getData();
        }else{
            getBmDataByLocal(1);
        }
    }

    @Override
    protected void initView() {
        super.initView();

        setTitle(getIntent().getStringExtra("title"));

        fab = (FloatingActionButton) findViewById(R.id.fab);
        mdialog = (RelativeLayout)findViewById(R.id.mdialog);
        error_layout = (ErrorLayout) findViewById(R.id.error_layout);
        xbts_lin = (LinearLayout) findViewById(R.id.xbts_lin);
        bm_lin = (LinearLayout) findViewById(R.id.bm_lin);
        mRecyclerView = (RecyclerView) findViewById(R.id.xbts_recycle);
        mRecyclerView.setHasFixedSize ( true );
        mRecyclerView.setLayoutManager ( new LinearLayoutManager( mContext ) );
        mAdapter = new TxlAdapter ( mContext, TxlMainActivity.this,mList,1);
        mRecyclerView.setAdapter ( mAdapter );
        mRecyclerView.addItemDecoration(new RecycleViewDivider(mContext, LinearLayoutManager.HORIZONTAL, DeviceUtil.dip2px(mContext,0.8f), Color.rgb(229, 229, 229)));
        mRecyclerView.setNestedScrollingEnabled(false);

        mBmRecyclerView = (RecyclerView) findViewById(R.id.bm_recycle);
        mBmRecyclerView.setHasFixedSize ( true );
        mBmRecyclerView.setLayoutManager ( new LinearLayoutManager( mContext ) );
        bAdapter = new TxlAdapter ( mContext, TxlMainActivity.this, mData,2);
        mBmRecyclerView.setAdapter ( bAdapter );
        mBmRecyclerView.addItemDecoration(new RecycleViewDivider(mContext, LinearLayoutManager.HORIZONTAL, DeviceUtil.dip2px(mContext,0.8f),Color.rgb(229, 229, 229)));
        mBmRecyclerView.setNestedScrollingEnabled(false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext,SearchActivity.class);
                startActivity(intent);
            }
        });

        BroadcastUtil.registerReceiver(mContext, mRefreshBroadcastReceiver, new String[]{"txl_persons"});
    }

    private BroadcastReceiver mRefreshBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("txl_persons")) {
                LoginMsg loginMsg = getSharedPreferences().getLoginMsg();
                if(loginMsg!=null){
                    uid = loginMsg.getUid();
                }
                getXb(uid);
            }
        }
    };

    /**
     * 获取所有部门
     */
    public void getData(){
        //http://wap.cztljx.org/apps/txl/getBmList
        HttpUtil.getInstance().postJsonObjectRequest("apps/txl/getBmList", null, new HttpListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result)  {
                try {
                    if (result.getInt("code") != 200) {
                        getBmDataByLocal(2);
                    }else{
                        String json =  result.getJSONArray("data").toString();
                        List<Department> departments = new Gson().fromJson(json,new TypeToken<List<Department>>(){}.getType());
                        mRealmHelper.deleteDepartment();
                        mRealmHelper.addDepartment(departments);
                        mData.clear();
                        mData.addAll(departments);
                        bAdapter.notifyDataSetChanged();
                        error_layout.setErrorType(ErrorLayout.HIDE_LAYOUT);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    getBmDataByLocal(2);
                }
            }
            @Override
            public void onError(VolleyError error) {
                getBmDataByLocal(2);
            }
        });
    }

    /**
     * 本地获取数据
     * @param par  1 无网络  2，获取数据失败
     */
    public void getBmDataByLocal(int par){
        List<Department> departments = mRealmHelper.queryAllDepartment();//从本地存储获取数据
        if(departments != null && departments.size()>0){
            bm_lin.setVisibility(View.VISIBLE);
            error_layout.setErrorType(ErrorLayout.HIDE_LAYOUT);
            mData.addAll(departments);
            bAdapter.notifyDataSetChanged();
            if(par == 1) {
//                SnackBarUtil.showShort(mdialog, getReString(R.string.net_no2));
            }else{
                SnackBarUtil.showShort(mdialog, getReString(R.string.data_fail));
            }
        }else{
            bm_lin.setVisibility(View.GONE);
            if(isxb){//有星标同事
                if(par == 1) {
                    SnackBarUtil.showShort(mdialog, getReString(R.string.net_no2));
                }else{
                    SnackBarUtil.showShort(mdialog, getReString(R.string.data_fail));
                }
            }else {
                if(par == 1) {
//                    error_layout.setErrorType(ErrorLayout.NETWORK_ERROR);
                    error_layout.setErrorType(ErrorLayout.HIDE_LAYOUT);
                    return;
                }else{
                    error_layout.setErrorType(ErrorLayout.DATAFAIL);
                }

            }
        }
    }

    /**
     * 获取星标用户
     */
    public void getXb(String uid){
        List<XbPersons> xbtss = mRealmHelper.queryALlXb(uid);
        if(xbtss!= null && xbtss.size()>0){
            isxb = true;
            xbts_lin.setVisibility(View.VISIBLE);
            List<Jbxx> jbxxs = new ArrayList<>();
            for(XbPersons x : xbtss){
                Jbxx j = new Jbxx();
                j.setId(x.getId());
                j.setDepartmentname(x.getDepartmentname());
                j.setDepartmentid(x.getDepartmentid());
                j.setName(x.getName());
                j.setPrikey(x.getId());
                j.setPhones(x.getPhones());
                j.setLoginname(x.getLoginname());
                jbxxs.add(j);
            }
            mList.clear();
            mList.addAll(jbxxs);
            mAdapter.notifyDataSetChanged();
        }else{
            isxb = false;
            xbts_lin.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(mContext, "请正确授权", Toast.LENGTH_LONG).show();
    }

    /**
     * 判断是否有读写电话簿的权限
     */
    private void startExprotPhonesByPermissions(){
        String[] perms = {Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS};
        if (EasyPermissions.hasPermissions(mContext, perms)) {

        }else{
            EasyPermissions.requestPermissions(this,"请求获取读写电话簿权限",
                    1, perms);
        }
    }

    public CoreSharedPreferencesHelper getSharedPreferences() {
        if (preference == null){
            preference = new CoreSharedPreferencesHelper(mContext);
        }
        return preference;
    }

    /**
     * 获取string
     * @param string R.string.x
     * @return  ""
     */
    public String getReString(int string) {
        return getResources().getString(string);
    }
}
