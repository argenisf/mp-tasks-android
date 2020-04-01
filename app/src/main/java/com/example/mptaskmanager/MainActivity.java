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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.util.Formatter;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
