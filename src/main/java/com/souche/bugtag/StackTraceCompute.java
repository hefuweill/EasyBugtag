package com.souche.bugtag;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.souche.bugtag.api.model.*;
import com.souche.bugtag.api.service.APIManager;
import com.souche.bugtag.utils.OpenFileUtils;
import com.souche.bugtag.utils.SourceDetail;
import okhttp3.Request;
import org.apache.commons.collections.map.LRUMap;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StackTraceCompute implements ToolWindowFactory, OnShowTextListener {

    private static ToolWindow myToolWindow;
    private JPanel mPanel;
    private JScrollPane mScrollPane;
    private JEditorPane mDetail;
    private JComboBox mCb;
    private Project mProject;
    private Content content;
    private String appId;
    private APIManager manager;
    private IssueDetail mIssueDetail;
    private final String STACKTRACE = "堆栈信息";
    private final String REVIEW = "重现步骤";
    private final String USERDATA = "用户数据";
    private final String CONSOLELOG = "控制台日志";
    private final String BUGTAGLOG = "Bugtag日志";
    private final String ANRLOG = "Anr日志";
    private final String SNAPSHOT = "截图";
    private final String PREFIX = "com.souche.fengche";
    private LRUMap map = new LRUMap(5);
    private String currentContent = STACKTRACE;
    private IssueInfo issue;
    private String[] levels = {"Verbose","Debug","Info","Warn","Error"};

    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {
        StackTraceCompute mStackTraceCompute = new StackTraceCompute();
        mStackTraceCompute.process(project,toolWindow);
    }

    private void process(Project project, ToolWindow toolWindow){
        ToolFactoryCompute.setOnShowTextListener(this);
        mDetail.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON3){
                    myToolWindow.hide(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                    ToolWindowManager.getInstance(mProject).getToolWindow("SimpleBugtag").show(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        myToolWindow = toolWindow;
        mProject = project;
        initContent();
        initDetail();
    }

    private void initContent() {
        mPanel.setOpaque(false);
        mScrollPane.setOpaque(false);
        mScrollPane.getViewport().setOpaque(false);
        mCb.setModel(new DefaultComboBoxModel(levels));
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        content = contentFactory.createContent(mPanel, STACKTRACE, false);
        Content content_review = contentFactory.createContent(mPanel, REVIEW, false);
        Content content_userdata = contentFactory.createContent(mPanel, USERDATA, false);
        Content content_console = contentFactory.createContent(mPanel, CONSOLELOG, false);
        Content content_bugtag = contentFactory.createContent(mPanel, BUGTAGLOG, false);
        Content content_anr = contentFactory.createContent(mPanel, ANRLOG ,false);
        Content content_image = contentFactory.createContent(mPanel, SNAPSHOT ,false);
        myToolWindow.getContentManager().addContent(content);
        myToolWindow.getContentManager().addContent(content_review);
        myToolWindow.getContentManager().addContent(content_userdata);
        myToolWindow.getContentManager().addContent(content_console);
        myToolWindow.getContentManager().addContent(content_bugtag);
        myToolWindow.getContentManager().addContent(content_anr);
        myToolWindow.getContentManager().addContent(content_image);
        myToolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentAdded(ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void contentRemoved(ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void contentRemoveQuery(ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void selectionChanged(ContentManagerEvent contentManagerEvent) {
                if(currentContent.equals(contentManagerEvent.getContent().getDisplayName())){//系统会调用两次  一次是退出一次是进入
                    return ;
                }
                if(mIssueDetail == null)
                    return ;
                if(map.get(issue.id) == null){//保存id
                    Map<String,String> tabMap = new HashMap<>();
                    map.put(issue.id,tabMap);
                }
                Map<String,String> tabMap = (Map<String, String>) map.get(issue.id);
                Content content = contentManagerEvent.getContent();
                mDetail.setText("");
                String name = content.getDisplayName();
                String url = "";
                try{//不使用URL做为key
                    switch (name){
                        case STACKTRACE:
                            url = mIssueDetail.latest_crash.occurrence_info.crash_log;
                            break;
                        case REVIEW:
                            url = mIssueDetail.latest_crash.occurrence_info.user_steps;
                            break;
                        case USERDATA:
                            url = mIssueDetail.latest_crash.occurrence_info.user_data;
                            break;
                        case CONSOLELOG:
                            url = mIssueDetail.latest_crash.occurrence_info.console_log;
                            break;
                        case BUGTAGLOG:
                            url = mIssueDetail.latest_crash.occurrence_info.btg_log;
                            break;
                        case ANRLOG:
                            url = mIssueDetail.latest_crash.occurrence_info.anr_log;
                            break;
                        case SNAPSHOT:
                            url = issue.snapshots.get(0).url;
                            break;
                    }
                    if(name.equals(CONSOLELOG)){
                        mCb.setVisible(true);
                    }else{
                        mCb.setVisible(false);
                    }
                    if(tabMap.get(name) != null){//读取缓存
                        mDetail.setText(tabMap.get(name));
                        mDetail.setCaretPosition(0);
                        return ;
                    }
                    okhttp3.Response resp = manager.getClient().newCall(new Request.Builder().url(url)
                            .get().build()).execute();
                    String text = "";
                    if(name.equals(STACKTRACE)){
                        text = parseString(resp.body().string());
                    }else if(name.equals(REVIEW)){
                        text = parseUserStepsString(resp.body().string());
                    }else if(name.equals(CONSOLELOG)){
                        text = parseConsoleString(resp.body().string());
                    }else if(name.equals(SNAPSHOT)){
                        text = parseImageString(url);
                    }else{
                        text = resp.body().string().replace("\n","<br />");
                    }
                    mDetail.setText(text);
                    mDetail.setCaretPosition(0);
                    tabMap.put(name,text);
                    resp.body().close();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    JScrollBar bar = mScrollPane.getVerticalScrollBar();
                    bar.setValue(bar.getMinimum());
                    currentContent = contentManagerEvent.getContent().getDisplayName();
                }
            }
        });
        mCb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int level = mCb.getSelectedIndex();
                mDetail.setText(getLevelText(level));
                mDetail.setCaretPosition(0);
            }
        });
    }

    private String getLevelText(int level) {
        Map<String,String> tabMap = (Map<String, String>) map.get(issue.id);
        String content = tabMap.get(CONSOLELOG);
        String rows[] = content.split("<br />");
        StringBuffer sb = new StringBuffer();
        for(String row:rows) {
            String tag = row.split(">")[1].split(" ")[2].substring(0, 1);
            switch (level){
                case 0://Verbose
                    sb.append(row+"<br />");
                    break;
                case 1://Debug
                    if(!tag.equals("V"))
                        sb.append(row+"<br />");
                    break;
                case 2://Info
                    if(!tag.equals("V")&&!tag.equals("D"))
                        sb.append(row+"<br />");
                    break;
                case 3://Warn
                    if(tag.equals("W")||tag.equals("E"))
                        sb.append(row+"<br />");
                    break;
                case 4://Error
                    if(tag.equals("E"))
                        sb.append(row+"<br />");
                    break;
            }
        }
        return sb.toString();
    }

    private String parseImageString(String url){
        System.out.println(url);
        return "<img src="+url+" width=330 height=600></img>";
    }

    private String parseConsoleString(String data) {
        StringBuffer sb = new StringBuffer();
        String rows[] = data.split("\n");
        for(String str:rows){
            String level = str.split(" ")[2].substring(0,1);
            String row = "";
            switch (level){
                case "A":
                case "E":
                    row = "<font color=#7f0000>"+str+"</font><br />";
                    break;
                case "W":
                    row = "<font color=#00007f>"+str+"</font><br />";
                    break;
                case "D":
                case "I":
                case "V":
                    row = "<font color=#000000>"+str+"</font><br />";
                    break;
            }
            if(row.contains(PREFIX)){
                row = row.replace(PREFIX,"<strong>"+PREFIX+"</strong>");
            }
            sb.append(row);
        }
        return sb.toString();
    }

    private String parseUserStepsString(String data) {
        try{
            StringBuffer sb = new StringBuffer();
            String rows[] = data.split("\n");
            for(String str:rows){
                String[] split = str.split(" ");
                sb.append(split[0]+" "+split[1]+"<br />");//设置时间
                String className = split[2].replace(":","");
                String href = "http://" + className.substring(0,className.lastIndexOf("."))
                        + "/" + className.substring(className.lastIndexOf(".")+1)+".java?" + "1";
                if(isFileExist(new URL(href))){
                    sb.append("<a href="+ href +">"+className+"</a><br />");
                }else{
                    sb.append(className+"<br />");
                }
                for(int i=3;i<split.length;i++){
                    sb.append(split[i]);
                }
                sb.append("<br />");
            }
            return sb.toString();
        }catch (Exception e){
            return null;
        }
    }

    private void initDetail() {
        mDetail.setEditable(false);
        mDetail.setContentType("text/html");
        mDetail.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    System.out.println(e.getURL());
                    try {
                        openFileFromLink(e.getURL());
                    } catch (Throwable var5) {
                        var5.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void init(ToolWindow window) {

    }

    @Override
    public void onShowText(APIManager manager, IssueInfo issue, String app_id) {
        this.issue = issue;
        this.appId = app_id;
        this.manager = manager;
        if(content!=null)
        myToolWindow.getContentManager().setSelectedContent(content);
        Call<BaseMessage<PageInfo<CrashInfo>>> baseMessageCall = manager.getBugtagAPI().issueCrashes(app_id, issue.id, 1);
        try {
            Response<BaseMessage<PageInfo<CrashInfo>>> response = baseMessageCall.execute();
            if (response.isSuccessful()) {
                BaseMessage<PageInfo<CrashInfo>> body = response.body();
                if (body != null) {
                    if (body.isSuccess()) {
                        okhttp3.Response resp = manager.getClient().newCall(new Request.Builder().url(body.data.list.get(0).occurrence_info.crash_log)
                                .get().build()).execute();
                        String result = parseString(resp.body().string());
                        resp.body().close();
                        mDetail.setText(result);
                        mDetail.setCaretPosition(0);
                        getIssueDetail();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getIssueDetail(){
        try {
            Call<BaseMessage<IssueDetail>> call = manager.getBugtagAPI().issueDetail(appId, issue.id);
            Response<BaseMessage<IssueDetail>> response = call.execute();
            if (response.isSuccessful()) {
                BaseMessage<IssueDetail> body = response.body();
                if(body != null && body.isSuccess()){
                    mIssueDetail = body.getData();
                    System.out.println("");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseString(String text) {
        String[] rows = text.split("\n");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            if (!row.contains("(")) {
                sb.append(row + "<br/>");
                continue;
            } else {
                String str = row.substring(4, row.indexOf("("));
                String[] split = str.split("\\.");
                StringBuffer sbChild = new StringBuffer();
                for (int j = 0; j < split.length - 2; j++) {
                    sbChild.append(split[j]);
                    if (j != split.length - 3) {
                        sbChild.append(".");
                    }
                }
                String strEnd = row.substring(row.indexOf("(")+1);
                String filename = strEnd.split(":")[0];
                String href = "";
                try {
                    href = "http://" + sbChild.toString() + "/" + filename+"?" + strEnd.split(":")[1].replace(")", "");
                    for (int j = 0; j < row.length(); j++) {
                        char c = row.charAt(j);
                        sb.append(c);
                        if (c == '(' && isFileExist(new URL(href))) {
                            sb.append("<a href=" + href + " color=blue>");
                            for (int k = j+1; k < row.length(); k++) {
                                sb.append(row.charAt(k));
                                if (row.charAt(k) == ')') {
                                    sb.append("</a>");
                                    j = k;
                                    break;
                                }
                            }
                        }
                    }
                    sb.append("<br />");
                } catch (Exception e) {

                }
            }
        }
        return sb.toString();
    }
    private void openFileFromLink(URL link) {
        SourceDetail sd = new SourceDetail();
        String url = link.getPath();
        sd.packageName = link.getHost();
        sd.fileName = link.getPath().replace("/","");
        sd.lineNumber = Integer.parseInt(link.getQuery());
        OpenFileUtils.openFile(sd,mProject);
    }
    private boolean isFileExist(URL link){
        SourceDetail sd = new SourceDetail();
        String url = link.getPath();
        sd.packageName = link.getHost();
        sd.fileName = link.getPath().replace("/","");
        sd.lineNumber = Integer.parseInt(link.getQuery());
        return OpenFileUtils.isFileExist(sd,mProject);
    }
}