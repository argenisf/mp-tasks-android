package com.example.mptaskmanager;

import android.content.Context;
import android.util.Log;
import android.widget.MultiAutoCompleteTextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MPTaskState {

    private static MPTaskState Instance = null;
    public String logTag = "MPTaskMessage";

    private String mServerURL = "https://mp-server-side-training-v-1.herokuapp.com/";
    private Context mContext;
    private UserAuth mUser;
    private RequestQueue queue;
    private MPTaskStateListener listener;

    public ArrayList<MPTask> mPendingTasksArray = new ArrayList<>();
    public ArrayList<MPTask> mCompletedTasksArray = new ArrayList<>();

    public static MPTaskState getInstance(Context context){
        if(Instance == null){
            Instance = new MPTaskState();
            Instance.mContext = context;
            Instance.mUser = UserAuth.getInstance(context);
            Instance.queue = Volley.newRequestQueue(context);
        }

        return(Instance);
    }//end of getInstance function

    public void setServerURL(String URL){ mServerURL = URL; }
    public String getServerURL(){ return mServerURL; }

    public void addTaskToList(MPTask task){
        if(task.getCompleted() == true){
            mCompletedTasksArray.add(task);
        }else{
            mPendingTasksArray.add(task);
        }
    }

    public MPTask findTaskById(int id){
        MPTask task = new MPTask();

        for(int i=0; i < mPendingTasksArray.size(); i++){
            if(mPendingTasksArray.get(i).getId() == id){
                task = mPendingTasksArray.get(i);
            }
        }

        if(task.getId() == 0){
            for(int i=0; i < mCompletedTasksArray.size(); i++){
                if(mCompletedTasksArray.get(i).getId() == id){
                    task = mCompletedTasksArray.get(i);
                }
            }
        }

        return task;
    }

    /*
    public void removeTasFromList(MPTask task){
        int position = getTaskPosition(task);
        if(position >= 0){
            if(task.getCompleted() == true){
                mCompletedTasksArray.remove(position);
            }else{
                mPendingTasksArray.remove(position);
            }
        }
    }
    private int getTaskPosition(MPTask task){
        ArrayList<MPTask> newArray;
        if(task.getCompleted() == true){
            for (int i = 0; i < mCompletedTasksArray.size(); i++){
                if(mCompletedTasksArray.get(i).getId() == task.getId()){
                    return i;
                }
            }
        }else{
            for (int i = 0; i < mPendingTasksArray.size(); i++){
                if(mPendingTasksArray.get(i).getId() == task.getId()){
                    return i;
                }
            }
        }
        return -1;
    }*/

    public void resetLists(){
        mPendingTasksArray = new ArrayList<>();
        mCompletedTasksArray = new ArrayList<>();
    }

    public void fillTaskList(JSONArray tasks){
        JSONObject task;
        try {
            for(int i = 0; i < tasks.length(); i++) {
                task = tasks.getJSONObject(i);
                if(task.getBoolean("completed") == true){
                    mCompletedTasksArray.add(new MPTask(task.getInt("id"),task.getString("text"),true));
                }else{
                    mPendingTasksArray.add(new MPTask(task.getInt("id"),task.getString("text"),false));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendTaskRequest(final String requestType, MPTask task){
        String urlRequest = "";

        switch (requestType){
            case "list_tasks":
                urlRequest = Instance.getServerURL() + "api/get_tasks.php?";
                urlRequest+= "user_id=" + mUser.getUserID();
                break;
            case "new_task":
                urlRequest = Instance.getServerURL() + "api/create_task.php?";
                urlRequest+= "user_id=" + mUser.getUserID();
                urlRequest+= "&text=" + task.getText();
                break;
            case "update_task":
                urlRequest = Instance.getServerURL() + "api/update_task.php?";
                urlRequest+= "user_id=" + mUser.getUserID();
                urlRequest+= "&task_id=" + task.getId();
                urlRequest+= "&text=" + task.getText();
                urlRequest+= "&completed=" + ((task.getCompleted() == true)?"false":"true");
                break;
            case "delete_task":
                urlRequest = Instance.getServerURL() + "api/delete_task.php?";
                urlRequest+= "user_id=" + mUser.getUserID();
                urlRequest+= "&task_id=" + task.getId();
                break;
        }

        if(!urlRequest.equals("")){
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, urlRequest, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Instance.listener.taskRequestReceived(requestType,response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v(logTag,"Error: "+error.toString());
                            if(Instance.listener!= null){
                                Instance.listener.taskRequestReceived(requestType, Instance.getBaseResponse());
                            }
                        }
                    });
            queue.add(jsonObjectRequest);
        }
    }

    public interface MPTaskStateListener{
        public void taskRequestReceived(String operation, JSONObject result);
    }// end of interface
    public void setMPTaskStateListener(MPTaskStateListener listener) {
        this.listener = listener;
    }

    private JSONObject getBaseResponse(){
        JSONObject response = new JSONObject();
        try {
            response.put("status", false);
            response.put("error","Issues connecting to the server");
            response.put("message","");
            response.put("tasks",new JSONArray() );
            response.put("task",new JSONObject() );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }
}// end of class
