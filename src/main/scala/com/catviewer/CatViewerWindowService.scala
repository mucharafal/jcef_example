package com.catviewer

import com.intellij.openapi.project.Project
import com.catviewer.CatViewerWindow

class CatViewerWindowService(val project: Project) {
  val catViewerWindow = CatViewerWindow(project)
}