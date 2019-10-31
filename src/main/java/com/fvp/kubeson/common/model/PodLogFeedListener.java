package com.fvp.kubeson.common.model;

import com.fvp.kubeson.logs.model.LogLine;
import com.fvp.kubeson.logs.model.LogSource;

public interface PodLogFeedListener {

    void onNewLogLine(LogSource logSource, LogLine logLine);

    void onLogLineRemoved(LogLine logLine);

    void onPodLogFeedTerminated(K8SPod pod);

}
