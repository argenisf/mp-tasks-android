package com.example.mptaskmanager;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class UserAuth {
    //0: pending authentication
    //1: authenticated
    //-1: issue authenticating
    private int mState;
    private Boolean mDemo;
    private int mUserID;
    private String mEmail;
    RequestQueue queue;
    private String logTag = "MPTaskMessage";
    private static UserAuth Instance = null;
    private UserAuthListener listener;

    private void initializeValues(){
        this.mState = 0;
        this.mDemo = false;
        this.mUserID = 0;
        this.mEmail = "";
    }

    private UserAuth(){
        initializeValues();
        this.listener = null;
    }// end of constructor

    public static UserAuth getInstance(Context mContext){
        if(Instance == null){
            Instance = new UserAuth();
            Instance.queue = Volley.newRequestQueue(mContext);
        }
        return Instance;
    }

    public void setState(int value){ this.mState = value; }
    public void setDemo(boolean value){ this.mDemo = value; }
    public void setUserID(int value){ this.mUserID = value; }
    public void setEmail(String value){ this.mEmail = value; }

    public int getState(){ return this.mState; };
    public Boolean getDemo(){ return this.mDemo; };
    public int getUserID(){ return this.mUserID; };
    public String getEmail(){ return this.mEmail; };

    public void resetUser(){
        initializeValues();
    }

    public void sendAuthenticationRequest(Context context, String emailAddress){
        MPTaskState mTaskState = MPTaskState.getInstance(context);
        String urlRequest = mTaskState.getServerURL() + "api/auth_user.php?email="+emailAddress;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, (urlRequest), null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.v(logTag,"Server Connection Succcesfull");
                            if(response.getBoolean("status") == true){
                                setState(1);
                                if(Instance.listener!= null){
                                    Instance.listener.authenticationRequestReceived(response);
                                }
                            }else{
                                setState(-1);
                                if(Instance.listener!= null){
                                    Instance.listener.authenticationRequestReceived(response);
                                }
                            }
                        } catch (JSONException e) {
                            Log.v(logTag,"Error: "+e.toString());
                            setState(-1);
                            if(Instance.listener!= null){
                                Instance.listener.authenticationRequestReceived(Instance.getBaseResponse());
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(logTag,"Error: "+error.toString());
                        setState(-1);
                        if(Instance.listener!= null){
                            Instance.listener.authenticationRequestReceived(Instance.getBaseResponse());
                        }
                    }
                });
        queue.add(jsonObjectRequest);
    }// end of sendAuthenticationRequest function

    public void setUserDetails(JSONObject user){
        try {
            Instance.setUserID(user.getInt("id"));
            Instance.setEmail(user.getString("email"));
            Instance.setState(1);
            if(Instance.getEmail().contains("demo@mixpanel.com")){
                Instance.setDemo(true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface UserAuthListener{
        public void authenticationRequestReceived(JSONObject user);
    }// end of interface
    public void setUserAuthListener(UserAuthListener listener) {
        this.listener = listener;
    }

    private JSONObject getBaseResponse(){
        JSONObject response = new JSONObject();
        try {
            response.put("status", false);
            response.put("error","Issues connecting to the server");
            response.put("message","");
            response.put("user",new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

}// end of UserAuth class
