package com.dk.edu.csyxy.ui;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.volley.VolleyError;
import com.dk.edu.core.entity.Jbxx;
import com.dk.edu.core.http.HttpUtil;
import com.dk.edu.core.http.request.HttpListener;
import com.dk.edu.core.ui.MyActivity;
import com.dk.edu.core.util.DeviceUtil;
import com.dk.edu.core.util.SnackBarUtil;
import com.dk.edu.core.view.RecycleViewDivider;
import com.dk.edu.core.widget.ErrorLayout;
import com.dk.edu.csyxy.R;
import com.dk.edu.csyxy.adapter.TxlAdapter;
import com.dk.edu.csyxy.db.RealmHelper;
import com.dk.edu.csyxy.http.Manager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部門人員
 * 作者：janabo on 2016/12/23 14:48
 */
public class TxlPersonsActivity extends MyActivity {
    RelativeLayout mdialog;
    ErrorLayout mErrorLayout;
    RecyclerView mRecyclerView;
    List<Jbxx> mList = new ArrayList<>();
    TxlAdapter mAdapter;
    private RealmHelper mRealmHelper;
    private String bmname;//部门
    private String bmid;//部门id

    @Override
    protected int getLayoutID() {
        return R.layout.app_persons;
    }

    @Override
    protected void initialize() {
        super.initialize();
        bmname = getIntent().getStringExtra("title");
        setTitle(bmname);
        bmid = getIntent().getStringExtra("id");
        mRealmHelper = new RealmHelper(this);
        mErrorLayout.setErrorType(ErrorLayout.LOADDATA);
        if(DeviceUtil.checkNet()) {
            getData();
        }else{
            getBmDataByLocal(1);
        }

        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                back();
            }
        });

    }

    /**
     * 初始化樣式
     */
    @Override
    protected void initView() {
        mErrorLayout = (ErrorLayout) findViewById(R.id.error_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.bm_recycle);
        mdialog = (RelativeLayout) findViewById(R.id.mdialog);
        mRecyclerView.setHasFixedSize ( true );
        mRecyclerView.setLayoutManager ( new LinearLayoutManager( TxlPersonsActivity.this ) );
        mAdapter = new TxlAdapter( TxlPersonsActivity.this, TxlPersonsActivity.this, mList,1);
        mRecyclerView.setAdapter ( mAdapter );
        mRecyclerView.addItemDecoration(new RecycleViewDivider(mContext, LinearLayoutManager.HORIZONTAL, DeviceUtil.dip2px(mContext,0.8f), Color.rgb(229, 229, 229)));
    }

    public void getData(){
        Map<String,Object> map = new HashMap<>();
//        map.put("id",bmid);
        HttpUtil.getInstance().postJsonObjectRequest("apps/txl/getlxr?id="+bmid, map, new HttpListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result)  {
                try {
                    if (result.getInt("code") != 200) {
                        getBmDataByLocal(2);
                    }else{
                        List<Jbxx> departments = Manager.getPeoples(result,bmid,bmname);
                        mRealmHelper.deletePersonsByDepartmentid(bmid);
                        mRealmHelper.addPersons(departments);
                        mList.addAll(departments);
                        mAdapter.notifyDataSetChanged();
                        if(departments.size()>0) {
                            mErrorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
                        }else{
                            mErrorLayout.setErrorType(ErrorLayout.NODATA);
                        }
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
     * 搜索
     * @param v
     */
    public void toSearch(View v){
        Intent intent = new Intent(this,SearchActivity.class);
        startActivity(intent);
    }

    /**
     * 本地获取数据
     * @param par  1 无网络  2，获取数据失败
     */
    public void getBmDataByLocal(int par){
        List<Jbxx> persons = mRealmHelper.queryPersonsByDepartmentid(bmid);//从本地存储获取数据
        if(persons != null && persons.size()>0){
            mErrorLayout.setErrorType(ErrorLayout.HIDE_LAYOUT);
            mList.addAll(persons);
            mAdapter.notifyDataSetChanged();
            if(par == 1) {
                SnackBarUtil.showShort(mdialog, getReString(R.string.net_no2));
            }else{
                SnackBarUtil.showShort(mdialog, getReString(R.string.data_fail));
            }
        }else{
            if(par == 1) {
                mErrorLayout.setErrorType(ErrorLayout.NETWORK_ERROR);
            }else{
                mErrorLayout.setErrorType(ErrorLayout.DATAFAIL);
            }

        }
    }
}
