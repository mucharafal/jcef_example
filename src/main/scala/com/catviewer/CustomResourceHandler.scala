package com.catviewer

import java.io.{IOException, InputStream}
import java.net.JarURLConnection

import org.cef.callback.CefCallback
import org.cef.handler.{CefLoadHandler, CefResourceHandler}
import org.cef.misc.{IntRef, StringRef}
import org.cef.network.{CefRequest, CefResponse}

class CustomResourceHandler extends CefResourceHandler {
  var connectionOption:  Option[JarURLConnection] = None
  var inputStreamOption: Option[InputStream]      = _
  override def processRequest(cefRequest: CefRequest, cefCallback: CefCallback): Boolean = {
    val urlOption = Option(cefRequest.getURL)
    urlOption match {
      case Some(processedUrl) =>
        val pathToResource = processedUrl.replace("http://myapp", "webview/")
        val newUrl         = getClass.getClassLoader.getResource(pathToResource)
        connectionOption = Some(newUrl.openConnection().asInstanceOf[JarURLConnection])
        cefCallback.Continue()
        true
      case None => false
    }
  }

  override def getResponseHeaders(cefResponse: CefResponse, responseLength: IntRef, redirectUrl: StringRef): Unit = {
    if (connectionOption.isDefined) {
      try {
        val connection = connectionOption.get
        cefResponse.setMimeType(connection.getContentType)
        if (connectionOption.get.getURL.toString.contains("css")) {
          cefResponse.setMimeType("text/css")
        } else {
          if (connectionOption.get.getURL.toString.contains("js")) {
            cefResponse.setMimeType("text/javascript")
          }
        }
        inputStreamOption = Some(connection.getInputStream)
        responseLength.set(inputStreamOption.get.available())
        cefResponse.setStatus(200)
      } catch {
        case e: IOException =>
          cefResponse.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND)
          cefResponse.setStatusText(e.getLocalizedMessage)
          cefResponse.setStatus(404)
      }
    } else {
      cefResponse.setStatus(404)
    }
  }

  override def readResponse(
    dataOut:             Array[Byte],
    designedBytesToRead: Int,
    bytesRead:           IntRef,
    callback:            CefCallback
  ): Boolean = {
    if (inputStreamOption.isDefined) {
      val inputStream   = inputStreamOption.get
      val availableSize = inputStream.available()
      if (availableSize > 0) {
        val bytesToRead           = Math.min(availableSize, designedBytesToRead)
        val realNumberOfReadBytes = inputStream.read(dataOut, 0, bytesToRead)
        bytesRead.set(realNumberOfReadBytes)
        true
      } else {
        inputStreamOption.get.close()
        connectionOption  = None
        inputStreamOption = None
        false
      }
    } else {
      false
    }
  }

  override def cancel(): Unit = {
    inputStreamOption = None
    connectionOption  = None
  }
}