package com.lennydennis.zerakiapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class TwilioRetrofitInstance {

   private static final String BASE_URL = "http://10.0.2.2:8080/Twilio/api/twilio/";

   //private static final String BASE_URL = "http://192.168.1.103:8080/Twilio/api/twilio/";

    private static Retrofit sRetrofit = null;

    public static TwilioApi getTwilioReftrofitInstance(){

        if(sRetrofit == null){
            sRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return sRetrofit.create(TwilioApi.class);
    }
}
