package com.souche.bugtag;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.souche.bugtag.api.model.*;
import com.souche.bugtag.api.service.APIManager;
import com.souche.bugtag.utils.SettingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class ToolFactoryCompute implements ToolWindowFactory, OnSettingApplyListener, ItemListener {

    private ToolWindow myToolWindow;
    private JPanel mSpanel;
    private JScrollPane mScrollPane;
    private JList<IssueInfo> mList;
    private JButton mBt_left;
    private JButton mBt_right;
    private JEditorPane mEt_desc;
    private JCheckBox mCb_bengkui;
    private JCheckBox mCb_anr;
    private JCheckBox mCb_exception;
    private JCheckBox mCb_new;
    private JCheckBox mCb_ongoing;
    private JCheckBox mCb_reopen;
    private JCheckBox mCb_handle;
    private JCheckBox mCb_suspension;
    private JCheckBox mCb_nothandle;
    private JCheckBox mCb_close;
    private JComboBox<String> mCb;
    private JComboBox<String> mCb_sort;
    private JComboBox<String> mCb_project;
    private APIManager manager;
    private Project mProject;
    private static OnShowTextListener mListener;
    private int currentPage = 1;
    private BugtagAPP currentBugtagApp;
    private final String BETA = "Beta";
    private final String LIVE = "Live";
    private String currentContent = BETA;
    private BugtagAPP app_beta;
    private BugtagAPP app_live;
    private static final int TYPE_BENGKUI = 3;
    private static final int TYPE_ANR = 8;
    private static final int TYPE_EXCEPTION = 7;
    private static final int FLAG_NEW = 0;
    private static final int FLAG_ONGOING = 1;
    private static final int FLAG_HANDLE = 2;
    private static final int FLAG_CLOSE = 3;
    private static final int FLAG_REOPEN = 4;
    private static final int FLAG_NOTHANDLE = 5;
    private static final int FLAG_SUSPENSION = 6;
    private int totalCount;
    private ArrayList<Integer> types = new ArrayList<>();
    private ArrayList<String> versions = new ArrayList<>();
    private ArrayList<Integer> flags = new ArrayList<>();
    private Map<String, Map<String, String>> map;//已经排好序的version map
    private String sortTypeName[] = {"提交时间","更新时间","优先级","崩溃次数","影响机型数",
    "影响用户数"};
    private String sortType[] = {"created_at","updated_at","priority","crash_num","model_num","device_num"};
    private int currentSortType = 0;
    private boolean isSearch = false;
    private List<IssueInfo> mIssueInfos;
    private List<List<BugtagAPP>> projects;
    private boolean isInitComponent = false;//初始化组件屏蔽事件响应


    @Override
    public void createToolWindowContent(@NotNull Project project,
                                        @NotNull ToolWindow toolWindow) {
        ToolFactoryCompute myToolWindowFactory = new ToolFactoryCompute();
        myToolWindowFactory.process(project,toolWindow);//开启多个window的时候不会只显示一个
    }

    private void process(Project project, ToolWindow toolWindow){
        mCb_sort.setModel(new DefaultComboBoxModel<>(sortTypeName));
        myToolWindow = toolWindow;
        mProject = project;
        //检测新版本
        checkNewVersion();
        Setting.setOnSettingApplyListener(this);
        mSpanel.setOpaque(false);
        mScrollPane.setOpaque(false);
        mScrollPane.getViewport().setOpaque(false);
        String cookie = SettingUtils.getInstance().getCookie();
        if(cookie == null || "".equals(cookie)){
            int index = Messages.showDialog(project, "请先前往Setting界面设置Cookie", "提示", new String[]{"取消","确定"}, 1, Messages.getInformationIcon());
            if(index == 1){
                ShowSettingsUtil.getInstance().editConfigurable(project, Setting.getInstance());
            }
            return ;
        }
        initData(cookie);
    }

    private void checkNewVersion() {
        Properties p = new Properties();
        try {
            p.load(getClass().getResourceAsStream("/config.properties"));
            String version = p.getProperty("version");
            Call<BaseMessage<NewVersion>> call = APIManager.getInstance().getPluginAPI().checkNewVersion("{}");
            Response<BaseMessage<NewVersion>> resp = call.execute();
            if(resp.isSuccessful()) {
                BaseMessage<NewVersion> body = resp.body();
                if(body != null && !version.equals(body.data.versionName)){
                    String savedVersionName = SettingUtils.getInstance().getVersion();
                    if(body.data.versionName.equals(savedVersionName)){
                        return ;
                    }
                    int index = Messages.showDialog(body.data.desc, body.data.title, new String[]{"取消","跳过该版本","去更新"}, 2, null);
                    if(index == 2){
                        try {
                            Desktop.getDesktop().browse(new URL(body.data.url).toURI());
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }else if(index == 1){
                        SettingUtils.getInstance().saveVersion(body.data.versionName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initData(String cookie) {
        try{
            manager = APIManager.getInstance();
            manager.initByCookies(cookie);
            Call<BaseMessage<List<BugtagAPP>>> apps = manager.getBugtagAPI().apps();
            Response<BaseMessage<List<BugtagAPP>>> response = apps.execute();
            if(response.isSuccessful()){
                BaseMessage<List<BugtagAPP>> body = response.body();
                if(body!=null){
                    if(body.isSuccess()){
                        packProject(body);
                        initTab(0);
                    }
                    else{
                        cookieNotRight(null);
                    }
                }
                else{
                    cookieNotRight(null);
                }
            }else{
                cookieNotRight(null);
            }
            initListener();
        }catch (Exception e){
            cookieNotRight("未知错误");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void initListener() {
        mBt_left.setEnabled(false);
        if(Integer.parseInt(currentBugtagApp.issue_num) / 20 + 1 <= 1){
            mBt_right.setEnabled(true);
        }
        mBt_left.addActionListener(e -> {
            if(e.getSource() == mBt_left){
                if(currentPage > 1){
                    currentPage -= 1;
                    if(currentPage <= 1){
                        mBt_left.setEnabled(false);
                    }
                    mBt_right.setEnabled(true);
                    loadPage();
                }
            }
        });
        mBt_right.addActionListener(e -> {
            if(e.getSource() == mBt_right){
                int maxPage = totalCount / 20 + 1;
                if(currentPage < maxPage){
                    mBt_left.setEnabled(true);
                    mBt_right.setEnabled(true);
                    currentPage++;
                    loadPage();
                }
                if(currentPage == maxPage){
                    mBt_right.setEnabled(false);
                }
            }
        });
        mCb_sort.addActionListener(e -> {
            if(!isInitComponent) {
                currentSortType = mCb_sort.getSelectedIndex();
                currentPage = 1;
                mBt_left.setEnabled(false);
                mBt_right.setEnabled(true);
                if (currentSortType >= 3) {
                    isSearch = true;
                } else {
                    isSearch = false;
                }
                loadPage();
            }
        });
        mCb.addActionListener(e -> {
            if(isInitComponent){
               return ;
            }
            if(map!=null){
                if(versions == null){
                    versions = new ArrayList<>();
                }
                versions.clear();
                Map<String, String> versionChildren = map.get(mCb.getSelectedItem());
                if(versionChildren != null){//不过滤的情况
                    for(Map.Entry<String,String> entry:versionChildren.entrySet()){
                        versions.add(entry.getValue());
                    }
                }
                currentPage = 1;
                mBt_left.setEnabled(false);
                mBt_right.setEnabled(true);
                loadPage();
            }
        });
        mCb_project.addActionListener(e -> {
            currentBugtagApp = projects.get(mCb_project.getSelectedIndex()).get(0);
            myToolWindow.getContentManager().removeContentManagerListener(listener);
            myToolWindow.getContentManager().removeAllContents(true);
            System.out.println("执行");
            initComponent();
            initTab(mCb_project.getSelectedIndex());
        });
        mCb_exception.addItemListener(this);
        mCb_bengkui.addItemListener(this);
        mCb_anr.addItemListener(this);
        mCb_close.addItemListener(this);
        mCb_handle.addItemListener(this);
        mCb_new.addItemListener(this);
        mCb_nothandle.addItemListener(this);
        mCb_suspension.addItemListener(this);
        mCb_reopen.addItemListener(this);
        mCb_ongoing.addItemListener(this);
    }

    private void cookieNotRight(String info){
        if(info == null){
            info = "Cookie设置不正确!";
        }
        Messages.showMessageDialog(mProject, info, "Information", Messages.getInformationIcon());
        myToolWindow.getContentManager().removeAllContents(true);
    }
    private void initTab(int index){
        currentBugtagApp = projects.get(index).get(0);
        String actionName;
        for (int i=0;i<projects.get(index).size();i++){
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            if(i == 0){
                actionName = BETA;
                app_beta = projects.get(index).get(i);
            }else{
                actionName = LIVE;
                app_live = projects.get(index).get(i);
            }
            Content content = contentFactory.createContent(mSpanel, actionName, false);
            myToolWindow.getContentManager().addContent(content);
        }
        loadPage();
        loadVersion();
        loadTotalCount();
        myToolWindow.getContentManager().addContentManagerListener(listener);
    }

    private void packProject(BaseMessage<List<BugtagAPP>> body) {
        projects = new ArrayList<>();
        List<String> alreadyProjectName = new ArrayList<>();
        for(BugtagAPP app:body.data){
            if(!alreadyProjectName.contains(app.name)){
                alreadyProjectName.add(app.name);
                List<BugtagAPP> apps = new ArrayList<>();
                apps.add(app);
                if(app.sub_apps!=null&&app.sub_apps.size()>0){
                    apps.add(app.sub_apps.get(0));
                }
                projects.add(apps);
            }
        }
        Vector<String> vector_complete = new Vector<>(alreadyProjectName);
        mCb_project.setModel(new DefaultComboBoxModel<>(vector_complete));
    }

    private void initComponent() {
        isInitComponent = true;
        clearList();
        versions.clear();
        isSearch = false;
        mCb_anr.setSelected(false);
        mCb_bengkui.setSelected(false);
        mCb_exception.setSelected(false);
        mCb_close.setSelected(false);
        mCb_handle.setSelected(false);
        mCb_new.setSelected(false);
        mCb_nothandle.setSelected(false);
        mCb_suspension.setSelected(false);
        mCb_reopen.setSelected(false);
        mCb_ongoing.setSelected(false);
        mCb_sort.setSelectedIndex(0);
        mCb.setSelectedIndex(0);
        currentPage = 1;
        mEt_desc.setText("");
        currentSortType = 0;
        isInitComponent = false;
    }

    private void loadVersion() {
        try {
            Call<BaseMessage<Map<String, Map<String, String>>>> versions = manager.getBugtagAPI().versions(currentBugtagApp.app_id);
            Response<BaseMessage<Map<String, Map<String, String>>>> response = versions.execute();
            if(response.isSuccessful()){
                BaseMessage<Map<String, Map<String, String>>> body = response.body();
                if(body!=null){
                    if(body.isSuccess()){
                        Vector<String> vector = new Vector<>();
                        //转换为有序的map
                        map = new TreeMap<>(Comparator.reverseOrder());
                        for(Map.Entry<String,Map<String,String>> entry:body.getData().entrySet()){
                            map.put(entry.getKey(),entry.getValue());
                        }
                        vector.add("未选择");
                        for(Map.Entry<String,Map<String,String>> entry: map.entrySet()){
                            vector.add(entry.getKey());
                        }
                        mCb.setModel(new DefaultComboBoxModel<>(vector));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPage() {
        try{
            Call<BaseMessage<PageInfo<IssueInfo>>> issues;
            if(isSearch){
                issues = manager.getBugtagAPI().issuesSearch(currentBugtagApp.app_id, currentPage,
                        types, flags, versions,sortType[currentSortType],null, null);
            }else{
                issues = manager.getBugtagAPI().issuesFilter(currentBugtagApp.app_id, currentPage,
                        types, flags, versions,sortType[currentSortType],null, null);
            }
            Response<BaseMessage<PageInfo<IssueInfo>>> response = issues.execute();
            System.out.println(issues.request().url());
            handleResponse(response);
            if(mScrollPane.getVerticalScrollBar()!=null){
                mScrollPane.getVerticalScrollBar().setValue(0);
            }
            loadTotalCount();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initIssusInfos(){
        mList.removeMouseListener(mouseAdapter);
        mList.setModel(new AbstractListModel<IssueInfo>() {
            @Override
            public int getSize() {
                return mIssueInfos.size();
            }

            @Override
            public IssueInfo getElementAt(int index) {
                return mIssueInfos.get(index);
            }
        });
        mList.addMouseListener(mouseAdapter);
        mList.setCellRenderer(new MyCellRender<IssueInfo>());
    }

    @Override
    public void init(ToolWindow window) {
        System.out.println("init");
    }

    private void createUIComponents() {
        mCb_project = new JComboBox<String>(){
            @Nullable
            @Override
            public Object getSelectedItem() {
                return super.getSelectedItem();
            }
        };
    }

    public static void setOnShowTextListener(OnShowTextListener listener){
        mListener = listener;
    }

    @Override
    public void onSettingApply(String cookie) {
        initData(cookie);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {//根据e获取组件更容易些
        currentPage = 1;
        clearList();
        if(mCb_anr.isSelected()){
            types.add(TYPE_ANR);
        }
        if(mCb_exception.isSelected()){
            types.add(TYPE_EXCEPTION);
        }
        if(mCb_bengkui.isSelected()){
            types.add(TYPE_BENGKUI);
        }
        if(mCb_ongoing.isSelected()){
            flags.add(FLAG_ONGOING);
        }
        if(mCb_reopen.isSelected()){
            flags.add(FLAG_REOPEN);
        }
        if(mCb_suspension.isSelected()){
            flags.add(FLAG_SUSPENSION);
        }
        if(mCb_new.isSelected()){
            flags.add(FLAG_NEW);
        }
        if(mCb_handle.isSelected()){
            flags.add(FLAG_HANDLE);
        }
        if(mCb_nothandle.isSelected()){
            flags.add(FLAG_NOTHANDLE);
        }
        if(mCb_close.isSelected()){
            flags.add(FLAG_CLOSE);
        }
        loadPage();
        mBt_left.setEnabled(false);
        mBt_right.setEnabled(true);
    }

    private void clearList(){
        flags.clear();
        types.clear();
    }
    private void handleResponse(Response<BaseMessage<PageInfo<IssueInfo>>> response){
        if(response.isSuccessful()){
            BaseMessage<PageInfo<IssueInfo>> body = response.body();
            if(body!=null){
                if(body.isSuccess()){
                    if(mIssueInfos == null){
                        mIssueInfos = new ArrayList<>();
                    }
                    mIssueInfos.clear();
                    mIssueInfos.addAll(body.getData().list);
                    initIssusInfos();
                }
            }
        }
    }
    private void loadTotalCount(){
        try {
            Call<BaseMessage<PageInfoEx>> call = manager.getBugtagAPI().issuesPageInfo(currentBugtagApp.app_id, types, flags, versions, sortType[currentSortType],"", "");
            Response<BaseMessage<PageInfoEx>> response = call.execute();
            if(response.isSuccessful()){
                BaseMessage<PageInfoEx> body = response.body();
                if(body!=null){
                    if(body.isSuccess()){
                        PageInfoEx bean = body.getData();
                        totalCount = bean.total;
                        mEt_desc.setText("第"+(((currentPage-1)*20)>=totalCount?totalCount:((currentPage-1)*20+1))+"-"+((currentPage*20)>totalCount?totalCount:(currentPage*20))+"条,共"+totalCount+"条");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JList list = (JList) e.getSource();
                int index = list.getLeadSelectionIndex();
                try{
                    myToolWindow.hide(null);
                }catch (Exception ex){ex.printStackTrace();}
                ToolWindow toolWindow = ToolWindowManager.getInstance(mProject).getToolWindow("Info");
                toolWindow.activate(() -> {
                    IssueInfo info = mIssueInfos.get(index);
                    if (mListener != null && mListener instanceof StackTraceCompute) {
                        mListener.onShowText(manager, info, currentBugtagApp.app_id);
                    }
                });
            }
        }
    };

    private ContentManagerListener listener = new ContentManagerListener() {
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
            Content content = contentManagerEvent.getContent();
            if(content.getDisplayName().equals(currentContent)){
                return ;
            }
            currentContent = content.getDisplayName();
            if(content.getDisplayName().equals(BETA)){
                currentBugtagApp = app_beta;
            }else{
                currentBugtagApp = app_live;
            }
            initComponent();
            loadPage();
            loadVersion();
        }
    };
}
