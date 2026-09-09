package com.example.data.model

enum class GameMode(val title: String, val subtitle: String) {
    ONLINE_MULTIPLAYER("Play Online", "Match with global players or invite friends"),
    PLAY_VS_BOT("Vs Computer", "Play offline against tactical AI bots"),
    LOCAL_PASS_AND_PLAY("Pass & Play", "Play locally on one device with friends")
}

enum class BoardTheme(val title: String, val primaryBgHex: Long) {
    CLASSIC("Classic Gold", 0xFFFFF8E7),
    ROYAL_DARK("Royal Navy", 0xFF0F172A),
    FESTIVE_EMERALD("Festive Emerald", 0xFF064E3B)
}
