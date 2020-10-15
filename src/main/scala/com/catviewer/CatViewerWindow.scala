package com.catviewer

import java.util.concurrent.atomic.AtomicBoolean

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.catviewer.CustomSchemeHandlerFactory
import javax.swing.JComponent
import org.cef.CefApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future, Promise }
import java.util.concurrent.TimeoutException

case class CatViewerWindow(val project: Project) {
  private lazy val webView: JBCefBrowser = {
    createAndSetUpWebView()
  }

  def content: JComponent = webView.getComponent

  private def createAndSetUpWebView(): JBCefBrowser = {
    val webView = new JBCefBrowser()

    registerAppSchemeHandler()

    webView.loadURL("http://myapp/index.html")

    Disposer.register(project, webView)

    webView
  }

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