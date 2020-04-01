package com.example.mptaskmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainListView extends AppCompatActivity {

    MPTaskState mState;
    UserAuth mUser;
    Context mContext;

    Button mLogOut;
    Button mChangeAccount;
    Button mBtnNewTask;
    TextView mAuthLabel;
    ConstraintLayout mMainForm;
    EditText mNewTaskInput;

    RecyclerView pendingTasksRecycler;
    RecyclerView completedTasksRecycler;

    RecyclerViewAdapter pendingAdapter;
    RecyclerViewAdapter completedAdapter;

    private MixpanelAPI mixpanel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list_view);
        mContext = this;
        mixpanel = MixpanelAPI.getInstance(this,MainActivity.MPToken);

        mMainForm = (ConstraintLayout) findViewById(R.id.mainFormLayout);
        mLogOut = (Button) findViewById(R.id.btnLogOut);
        mChangeAccount = (Button) findViewById(R.id.btnChangeAccount);
        mAuthLabel = (TextView) findViewById(R.id.authenticationLabel);
        mNewTaskInput = (EditText) findViewById(R.id.newTaskInput);
        mBtnNewTask = (Button) findViewById(R.id.btnNewTask);

        mState = MPTaskState.getInstance(mContext);
        mUser = UserAuth.getInstance(mContext);

        //set user
        if (mUser.getEmail().equals("demo@mixpanel.com")) {
            mMainForm.removeView(mLogOut);
            mMainForm.removeView(mAuthLabel);
        }else{
            mMainForm.removeView(mChangeAccount);
        }
        mAuthLabel.setText(mUser.getEmail());


        //add listeners
        mLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityLogOut();
            }
        });
        mChangeAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityLogOut();
            }
        });

        mNewTaskInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!mNewTaskInput.getText().toString().equals("") &&  (actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE)) {
                    sendNewTaskRequest();
                    return true;
                }
                return false;
            }
        });

        mBtnNewTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mNewTaskInput.getText().toString().equals("")){
                    sendNewTaskRequest();
                }
            }
        });

        //load tasks for user
        Toast.makeText(mContext,"Loading tasks",Toast.LENGTH_LONG).show();

        mState.sendTaskRequest("list_tasks", new MPTask());
        mState.setMPTaskStateListener(new MPTaskState.MPTaskStateListener() {
            @Override
            public void taskRequestReceived(String operation, JSONObject result) {
                Log.v(mState.logTag,operation);
                switch (operation){
                    case "list_tasks":
                        receivedTasksForUser(result);
                        break;
                    case "new_task":
                        receivedNewTaskRequest(result);
                        break;
                    case "update_task":
                        receivedUpdateTaskRequest(result);
                        break;
                    case "delete_task":
                        receivedDeleteTaskRequest(result);
                        break;
                }
            }
        });

    }//end of onCreate

    @Override
    protected void onResume() {
        super.onResume();
        JSONObject eventProps = new JSONObject();
        try {
            eventProps.put("screen", "Home Screen");

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
    } //onResume

    private void receivedTasksForUser(JSONObject response){
        try {
            if(response.getBoolean("status") == true) {
                mState.fillTaskList(response.getJSONArray("tasks"));
                initRecyclerViews();
            }else{
                Toast.makeText(mContext,response.getString("error"),Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext,"Error loading tasks",Toast.LENGTH_SHORT).show();
        }
    }


    private void sendNewTaskRequest(){
        mState.sendTaskRequest("new_task", new MPTask(0,mNewTaskInput.getText().toString(),false));
        mNewTaskInput.setEnabled(false);
        mBtnNewTask.setEnabled(false);
    }
    private void receivedNewTaskRequest(JSONObject response){
        mNewTaskInput.setText("");
        mNewTaskInput.setEnabled(true);
        mBtnNewTask.setEnabled(true);
        try {
            if(response.getBoolean("status") == true) {
                mixpanel.track("Task Created");
                Log.v(mState.logTag,"new task created");
                Log.v(mState.logTag,response.toString());
                JSONObject task = response.getJSONObject("task");
                mState.addTaskToList(new MPTask(task.getInt("id"),task.getString("text"),false));
                pendingAdapter.updateRecyclerView();
                if(response.has("message") && !response.getString("message").equals("")){
                    communicateToTheUI(response.getString("message"),false);
                }else{
                    communicateToTheUI("Task created",false);
                }
            }else{
                communicateToTheUI(response.getString("error"),true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            communicateToTheUI("Error creating task",true);
        }
    }

    private void receivedDeleteTaskRequest(JSONObject response){
        try {
            if(response.getBoolean("status") == true) {
                mixpanel.track("Task Deleted");
                Log.v(mState.logTag,"task deleted");
                Log.v(mState.logTag,response.toString());
                JSONObject task = response.getJSONObject("task");
                MPTask currentTask = mState.findTaskById(task.getInt("id"));

                if(currentTask.getCompleted() == false){
                    mState.mPendingTasksArray.remove(currentTask);
                    pendingAdapter.updateRecyclerView();
                }else{
                    mState.mCompletedTasksArray.remove(currentTask);
                    completedAdapter.updateRecyclerView();
                }
                if(response.has("message") && !response.getString("message").equals("")){
                    communicateToTheUI(response.getString("message"),false);
                }else{
                    communicateToTheUI("Task deleted",false);
                }
            }else{
                communicateToTheUI(response.getString("error"),true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            communicateToTheUI("Error deleting task",true);
        }
    }

    private void receivedUpdateTaskRequest(JSONObject response){
        try {
            if(response.getBoolean("status") == true) {
                mixpanel.track("Task Updated");
                Log.v(mState.logTag,"task updated");
                Log.v(mState.logTag,response.toString());
                JSONObject task = response.getJSONObject("task");
                MPTask currentTask = mState.findTaskById(task.getInt("id"));
                MPTask updatedTask = new MPTask(currentTask.getId(),currentTask.getText(),task.getBoolean("completed"));

                if(updatedTask.getCompleted() == true){
                    mState.mPendingTasksArray.remove(currentTask);
                    mState.mCompletedTasksArray.add(updatedTask);
                }else{
                    mState.mPendingTasksArray.add(updatedTask);
                    mState.mCompletedTasksArray.remove(currentTask);
                }
                pendingAdapter.updateRecyclerView();
                completedAdapter.updateRecyclerView();

                if(response.has("message") && !response.getString("message").equals("")){
                    communicateToTheUI(response.getString("message"),false);
                }else{
                    communicateToTheUI("Task updated",false);
                }
            }else{
                communicateToTheUI(response.getString("error"),true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            communicateToTheUI("Error deleting task",true);
        }
    }

    private void communicateToTheUI(String message, Boolean error){
        if(error == false){
            Toast.makeText(mContext,message,Toast.LENGTH_SHORT).show();
        }else{
            new MaterialAlertDialogBuilder(mContext)
                    .setTitle("Error")
                    .setMessage(message)
                    .setPositiveButton("Ok", null)
                    .show();
        }
    }

    private void initRecyclerViews(){
        pendingTasksRecycler = findViewById(R.id.pendingTasksRecycler);
        completedTasksRecycler = findViewById(R.id.completedTasksRecycler);

        pendingAdapter = new RecyclerViewAdapter(mContext,false);
        completedAdapter = new RecyclerViewAdapter(mContext,true);

        pendingTasksRecycler.setAdapter(pendingAdapter);
        completedTasksRecycler.setAdapter(completedAdapter);

        new ItemTouchHelper(pendingSwipeCallback).attachToRecyclerView(pendingTasksRecycler);
        pendingTasksRecycler.setLayoutManager(new LinearLayoutManager(mContext));

        new ItemTouchHelper(completedSwipeCallback).attachToRecyclerView(completedTasksRecycler);
        completedTasksRecycler.setLayoutManager(new LinearLayoutManager(mContext));
    }
    ItemTouchHelper.SimpleCallback pendingSwipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            pendingAdapter.removeItemAtPosition(viewHolder.getAdapterPosition(), false);
            mState.sendTaskRequest("delete_task", mState.mPendingTasksArray.get(viewHolder.getAdapterPosition()));
        }
    };
    ItemTouchHelper.SimpleCallback completedSwipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            completedAdapter.removeItemAtPosition(viewHolder.getAdapterPosition(), true);
            mState.sendTaskRequest("delete_task", mState.mCompletedTasksArray.get(viewHolder.getAdapterPosition()));
        }
    };

    @Override
    public void onBackPressed() { }

    public void activityLogOut(){
        //let's only reset if the user was not logged in as demo
        if(!mUser.getEmail().equals("demo@mixpanel.com")){
            mixpanel.track("Logout");
            mixpanel.flush();
            mixpanel.reset();
        }

        // Change saved preferences
        SharedPreferences sharedPref = mContext.getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("userId",0);
        editor.putString("email","");
        editor.commit();

        // Update user
        mUser.resetUser();
        this.finish();

        //update Lists
        mState.resetLists();
    }
}// end of activity
