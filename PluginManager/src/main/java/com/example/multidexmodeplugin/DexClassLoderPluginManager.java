package com.example.multidexmodeplugin;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by lxy on 16-6-1.
 */
public class DexClassLoderPluginManager {

    private static final String PLUGIN_DEX_FOLDER_NAME = "plugin_dexes_2";
    private static final String PLUGIN_LIB_FOLDER_NAME = "plugin_lib_2";

    private static List<ClassLoader> sClassLoaderList = new ArrayList<>();

    public static boolean install(Context context, String pluginApkName) {
        try {
            File apkFile = AssetsManager.copyAssetsApk(context, pluginApkName);
            File pluginDexPath = context.getDir(PLUGIN_DEX_FOLDER_NAME, Context.MODE_PRIVATE);
            File pluginLibPath = context.getDir(PLUGIN_LIB_FOLDER_NAME, Context.MODE_PRIVATE);
            ClassLoader parentClassLoader = context.getClassLoader().getParent();
            DexClassLoader dexClassLoader = new DexClassLoader(apkFile.getAbsolutePath(), pluginDexPath.getAbsolutePath(), pluginLibPath.getAbsolutePath(), parentClassLoader);
            synchronized (sClassLoaderList) {
                sClassLoaderList.add(dexClassLoader);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
