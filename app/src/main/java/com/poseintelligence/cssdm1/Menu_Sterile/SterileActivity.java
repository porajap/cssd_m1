package com.poseintelligence.cssdm1.Menu_Sterile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.poseintelligence.cssdm1.CssdProject;
import com.poseintelligence.cssdm1.MainMenu;
import com.poseintelligence.cssdm1.R;
import com.poseintelligence.cssdm1.adapter.ListBoxBasketAdapter;
import com.poseintelligence.cssdm1.adapter.ListBoxMachineAdapter;
import com.poseintelligence.cssdm1.adapter.ListItemBasketAdapter;
import com.poseintelligence.cssdm1.core.connect.HTTPConnect;
import com.poseintelligence.cssdm1.core.string.Cons;
import com.poseintelligence.cssdm1.model.BasketTag;
import com.poseintelligence.cssdm1.model.Item;
import com.poseintelligence.cssdm1.model.ModelMachine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SterileActivity extends AppCompatActivity{
    private String TAG_RESULTS = "result";
    private JSONArray rs = null;
    public HTTPConnect httpConnect = new HTTPConnect();
    String folder_php = "sterile_basket/";
    public String getUrl;
    public String p_DB;

    boolean get_data = false;
    boolean show_wait = false;

    String mass_onkey="";

    RecyclerView list_mac;
    RecyclerView list_basket;
    ListView list_item_basket;
    TextView bt_delete;
    TextView bt_select_all;
    LinearLayoutManager lm;

    boolean on_scan_program_mac = false;
    public int mac_id_non_approve = -1;
    public int basket_pos_non_approve = -1;

    String ProgramID = "";

    boolean is_select_all = false;
    boolean is_add_item = false;

    ArrayList<ModelMachine> list = new ArrayList<>();
    ListBoxMachineAdapter list_mac_adapter;
    public String mac_empty_id = "Empty";

    ArrayList<BasketTag> xlist_basket = new ArrayList<>();
    ListBoxBasketAdapter list_basket_adapter;

    ArrayList<Item> xlist_item_basket = new ArrayList<>();
    ListItemBasketAdapter list_item_basket_adapter;

    public AlertDialog.Builder alert_builder;
    public AlertDialog alert;

    ProgressDialog program_dialog;
    ProgressDialog loadind_dialog;

    Handler handler  = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (alert.isShowing()) {
                alert.dismiss();
            }

            handler.removeCallbacks(runnable);
        }
    };

    Handler handler_re_scan_text  = new Handler();
    Runnable runnable_re_scan_text = new Runnable() {
        @Override
        public void run() {
            mass_onkey = "";
            handler_re_scan_text.postDelayed(runnable_re_scan_text, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sterile);

        byIntent();

        byWidget();

//        set_program_dialog();

        handler_re_scan_text.postDelayed(runnable_re_scan_text, 500);
    }

    public void byIntent(){
        getUrl=((CssdProject) getApplication()).getxUrl()+folder_php;
        p_DB = ((CssdProject) getApplication()).getD_DATABASE();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SterileActivity.this, MainMenu.class);
        startActivity(intent);
        finish();
    }

    public void byWidget(){
        list_mac = (RecyclerView) findViewById(R.id.list_mac);
        list_basket = (RecyclerView) findViewById(R.id.list_basket);
        list_item_basket = (ListView) findViewById(R.id.list_item_basket);

        bt_delete = (TextView) findViewById(R.id.bt_delete);
        bt_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i=xlist_item_basket.size()-1;i>=0;i--){
                    Log.d("tog_delete","position = "+i);
                    if(xlist_item_basket.get(i).isChk()){
                        item_to_delete(i);
                        Log.d("tog_delete","position = delete");
                    }
                }
            }
        });

        bt_select_all = (TextView) findViewById(R.id.bt_select);
        bt_select_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!is_select_all){
                    is_select_all = true;
                    bt_select_all.setText("Unselect all");
                }else{
                    is_select_all = false;
                    bt_select_all.setText("Select all");
                }

                for(int i=0;i<xlist_item_basket.size();i++){
                    xlist_item_basket.get(i).setChk(is_select_all);
                }

                list_item_basket.invalidateViews();
                get_list_checkbox_to_delete();

            }
        });

        lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        list_mac.setLayoutManager(lm);

        list_basket.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        alert_builder = new AlertDialog.Builder(SterileActivity.this);

        loadind_dialog = new ProgressDialog(SterileActivity.this);
        loadind_dialog.setMessage("Loading...");

        get_machine("null");
        get_basket("null");
    }

    public void loadind_dialog_show(){
        if(!loadind_dialog.isShowing()){
            loadind_dialog.show();
        }
    }

    public void loadind_dialog_dismis(){
        if(loadind_dialog.isShowing()){
            loadind_dialog.dismiss();
        }
    }

    public void get_machine(String mac_id) {

        class get_machine extends AsyncTask<String, Void, String> {

            int mac_select_id = -1;

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadind_dialog_show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);

                    if(mac_id.equals("null")){
                        list.clear();

                        for (int i = 0; i < rs.length(); i++) {
                            JSONObject c = rs.getJSONObject(i);

                            if (c.getString("result").equals("A")) {
                                list.add(new ModelMachine(c.getString("xID"),c.getString("xMachineName2"),c.getString("IsActive"),c.getString("IsBrokenMachine"),c.getString("DocNo")));
                            }
                        }

                        list.add(new ModelMachine(mac_empty_id,mac_empty_id,"0","0",mac_empty_id));

                        list_mac_adapter = new ListBoxMachineAdapter(SterileActivity.this, list,list_mac);
                        list_mac.setAdapter(list_mac_adapter);

                        loadind_dialog_dismis();
                    }else{
                        if(mac_id.equals(mac_empty_id)){
                            list.get(list.size()-1).setIsActive("0");
                            mac_id_non_approve = list.size()-1;
                            reload_basket();
                        }else{
                            for (int i = 0; i < rs.length(); i++) {
                                JSONObject c = rs.getJSONObject(i);
                                Log.d("tog_scan_basket","xID = "+c.getString("xID")+"---"+mac_id);

                                for (int j = 0; j < list.size(); j++) {
                                    if(list.get(j).getMachineID().equals(c.getString("xID"))){
                                        list.get(j).setIsActive(c.getString("IsActive"));
                                        list.get(j).setIsBrokenMachine(c.getString("IsBrokenMachine"));
                                        list.get(j).setIDocNo(c.getString("DocNo"));

                                        if(c.getString("xID").equals(mac_id)){
                                            if(c.getString("IsActive").equals("1")){
                                                show_dialog("Warning","Machine is running");

                                            }
                                            else if(c.getString("IsBrokenMachine").equals("1")){
                                                show_dialog("Warning","Machine is broken");
                                            }
                                            else{
                                                if(c.getString("DocNo").equals("null")){

                                                    show_dialog("Warning","No Document in machine");
//                                                    set_mac_program_id(c.getString("xMachineName2"),c.getString("xID"));
                                                }else{
                                                    get_doc_in_mac(c.getString("DocNo"));
                                                    mac_id_non_approve = j;
                                                }

                                            }


                                        }
                                    }
                                }

                            }
                        }

                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String, String>();

                data.put("mac_id", mac_id);
                data.put("p_DB", p_DB);

                String result = null;

                try {
                    result = httpConnect.sendPostRequest(getUrl + "get_sterilemachine.php", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            // =========================================================================================
        }

        get_machine obj = new get_machine();

        obj.execute();
    }

    public void get_basket(String basket_id) {

        class get_basket extends AsyncTask<String, Void, String> {

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadind_dialog_show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);

                    if(basket_id.equals("null")){
                        xlist_basket.clear();
                        for (int i = 0; i < rs.length(); i++) {
                            JSONObject c = rs.getJSONObject(i);

                            if (c.getString("result").equals("A")) {
                                xlist_basket.add(new BasketTag(c.getString("xID"),c.getString("BasketName"),c.getString("BasketCode"),c.getString("InMachineID"),0));
                            }
                        }

                        list_basket_adapter = new ListBoxBasketAdapter(SterileActivity.this, xlist_basket,list_basket);
                        list_basket.setAdapter(list_basket_adapter);

                        loadind_dialog_dismis();
                    }else{
                        for (int i = 0; i < rs.length(); i++) {
                            JSONObject c = rs.getJSONObject(i);

                            if (c.getString("result").equals("A")) {

                                if(c.getString("BasketCode").toLowerCase().equals(basket_id.toLowerCase())){

                                    for (int j = 0; j < xlist_basket.size(); j++) {
                                        if(xlist_basket.get(j).getBasketCode().equals(c.getString("BasketCode"))){
                                            xlist_basket.get(j).setMacId(c.getString("InMachineID"));

                                            int mac_pos = mac_id_non_approve;

                                            Log.d("tog_getbasket","j = "+j);

                                            if(mac_pos>0){
                                                String mac_id = list.get(mac_pos).getMachineID();
                                                if(c.getString("InMachineID").equals("null")){//ตะกร้าไม่มีเครื่อง
                                                    //add basket in mac
                                                    Log.d("tog_getbasket","mac_empty_id = "+mac_id.equals(mac_empty_id));
                                                    if(mac_id.equals(mac_empty_id)){
                                                        list_mac_adapter.onScanSelect(mac_id_non_approve);
                                                        list_basket_adapter.onScanSelect(j);
                                                    }else{
                                                        is_add_item = true;
                                                    }

                                                    get_item_in_basket(j,list.get(mac_pos).getIDocNo());

                                                }else if (c.getString("InMachineID").equals(mac_id)){
                                                    get_item_in_basket(j,list.get(mac_pos).getIDocNo());
                                                }else{
                                                    list_basket_adapter.onScanSelect(-1);
                                                    if(mac_pos==list.size()-1){
                                                        list_mac_adapter.onScanSelect(-1);
                                                        show_dialog("Warning","Basket is in some machine");
                                                    }else{
                                                        show_dialog("Warning","Basket is in another machine");
                                                    }
                                                }
                                            }else{
                                                list_basket_adapter.onScanSelect(j);
                                                get_item_in_basket(j,"");
                                            }

                                            list_mac_adapter.notifyDataSetChanged();
                                            list_basket_adapter.notifyDataSetChanged();
                                        }
                                    }

                                }
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String, String>();

                data.put("basket_id", basket_id);
                data.put("p_DB", p_DB);

                String result = null;

                try {
                    result = httpConnect.sendPostRequest(getUrl + "get_sterilebasket.php", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            // =========================================================================================
        }

        get_basket obj = new get_basket();

        obj.execute();
    }

    public void show_bt_delete(boolean s){
        if(s){
            bt_delete.setVisibility(View.VISIBLE);
        }else{
            bt_delete.setVisibility(View.GONE);
        }
    }

    public void get_list_checkbox_to_delete(){
        int x = 0;
        for(int i=0;i<xlist_item_basket.size();i++){
            if(xlist_item_basket.get(i).isChk()){

                x++;
            }
        }

        if(x==xlist_item_basket.size()){
            is_select_all = true;
            bt_select_all.setText("Unselect all");
        }else{
            is_select_all = false;
            bt_select_all.setText("Select all");
        }

        if(x==0){
            show_bt_delete(false);
        }else{
            show_bt_delete(true);
        }

    }

    public void item_to_delete(int index){
        xlist_item_basket.remove(index);
        list_item_basket.invalidateViews();

        get_item_in_basket(list_basket_adapter.select_basket_pos,"");
    }

    public void add_basket_to_mac(String docno,String basket_id) {
        class add_basket_to_mac extends AsyncTask<String, Void, String> {

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadind_dialog_show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);
                    for (int i = 0; i < rs.length(); i++) {
                        JSONObject c = rs.getJSONObject(i);

                        if (c.getString("result").equals("A")) {
                            reload_basket();
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String, String>();

                data.put("basket_id", basket_id);
                data.put("docno", docno);
                data.put("p_DB", p_DB);

                String result = null;

                try {
                    result = httpConnect.sendPostRequest(getUrl + "set_basket_to_sterile.php", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("tog_add_item","data = " + data);
                Log.d("tog_add_item","result = " + result);
                return result;
            }

            // =========================================================================================
        }

        add_basket_to_mac obj = new add_basket_to_mac();

        obj.execute();
    }

    public void add_item_to_basket(String basket_id){
        class add_item extends AsyncTask<String, Void, String> {
            private ProgressDialog dialog = new ProgressDialog(SterileActivity.this);

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                this.dialog.setMessage(Cons.WAIT_FOR_PROCESS);
                this.dialog.show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);
                    for (int i = 0; i < rs.length(); i++) {
                        JSONObject c = rs.getJSONObject(i);

                        if (c.getString("result").equals("A")) {

                            if(!list.get(list_mac_adapter.select_mac_pos).getDocNo().equals("null")){
                                addSterileDetailById(
                                        list.get(list_mac_adapter.select_mac_pos).getDocNo(),
                                        c.getString("w_id"),
                                        basket_id
                                );
                            }
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (dialog.isShowing()) {
                    dialog.dismiss();
                }


            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String, String>();

                data.put("basket_id", basket_id);
                data.put("usage_code", basket_id);
                data.put("p_DB", p_DB);

                String result = null;

                try {
                    result = httpConnect.sendPostRequest(getUrl + "get_item_in_sterile_basket.php", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("tog_add_item","data = " + data);
                Log.d("tog_add_item","result = " + result);
                return result;
            }

            // =========================================================================================
        }

        add_item obj = new add_item();

        obj.execute();
    }

    public void addSterileDetailById(final String p_docno, final String p_data,String basket_id) {

        final boolean mode = false;

        class AddSterileDetail extends AsyncTask<String, Void, String> {

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                loadind_dialog_show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);

                    for(int i=0;i<rs.length();i++){
                        JSONObject c = rs.getJSONObject(i);

                        if(c.getString("result").equals("A")) {
                            if(is_add_item){
                                add_basket_to_mac(p_docno,basket_id);
                            }else{
                                reload_basket();
                            }
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }finally{
                    is_add_item = false;
                }
            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String,String>();

                data.put("p_list_id", p_data);
                data.put("p_docno", p_docno);
                data.put("p_IsUsedUserOperationDetail", ((CssdProject) getApplication()).isSR_IsUsedUserOperationDetail() ? "1" : "0");

                data.put("p_is_status", mode ? "-1" : "1");

                data.put("p_DB", ((CssdProject) getApplication()).getD_DATABASE());

                // Add Select Insert
                String result = httpConnect.sendPostRequest(getUrl + "cssd_add_sterile_detail_by_id.php", data);

                return result;
            }

            // // ---------------------------------------------------------------
        }

        AddSterileDetail obj = new AddSterileDetail();
        obj.execute();
    }

    public void get_item_in_basket(int pos,String doc){

        Log.d("tog_get_item","pos = " + pos);
        if(pos>=0){
            String basket_id = xlist_basket.get(pos).getID();
            class get_item extends AsyncTask<String, Void, String> {
                String w_id = "";
                boolean get_data = true;

                // variable
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    loadind_dialog_show();
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);

                    try {
                        JSONObject jsonObj = new JSONObject(result);
                        rs = jsonObj.getJSONArray(TAG_RESULTS);
                        xlist_item_basket.clear();
                        for (int i = 0; i < rs.length(); i++) {
                            JSONObject c = rs.getJSONObject(i);

                            if (c.getString("result").equals("A")) {

                                xlist_item_basket.add(new Item(
                                        c.getString("xID"),
                                        c.getString("ItemStockID"),
                                        c.getString("itemname"),
                                        c.getString("UsageCode"),
                                        c.getString("WashDetailID"),
                                        c.getString("SterileDetailID"),
                                        c.getString("SterileProgramID"),
                                        false
                                ));

                                w_id = w_id+c.getString("WashDetailID")+",";

                                if(is_add_item&&!c.getString("SterileProgramID").equals(ProgramID)){
                                    ProgramID = "";
                                    show_dialog("Warning","Some item in basket program mismatch");
                                    is_add_item = false;
                                    get_data =false;
                                }
                            }else{
                                is_add_item = false;
                            }
                        }

                        Log.d("tog_get_item","is_add_item && rs.length()!=0 = " + (is_add_item && rs.length()!=0));
                        if(get_data){
                            if(is_add_item){
//                            add_item_to_basket(pos,basket_id);
                                addSterileDetailById( doc, w_id,basket_id);
                            }else{
                                list_item_basket_adapter = new ListItemBasketAdapter(SterileActivity.this,xlist_item_basket);
                                list_item_basket.setAdapter(list_item_basket_adapter);
                                list_basket_adapter.onScanSelect(pos);
                                xlist_basket.get(pos).setQty(xlist_item_basket.size());

                                list_basket_adapter.notifyDataSetChanged();

                                Log.d("tog_get_item","mac_id_non_approve = " + mac_id_non_approve);
                                list_mac_adapter.onScanSelect(mac_id_non_approve);
                                list_mac_adapter.notifyDataSetChanged();

                                loadind_dialog_dismis();
                            }
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                protected String doInBackground(String... params) {
                    HashMap<String, String> data = new HashMap<String, String>();

                    data.put("basket_id", basket_id);
                    data.put("p_DB", p_DB);

                    String result = null;

                    try {
                        result = httpConnect.sendPostRequest(getUrl + "get_item_in_sterile_basket.php", data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.d("tog_get_item","data = " + data);
                    Log.d("tog_get_item","result = " + result);
                    return result;
                }

                // =========================================================================================
            }

            get_item obj = new get_item();

            obj.execute();
        }

    }

    public void show_dialog(String title,String mass){
        mac_id_non_approve = list_mac_adapter.select_mac_pos;
        basket_pos_non_approve = list_basket_adapter.select_basket_pos;

        list_basket_adapter.notifyDataSetChanged();
        list_mac_adapter.notifyDataSetChanged();

        loadind_dialog_dismis();

        alert_builder.setTitle(title);
        alert_builder.setMessage(mass);

        alert = alert_builder.create();

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Log.d("tog_alert_onDismiss","alert_onDismiss = " + on_scan_program_mac);
                if(show_wait){
                    on_scan_program_mac = true;
                    show_wait = false;
                    program_dialog.show();
                }
            }
        });

        alert.show();

        handler.postDelayed(runnable, 1500);
    }

//    public void show_dialog_and_wait(String title,String mass){
//        show_wait = true;
//        show_dialog(title,mass);
//    }


    public void reload_mac(){
        if(list_mac_adapter.select_mac_pos<list.size()&&list_mac_adapter.select_mac_pos>=0){
            String mac_ID = list.get(list_mac_adapter.select_mac_pos).getMachineID();
            get_machine(mac_ID);
        }else{
            get_machine(mac_empty_id);
        }
    }



    public void reload_basket(){
        int basket_pos = basket_pos_non_approve;

        Log.d("tog_basket","basket pos = "+basket_pos);
        if(basket_pos>=0){
            Log.d("tog_basket","getBasketCode = "+xlist_basket.get(basket_pos).getBasketCode());
            get_basket(xlist_basket.get(basket_pos).getBasketCode());
        }else{
            list_mac_adapter.onScanSelect(mac_id_non_approve);
            list_basket_adapter.notifyDataSetChanged();
            list_mac_adapter.notifyDataSetChanged();
        }
    }

//    public void set_program_dialog(){
//        program_dialog = new ProgressDialog(SterileActivity.this);
//        program_dialog.setMessage("Please scan program");
//        program_dialog.setCancelable(false);
//
//        program_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialogInterface) {
//                Log.d("tog_alert_onDismiss","program_dialog_onDismiss = " + on_scan_program_mac);
//                on_scan_program_mac = false;
//            }
//        });
//
//        program_dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                Log.d("tog_alert_onDismiss","program_dialog = " + on_scan_program_mac);
//                program_dialog.dismiss();//dismiss dialog
//
//            }
//        });
//
//        program_dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
//            @Override
//            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
//                int keyCode = keyEvent.getKeyCode();
//                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
//                {
//                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
//                        Log.d("tog_diKeyListener","enter = "+mass_onkey);
//                        String key = mass_onkey.substring(0,1);
//                        key= key.toLowerCase();
//                        switch (key)
//                        {
//                            case "p":
//                                if(on_scan_program_mac){
//                                    check_pro_id(mass_onkey.substring(1));
//                                }
//                                break;
//                            default:
//                                show_dialog_and_wait("Warning","Not found program id");
//                        }
//                        mass_onkey = "";
//                        return false;
//                    }
//
//                    if(keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT){
//                        char unicodeChar = (char)keyEvent.getUnicodeChar();
//                        mass_onkey=mass_onkey+unicodeChar;
//                    }
//
//                    return false;
//                }
//                return false;
//            }
//        });
//    }

    public void get_doc_in_mac(String docno){

        class get_doc_in_mac extends AsyncTask<String, Void, String> {

            // variable
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadind_dialog_show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);

                try {
                    JSONObject jsonObj = new JSONObject(result);
                    rs = jsonObj.getJSONArray(TAG_RESULTS);

                    for (int i = 0; i < rs.length(); i++) {
                        JSONObject c = rs.getJSONObject(i);

                        if (c.getString("result").equals("A")) {
                            list.get(mac_id_non_approve).setSterileProgramID(c.getString("SterileProgramID"));
                            list.get(mac_id_non_approve).setSterileRoundNumber(c.getString("SterileRoundNumber"));

                            ProgramID = c.getString("SterileProgramID");
                            get_data =true;
                            reload_basket();
                        }
                    }

                    if(!get_data){
                        show_dialog("Warning","Not found program id");
                        get_data = false;
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> data = new HashMap<String, String>();

                data.put("docno", docno);
                data.put("p_DB", p_DB);

                String result = null;

                try {
                    result = httpConnect.sendPostRequest(getUrl + "get_sterile_doc.php", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("tog_sterile_process","result = "+result);

                return result;
            }

        }

        get_doc_in_mac obj = new get_doc_in_mac();

        obj.execute();
    }

//    public void set_mac_program_id(String mac_name,String mac_id){
//        mac_id_non_approve = mac_id;
//        on_scan_program_mac = true;
//        program_dialog.setTitle(mac_name);
//
//        program_dialog.show();
//    }

//    public void check_pro_id(String pro_id){
//
//        Log.d("tog_sterile_process","pro_id = "+pro_id);
//        class check_pro_id extends AsyncTask<String, Void, String> {
//            private ProgressDialog dialog = new ProgressDialog(SterileActivity.this);
//
//            // variable
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                this.dialog.setMessage(Cons.WAIT_FOR_PROCESS);
//                this.dialog.show();
//            }
//
//            @Override
//            protected void onPostExecute(String result) {
//                super.onPostExecute(result);
//
//                try {
//                    JSONObject jsonObj = new JSONObject(result);
//                    rs = jsonObj.getJSONArray(TAG_RESULTS);
//
//                    for (int i = 0; i < rs.length(); i++) {
//                        JSONObject c = rs.getJSONObject(i);
//
//                        if (c.getString("result").equals("A")) {
//                            if(pro_id.equals(c.getString("process_id"))){
//                                process_id = c.getString("process_id");
//                                get_data = true;
//                                get_machine(mac_id_non_approve);
////                                reload_basket();
//                            }
//                        }
//                    }
//
//                    if(!get_data){
//                        show_dialog_and_wait("Warning","Not found program id");
//                        get_data = false;
//                    }
//
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//
//                if (dialog.isShowing()) {
//                    dialog.dismiss();
//                }
//
//
//            }
//
//            @Override
//            protected String doInBackground(String... params) {
//                HashMap<String, String> data = new HashMap<String, String>();
//
//                data.put("mac_id", mac_id_non_approve);
//                data.put("p_DB", p_DB);
//
//                String result = null;
//
//                try {
//                    result = httpConnect.sendPostRequest(getUrl + "get_sterile_process.php", data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//                Log.d("tog_sterile_process","result = "+result);
//
//                return result;
//            }
//
//        }
//
//        check_pro_id obj = new check_pro_id();
//
//        obj.execute();
//    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        int keyCode = event.getKeyCode();

        Log.d("tog_allKey","keyCode = "+keyCode);
        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            if(keyCode == KeyEvent.KEYCODE_BACK ){
                onBackPressed();
            }
            else if (keyCode == KeyEvent.KEYCODE_ENTER) {
                Log.d("tog_dispatchKey","enter = "+mass_onkey);
                String key = mass_onkey.substring(0,1);
                key= key.toLowerCase();
                switch (key)
                {
                    case "m":
                        get_machine(mass_onkey.substring(1));
                        break;
                    case "b":
                        for(int i=0;i<xlist_basket.size();i++){
                            if(xlist_basket.get(i).getBasketCode().equals(mass_onkey)){
                                basket_pos_non_approve=i;
                                reload_mac();
                                mass_onkey = "";
                                return false;
                            }
                        }
                        show_dialog("Warning","Not found basket");
                        break;
                    default:
                }
                mass_onkey = "";
                return false;
            }

            if(keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT){
                char unicodeChar = (char)event.getUnicodeChar();
                mass_onkey=mass_onkey+unicodeChar;
            }

            Log.d("tog_dispatchKey","keyCode = "+keyCode);
            Log.d("tog_dispatchKey","mass_onkey = "+mass_onkey);

            return false;
        }

        return super.dispatchKeyEvent(event);
    }


}