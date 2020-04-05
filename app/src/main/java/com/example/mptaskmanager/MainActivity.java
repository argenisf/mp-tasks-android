package com.example.mptaskmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    MPTaskState mState;
    UserAuth mUser;
    Button demoAuthBtn;
    Button emailAuthBtn;
    EditText emailInput;
    Boolean activeRequest = false;
    Context mContext;
    ProgressBar mProgress;
    SharedPreferences sharedPref;
    String customServerURL = "http://10.0.0.228:8888/mp-server-side-training/";
    public static String MPToken = "5796d9a5a728ba2493188cef50389c61";
    private MixpanelAPI mixpanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mixpanel = MixpanelAPI.getInstance(this, MPToken);
        mixpanel.track("App Launch");

        mContext = this;
        demoAuthBtn = (Button) findViewById(R.id.demoAuthBtn);
        emailAuthBtn = (Button) findViewById(R.id.emailAuthBtn);
        emailInput = (EditText) findViewById(R.id.inputEmail);
        mProgress = (ProgressBar) findViewById(R.id.requestInProgress);
        sharedPref = mContext.getSharedPreferences("user_auth", Context.MODE_PRIVATE);

        //hide progress bar by default
        mProgress.setVisibility(View.INVISIBLE);

        mState = MPTaskState.getInstance(mContext);
        //mState.setServerURL(customServerURL);
        mUser = UserAuth.getInstance(mContext);
        loadUserInfo();

        if(mUser.getState() == 1){
            //user already logged in, let's go to the activity
            createOrUpdateProfile();
            Intent intent = new Intent(mContext, MainListView.class);
            startActivity(intent);
        }

        // Listener for Demo Button
        demoAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!activeRequest){
                    setActiveRequest();
                    mUser.sendAuthenticationRequest(mContext, "demo@mixpanel.com");
                }
            }
        });
        // Listener for Email auth button
        emailAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString();
                if (email == "" || !email.contains("@mixpanel.com")) {
                    sendErrorMessage("mixpanel.com email required");
                }else{
                    if(!activeRequest){
                        setActiveRequest();
                        mUser.sendAuthenticationRequest(mContext, email);
                    }
                }
            }
        });

        // Listener for Authentication response
        mUser.setUserAuthListener(new UserAuth.UserAuthListener() {
            @Override
            public void authenticationRequestReceived(JSONObject response) {
                try {
                    if(!response.getBoolean("status")){
                        sendErrorMessage(response.getString("error"));
                        setInactiveRequest();
                    }else{
                        JSONObject user = response.getJSONObject("user");
                        //save values to singleton class
                        mUser.setUserDetails(user);
                        setInactiveRequest();
                        emailInput.setText("");

                        //save values to shared preferences
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("userId",mUser.getUserID());
                        editor.putString("email",mUser.getEmail());
                        editor.commit();

                        //track to Mixpanel
                        if(!mUser.getEmail().equals("demo@mixpanel.com")){
                            if(response.getString("message").contains("New account")){
                                //response for new users
                                mixpanel.track("Signup");
                                mixpanel.alias(mUser.getEmail(), null);
                                mixpanel.getPeople().identify(mixpanel.getDistinctId());
                            }else{
                                mixpanel.identify(mUser.getEmail());
                                mixpanel.getPeople().identify(mUser.getEmail());
                                mixpanel.track("Login");
                            }

                            //setup profile
                            createOrUpdateProfile();
                        }

                        Intent intent = new Intent(mContext, MainListView.class);
                        startActivity(intent);
                    }
                } catch (JSONException e) {
                    setInactiveRequest();
                    e.printStackTrace();
                }
            }
        });
    }// end of onCreate

    @Override
    protected void onResume() {
        super.onResume();
        JSONObject eventProps = new JSONObject();
        try {
            eventProps.put("screen", "Authentication");

            JSONObject superProps = new JSONObject();
            superProps.put("demo",false);
            if(mUser.getEmail().contains("@mixpanel.com")){
                if(mUser.getEmail().equals("demo@mixpanel.com")){
                    superProps.put("demo",true);
                }
            }
            mixpanel.registerSuperProperties(superProps);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mixpanel.track("Screen Loaded", eventProps);
    }

    private void createOrUpdateProfile(){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        mixpanel.getPeople().set("$email",mUser.getEmail());
        mixpanel.getPeople().setOnce("$created",nowAsISO);
        mixpanel.getPeople().set("Last App Activity", nowAsISO);
        mixpanel.flush();
    }


    private void setActiveRequest(){
        activeRequest = true;
        mProgress.setVisibility(View.VISIBLE);
        demoAuthBtn.setEnabled(false);
        emailAuthBtn.setEnabled(false);
        emailInput.setEnabled(false);
    }

    private void setInactiveRequest(){
        activeRequest = false;
        mProgress.setVisibility(View.INVISIBLE);
        demoAuthBtn.setEnabled(true);
        emailAuthBtn.setEnabled(true);
        emailInput.setEnabled(true);
    }

    private void sendErrorMessage(String message){
        new MaterialAlertDialogBuilder(mContext)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show();
    }

    //user authentication
    private void loadUserInfo(){
        int userId = sharedPref.getInt("userId",0);
        if(userId > 0){
            mUser.setUserID(userId);
            mUser.setState(1);
            mUser.setEmail(sharedPref.getString("email",""));
            if(mUser.getEmail().equals("demo@mixpanel.com")){
                mUser.setDemo(true);
            }
        }
        //int highScore = sharedPref.getInt(getString(R.string.saved_high_score_key), defaultValue);
    }

}// end of MainActivity Class
