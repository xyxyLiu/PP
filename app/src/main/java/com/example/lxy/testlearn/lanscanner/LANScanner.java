package com.example.lxy.testlearn.lanscanner;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by lxy on 16-5-23.
 */
public class LANScanner {

    private static final String TAG = "LANScanner";
    private static final boolean DEBUG = true;

    private Context mContext;
    private WifiManager mWifiManager;
    private ThreadPoolExecutor mThreadPoolExecutor;

    private static LANScanner sLanScanner;
    private AtomicBoolean mIsWorking = new AtomicBoolean(false);

    public static synchronized LANScanner getInstance(Context context) {
        if (sLanScanner == null) {
            sLanScanner = new LANScanner(context);
        }
        return sLanScanner;
    }


    private LANScanner(Context context) {
        mContext = context.getApplicationContext();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mThreadPoolExecutor = new ThreadPoolExecutor(1, 1, 1,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        mThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public void startScan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                startScanSync();
            }
        }).start();
    }

    public boolean startScanSync() {
        if (mIsWorking.compareAndSet(false, true)) {
            if (DEBUG) {
                Log.d(TAG, "startScanSync() start...");
            }
            long startTime = SystemClock.elapsedRealtime();
            try {
                DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
                Log.d(TAG, "dhcpInfo = " + dhcpInfo);

                SubnetUtils subnetUtils = new SubnetUtils(intToInetAddress(dhcpInfo.ipAddress).getHostAddress() +
                        "/" + getNetworkPrefixLength(dhcpInfo));
                String[] subnetAddrs = subnetUtils.getInfo().getAllAddresses();
                Log.d(TAG, "subnet size = " + subnetAddrs.length);

                configThreadPool(subnetAddrs.length);
                List<Callable<Void>> taskList = new ArrayList<>(subnetAddrs.length);
                for (final String subnetAddr : subnetAddrs) {
                    taskList.add(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            Log.d(TAG, Thread.currentThread().getId() + " scan " + subnetAddr);
                            sendUPDPacket(subnetAddr, 80);
                            return null;
                        }
                    });
                }

                mThreadPoolExecutor.invokeAll(taskList);
                return true;
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                return false;
            } finally {
                long endTime = SystemClock.elapsedRealtime();
                if (DEBUG) {
                    Log.d(TAG, "startScanSync() end. time consumed = " + (endTime - startTime) + " ms");
                }
                mIsWorking.set(false);
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "startScanSync() error, working now!");
            }
            return false;
        }
    }

    private void configThreadPool(int taskSize) {
        int threadSize;
        if (taskSize < 4) {
            threadSize = 1;
        } else if (taskSize < 255) {
            threadSize = 4;
        } else {
            threadSize = 8;
        }

        mThreadPoolExecutor.setCorePoolSize(threadSize);
        mThreadPoolExecutor.setMaximumPoolSize(threadSize);

        if (DEBUG) {
            Log.d(TAG, "configThreadPool() threadSize = " + threadSize);
        }
    }

    private static void sendUPDPacket(String addr, int port) {
        DatagramSocket datagramSocket = null;
        try {
            if (DEBUG) {
                Log.d(TAG, String.format("sendUPDPacket(%s, %d)", addr, port));
            }
            datagramSocket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(addr);
            byte[] buffer = "hi, there!".getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            packet.setAddress(address);
            packet.setPort(port);
            datagramSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }

    }

    private static int getNetworkPrefixLength(DhcpInfo dhcpInfo) {
        try {
            if (DEBUG) {
                Log.d(TAG, "getNetworkPrefixLength = " + dhcpInfo);
            }
            InetAddress inetAddress = intToInetAddress(dhcpInfo.ipAddress);
            Log.d(TAG, "inetAddress = " + inetAddress);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            Log.d(TAG, "networkInterface = " + networkInterface);
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                //short netPrefix = address.getNetworkPrefixLength();
                if (address.getAddress().getHostAddress().equals(inetAddress.getHostAddress())) {
                    Log.d(TAG, "InterfaceAddress = " + address.toString());
                    return address.getNetworkPrefixLength();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public static List<Pair<String, String>> getAllCacheArp() {
        return ProcArpParser.getAllCacheArp();
    }


    private static class ProcArpParser {
        private static final String FILE_NAME = "/proc/net/arp";
        private static final int COL_IP = 0;
        private static final int COL_HW_TYPE = 1;
        private static final int COL_FLAGS = 2;
        private static final int COL_MAC = 3;
        private static final int COL_MASK = 4;
        private static final int COL_DEVICE = 5;

        public static List<Pair<String, String>> getAllCacheArp() {
            List<Pair<String, String>> res = new ArrayList<>();
            File file = new File(FILE_NAME);
            if (file.exists()) {
                BufferedReader br = null;
                try {

                    br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("|")) {
                            continue;
                        }
                        String[] cols = line.trim().split("\\s+");
                        if (cols.length == 6) {
                            String ip = cols[COL_IP];
                            String mac = cols[COL_MAC];
                            if (isValid(ip, mac)) {
                                Pair<String, String> pair = new Pair<>(cols[COL_IP], cols[COL_MAC]);
                                res.add(pair);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return res;
        }

        public static boolean isValid(String ip, String mac) {
            if (mac.equals("00:00:00:00:00:00")) {
                return false;
            }

            return true;
        }

    }
}
