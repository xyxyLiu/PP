package com.example.lxy.testlearn;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lxy.testlearn.lanscanner.DeviceInfoResolver;
import com.example.lxy.testlearn.lanscanner.LANScanner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "testAndLearn";
    private static final boolean DEBUG = false;
    private TextView mTextView;
    private ListView mListview;
    private Button mBtn;
    private ArrayAdapter mAdapter;
    private List<Pair<String, String>> mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text);
        mListview = (ListView) findViewById(R.id.listview);
        mBtn = (Button) findViewById(R.id.btn);
        mAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1);
        mListview.setAdapter(mAdapter);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtn.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long startTime = SystemClock.elapsedRealtime();
                        LANScanner lanScanner = LANScanner.getInstance(MainActivity.this);
                        lanScanner.startScanSync();
                        long updTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "scanning time = " + (updTime - startTime) + "ms");

                        long startParseTime = SystemClock.elapsedRealtime();
                        List<Pair<String, String>> devicesList = LANScanner.getAllCacheArp();
                        mData = devicesList;
                        long endTime = SystemClock.elapsedRealtime();
                        Log.d(TAG, "parse time = " + (endTime - startParseTime) + "ms");
                        Log.d(TAG, "total time = " + (endTime - startTime) + "ms");
                        Log.d(TAG, "devicesList = " + devicesList.size());
                        int i = 0;
                        final List<String> itemText = new ArrayList<String>();
                        for(Pair<String, String> pair : devicesList) {
                            Log.d(TAG, "devicesList No." + ++i + " : " + pair.first + " , " + pair.second);
                            itemText.add("No." + i + " ip: " + pair.first + "\nmac: " + pair.second);

                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextView.setText("total: " + itemText.size());
                                mAdapter.clear();
                                mAdapter.addAll(itemText);
                                mBtn.setEnabled(true);
                            }
                        });

                    }
                }).start();
            }
        });

        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Pair<String, String> data = mData.get(position);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String ip = data.first;
                        final String deviceInfo = DeviceInfoResolver.requestDeviceInfoByMac(data.second);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "ip : " + ip + "\n" + deviceInfo,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();

            }
        });
    }












    private void debug(String str){
        if (DEBUG) {
            String a = str;
        }
    }

    private void test() {
        try {
            final A a = new A();
            Class classA = Class.forName("com.example.lxy.testlearn.MainActivity$A", true, MainActivity.class.getClassLoader());

            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            classLoader.loadClass("com.example.lxy.testlearn.MainActivity$B");

            mTextView.setOnClickListener(new ClickListener() {
                @Override
                public void onClick(View v) {
                    mTextView.setText("a" + a.a);
                }
            });
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextView.setText("a");
                }
            });
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextView.setText("a");
                }
            });
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextView.setText("a");
                }
            });
            mTextView.setOnClickListener(new View.OnClickListener() {


                @Override
                public void onClick(View v) {

                }
            });

            C c = new C();
            c.a();
            C c1 = new C() {
                String tag = "inner C";
                @Override
                public void a() {
                    super.b();
                }
            };

            c1.a();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static class ClickListener implements View.OnClickListener {
        String tag = "ClickListener";
        @Override
        public void onClick(View v) {
            tag = "ClickListener1";
        }
    }

    private static class A {
        static {
            Log.d(TAG, "A init!");
        }
        public A() {
            b = "a_member in A()";
        }
        String a = getA();
        String b;
        public String getA(){
            return b;
        }
    }

    private static class B {
        static {
            Log.d(TAG, "B init!");
        }
        String b = "b_member";
    }

    private static class C {
        String tag = "C";
        void a(){
            tag = "a";
            b();
        }
        void b(){
            tag = "b";
            c();
        }
        void c(){
            tag = "c";
            d();
        }
        void d(){
            tag = "d";
            e();
        }
        void e(){
            tag = "e";
        }
    }




}
