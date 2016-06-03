package com.example.multidexmodeplugin;

import android.content.Context;
import android.util.Log;

import com.example.multidexmodeplugin.reflect.FieldUtils;
import com.example.multidexmodeplugin.reflect.MethodUtils;
import com.example.multidexmodeplugin.reflect.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by lxy on 16-6-1.
 */
public class DexClassLoaderPluginManager {
    private static final String TAG = "DexClassLoderPM";
    private static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    private static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";

    private static List<ClassLoader> sClassLoaderList = new ArrayList<>();
    private static Object sClassLoaderProxy;

    public static boolean install(Context hostContext, String pluginApkName) {
        return install(hostContext, pluginApkName, false);
    }

    public static boolean install(Context hostContext, String pluginApkName, boolean injectPluginClassLoaderInHost) {
        try {
            File apkFile = AssetsManager.copyAssetsApk(hostContext, pluginApkName);
            File pluginDexPath = hostContext.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = hostContext.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            ClassLoader parentClassLoader = hostContext.getClassLoader().getParent();
            DexClassLoader dexClassLoader = new DexClassLoader(apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginLibPath.getAbsolutePath(), parentClassLoader);
            synchronized (sClassLoaderList) {
                sClassLoaderList.add(dexClassLoader);
            }

            if (injectPluginClassLoaderInHost) {
                injectPluginClassLoader(hostContext, dexClassLoader);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public static void injectPluginClassLoader(Context hostContext, ClassLoader pluginClassLoader) {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThreadObj = MethodUtils.invokeStaticMethod(activityThreadClass, "currentActivityThread");
            if (activityThreadObj != null) {
                Object mPackagesObj = FieldUtils.readField(activityThreadObj, "mPackages");
                Object hostLoadedApkRef = MethodUtils.invokeMethod(mPackagesObj, "get", hostContext.getPackageName());
                if (hostLoadedApkRef != null) {
                    Log.d(TAG, "hostLoadedApkRef = " + hostLoadedApkRef);
                    final Object hostLoadedApk = MethodUtils.invokeMethod(hostLoadedApkRef, "get");
                    Log.d(TAG, "hostLoadedApk = " + hostLoadedApk);
                    final Object classLoaderObj = FieldUtils.readField(hostLoadedApk, "mClassLoader");
                    Log.d(TAG, "classLoaderObj = " + classLoaderObj);
                    if (classLoaderObj != null) {
                        List<Class<?>> interfaces = Utils.getAllInterfaces(classLoaderObj.getClass());
                        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
                        Object hostClassLoaderProxy = Proxy.newProxyInstance(hostContext.getClassLoader(), ifs, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                Log.d(TAG, "invoke = " + method.getName());
                                if (method.getName().contains("findClass")) {
                                    try {
                                        Class<?> clazz = (Class)method.invoke(classLoaderObj, args);
                                        return clazz;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception = " + e);
                                        return null;
                                    }
                                } else {
                                    return method.invoke(classLoaderObj, args);
                                }
                            }
                        });
                        sClassLoaderProxy = hostLoadedApk;
//                        Object classObj = MethodUtils.invokeMethod(classLoaderObj, "findClass", "java.lang.Object");
//                        Log.d(TAG, "classObj = " + classObj);
                        Log.d(TAG, "hostClassLoaderProxy = " + hostClassLoaderProxy);
                        // java.lang.IllegalArgumentException: field android.app.LoadedApk.mClassLoader has type java.lang.ClassLoader, got $Proxy0
                        FieldUtils.writeField(hostLoadedApk, "mClassLoader", hostClassLoaderProxy);
                    }

                }

            }



            activityThreadClass = Class.forName("android.app.ActivityThread");
            activityThreadObj = MethodUtils.invokeStaticMethod(activityThreadClass, "currentActivityThread");
            Log.d(TAG, "after currentActivityThread = " + activityThreadObj);
            if (activityThreadObj != null) {
                Object mPackagesObj = FieldUtils.readField(activityThreadObj, "mPackages");
                Object hostLoadedApkRef = MethodUtils.invokeMethod(mPackagesObj, "get", hostContext.getPackageName());
                if (hostLoadedApkRef != null) {
                    Log.d(TAG, "after hostLoadedApkRef = " + hostLoadedApkRef);
                    final Object hostLoadedApk = MethodUtils.invokeMethod(hostLoadedApkRef, "get");
                    Log.d(TAG, "after hostLoadedApk = " + hostLoadedApk);
                    final Object classLoaderObj = FieldUtils.readField(hostLoadedApk, "mClassLoader");
                    Log.d(TAG, "after classLoaderObj = " + classLoaderObj);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Class<?> loadClass(Context context, String className) throws ClassNotFoundException{

        try {
            Class<?> clazz = context.getClassLoader().loadClass(className);
            if (clazz != null) {
                return clazz;
            }
        }catch (Exception e) {
//            e.printStackTrace();
        }


        synchronized (sClassLoaderList) {
            for (ClassLoader classLoader : sClassLoaderList) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (Exception e) {
//                    e.printStackTrace();
                }

            }
        }

        throw new ClassNotFoundException(className);
    }
}
