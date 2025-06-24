package com.example.ivdemo.model

import java.io.File

class P2PConnStateInfo {
    var recvAudioFile: File? = null
    var recvVideoFile: File? = null
    var clientReady: Boolean = false
    var clientSendError: Boolean = false
    var decodeAac: Boolean = false
}