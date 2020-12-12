package com.catviewer

import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest

class CustomSchemeHandlerFactory extends CefSchemeHandlerFactory {
  override def create(
    cefBrowser: CefBrowser,
    cefFrame:   CefFrame,
    s:          String,
    cefRequest: CefRequest
  ): CefResourceHandler = {
    new CustomResourceHandler()
  }
}