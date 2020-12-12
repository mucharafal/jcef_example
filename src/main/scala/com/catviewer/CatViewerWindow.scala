package com.catviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent
import org.cef.CefApp

case class CatViewerWindow(project: Project) {
  private lazy val webView: JBCefBrowser = {
    val browser = new JBCefBrowser()
    registerAppSchemeHandler()
    browser.loadURL("http://myapp/index.html")
    Disposer.register(project, browser)
    browser
  }

  def content: JComponent = webView.getComponent

  private def registerAppSchemeHandler(): Unit = {
    CefApp
      .getInstance()
      .registerSchemeHandlerFactory(
        "http",
        "myapp",
        new CustomSchemeHandlerFactory
      )
  }
}
