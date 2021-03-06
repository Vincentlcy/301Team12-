package com.example.dada.Model.Task;

import android.location.Location;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;

import com.example.dada.Constant;
import com.example.dada.Exception.TaskException;
import com.example.dada.Model.OnAsyncTaskCompleted;
import com.example.dada.Model.OnAsyncTaskFailure;
import com.example.dada.Util.TaskUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.searchly.jestdroid.DroidClientConfig;
import com.searchly.jestdroid.JestClientFactory;
import com.searchly.jestdroid.JestDroidClient;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.searchbox.core.Delete;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

public abstract class Task {

    private String ID;
    private double price;
    private String title;
    private String taskDescription;
    private String status;
    private String requesterUserName;
    private String providerUserName;
    private Boolean isCompleted;

    private ArrayList<String> providerList = new ArrayList<>();

    private transient static JestDroidClient client;

    /**
     * Empty constructor for fun :)
     */
    public Task() {

    }

    /**
     * Constructor for a new task.
     *
     * @param title         title of the task
     * @param description   description of the task
     * @param
     */
    public Task(String title, String description, String requesterUserName, String status) {
        this.title = title;
        this.taskDescription = description;
        this.status = status;
        this.requesterUserName = requesterUserName;
    }

    /**
     * Constructor for AssignedTask or CompletedTask.
     *
     * @param requesterUserName  the requester user name
     * @param providerUserName   the provider user name
     * @param price              the price
     */
    public Task(String requesterUserName, String providerUserName, double price) {
        this.requesterUserName = requesterUserName;
        this.providerUserName = providerUserName;
        this.price = price;
    }

    /**
     * Constructor for BiddedTask
     *
     * @param requesterUserName     the requester user name
     * @param providerList          the list of providers username who bidded the task
     * @param price                 the price
     */
    public Task(String requesterUserName, ArrayList<String> providerList, Double price){
        this.requesterUserName = requesterUserName;
        this.providerList = providerList;
        this.price = price;
    }

    /**
     * Static class that adds the task
     */
    public static class CreateTaskTask extends AsyncTask<Task, Void, Task> {
        /**
         * The Listener.
         */
        public OnAsyncTaskCompleted listener;
        public OnAsyncTaskFailure offlineHandler;
        // http://stackoverflow.com/questions/9963691/android-asynctask-sending-callbacks-to-ui
        // Author: Dmitry Zaitsev
        private TaskException taskException;

        /**
         * Constructor for CreateRequestTask class
         *
         * @param listener the customize job after the async task is done
         */
        public CreateTaskTask(OnAsyncTaskCompleted listener) {
            this.listener = listener;
        }

        /**
         * Constructor for create request async task
         * @param listener the customize job after the async task is done
         * @param offlineHandler the customize job after the async task is fail
         */
        public CreateTaskTask(OnAsyncTaskCompleted listener, OnAsyncTaskFailure offlineHandler) {
            this.listener = listener;
            this.offlineHandler = offlineHandler;
        }

        /**
         * Update the task when user bid a bidded or requested task
         * @param tasks the task object to be create
         */
        @Override
        protected Task doInBackground(Task... tasks) {
            verifySettings();
            Task task = new RequestedTask();
            for (Task t : tasks) {
                Index index = new Index.Builder(t).index("team12").type("task").id(t.getID()).build();
                try {
                    DocumentResult result = client.execute(index);
                    if (result.isSucceeded()) {
                        // set ID
                        t.setID(result.getId());
                        task = t;
                        Log.i("Debug", "Successful create task");
                    } else {
                        Log.i("Debug", "Elastic search was not able to add the request.");
                    }
                } catch (Exception e) {
                    taskException = new TaskException("Application lost connection to the server");
                    Log.i("Debug", "We failed to add a request to elastic search!");
                    e.printStackTrace();
                }
            }
            return tasks[0];
        }

        /**
         * Excute after async task is finished
         * Stuff like notify arrayadapter the data set is changed
         * @param task the request
         */
        @Override
        protected void onPostExecute(Task task) {
            if (listener != null && taskException == null) {
                listener.onTaskCompleted(task);
            } else if (offlineHandler != null && taskException != null) {
                Log.i("Debug", "Fail to upload");
                offlineHandler.onTaskFailed(task);
            }
        }
    }

