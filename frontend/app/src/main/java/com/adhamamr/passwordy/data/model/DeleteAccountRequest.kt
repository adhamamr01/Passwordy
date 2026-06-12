package com.adhamamr.passwordy.data.model

/** Confirms account deletion by re-supplying the current master password to DELETE /api/account. */
data class DeleteAccountRequest(val masterPassword: String)
