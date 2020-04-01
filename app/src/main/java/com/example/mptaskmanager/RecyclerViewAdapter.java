package com.example.mptaskmanager;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";
    private ArrayList<MPTask> mTaskListArray = new ArrayList<>();
    private Context mContext;
    private Boolean mCompleted;

    MPTaskState mState;

    public RecyclerViewAdapter(Context context, Boolean completed){
        mContext = context;
        mCompleted = completed;
        mState = MPTaskState.getInstance(mContext);
        if(completed == true){
            mTaskListArray = mState.mCompletedTasksArray;
        }else{
            mTaskListArray = mState.mPendingTasksArray;
        }
    }// end of constructor

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(mCompleted){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item_completed_layout, parent, false);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item_pending_layout, parent, false);
        }
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.v(TAG,"onBindViewHolder called");
        holder.mText.setText(mTaskListArray.get(position).getText());
        holder.mActionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MPTask task = mTaskListArray.get(position);
                Log.v(TAG,"update click: "+position+"|id:"+mTaskListArray.get(position).getId());
                mState.sendTaskRequest("update_task",mTaskListArray.get(position));


            }
        });
    }

    @Override
    public int getItemCount() {
        return mTaskListArray.size();
    }

    public void updateRecyclerView(){
        this.notifyDataSetChanged();
    }

    public void removeItemAtPosition(int position, Boolean completed){
        Log.v(TAG,"removed item: "+position+"|id:"+mTaskListArray.get(position).getId());
    }



    public class ViewHolder extends RecyclerView.ViewHolder{

        Button mActionBtn;
        TextView mText;
        Chip mPill;
        ConstraintLayout mListLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mActionBtn = (Button) itemView.findViewById(R.id.btnAction);
            mText = (TextView) itemView.findViewById(R.id.taskText);
            mPill = (Chip) itemView.findViewById(R.id.badge);
            mListLayout = (ConstraintLayout)itemView.findViewById(R.id.listLayout);
        }

    }//end of class ViewHolder
}//end of RecyclerViewAdapter
