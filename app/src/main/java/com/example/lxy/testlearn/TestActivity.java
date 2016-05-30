package com.example.lxy.testlearn;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        testClassLoader();
    }

    private void testClassLoader() {
        ClassLoader myClassLoader = getClassLoader();
        while(myClassLoader != null) {
            Log.d(TAG, "classLoader1 = " + myClassLoader);
            myClassLoader = myClassLoader.getParent();

        }
    }


    public void testSerializable() {
        A a = new A();
        a.str = "old a";
        a.view = getWindow().getDecorView();
        Log.d(TAG, "a = " + a);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(a);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            try {
                A b = (A) in.readObject();
                Log.d(TAG, "b = " + b);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class A implements Serializable {
        public String str;
        public transient View view;
        public String toString() {
            return "str = " + str + " , view = " + view;
        }
    }
}
