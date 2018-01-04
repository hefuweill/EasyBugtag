
package com.souche.bugtag;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import javax.swing.JOptionPane;

public class OpenFileUtils {

    public OpenFileUtils() {
    }

    private static VirtualFile[] getSourceRoots(ProjectRootManager pm) {
        VirtualFile[] vfs = pm.getContentSourceRoots();
        return vfs;
    }

    public static void openFile(SourceDetail sd, Project myProject) {
        String fileName = getFileName(sd);
        ProjectRootManager pm = ProjectRootManager.getInstance(myProject);
        FileEditorManager fem = FileEditorManager.getInstance(myProject);
        VirtualFile[] vfs = getSourceRoots(pm);
        int i = 0;
        VirtualFile vf = null;
        String fullPath = null;
        boolean opened = false;

        while(i < vfs.length && !opened) {
            vf = vfs[i];
            fullPath = vf.getPresentableUrl() + fileName;
            fullPath = fullPath.replace('\\', '/');
            fullPath = fullPath.replaceAll(".zip/", ".zip!/");
            fullPath = fullPath.replaceAll(".jar/", ".jar!/");
            vf = vf.getFileSystem().findFileByPath(fullPath);
            OpenFileDescriptor efd;
            if (vf != null) {
                ++i;
                efd = new OpenFileDescriptor(myProject, vf, sd.lineNumber - 1, 1);
                fem.openTextEditor(efd, true);
                opened = true;
            } else {
                vf = vfs[i];
                fullPath = vf.getPresentableUrl() + "/" + sd.fileName;
                fullPath = fullPath.replace('\\', '/');
                fullPath = fullPath.replaceAll(".zip/", ".zip!/");
                fullPath = fullPath.replaceAll(".jar/", ".jar!/");
                vf = vf.getFileSystem().findFileByPath(fullPath);
                ++i;
                if (vf != null) {
                    efd = new OpenFileDescriptor(myProject, vf, sd.lineNumber - 1, 1);
                    fem.openTextEditor(efd, true);
                    opened = true;
                }
            }
        }

        if (!opened) {
            JOptionPane.showMessageDialog(WindowManager.getInstance().suggestParentWindow(myProject), "The class " + sd.packageName + "." + sd.fileName + " is not found in any of the source paths");
        }

    }

    public static String getFileName(SourceDetail sd) {
        return "/" + sd.packageName.replace('.', '/') + "/" + sd.fileName;
    }

    public static boolean isFileExist(SourceDetail sd, Project myProject){
        String fileName = getFileName(sd);
        ProjectRootManager pm = ProjectRootManager.getInstance(myProject);
        FileEditorManager fem = FileEditorManager.getInstance(myProject);
        VirtualFile[] vfs = getSourceRoots(pm);
        int i = 0;
        VirtualFile vf = null;
        String fullPath = null;
        boolean opened = false;

        while(i < vfs.length && !opened) {
            vf = vfs[i];
            fullPath = vf.getPresentableUrl() + fileName;
            fullPath = fullPath.replace('\\', '/');
            fullPath = fullPath.replaceAll(".zip/", ".zip!/");
            fullPath = fullPath.replaceAll(".jar/", ".jar!/");
            vf = vf.getFileSystem().findFileByPath(fullPath);
            OpenFileDescriptor efd;
            if (vf != null) {
                ++i;
                return true;
            } else {
                vf = vfs[i];
                fullPath = vf.getPresentableUrl() + "/" + sd.fileName;
                fullPath = fullPath.replace('\\', '/');
                fullPath = fullPath.replaceAll(".zip/", ".zip!/");
                fullPath = fullPath.replaceAll(".jar/", ".jar!/");
                vf = vf.getFileSystem().findFileByPath(fullPath);
                ++i;
                if (vf != null) {
                    return true;
                }
            }
        }
        return false;
    }

}
