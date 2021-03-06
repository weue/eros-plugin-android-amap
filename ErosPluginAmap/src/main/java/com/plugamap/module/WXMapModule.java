package com.plugamap.module;

import android.support.annotation.Nullable;
import android.util.Log;

import com.alibaba.weex.plugin.annotation.WeexModule;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.eros.framework.manager.ManagerFactory;
import com.plugamap.component.WXMapPolygonComponent;
import com.plugamap.manager.GeoManager;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.utils.WXLogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by budao on 2017/1/24.
 */

@WeexModule(name = "amap", lazyLoad = true)
public class WXMapModule extends WXModule {
    private static final String RESULT = "result";
    private static final String DATA = "data";

    private static final String RESULT_OK = "success";
    private static final String RESULT_FAILED = "failed";
    /**
     * get line distance between to POI.
     */
    @JSMethod
    public void getLineDistance(String posA, String posB, @Nullable final JSCallback callback) {
        Log.v("getDistance", posA + ", " + posB);
        float distance = -1;
        try {
            JSONArray jsonArray = new JSONArray(posA);
            LatLng latLngA = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));
            JSONArray jsonArrayB = new JSONArray(posB);
            LatLng latLngB = new LatLng(jsonArrayB.optDouble(1), jsonArrayB.optDouble(0));
            distance = AMapUtils.calculateLineDistance(latLngA, latLngB);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (callback != null) {
            HashMap map = new HashMap(2);
            HashMap data = new HashMap(1);
            data.put("distance", distance);
            map.put(DATA, data);
            map.put(RESULT, distance >= 0 ? RESULT_OK : RESULT_FAILED);
            callback.invoke(map);
        }

    }

    @JSMethod
    public void polygonContainsMarker(String position, String id, @Nullable final JSCallback callback) {
        boolean contains = false;
        boolean success = false;
        try {
            JSONArray jsonArray = new JSONArray(position);
            LatLng latLng = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));

            WXComponent component = findComponent(id);

            if (component != null && component instanceof WXMapPolygonComponent) {
                contains = ((WXMapPolygonComponent) component).contains(latLng);
                success = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (callback != null) {
            HashMap map = new HashMap(2);
            map.put(DATA, contains);
            map.put(RESULT, success ? RESULT_OK : RESULT_FAILED);
            callback.invoke(map);
        }

    }

    /**
     * get user location.
     */
    @JSMethod
    public void getUserLocation(String id, @Nullable final JSCallback callback) {
        final AMapLocationClient client = new AMapLocationClient(
                WXEnvironment.getApplication().getApplicationContext());
        final AMapLocationClientOption clientOption = new AMapLocationClientOption();
        //设置定位监听
        client.setLocationListener(new AMapLocationListener() {
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                    if (callback != null) {
                        HashMap map = new HashMap(2);
                        HashMap data = new HashMap(1);
                        ArrayList position = new ArrayList();
                        position.add(aMapLocation.getLongitude());
                        position.add(aMapLocation.getLatitude());
                        data.put("position", position);
                        map.put(DATA, data);
                        map.put(RESULT, aMapLocation.getLongitude() > 0 && aMapLocation.getLatitude() > 0 ? RESULT_OK : RESULT_FAILED);
                        callback.invoke(map);
                    }
                } else {
                    String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                    WXLogUtils.e("WXMapModule", errText);
                }
                if (client != null) {
                    client.stopLocation();
                    client.onDestroy();
                }
            }
        });
        //设置为高精度定位模式
        clientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        clientOption.setOnceLocation(true);
        //设置定位参数
        client.setLocationOption(clientOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        client.startLocation();
    }

    @JSMethod
    public void initAmap(final String amapKey) {
        GeoManager mGeoManager = ManagerFactory.getManagerService(GeoManager.class);
        mGeoManager.initAmap(amapKey);
    }

    /**
     * 响应逆地理编码
     */
    @JSMethod
    public void geoAddress(String searchLatlonPoint, @Nullable final JSCallback callback) {
       if (searchLatlonPoint != null){
         GeocodeSearch geocoderSearch = new GeocodeSearch(this.mWXSDKInstance.getContext());
         geocoderSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
           @Override
           public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
             if (i == 1000 && callback != null) {
               if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null
                       && regeocodeResult.getRegeocodeAddress().getFormatAddress() != null) {
                 HashMap map = new HashMap(2);
                 map.put("country", regeocodeResult.getRegeocodeAddress().getCountry());
                 map.put("province", regeocodeResult.getRegeocodeAddress().getProvince());
                 map.put("city", regeocodeResult.getRegeocodeAddress().getCity());
                 map.put("district", regeocodeResult.getRegeocodeAddress().getDistrict());
                 map.put("address", regeocodeResult.getRegeocodeAddress().getFormatAddress());
                 map.put("adCode", regeocodeResult.getRegeocodeAddress().getAdCode());
                 map.put("towncode", regeocodeResult.getRegeocodeAddress().getTowncode());
                 callback.invoke(map);
//                 WXLogUtils.d(TAG, "Amap: " + city + "," + address);
               }
             }
             if (callback != null) {

             }
           }

           @Override
           public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

           }
         });
         try {
//            {"longitude":116.486999783908,"latitude":40.00003008347923}
           JSONObject jsonObject = new JSONObject(searchLatlonPoint);
           LatLonPoint latLng = new LatLonPoint(jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude"));
           // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
           RegeocodeQuery query = new RegeocodeQuery(latLng, 100,GeocodeSearch.AMAP);
           geocoderSearch.getFromLocationAsyn(query);

         } catch (JSONException e) {
           e.printStackTrace();
         }


        }
    }



}
