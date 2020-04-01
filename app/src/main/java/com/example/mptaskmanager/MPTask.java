package com.example.mptaskmanager;

public class MPTask {

    private int id = 0;
    private String text = "";
    private Boolean completed = false;

    public MPTask(){ }

    public MPTask(int idVal, String textVal, Boolean completedVal){
        this.id = idVal;
        this.text = textVal;
        this.completed = completedVal;
    }//constructor


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}//end of class
