package com.pslib.util.apkinfo;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;


import com.logprint.BLog;
import com.pslib.util.RefInvoke;
import com.pslib.util.crypt.CryTool;
import com.pslib.util.io.IOUtils;
import com.pslib.util.string.MD5Util;
import com.pslib.util.string.StringUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author wangbx
 * @version V1.0
 * @ClassName: ApkInfo.java
 * @Description: 通用apk信息处理类
 * @Date 2012-12-4 下午2:30:24
 */
public class ApkUtils {
    private static final int FLAG_EXTERNAL_STORAGE = 262144;
    public static final int GET_PKG_SIZE_OK = 0;
    
    /**
     * 判断对象是否为空
     *
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    private static boolean isNull(Object object) {
        if (object == null || object.equals("")) {
            return true;
        }
        return false;
    }
    
    /**
     * 某个app是否是系统应用
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isSystemApp(Context context, String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(context, packageName);
        if (applicationInfo != null)
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0;
        return false;
    }
    
    
    public static ApplicationInfo getApplicationInfo(Context context, String packageName) {
        if (context == null)
            return null;
        PackageManager pm = context.getPackageManager();
        
        ApplicationInfo appInfo = getAppInfo(pm, packageName);
        return appInfo;
        
    }
    
    /**
     * 系统升级应用
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isSystemUpdateApp(Context context, String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(context, packageName);
        if (applicationInfo != null)
            return (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;
        return false;
    }
    
    /**
     * 普通安装的应用
     *
     * @return
     */
    public static boolean isUserApp(Context context, String packageName) {
        return (!isSystemApp(context, packageName) && !isSystemUpdateApp(context, packageName));
    }
    
