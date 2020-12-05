package com.catviewer

import java.io.{IOException, InputStream}
import java.net.JarURLConnection
import org.cef.callback.CefCallback
import org.cef.handler.{CefLoadHandler, CefResourceHandler}
import org.cef.misc.{IntRef, StringRef}
import org.cef.network.{CefRequest, CefResponse}

class CustomResourceHandler extends CefResourceHandler {
  private var state: ResourceHandlerState = ClosedConnection
  override def processRequest(
      cefRequest: CefRequest,
      cefCallback: CefCallback
  ): Boolean = {
    val urlOption = Option(cefRequest.getURL)
    urlOption match {
      case Some(processedUrl) =>
        val pathToResource = processedUrl.replace("http://myapp", "webview/")
        val newUrl = getClass.getClassLoader.getResource(pathToResource)
        state = OpenedConnection(
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
    state.getResponseHeaders(cefResponse, responseLength, redirectUrl)
  }

  override def readResponse(
      dataOut: Array[Byte],
      designedBytesToRead: Int,
      bytesRead: IntRef,
      callback: CefCallback
  ): Boolean = {
    state.readResponse(dataOut, designedBytesToRead, bytesRead, callback)
  }

  override def cancel(): Unit = {
    state.close()
    state = ClosedConnection
  }
}

sealed trait ResourceHandlerState {
   def getResponseHeaders(
      cefResponse: CefResponse,
      responseLength: IntRef,
      redirectUrl: StringRef
  ): Unit

   def readResponse(
      dataOut: Array[Byte],
      designedBytesToRead: Int,
      bytesRead: IntRef,
      callback: CefCallback
  ): Boolean

  def close(): Unit = {}
}
case class OpenedConnection(connection: JarURLConnection) extends ResourceHandlerState {
  private lazy val inputStream: InputStream = connection.getInputStream
  override def getResponseHeaders(
      cefResponse: CefResponse,
      responseLength: IntRef,
      redirectUrl: StringRef
  ): Unit = {
    try {
      val url = connection.getURL.toString
      url match {
        case x if x.contains("css") => cefResponse.setMimeType("text/css")
        case x if x.contains("js") =>
          cefResponse.setMimeType("text/javascript")
        case _ => cefResponse.setMimeType(connection.getContentType)
      }
      responseLength.set(inputStream.available())
      cefResponse.setStatus(200)
    } catch {
      case e: IOException =>
        cefResponse.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND)
        cefResponse.setStatusText(e.getLocalizedMessage)
        cefResponse.setStatus(404)
    }
  }

  override def readResponse(
      dataOut: Array[Byte],
      designedBytesToRead: Int,
      bytesRead: IntRef,
      callback: CefCallback
  ): Boolean = {
    val availableSize = inputStream.available()
    if (availableSize > 0) {
      val maxBytesToRead = Math.min(availableSize, designedBytesToRead)
      val realNumberOfReadBytes =
        inputStream.read(dataOut, 0, maxBytesToRead)
      bytesRead.set(realNumberOfReadBytes)
      true
    } else {
      inputStream.close()
      false
    }
  }

  override def close(): Unit = {
    inputStream.close()
  }
}

case object ClosedConnection extends ResourceHandlerState {
  override def getResponseHeaders(
      cefResponse: CefResponse,
      responseLength: IntRef,
      redirectUrl: StringRef
  ): Unit = {
    cefResponse.setStatus(404)
  }

  override def readResponse(
      dataOut: Array[Byte],
      designedBytesToRead: Int,
      bytesRead: IntRef,
      callback: CefCallback
  ): Boolean = {
    false
  }
}
