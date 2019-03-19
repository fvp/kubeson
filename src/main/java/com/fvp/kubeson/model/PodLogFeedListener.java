package com.fvp.kubeson.model;

public interface PodLogFeedListener {

    void onNewLogLine(LogSource logSource, LogLine logLine);

    void onLogLineRemoved(LogLine logLine);

    void onPodLogFeedTerminated(Pod pod);

}
