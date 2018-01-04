package com.souche.bugtag.utils;

import com.intellij.ide.util.PropertiesComponent;


public class SettingUtils {

    public static final String KEY = "cookie";
    public static final String KEY_VERSION = "version";


    private SettingUtils(){

    }

    public static SettingUtils getInstance(){
        return SettingProxy.utils;
    }

    static class SettingProxy{
        static SettingUtils utils = new SettingUtils();
    }

    public void saveCookie(String cookie){
        PropertiesComponent.getInstance().setValue(KEY,cookie);
    }

    public String getCookie(){
        if(PropertiesComponent.getInstance().isValueSet(KEY) ||
                PropertiesComponent.getInstance().getValue(KEY) != null ||
                !"".equals(PropertiesComponent.getInstance().getValue(KEY))){
            return PropertiesComponent.getInstance().getValue(KEY);
        }
        else{
            return null;
        }
    }

    public void saveVersion(String versionName){
        PropertiesComponent.getInstance().setValue(KEY_VERSION,versionName);
    }

    public String getVersion(){
        if(PropertiesComponent.getInstance().isValueSet(KEY_VERSION) ||
                PropertiesComponent.getInstance().getValue(KEY_VERSION) != null ||
                !"".equals(PropertiesComponent.getInstance().getValue(KEY_VERSION))){
            return PropertiesComponent.getInstance().getValue(KEY_VERSION);
        }
        else{
            return null;
        }
    }
}