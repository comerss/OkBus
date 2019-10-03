package com.comers.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


class OkBusPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "this is my custom plugin OkBusPlugin"

        project.android.registerTransform(new OkBusTransform(project))
    }

}