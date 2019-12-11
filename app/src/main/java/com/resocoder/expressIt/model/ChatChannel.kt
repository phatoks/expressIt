package com.resocoder.expressIt.model


data class ChatChannel(val userIds: MutableList<String>) {
    constructor() : this(mutableListOf())
}