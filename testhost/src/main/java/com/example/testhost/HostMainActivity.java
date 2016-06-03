package com.example.testhost;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.multidexmodeplugin.DexClassLoaderPluginManager;
import com.example.multidexmodeplugin.MultiDexPluginManager;

import java.lang.reflect.Method;

public class HostMainActivity extends AppCompatActivity {

    static final String TAG = "HostMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_main);
//        testMultiDexModePlugin();
        testDexCLassLoaderModePlugin();
    }

    private void testMultiDexModePlugin() {
        MultiDexPluginManager.install(getApplicationContext(), "testplugin-debug.apk");
        try {
            Class<?> clazz = Class.forName("com.example.testplugin.TestUtils");

            Object testUtilsObj = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("test");
            Log.d(TAG, "testPlugin success! \n testUtilsObj.test() = " + method.invoke(testUtilsObj));

            Intent pluginIntent = new Intent();
            pluginIntent.setClassName(this, "com.example.testplugin.PluginMainActivity");
            startActivity(pluginIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDexCLassLoaderModePlugin() {
        DexClassLoaderPluginManager.install(getApplicationContext(), "testplugin-debug.apk", true);
        try {
            Class<?> clazz = DexClassLoaderPluginManager.loadClass(this, "com.example.testplugin.TestUtils");

            Object testUtilsObj = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("test");
            Log.d(TAG, "testPlugin success! \n testUtilsObj.test() = " + method.invoke(testUtilsObj));

            Intent pluginIntent = new Intent();
            pluginIntent.setClassName(this, "com.example.testplugin.PluginMainActivity");
            startActivity(pluginIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
