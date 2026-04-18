package com.example.grpcdemo.job;

final class JobNaming {

    private JobNaming() {}

    static boolean validName(String name) {
        return name != null && name.matches("[a-zA-Z][a-zA-Z0-9_-]{0,63}");
    }

    /** Safe actor path segment for a job name. */
    static String actorName(String jobName) {
        return "job-" + jobName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
