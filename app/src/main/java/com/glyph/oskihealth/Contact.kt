package com.glyph.oskihealth

data class Contact(
    val type: ContactType,
    val name: String
) {
    enum class ContactType {
        PERSON, AI
    }
}