    /**
     * Static class that update the task
     */
    public static class UpdateTaskTask extends AsyncTask<Task, Void, Task> {
        /**
         * The Listener.
         */
        public OnAsyncTaskCompleted listener;
        public OnAsyncTaskFailure offlineHandler;
        private TaskException taskException;

        /**
         * Constructor for UpdateTaskTask class
         *
         * @param listener the customize job after the async task is done
         */
        public UpdateTaskTask(OnAsyncTaskCompleted listener) {
            this.listener = listener;
        }

        /**
         * Constructor for UpdateTaskTask async task
         * @param listener the customize job after the async task is done
         * @param offlineHandler the customize job after the async task is fail
         */
        public UpdateTaskTask(OnAsyncTaskCompleted listener, OnAsyncTaskFailure offlineHandler) {
            this.listener = listener;
            this.offlineHandler = offlineHandler;
        }

        /**
         * Update the task when user assigned, delete a task
         * @param tasks the task object to be updated
         */
        @Override
        protected Task doInBackground(Task... tasks) {
            verifySettings();
            // Constructs json string
            String query = TaskUtil.serializer(tasks[0]);
            Log.i("Debug", query);
            Index index = new Index.Builder(query)
                    .index("team12")
                    .type("task")
                    .id(tasks[0].getID()).build();
            try {
                DocumentResult result = client.execute(index);

                if (result.isSucceeded()) {
                    Log.i("Debug", "Successful update the request");
                } else {
                    Log.i("Debug", "Elastic search was not able to add the request.");
                }
            } catch (Exception e) {
                taskException = new TaskException("Application lost connection to the server");
                Log.i("Debug", "We failed to add a request to elastic search!");
                e.printStackTrace();
            }
            return tasks[0];
        }

        /**
         * Excute after async task is finished
         * Stuff like notify arrayadapter the data set is changed
         * @param task the task
         */
        @Override
        protected void onPostExecute(Task task) {
            if (listener != null && taskException == null) {
                listener.onTaskCompleted(task);
            } else if (offlineHandler != null && taskException != null) {
                Log.i("Debug", "Fail to upload");
                offlineHandler.onTaskFailed(task);
            }
        }
    }

    /**
     * TODO Static class that cancel the task
     */
    public static class DeleteTaskTask extends AsyncTask<Task, Void, Task> {
        /**
         * The Listener.
         */
        public OnAsyncTaskCompleted listener;

        /**
         * Constructor for DeleteTaskTask class
         *
         * @param listener the customize job after the async task is done
         */
        public DeleteTaskTask(OnAsyncTaskCompleted listener) {
            this.listener = listener;
        }

        /**
         * Cancel the request
         * @param tasks the request object to be canceled
         */
        @Override
        protected Task doInBackground(Task... tasks) {
            verifySettings();

            for (Task t : tasks) {
                Delete delete = new Delete.Builder(t.getID()).index("team12").type("task").build();
                try {
                    DocumentResult result = client.execute(delete);
                    if (result.isSucceeded()) {
                        Log.i("Debug", "Successful delete request");
                    } else {
                        Log.i("Error", "Elastic search was not able to add the request.");
                    }
                } catch (Exception e) {
                    Log.i("Error", "We failed to add a request to elastic search!");
                    e.printStackTrace();
                }
            }
            return tasks[0];
        }

        /**
         * Excute after async task is finished
         * Stuff like notify arrayadapter the data set is changed
         * @param task nothing
         */
        @Override
        protected void onPostExecute(Task task) {
            if (listener != null) {
                listener.onTaskCompleted(task);
            }
        }
    }

    /**
     * Static class that fetch request from server
     */
    public static class GetTasksListTask extends AsyncTask<String, Void, ArrayList<NormalTask>> {
        /**
         * The Listener.
         */
        public OnAsyncTaskCompleted listener;

        /**
         * Instantiates a new Get requests list task.
         *
         * @param listener the listener
         */
        public GetTasksListTask(OnAsyncTaskCompleted listener) {
            this.listener = listener;
        }

