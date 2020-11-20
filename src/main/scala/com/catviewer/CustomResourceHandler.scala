package com.catviewer

import java.io.{IOException, InputStream}
import java.net.JarURLConnection

import org.cef.callback.CefCallback
import org.cef.handler.{CefLoadHandler, CefResourceHandler}
import org.cef.misc.{IntRef, StringRef}
import org.cef.network.{CefRequest, CefResponse}

class CustomResourceHandler extends CefResourceHandler {
  var maybeConnection: Option[JarURLConnection] = None
  var maybeInputStream: Option[InputStream] = None
  override def processRequest(
      cefRequest: CefRequest,
      cefCallback: CefCallback
  ): Boolean = {
    val urlOption = Option(cefRequest.getURL)
    urlOption match {
      case Some(processedUrl) =>
        val pathToResource = processedUrl.replace("http://myapp", "webview/")
        val newUrl = getClass.getClassLoader.getResource(pathToResource)
        maybeConnection = Some(
          newUrl.openConnection().asInstanceOf[JarURLConnection]
        )
        cefCallback.Continue()
        true
      case None => false
    }
  }

  override def getResponseHeaders(
      cefResponse: CefResponse,
      responseLength: IntRef,
      redirectUrl: StringRef
  ): Unit = {
    maybeConnection match {
      case None => cefResponse.setStatus(404)
      case Some(connection) =>
        try {
          val url = connection.getURL.toString
          url match {
            case x if x.contains("css") => cefResponse.setMimeType("text/css")
            case x if x.contains("js") =>
              cefResponse.setMimeType("text/javascript")
            case _ => cefResponse.setMimeType(connection.getContentType)
          }
          maybeInputStream = Some(connection.getInputStream)
          responseLength.set(maybeInputStream.get.available())
          cefResponse.setStatus(200)
        } catch {
          case e: IOException =>
            cefResponse.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND)
            cefResponse.setStatusText(e.getLocalizedMessage)
            cefResponse.setStatus(404)
        }
    }
  }

  override def readResponse(
      dataOut: Array[Byte],
      designedBytesToRead: Int,
      bytesRead: IntRef,
      callback: CefCallback
  ): Boolean = {
    maybeInputStream match {
      case None => false
      case Some(inputStream) =>
        val availableSize = inputStream.available()
        if (availableSize > 0) {
          val maxBytesToRead = Math.min(availableSize, designedBytesToRead)
          val realNumberOfReadBytes =
            inputStream.read(dataOut, 0, maxBytesToRead)
          bytesRead.set(realNumberOfReadBytes)
          true
        } else {
          inputStream.close()
          maybeConnection = None
          maybeInputStream = None
          false
        }
    }
  }

  override def cancel(): Unit = {
    maybeInputStream = None
    maybeConnection = None
  }
}
