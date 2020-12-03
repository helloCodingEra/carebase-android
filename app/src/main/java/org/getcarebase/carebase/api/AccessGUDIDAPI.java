package org.getcarebase.carebase.api;

import org.getcarebase.carebase.models.DeviceModel;
import org.getcarebase.carebase.models.ParseUDIResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AccessGUDIDAPI {
    @GET("parse_udi.json")
    Call<ParseUDIResponse> getParseUdiResponse(@Query("udi") String udi);

    @GET("devices/lookup.json")
    Call<DeviceModel> getDeviceModel(@Query("udi") String udi);

}