    /**
     * 获取Apk安装路径
     *
     * @param context
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getInstallPath(Context context) {
        if (context == null)
            return null;
        return getInstallPath(context, context.getPackageName());
    }
    
    public static String getInstallPath(Context context, String packageName) {
        if (context == null)
            return null;
        PackageManager pm = context.getPackageManager();
        
        ApplicationInfo appInfo = getAppInfo(pm, packageName);
        if (appInfo != null) {
            if (TextUtils.isEmpty(appInfo.sourceDir)) {
                return appInfo.publicSourceDir;
            }
            return appInfo.sourceDir;
        }
        return null;
    }
    
    
    /**
     * 文件是否存在于zip中
     *
     * @param file
     * @param fileName
     * @return
     */
    public static boolean fileExsitInZip(String file, String fileName) {
        ZipFile zf = null;
        InputStream in = null;
        ZipInputStream zin = null;
        ZipEntry ze = null;
        
        InputStream zeInput = null;
        try {
            zf = new ZipFile(file);
            in = new BufferedInputStream(new FileInputStream(file));
            zin = new ZipInputStream(in);
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().equals(fileName)) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            
            IOUtils.closeStream(zeInput);
            IOUtils.closeStream(zin);
            IOUtils.closeStream(in);
            IOUtils.closeStream(zf);
            
        }
        return false;
    }
    
    
    /**
     * 读取自定的文件数据
     *
     * @param file
     * @param fileName
     * @return
     */
    public static String readZipFile(String file, String fileName) {
        ZipFile zf = null;
        InputStream in = null;
        ZipInputStream zin = null;
        ZipEntry ze = null;
        
        InputStream zeInput = null;
        try {
            zf = new ZipFile(file);
            in = new BufferedInputStream(new FileInputStream(file));
            zin = new ZipInputStream(in);
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                } else {
                    if (ze.getName().equals(fileName)) {
                        zeInput = zf.getInputStream(ze);
                        String ss = IOUtils.input2String(zeInput);
                        return ss;
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(zeInput);
            IOUtils.closeStream(zin);
            IOUtils.closeStream(in);
            IOUtils.closeStream(zf);
        }
        return null;
    }
    
    
    /**
     * 取得已经安装APK文件的MD5
     *
     * @param context
     * @return
     */
    
    public static synchronized String getInstallApkFileMd5(Context context, String packageName) {
        String path = getInstallPath(context, packageName);
        if (path == null) {
            return null;
        }

//        BLog.d("filemd5:", "file path:" + path);
        try {
            String md5 = MD5Util.getFileMD5String(new File(path));
            //          BLog.d("filemd5:", "file md5:" + md5);
            return md5;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static synchronized String getFileMd5(String path) {
        if (path == null) {
            return null;
        }
        
        //BLog.d("filemd5:", "file path:" + path);
        try {
            String md5 = MD5Util.getFileMD5String(new File(path));
            //  BLog.d("filemd5:", "file md5:" + md5);
            return md5;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    /**
     * 取得原始签名
     *
     * @param context
     * @param packageName
     * @return
     */
    public static Signature[] getRawSignature(Context context, String packageName) {
        if ((packageName == null) || (packageName.length() == 0)) {
            return null;
        }
        PackageManager pkgMgr = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = pkgMgr.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            return null;
        }
        if (info == null) {
            return null;
        }
        return info.signatures;
    }
    
    /**
     * 判断APK文件是否存在,并且是健康的APK文件
     *
     * @return boolean
     * @author wangbx
     * @date 2013-5-8
     */
    public static boolean fileExsitAndIsOk(Context context, File file) {
        if (!file.exists())
            return false;
        if (IOUtils.isExternalStorageAvailable()) {
            String s = getUninstalledAppPackageName(context, file.getAbsolutePath());
            return file.exists() && !StringUtils.isNullAndBlank(s);
        } else {
            String s = getUninstalledAppPackageName(context, file.getAbsolutePath());
            return file.exists() && !StringUtils.isNullAndBlank(s);
        }
    }
    
    /**
     * 获取一个APK File 对象
     *
     * @return File
     * @author zhangf
     * @date 2013-5-1
     */
    public static File getApkFile(Context context, String destFileName) {
        if (IOUtils.isExternalStorageAvailable()) {
            return getApkFile(context, IOUtils.getDownloadFileDictory(), destFileName);
        } else {
            return new File(context.getFilesDir(), destFileName);
        }
    }
    
    /**
     * get apk file
     *
     * @return File
     * @author zhangf
     * @date 2013-5-1
     */
    public static File getApkFile(Context context, File destFilePath, String destFileName) {
        if (IOUtils.isExternalStorageAvailable()) {
            return new File(destFilePath, destFileName);
        } else {
            return new File(context.getFilesDir(), destFileName);
        }
    }
    
    /**
     * 获取app信息
     *
     * @param pm          {@link PackageManager}
     * @param packageName
     * @return ApplicationInfo
     * @author wangbx
     * @date 2013-4-23
     */
    public static ApplicationInfo getAppInfo(PackageManager pm, String packageName) {
        ApplicationInfo appInfo = null;
        if (pm == null || isNull(packageName))
            return appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
        } catch (Exception e) {
        }
        return appInfo;
    }
    
    /**
     * 根据包名获取程序名
     *
     * @param context     {@link Context}
     * @param packageName
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getAppNameByPackageName(Context context, String packageName) {
        if (context == null || isNull(packageName))
            return null;
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = getAppInfo(pm, packageName);
        if (applicationInfo != null) {
            CharSequence appName = pm.getApplicationLabel(applicationInfo);
            if (appName != null)
                return appName.toString();
            
        }
        return null;
    }
    
    /**
     * 获取已安装App ICON
     *
     * @param context     {@link Context}
     * @param packageName
     * @return Drawable
     * @author wangbx
     * @date 2013-5-9
     */
    public static Drawable getAppIcon(Context context, String packageName) {
        if (context == null || isNull(packageName))
            return null;
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = getAppInfo(pm, packageName);
        if (applicationInfo != null) {
            return applicationInfo.loadIcon(pm);
            
        }
        return null;
    }
    
    /**
     * 根据包名获取版本名
     *
     * @param context     {@link Context}
     * @param packageName
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getAppVersionName(Context context, String packageName) {
        String versionName = "0.0.0";
        if (context == null) {
            return versionName;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            if (packageName == null) {
                packageName = context.getPackageName();
            }
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
        }
        
        return versionName;
    }
    
    /**
     * 判断Service是否在运行
     *
     * @param context
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceName) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(300);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * App是否存在
     *
     * @param packageName
     * @return
     */
    public static boolean isAppExsit(Context context, String packageName) {
        int version = ApkUtils.getAppVersionCode(context, packageName);
        return version > 0;
    }
    
    /**
     * 根据包名获取版本号
     *
     * @param context     {@link Context}
     * @param packageName 包名 传入null 则取当前包名
     * @return int
     * @author wangbx
     * @date 2013-4-23
     */
    public static int getAppVersionCode(Context context, String packageName) {
        int versionCode = 0;
        if (context == null) {
            return versionCode;
        }
        if (StringUtils.isBlank(packageName)) {
            return versionCode;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            return 0;
        }
        
        return versionCode;
    }
    
    /**
     * 获取activity名
     *
     * @param context {@link Context}
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getTopActivityName(Context context) {
        if (context == null) {
            return null;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //android.permission.GET_TASKS
        String getTask = CryTool.decrypt3("9x5sSN81jnPpgtGpTi4qR5F/lrcFypzvPxrALLOWoLU=");//-ok
        //android.permission.REAL_GET_TASKS
        String relGetTask = CryTool.decrypt3("9x5sSN81jnPpgtGpTi4qR5QzeYRH1baSTKwyt5PB8G27p6UTc9TBPQ==");//-ok
        if (checkPermissions(context, getTask) || checkPermissions(context, relGetTask)) {
            List<RunningTaskInfo> a = am.getRunningTasks(1);
            if (a != null && a.size() > 0) {
                RunningTaskInfo d = a.get(0);
                if (d != null) {
                    ComponentName c = d.topActivity;
                    if (c != null) {
                        return c.getClassName();
                    }
                }
            }
        } else {
            return null;
        }
        return null;
        
    }
    
    
    /**
     * 获取当前运行app包名
     *
     * @param context {@link Context}
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getTopPackageName(Context context) {
        if (context == null) {
            return null;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (checkPermissions(context, "android.permission.GET_TASKS") || checkPermissions(context, "android.permission.REAL_GET_TASKS")) {
            List<RunningTaskInfo> a = am.getRunningTasks(1);
            if (a != null && a.size() > 0) {
                RunningTaskInfo d = a.get(0);
                if (d != null) {
                    ComponentName c = d.topActivity;
                    if (c != null) {
                        return c.getPackageName();
                    }
                }
            }
        } else {
            return null;
        }
        return null;
        
    }
    
    /**
     * 检查权限
     *
     * @param context    {@link Context}
     * @param permission
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean checkPermissions(Context context, String permission) {
        if (context == null || isNull(permission)) {
            return false;
        }
        
        //checkCallingOrSelfPermission
        String checkCallingOrSelfPermission = CryTool.decrypt3("fMYhMjy+C4oQaTBiQoSYmXIfAwlFSNW1sPQ7UjwTrTQ=");//-ok
        Object aa = RefInvoke.invokeMethod(context.getClass(), checkCallingOrSelfPermission, context, new Class[]{String.class}, new Object[]{permission});
        if (aa != null) {
            int xx = -1;
            try {
                xx = Integer.parseInt(aa.toString());
            } catch (Exception e) {
            }
            return xx == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }
    
    /**
     * 获取已安装apk信息
     *
     * @param context {@link Context}
     * @return String
     * packageName|versionCode,packageName|versionCode,packageName
     * |versionCode,packageName|versionCode
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getAllInstalledAppInfo(Context context) {
        if (context == null) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        StringBuilder installedAppInfo = new StringBuilder();
        boolean first = true;
        for (PackageInfo packageInfo : packageInfos) {
            if (!first) {
                installedAppInfo.append(",");
            }
            installedAppInfo.append(packageInfo.packageName).append("|")
                    .append(packageInfo.versionCode);
            first = false;
        }
        return installedAppInfo.toString();
    }
    
    /**
     * 获取已安装apk信息,排除预置app
     *
     * @param context {@link Context}
     * @return String
     * packageName|versionCode,packageName|versionCode,packageName
     * |versionCode,packageName|versionCode
     * @author wangbx
     * @date 2013-4-23
     */
    public static String packageInstalledAppInfo(Context context) {
        if (context == null) {
            return null;
        }
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        List<PackageInfo> tempPackageInfos = new ArrayList<PackageInfo>();
        for (PackageInfo packageInfo : packageInfos) {
            // 排除系统安装
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                tempPackageInfos.add(packageInfo);
            }
        }
        StringBuilder installedAppInfo = new StringBuilder();
        boolean first = true;
        for (PackageInfo packageInfo : tempPackageInfos) {
            if (!first) {
                installedAppInfo.append(",");
            }
            installedAppInfo.append(packageInfo.packageName).append("|")
                    .append(packageInfo.versionCode);
            first = false;
        }
        return installedAppInfo.toString();
    }
    
    /**
     * 判断apk是否被安装
     *
     * @param context         {@link Context}
     * @param archiveFilePath
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean isUninstalledApk(Context context, String archiveFilePath) {
        if (context == null || isNull(archiveFilePath)) {
            return false;
        }
        
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            return true;
        }
        return false;
    }
    
    /**
     * 获取没有安装的app包名
     *
     * @param context         {@link Context}
     * @param archiveFilePath
     * @return String
     * @author wangbx
     * @date 2013-4-23
     */
    public static String getUninstalledAppPackageName(Context context, String archiveFilePath) {
        if (context == null || isNull(archiveFilePath)) {
            return null;
        }
        if (archiveFilePath.startsWith("file://")) {
            archiveFilePath = archiveFilePath.replace("file://", "");
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        String packageName = "";
        if (info != null) {
            packageName = info.packageName;
        }
        return packageName;
    }
    
    /**
     * 获取没有安装的app的版本号
     *
     * @param context         {@link Context}
     * @param archiveFilePath
     * @return int versionCode
     * @author wangbx
     * @date 2013-4-23
     */
    public static int getUninstalledAppVersionCode(Context context, String archiveFilePath) {
        if (context == null || isNull(archiveFilePath)) {
            return 0;
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        int versionCode = 0;
        if (info != null) {
            versionCode = info.versionCode;
        }
        return versionCode;
    }
    
    /**
     * 根据包名判断是否安装
     *
     * @param context     {@link Context}
     * @param packageName 包名
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean isInstalledApk(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return false;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return true;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return false;
    }
    
    /**
     * 根据包名判断是否安装
     *
     * @param context     {@link Context}
     * @param packageName 包名
     * @return int versioncode
     * @author wangbx
     * @date 2013-4-23
     */
    public static int getInstalledApk(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return -1;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                return packageInfo.versionCode;
            }
        } catch (NameNotFoundException e) {
            return -1;
        }
        return -1;
    }
    
    /**
     * 根据包名,版本号判断是否已安装
     *
     * @param context     {@link Context}
     * @param packageName
     * @param versionCode
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean isInstalledApk(Context context, String packageName, String versionCode) {
        if (context == null || isNull(packageName) || isNull(versionCode)) {
            return false;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                String v = String.valueOf(packageInfo.versionCode);
                if (versionCode.equals(v))
                    return true;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return false;
    }
    
    
    /**
     * 根据包名判断是否有activity
     *
     * @param context     {@link Context}
     * @param packageName
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean hasActivities(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES);
            ActivityInfo activityInfo[] = packageInfo.activities;
            if (activityInfo != null) {
                return true;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return false;
    }
    
    /**
     * 打开已安装的包
     *
     * @return void
     * @author wangbx
     * @date 2013-4-23
     */
    public static void openInstalledPackage(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return;
        }
        Intent i = getMainIntent(context, packageName);
        if (i != null) {
            try {
                context.startActivity(i);
                return;
            } catch (Exception e) {
            }
            
        }
        
        try {
            final PackageManager packageManager = context.getPackageManager();
            Intent queryIntent = new Intent(Intent.ACTION_MAIN);
            queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(queryIntent, 0);
            ActivityInfo activityInfo = null;
            String mainActivityClass = "";
            for (ResolveInfo resolveInfo : resolveInfos) {
                activityInfo = resolveInfo.activityInfo;
                if (activityInfo.packageName.equals(packageName)) {
                    mainActivityClass = activityInfo.name;
                    break;
                }
            }
            if (!"".equals(mainActivityClass)) {
                Intent opentIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (null == opentIntent) {
                    opentIntent = new Intent();
                    opentIntent.setComponent(new ComponentName(packageName, mainActivityClass));
                    opentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(opentIntent);
                    return;
                }
                opentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(opentIntent);
            } else {
                Intent opentIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                opentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(opentIntent);
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * 根据包名判断是否在最前面显示
     *
     * @param context     {@link Context}
     * @param packageName
     * @return boolean
     * @author wangbx
     * @date 2013-4-23
     */
    public static boolean isTopActivity(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return false;
        }
        int id = context.checkCallingOrSelfPermission(Manifest.permission.GET_TASKS);
        if (PackageManager.PERMISSION_GRANTED != id) {
            return false;
        }
        
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        if (tasksInfo.size() > 0) {
            if (packageName.equals(tasksInfo.get(0).topActivity.getPackageName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 取得所有非内置的程序包名
     *
     * @param context
     * @return
     */
    public static List<String> getAllInstalledPackageName(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        if (packageInfos == null) {
            return null;
        }
        List<String> packages = new CopyOnWriteArrayList<String>();
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo == null) {
                continue;
            }
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                packages.add(packageInfo.packageName);
            }
        }
        return packages;
    }
    
    /**
     * 取得包与版本信息信息，包名与版本好用"|"相连,多个应用用,号隔开
     *
     * @param context
     * @return 0： appname, 1 ： pacakge name|versioncode,
     * @Title: getInstalledApp
     */
    public static String[] getInstalledAppForFirstStartApp(Context context) {
        String[] res = new String[2];
        StringBuffer appnames = new StringBuffer();
        StringBuffer packageNames = new StringBuffer();
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.applicationInfo != null) {
                appnames.append(
                        packageInfo.applicationInfo.loadLabel(packageManager))
                        .append("|");
                if (packageNames.length() > 0) {
                    packageNames.append(",").append(packageInfo.packageName)
                            .append("|").append(packageInfo.versionCode);
                } else {
                    packageNames.append(packageInfo.packageName).append("|")
                            .append(packageInfo.versionCode);
                }
                
            }
        }
        res[0] = appnames.toString();
        res[1] = packageNames.toString();
        return res;
    }
    
    /**
     * 取得包信息， 用|号隔开
     *
     * @param context
     * @return 0： appname, 1 ： pacakge name, 2 :versioncode
     * @Title: getInstalledApp
     */
    public static String[] getInstalledApp(Context context) {
        String[] res = new String[2];
        StringBuffer appnames = new StringBuffer();
        StringBuffer packageNames = new StringBuffer();
        final PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.applicationInfo != null) {
                appnames.append(
                        packageInfo.applicationInfo.loadLabel(packageManager))
                        .append("|");
                if (packageNames.length() > 0) {
                    packageNames.append(",").append(packageInfo.packageName)
                            .append("|").append(packageInfo.versionCode);
                } else {
                    packageNames.append(packageInfo.packageName).append("|")
                            .append(packageInfo.versionCode);
                }
                
            }
        }
        res[0] = appnames.toString();
        res[1] = packageNames.toString();
        return res;
    }
    
    
    /**
     * 打开位置来源
     *
     * @param context
     * @return
     */
    public static boolean openUnknowSource(Context context) {
        //android.permission.WRITE_SECURE_SETTINGS
        String ss = CryTool.decrypt3("9x5sSN81jnPpgtGpTi4qR9WqYNc1CNrO7bQiiPyGvnlup/b1TikT3fr/gDUU OP3M");//-ok
        boolean grant = ApkUtils.checkPermissions(context, ss);
        if (!grant) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 17) {
            Settings.Secure.putInt(context.getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 1);
            try {
                int anInt = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS);
                return anInt == 1;
            } catch (Exception e) {
                return false;
            }
        } else {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 1);
            try {
                int anInt = Settings.Global.getInt(context.getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS);
                return anInt == 1;
            } catch (Exception e) {
                return false;
            }
        }
        
    }
    
    
    public static boolean hasInstallPermission(Context context) {
        //android.permission.INSTALL_PACKAGES
        String s = CryTool.decrypt3("9x5sSN81jnPpgtGpTi4qR6aiPZYFEEeL56T2p4OfsPS4SzAwwUnMlA==");//-ok
        return checkPermissions(context, s);
    }
    
    /**
     * 通过路径安装APK
     *
     * @param context
     * @param path
     * @return void
     * @author wangbx
     * @date 2013-4-23
     * @see
     */
    public static void installPackage(Context context, String path) {
        if (context == null || isNull(path)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //android.intent.extra.ORIGINATING_UID
        String ss = CryTool.decrypt3("9x5sSN81jnMNsb0KW9i/KA7RYPYjYC7LtNTEMn6TTWFXAQ+3z5PO1g==");//-ok
        intent.putExtra(ss, 0);
        //android.intent.extra.RETURN_RESULT
        String resut = CryTool.decrypt3("9x5sSN81jnMNsb0KW9i/KJ6nFL0a/j5TYnyYoLsgHMrOA6T4Elfd4A==");//-ok
        intent.putExtra(resut, false);
        //application/vnd.android.package-archive
        String s4 = CryTool.decrypt3("tM9R7pyjnfOaPby9+nChH6ZSOCgPR1jNtRI4zj0Eq96VvTRb34HUdw==");//-ok
        intent.setDataAndType(Uri.fromFile(new File(path)), s4);
        context.startActivity(intent);
    }
    
    /**
     * 直接进行安装
     *
     * @param context
     * @param apkFile
     */
    public static void installDirect(Context context, String apkFile) {
        //android.permission.INSTALL_PACKAGES
        String s = CryTool.decrypt3("9x5sSN81jnPpgtGpTi4qR6aiPZYFEEeL56T2p4OfsPS4SzAwwUnMlA==");//-ok
        if (!checkPermissions(context, s)) {
            return;
        }
        final Uri uri = Uri.fromFile(new File(apkFile));
        BLog.d("i", "in_  ->" + uri.toString());
        InstallApkUtil.install(context, new File(apkFile), null, null);
    }
    
    public static void installPackageUri(Context context, Uri uri) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        //application/vnd.android.package-archive
        String s4 = CryTool.decrypt3("tM9R7pyjnfOaPby9+nChH6ZSOCgPR1jNtRI4zj0Eq96VvTRb34HUdw==");//-ok
        install.setDataAndType(uri, s4);
        
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(install);
    }
    
    /**
     * auto将会根据存储空间自适应
     **/
    public static final int auto = 0;
    
    /**
     * 仅放在内存卡上
     **/
    public static final int internalOnly = 1;
    
    /**
     * preferExternal可以优先推荐应用安装到SD卡
     **/
    public static final int preferExternal = 2;
    
    /**
     * 根据包名判断是否可以移动
     *
     * @param context     {@link Context}
     * @param packageName
     * @return int
     * @author wangbx
     * @date 2013-4-23
     */
    public static int getAppinstallLocation(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return 1;
        }
        AssetManager am;
        int installLocation = 1;
        try {
            am = context.createPackageContext(packageName, 0).getAssets();
            XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");
            int eventType = xml.getEventType();
            xmlloop:
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (!xml.getName().matches("manifest")) {
                            break xmlloop;
                        } else {
                            attrloop:
                            for (int j = 0; j < xml.getAttributeCount(); j++) {
                                if (xml.getAttributeName(j).matches("installLocation")) {
                                    switch (Integer.parseInt(xml.getAttributeValue(j))) {
                                        case auto:
                                            installLocation = 0;
                                            break;
                                        case internalOnly:
                                            installLocation = 1;
                                            break;
                                        case preferExternal:
                                            installLocation = 2;
                                            break;
                                        // default:
                                        // installLocation=0;
                                        // break;
                                    }
                                    break attrloop;
                                }
                            }
                        }
                        break;
                }
                eventType = xml.nextToken();
            }
        } catch (NameNotFoundException e) {
        } catch (XmlPullParserException e) {
        } catch (IOException e) {
        } catch (Exception e) {
        }
        return installLocation;
    }
    
    /**
     * 获取APK文件基本信息
     *
     * @param context {@link Context}
     * @param apkPath
     * @return AppInfoModel
     * @author wangbx
     * @date 2013-4-23
     */
    public static AppInfoModel getApkBaseInfo(Context context, String apkPath) {
        if (context == null || isNull(apkPath)) {
            return null;
        }
        AppInfoModel apkInfo = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath,
                    PackageManager.GET_ACTIVITIES);
            
            if (null == packageInfo) {
                return null;
            }
            
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            apkInfo = new AppInfoModel();
            apkInfo.packageName = packageInfo.packageName;
            apkInfo.apkPath = apkPath;
            apkInfo.apkSize = new File(apkPath).length();
            apkInfo.versionCode = packageInfo.versionCode;
            apkInfo.versionName = packageInfo.versionName;
            
            apkInfo.appIcon = appInfo.loadIcon(packageManager);
            apkInfo.appName = packageManager.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
        }
        
        return apkInfo;
    }
    
    
    /**
     * 获取已安装app大小
     *
     * @param context
     * @return void
     * @author wangbx
     * @date 2013-4-25
     */
    public static void getInstalledAppSize(Context context, final AppInfoModel ap) {
        
        if (context == null || ap == null) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        
        Method getPackageSizeInfo = null;
        try {
            getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class,
                    IPackageStatsObserver.class);
            getPackageSizeInfo.invoke(pm, ap.packageName, new IPackageStatsObserver.Stub() {
                @Override
                public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
                        throws RemoteException {
                    
                    if (succeeded) {
                        if (pStats == null || pStats.codeSize == 0) {
                            ap.appSize = -1;
                        } else {
                            ap.appSize = pStats.codeSize;
                        }
                    } else {
                        ap.appSize = -1;
                    }
                }
                
            });
        } catch (NoSuchMethodException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        }
        
        // return FormatUtil.formatFileSize(ap.appSize);
        
    }
    
    /**
     * 添加回调参数
     *
     * @param context
     * @param ap      add by mh
     */
    public static void getInstalledAppSize(Context context, final AppInfoModel ap, final Handler mHandler, final Object obj) {
        
        if (context == null || ap == null) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        
        Method getPackageSizeInfo = null;
        try {
            getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class,
                    IPackageStatsObserver.class);
            getPackageSizeInfo.invoke(pm, ap.packageName, new IPackageStatsObserver.Stub() {
                @Override
                public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
                        throws RemoteException {
                    
                    if (succeeded) {
                        if (pStats == null || pStats.codeSize == 0) {
                            ap.appSize = -1;
                        } else {
                            ap.appSize = pStats.codeSize;
                            Message msg = new Message();
                            msg.obj = obj;
                            Bundle bundle = new Bundle();
                            if (Build.VERSION.SDK_INT >= 14) {
                                bundle.putLong("apksize", pStats.codeSize + pStats.externalCodeSize);
                            } else {
                                bundle.putLong("apksize", pStats.codeSize);
                            }
                            msg.setData(bundle);
                            msg.what = GET_PKG_SIZE_OK;
                            mHandler.sendMessage(msg);
                        }
                    } else {
                        ap.appSize = -1;
                    }
                }
                
            });
        } catch (NoSuchMethodException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            //  Auto-generated catch block
            e.printStackTrace();
        }
        
        // return FormatUtil.formatFileSize(ap.appSize);
        
    }
    
    /**
     * 获取所有桌面app
     *
     * @param context {@link Context}
     * @return List<String>
     * @author wangbx
     * @date 2013-4-23
     */
    public static List<String> getHomes(Context context) {
        if (context == null) {
            return null;
        }
        List<String> names = new ArrayList<String>();
        PackageManager packageManager = context.getPackageManager();
        // 属性
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            names.add(ri.activityInfo.packageName);
        }
        return names;
    }
    
    private static String SCHEME = "package";
    
    /**
     * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.1及之前版本)
     */
    private static final String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
    
    /**
     * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.2)
     */
    private static final String APP_PKG_NAME_22 = "pkg";
    
    /**
     * InstalledAppDetails所在包名
     */
    private static final String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
    
    /**
     * InstalledAppDetails类名
     */
    private static final String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";
    
    public static final String ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS";
    
    public static final int APP_INSTALL_EXTERNAL = 2;
    
    /**
     * 调用系统InstalledAppDetails界面显示已安装应用程序的详细信息。 对于Android 2.3（Api Level
     * 9）以上，使用SDK提供的接口； 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）。
     *
     * @param context     {@link Context}
     * @param packageName
     * @return void
     * @author wangbx
     * @date 2013-4-23
     */
    public static void showInstalledAppDetails(Context context, String packageName) {
        if (context == null || isNull(packageName)) {
            return;
        }
        Intent intent = new Intent();
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= 9) { // 2.3（ApiLevel 9）以上，使用SDK提供的接口
            intent.setAction(ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, packageName, null);
            intent.setData(uri);
        } else { // 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）
            // 2.2和2.1中，InstalledAppDetails使用的APP_PKG_NAME不同。
            final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22 : APP_PKG_NAME_21);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName(APP_DETAILS_PACKAGE_NAME, APP_DETAILS_CLASS_NAME);
            intent.putExtra(appPkgName, packageName);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * 判断是否安装在SD卡的
     *
     * @param pm
     * @param packageName
     * @return
     */
    public static boolean isInstallOnSDCard(PackageManager pm, String packageName) {
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);
            if ((appInfo.flags & FLAG_EXTERNAL_STORAGE) != 0) {
                return true;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 卸载应用程序
     *
     * @param context
     * @param packageName
     */
    public static void unInstallPackage(Context context, String packageName) {
        Uri packageUri = Uri.fromParts("package", packageName, null);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(uninstallIntent);
    }
    
    /**
     * 卸载应用程序
     *
     * @param packageName
     */
    public static void unInstallPackageForResult(Activity activity, String packageName,
                                                 int requestCode) {
        Uri packageUri = Uri.fromParts("package", packageName, null);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
        // 注意:卸载这里不能使用FLAG_ACTIVITY_NEW_TASK 标签，不然广播不能被接收
        // uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(uninstallIntent, requestCode);
    }
    
    public static boolean hasExternalSdcard() {
        boolean isHas = false;
        try {
            // tmpfs /storage/sdcard1/.android_secure tmpfs ro,relatime,size=0k,mode=000 0
            // tmpfs 可认为外置sdcard
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("tmpfs") && (line.contains("sdcard") || line.contains("ext_sd"))) {
                    isHas = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isHas;
    }
    
    
    /**
     * 取得启动的Intent
     *
     * @param context
     * @param packageName
     * @return
     */
    public static Intent getMainIntent(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }
    
    public static String getMainIntentActivity(Context context, String packageName) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (i == null) {
            return null;
        }
        ComponentName componentName = i.getComponent();
        if (componentName == null) {
            return null;
        }
        return componentName.getClassName();
    }
    
    
    /**
     * get dex class loader
     *
     * @param dexPath
     * @param optimizedDirectory
     * @param libraryPath
     * @param parent
     * @return
     */
    public static Object dexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        //new DexClassLoader()
        //DexClassLoader.class
        //String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent
        //dalvik.system.DexClassLoader
        String clz = CryTool.decrypt3("xTmKxpQjTQYU71dJOjyHlhMGw6kxFu8gDvG7mzATMcw=");//-ok
        try {
            return Class.forName(clz)
                    .getConstructor(String.class, String.class, String.class, ClassLoader.class)
                    .newInstance(dexPath, optimizedDirectory, libraryPath, parent);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
        
    }
}
