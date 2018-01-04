package com.souche.bugtag;

import com.souche.bugtag.api.model.IssueInfo;
import com.souche.bugtag.api.service.APIManager;

public interface OnShowTextListener {
    void onShowText(APIManager manager, IssueInfo id, String app_id);
}
