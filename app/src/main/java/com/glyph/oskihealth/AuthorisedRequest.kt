package com.glyph.oskihealth

import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest

open class AuthorisedRequest(
    method: Int, url: String,
    listener: (response: String) -> Unit,
    errorListener: (error: VolleyError) -> Unit,
) : StringRequest(method, HOST+url, listener, errorListener)
{
    companion object {
        var USERNAME = "mingy"
        var PASSWORD = "123"
        var HOST = "http://13.229.240.184:8888"
    }

    override fun getHeaders(): MutableMap<String, String> {
        val old = super.getHeaders()
        val new = HashMap<String, String>()
        for ((key, value) in old) new[key] = value
        new["X-Username"] = USERNAME
        new["X-Password"] = PASSWORD
        return new
    }
}