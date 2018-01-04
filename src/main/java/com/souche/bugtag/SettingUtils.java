package com.souche.bugtag;

import com.intellij.ide.util.PropertiesComponent;


public class SettingUtils {

    public static final String KEY = "cookie";

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
}