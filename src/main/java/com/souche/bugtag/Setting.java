package com.souche.bugtag;


import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.souche.bugtag.utils.SettingUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class Setting implements Configurable {

    private JPanel mPanel;
    private JTextArea mTv;
    private JLabel mLb;
    private boolean isModify;
    private String mSavedCookie;
    private static OnSettingApplyListener mListener;

    @Nls
    @Override
    public String getDisplayName() {
        return "Bugtag";
    }

    public static void setOnSettingApplyListener(OnSettingApplyListener listener){
        mListener = listener;
    }
    @Nullable
    @Override
    public JComponent createComponent() {
        mSavedCookie = SettingUtils.getInstance().getCookie();
        mLb.setForeground(Color.blue);
        mLb.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    mLb.setForeground(Color.black);
                    Desktop.getDesktop().browse(new URL("http://git.souche.com/hefuwei/bugtagPlugin/issues").toURI());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        mTv.setLineWrap(true);        //激活自动换行功能
        mTv.setWrapStyleWord(true);
        mTv.setText(mSavedCookie);
        mTv.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if(!mTv.getText().equals(mSavedCookie)){
                    isModify = true;
                }else{
                    isModify = false;
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if(!mTv.getText().equals(mSavedCookie)){
                    isModify = true;
                }else{
                    isModify = false;
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        return mPanel;
    }

    @Override
    public boolean isModified() {
        return isModify;
    }

    @Override
    public void apply() throws ConfigurationException {
        if(isModify){
            SettingUtils.getInstance().saveCookie(mTv.getText());
            isModify = false;
            //通知ToolFactoryCompute更新
            if(mListener != null){
                mListener.onSettingApply(mTv.getText());
            }
        }
    }

}
