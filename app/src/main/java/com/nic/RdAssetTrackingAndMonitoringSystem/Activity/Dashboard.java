package com.nic.RdAssetTrackingAndMonitoringSystem.Activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.volley.VolleyError;
import com.nic.RdAssetTrackingAndMonitoringSystem.Adapter.RoadListAdapter;
import com.nic.RdAssetTrackingAndMonitoringSystem.Api.Api;
import com.nic.RdAssetTrackingAndMonitoringSystem.Api.ApiService;
import com.nic.RdAssetTrackingAndMonitoringSystem.Api.ServerResponse;
import com.nic.RdAssetTrackingAndMonitoringSystem.Constant.AppConstant;
import com.nic.RdAssetTrackingAndMonitoringSystem.DataBase.DBHelper;
import com.nic.RdAssetTrackingAndMonitoringSystem.DataBase.dbData;
import com.nic.RdAssetTrackingAndMonitoringSystem.Dialog.MyDialog;
import com.nic.RdAssetTrackingAndMonitoringSystem.Model.RoadListValue;
import com.nic.RdAssetTrackingAndMonitoringSystem.R;
import com.nic.RdAssetTrackingAndMonitoringSystem.Session.PrefManager;
import com.nic.RdAssetTrackingAndMonitoringSystem.Support.MyCustomTextView;
import com.nic.RdAssetTrackingAndMonitoringSystem.Utils.UrlGenerator;
import com.nic.RdAssetTrackingAndMonitoringSystem.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Dashboard extends AppCompatActivity implements View.OnClickListener, MyDialog.myOnClickListener, Api.ServerResponseListener {

    private MyCustomTextView on_road_tv, district_tv, block_tv , sync;
    private LinearLayout district_user_layout, block_user_layout;
    private ImageView logout_tv;
    Handler myHandler = new Handler();
    private PrefManager prefManager;
    public dbData dbData = new dbData(this);
    RelativeLayout vpr_layout,pur_layout,vpr_pur_layout,highway_layout;
    RoadListAdapter roadListAdapter;
    RecyclerView recyclerView;
    JSONObject dataset = new JSONObject();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        intializeUI();

    }

    public void intializeUI() {
        prefManager = new PrefManager(this);
        district_user_layout = (LinearLayout) findViewById(R.id.district_user_layout);
        block_user_layout = (LinearLayout) findViewById(R.id.block_user_layout);
        district_tv = (MyCustomTextView) findViewById(R.id.district_tv);
        block_tv = (MyCustomTextView) findViewById(R.id.block_tv);
        on_road_tv = (MyCustomTextView) findViewById(R.id.on_road_tv);
        logout_tv = (ImageView) findViewById(R.id.logout_tv);
        vpr_layout = (RelativeLayout) findViewById(R.id.VPR_Layout);
        pur_layout= (RelativeLayout) findViewById(R.id.PUR_Layout);
        vpr_pur_layout= (RelativeLayout) findViewById(R.id.VPR_PUR_Layout);
        highway_layout= (RelativeLayout) findViewById(R.id.Highway_Road_layout);
        sync = (MyCustomTextView) findViewById(R.id.sync);

        vpr_layout.setOnClickListener(this);
        pur_layout.setOnClickListener(this);
        vpr_pur_layout.setOnClickListener(this);
        highway_layout.setOnClickListener(this);
        sync.setOnClickListener(this);

        on_road_tv.setAlpha(0);
        final Runnable onroad = new Runnable() {
            @Override
            public void run() {

                on_road_tv.startAnimation(AnimationUtils.loadAnimation(Dashboard.this, R.anim.text_view_move));
                on_road_tv.setAlpha(1);
            }
        };
        myHandler.postDelayed(onroad, 700);
        block_user_layout.setAlpha(0);
        final Runnable block = new Runnable() {
            @Override
            public void run() {
                block_user_layout.setAlpha(1);
                block_user_layout.startAnimation(AnimationUtils.loadAnimation(Dashboard.this, R.anim.text_view_move));

            }
        };
        myHandler.postDelayed(block, 1000);
        district_user_layout.setAlpha(0);
        final Runnable district = new Runnable() {
            @Override
            public void run() {
                district_user_layout.setAlpha(1);
                district_user_layout.startAnimation(AnimationUtils.loadAnimation(Dashboard.this, R.anim.text_view_move));

            }
        };
        myHandler.postDelayed(district, 1200);


        district_tv.setText(prefManager.getDistrictName());
        block_tv.setText(prefManager.getBlockName());
        logout_tv.setOnClickListener(this);
        getRoadList();
        getAssetList();

        syncButtonVisibility();
    }

    public void syncButtonVisibility() {
        dbData.open();
        ArrayList<RoadListValue> assets = dbData.toUploadAsset();
        if(assets.size() >0 ){
            sync.setVisibility(View.VISIBLE);
        }else {
            sync.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.logout_tv:
                closeApplication();
                break;
            case R.id.VPR_Layout:
                roadlistScreen("2");
                break;
            case R.id.PUR_Layout:
                roadlistScreen("1");
                break;
            case R.id.VPR_PUR_Layout:
                roadlistScreen("4");
                break;
            case R.id.Highway_Road_layout:
                roadlistScreen("3");
                break;
            case R.id.sync:
                if(Utils.isOnline()) {
                    new toUploadTask().execute();
                }else{
                    Utils.showAlert(this,"Turn On Mobile Data To Save");
                }
                break;
        }
    }

    public void roadlistScreen(String code) {
        Intent intent = new Intent(this,RoadListScreen.class);
        intent.putExtra(AppConstant.KEY_ROAD_CATEGORY_CODE,code);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }
    private void closeApplication() {
        new MyDialog(Dashboard.this).exitDialog(Dashboard.this, "Are you sure you want to Logout?", "Logout");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                new MyDialog(this).exitDialog(this, "Are you sure you want to exit ?", "Exit");
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onButtonClick(AlertDialog alertDialog, String type) {
        alertDialog.dismiss();
        if ("Exit".equalsIgnoreCase(type)) {
            onBackPressed();
        } else {

            Intent intent = new Intent(getApplicationContext(), LoginScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("EXIT", false);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        }
    }

    public void getRoadList(){
        try {
            new ApiService(this).makeJSONObjectRequest("RoadList", Api.Method.POST, UrlGenerator.getRoadListUrl(), roadListJsonParams(), "not cache", this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getAssetList(){
        try {
            new ApiService(this).makeJSONObjectRequest("AssetList", Api.Method.POST, UrlGenerator.getRoadListUrl(), assetListJsonParams(), "not cache", this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject roadListJsonParams() throws JSONException {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), Utils.roadListJsonParams(this).toString());
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
        dataSet.put(AppConstant.DATA_CONTENT, authKey);
        Log.d("roadlist", "" + authKey);
        return dataSet;
    }

    public JSONObject assetListJsonParams() throws JSONException {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), Utils.assetListJsonParams(this).toString());
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
        dataSet.put(AppConstant.DATA_CONTENT, authKey);
        Log.d("assetlist", "" + authKey);
        return dataSet;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
    }

    @Override
    public void OnMyResponse(ServerResponse serverResponse) {
        try {
            String urlType = serverResponse.getApi();
            JSONObject responseObj = serverResponse.getJsonResponse();
                if ("RoadList".equals(urlType) && responseObj != null) {
                    String key = responseObj.getString(AppConstant.ENCODE_DATA);
                    String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                    JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                    if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                       new InsertRoadListTask().execute(jsonObject);
                    }
                    Log.d("response_RoadList", "" + responseDecryptedBlockKey);
                }
            if ("AssetList".equals(urlType) && responseObj != null) {
                String key = responseObj.getString(AppConstant.ENCODE_DATA);
                String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                   new InsertAssetListTask().execute(jsonObject);
                }
                Log.d("response_AssetList", "" + responseDecryptedBlockKey);
            }
            if ("save_data".equals(urlType) && responseObj != null) {
                String key = responseObj.getString(AppConstant.ENCODE_DATA);
                String responseDecryptedBlockKey = Utils.decrypt(prefManager.getUserPassKey(), key);
                JSONObject jsonObject = new JSONObject(responseDecryptedBlockKey);
                if (jsonObject.getString("STATUS").equalsIgnoreCase("OK") && jsonObject.getString("RESPONSE").equalsIgnoreCase("OK")) {
                    Utils.showAlert(this,"Saved");
                    dbData.open();
                    dbData.update_image();
                    finish();
                    startActivity(getIntent());
                }
                Log.d("saved_response", "" + responseDecryptedBlockKey);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class InsertRoadListTask extends AsyncTask<JSONObject ,Void ,Void> {

        @Override
        protected Void doInBackground(JSONObject... params) {
            dbData.open();
            ArrayList<RoadListValue> roadlist_count = dbData.getAll_Road("0");
            if (roadlist_count.size() <= 0) {
                if (params.length > 0) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        jsonArray = params[0].getJSONArray(AppConstant.JSON_DATA);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        RoadListValue roadListValue = new RoadListValue();
                        try {
                            roadListValue.setRoadID(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_ROAD_ID));
                            roadListValue.setRoadCode(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_ROAD_CODE));
                            roadListValue.setRoadCategoryCode(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_ROAD_CATEGORY_CODE));
                            roadListValue.setRoadCategory(jsonArray.getJSONObject(i).getString(AppConstant.KEY_ROAD_CATEGORY));
                            roadListValue.setRoadName(jsonArray.getJSONObject(i).getString(AppConstant.KEY_ROAD_NAME));
                            roadListValue.setRoadVillage(jsonArray.getJSONObject(i).getString(AppConstant.KEY_ROAD_VILLAGE_NAME));
                            dbData.create_newRoad(roadListValue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }
                return null;
        }
    }

    public class InsertAssetListTask extends AsyncTask<JSONObject ,Void ,Void> {

        @Override
        protected Void doInBackground(JSONObject... params) {
            dbData.open();
            ArrayList<RoadListValue> assetlist_count = dbData.getAll_Asset();
            if (assetlist_count.size() <= 0) {
                if (params.length > 0) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        jsonArray = params[0].getJSONArray(AppConstant.JSON_DATA);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        RoadListValue assetListValue = new RoadListValue();
                        try {
                            assetListValue.setRoadID(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_ROAD_ID));
                            assetListValue.setLocGroup(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_LOCATION_GROUP));
                            assetListValue.setLocID(jsonArray.getJSONObject(i).getInt(AppConstant.KEY_LOCATION_ID));
                            assetListValue.setGroupName(jsonArray.getJSONObject(i).getString(AppConstant.KEY_GROUP_NAME));
                            assetListValue.setSubgroupName(jsonArray.getJSONObject(i).getString(AppConstant.KEY_SUB_GROUP_NAME));
                            assetListValue.setColLabel(jsonArray.getJSONObject(i).getString(AppConstant.KEY_COLUMN_LABEL));
                            assetListValue.setLocationDetails(jsonArray.getJSONObject(i).getString(AppConstant.KEY_LOCATION_DETAILS));
                            dbData.create_newAsset(assetListValue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }
            return null;
        }
    }

    public class toUploadTask extends AsyncTask<Void, Void,
            JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... voids) {
            dbData.open();
            JSONArray track_data = new JSONArray();
            try {
                dataset.put(AppConstant.KEY_SERVICE_ID,AppConstant.KEY_ROAD_TRACK_ASSET_SAVE);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ArrayList<RoadListValue> assets = dbData.toUploadAsset();

            if (assets.size() > 0) {
                for (int i = 0; i < assets.size(); i++) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(AppConstant.KEY_ROAD_CATEGORY,assets.get(i).getRoadCategory());
                        jsonObject.put(AppConstant.KEY_ROAD_ID,assets.get(i).getRoadID());
                        jsonObject.put(AppConstant.KEY_ASSET_ID,assets.get(i).getAssetId());
                        jsonObject.put(AppConstant.KEY_ROAD_LAT,assets.get(i).getRoadLat());
                        jsonObject.put(AppConstant.KEY_ROAD_LONG,assets.get(i).getRoadLong());

                        Bitmap bitmap = assets.get(i).getImage();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                        byte[] imageInByte = baos.toByteArray();
                        String image_str = Base64.encodeToString(imageInByte, Base64.DEFAULT);

                        jsonObject.put(AppConstant.KEY_IMAGES,image_str);

                        track_data.put(jsonObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                try {
                    dataset.put(AppConstant.KEY_TRACK_DATA,track_data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return dataset;
        }

        @Override
        protected void onPostExecute(JSONObject dataset) {
            super.onPostExecute(dataset);
            syncData();
        }
    }


    public void syncData() {
        try {
            new ApiService(this).makeJSONObjectRequest("save_data", Api.Method.POST, UrlGenerator.getRoadListUrl(), dataTobeSavedJsonParams(), "not cache", this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject dataTobeSavedJsonParams() throws JSONException {
        String authKey = Utils.encrypt(prefManager.getUserPassKey(), getResources().getString(R.string.init_vector), dataset.toString());
        JSONObject dataSet = new JSONObject();
        dataSet.put(AppConstant.KEY_USER_NAME, prefManager.getUserName());
        dataSet.put(AppConstant.DATA_CONTENT, authKey);
        Log.d("saving", "" + authKey);
        return dataSet;
    }

    @Override
    public void OnError(VolleyError volleyError) {
    }
}