        /**
         * Fetch request list that matched the parameters, by keyword, geo-location, and all requests
         * @param search_parameters the parameter to search
         * @return an arraylist of tasks
         */
        @Override
        protected ArrayList<NormalTask> doInBackground(String... search_parameters) {
            verifySettings();

            ArrayList<NormalTask> tasks = new ArrayList<>();

            // assume that search_parameters[0] is the only search term we are interested in using
            Search search = new Search.Builder(search_parameters[0])
                    .addIndex("team12")
                    .addType("task")
                    .build();
            try {
                SearchResult result = client.execute(search);
                if (result.isSucceeded()) {
                    List<NormalTask> findTask = result.getSourceAsObjectList(NormalTask.class);
                    tasks.addAll(findTask);
                    Log.i("Debug", "Successful get the request list");
                }
                else {
                    Log.i("Error", "The search query failed to find any tweets that matched.");
                }
            }
            catch (Exception e) {
                Log.i("Error", "Something went wrong when we tried to communicate with the elasticsearch server!");
            }
            return tasks;
        }

        @Override
        protected void onPostExecute(ArrayList<NormalTask> normalTasks) {
            if (listener != null) {
                ArrayList<Task> tasksList = new ArrayList<>();
                for (NormalTask t : normalTasks) {
                    tasksList.add(t);
                }
                listener.onTaskCompleted(tasksList);
            }
        }
    }


    /**
     * Set up the connection with server
     */
    private static void verifySettings() {
        // if the client hasn't been initialized then we should make it!
        if (client == null) {
            DroidClientConfig.Builder builder = new DroidClientConfig
                    .Builder(Constant.ELASTIC_SEARCH_URL);
            DroidClientConfig config = builder.build();

            JestClientFactory factory = new JestClientFactory();
            factory.setDroidClientConfig(config);
            client = (JestDroidClient) factory.getObject();
        }
    }

    /**
     * Overide toString method
     * @return the descitipion
     */
    @Override
    public String toString() {
        if (taskDescription == null) return null;
        return taskDescription;
    }

    /**
     * Provider bids the requested task.
     *
     * @param providerUserName the provider user name who bids the requested task
     */
    public void providerBidTask(String providerUserName) {
        if ( this.status == "Requested" ){
            this.status = "Bidded";
        }
        providerList.add(providerUserName);
    }

    /**
     * Requester assign provider.
     *
     * @param  providerUserName the provider user name
     * @throws TaskException    the request exception
     * @throws TaskException    raise exception when request has not been confirmed
     */
    public void requesterAssignProvider(String providerUserName) throws TaskException {
        if (providerList == null || providerList.isEmpty()) {
            // If the task has not been bidded yet
            throw new TaskException("This task has not been bidded by any provider yet");
        } else {
            // Assigned provider
            assert this.status == "Bidded";
            this.providerUserName = providerUserName;
            this.status = "Assigned";
            providerList.clear();
        }
    }

    /**
     * Requester confirm task complete.
     */
    public void requesterConfirmTaskComplete() {
        this.isCompleted = true;
    }

    /**
     * Getters and Setters
     */
    public String getID(){ return this.ID; }

    public void setID(String ID){
        this.ID = ID;
    }

    public Double getPrice(){ return this.price; }

    public void setPrice(Double price){ this.price = price; }

    public String getTitle(){ return this.title; }

    public void setTitle(String title){ this.title = title; }

    public String getTaskDescription(){
        return this.taskDescription;
    }

    public void setTaskDescription(String taskDescription){
        this.taskDescription = taskDescription;
    }

    public String getStatus(){
        return this.status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getRequesterUserName(){ return this.requesterUserName; }

    public void setRequesterUserName(String requesterUserName){ this.requesterUserName = requesterUserName; }

    public String getProviderUserName(){ return this.providerUserName; }

    public void setProviderUserName(String providerUserName){ this.providerUserName = providerUserName; }

    public Boolean getIsCompleted(){ return this.isCompleted; }

    public void setIsCompleted(boolean isCompleted){ this.isCompleted = isCompleted; }

    public ArrayList<String> getProviderList(){ return this.providerList; }

    public void setProviderList(ArrayList<String> providerList){ this.providerList = providerList; }
}
