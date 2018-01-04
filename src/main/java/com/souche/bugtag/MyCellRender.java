package com.souche.bugtag;

import com.souche.bugtag.api.model.IssueInfo;


import javax.swing.*;
import java.awt.*;

public class MyCellRender<T>  extends DefaultListCellRenderer{

    private JEditorPane pane_descError;
    private JEditorPane pane_time;
    private JEditorPane pane_info;
    private JEditorPane pane_desc;
    private GridLayout gl;
    private GridLayout gl_childtop;
    private GridLayout gl_childbottom;
    private JPanel panel_first;
    private JPanel panel_second;
    private JPanel panel;
    private JPanel panel_par;


    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        panel_par = new JPanel();
        gl = new GridLayout(3,1);
        gl_childtop = new GridLayout(1,3);
        gl_childbottom = new GridLayout(1,1);
        panel = new JPanel();
        panel.setLayout(gl);
        panel_first = new JPanel();
        panel_second = new JPanel();
        panel.add(panel_first);
        panel.add(panel_second);
        panel_first.setLayout(gl_childtop);
        panel_second.setLayout(gl_childbottom);
        pane_descError = new JEditorPane();
        pane_descError.setContentType("text/html");
        pane_desc = new JEditorPane();
        pane_info = new JEditorPane();
        pane_time = new JEditorPane();
        JEditorPane pane = new JEditorPane();
        pane.setMaximumSize(new Dimension(10000,10));
        panel.setBackground(Color.WHITE);
        IssueInfo info = (IssueInfo)value;
        pane_descError.setText("<font color=red>"+info.description+"</font>");
        pane_time.setText(info.tags.get(0).created_at+"/"+info.tags.get(0).updated_at);
        pane_info.setText("跟踪人:"+info.tags.get(0).dev_user.nickname + " 次数:"+info.crash_num+" 机型:"+info.device_num
        +" 用户"+info.model_num + " 版本:"+info.tags.get(0).version_name);
        pane_desc.setContentType("text/html");
        if(info.succ_num.equals("1")){
            pane_desc.setText("<span style='text-decoration:line-through;color:gray'>"+info.tags.get(0).description+"</span>");
        }else{
            pane_desc.setText("<font color:blue>"+info.tags.get(0).description+"</font>");
        }
        panel_first.add(pane_descError);
        panel_first.add(pane_time);
        panel_first.add(pane_info);
        panel_second.add(pane_desc);
        JPanel panel_line = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.GRAY);
                g.drawLine(0,10,4500,10);
            }
        };
        panel_line.setMinimumSize(new Dimension(200,500));
        panel.add(panel_line);
        return panel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
