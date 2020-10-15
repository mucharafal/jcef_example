package com.catviewer

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowFactory }

class WindowFactory extends ToolWindowFactory {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val catViewerWindow = ServiceManager.getService(project, classOf[CatViewerWindowService]).catViewerWindow
    val component     = toolWindow.getComponent
    component.getParent.add(catViewerWindow.content)
  }
}