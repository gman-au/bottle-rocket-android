package au.com.gman.bottlerocket.interfaces

import android.net.Uri

interface IFileSaveListener {
    fun onFileSaveSuccess(uri: Uri)
    fun onFileSaveFailure()
}