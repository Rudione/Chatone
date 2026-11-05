package io.rudione.chatone.util.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.writeToFile
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal object IosFilePicker {
    private var active: NSObject? = null

    suspend fun pickImage(folder: String): String? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController() ?: return@suspendCancellableCoroutine continuation.resume(null)
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = configuration)
        val delegate = PhotoDelegate(folder) { path ->
            active = null
            if (continuation.isActive) continuation.resume(path)
        }
        active = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    suspend fun pickDocument(types: List<UTType>, folder: String): String? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController() ?: return@suspendCancellableCoroutine continuation.resume(null)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        val delegate = DocumentDelegate(folder) { path ->
            active = null
            if (continuation.isActive) continuation.resume(path)
        }
        active = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    suspend fun export(name: String, data: NSData): String? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController() ?: return@suspendCancellableCoroutine continuation.resume(null)
        val path = NSTemporaryDirectory() + name
        if (!data.writeToFile(path, atomically = true)) return@suspendCancellableCoroutine continuation.resume(null)
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true)
        val delegate = ExportDelegate { saved ->
            active = null
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            if (continuation.isActive) continuation.resume(saved)
        }
        active = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    fun persist(source: NSURL, folder: String): String? {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: return null
        val directory = "$base/$folder"
        val manager = NSFileManager.defaultManager
        manager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val extension = source.pathExtension?.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        val target = "$directory/${NSUUID().UUIDString}$extension"
        return if (manager.copyItemAtURL(source, toURL = NSURL.fileURLWithPath(target), error = null)) target else null
    }

    private fun topViewController(): UIViewController? {
        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
        var controller = scene?.keyWindow?.rootViewController ?: return null
        while (true) controller = controller.presentedViewController ?: return controller
    }

    private class PhotoDelegate(private val folder: String, private val onDone: (String?) -> Unit) :
        NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, completion = null)
            val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return onDone(null)
            provider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) { url, _ ->
                val path = url?.let { persist(it, folder) }
                dispatch_async(dispatch_get_main_queue()) { onDone(path) }
            }
        }
    }

    private class ExportDelegate(private val onDone: (String?) -> Unit) :
        NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onDone((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.path)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onDone(null)
    }

    private class DocumentDelegate(private val folder: String, private val onDone: (String?) -> Unit) :
        NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onDone((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let { persist(it, folder) })
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onDone(null)
    }
}
