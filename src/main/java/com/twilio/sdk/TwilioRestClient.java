package com.twilio.sdk;


/*
Copyright (c) 2008 Twilio, Inc.

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class TwilioRestClient {
    private static final String DEFAULT_ENDPOINT = "https://api.twilio.com";
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_READ_TIMEOUT = 300000;

    private final String endpoint;
    private final String accountSid;
    private final String authToken;
    private final int connectTimeout;
    private final int readTimeout;


    public TwilioRestClient(String accountSid, String authToken) {
        this(accountSid, authToken, DEFAULT_ENDPOINT);
    }

    public TwilioRestClient(final String accountSid, final String authToken, final String endpoint) {
        this(accountSid, authToken, endpoint, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public TwilioRestClient(final String accountSid, final String authToken, final String endpoint,
                            final int connectTimeout, final int readTimeout) {
        Validate.notEmpty(accountSid, "AccountSid cannot be NULL");
        Validate.notEmpty(authToken, "AuthToken cannot be NULL");
        Validate.notEmpty(endpoint, "Endpoint cannot be NULL");

        this.accountSid = accountSid;
        this.authToken = authToken;
        this.endpoint = endpoint;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /*
    * sendRequst
    *   Sends a REST Request to the Twilio REST API
    *   $path : the URL (relative to the endpoint URL, after the /v1)
    *   $method : the HTTP method to use, defaults to GET
    *   $vars : for POST or PUT, a key/value associative array of data to send, for GET will be appended to the URL as query params
    */
    public TwilioRestResponse request(String path, HttpMethod method, Map<String, String> vars) throws TwilioRestException {

        // JAF: If vars is empty map,
        // java.lang.StringIndexOutOfBoundsException: String index out of range: -1
        final String encoded = encodeVars(vars);

        // construct full url
        String url = this.endpoint + path;

        // if GET and vars, append them
        if (HttpMethod.GET.equals(method)) {
            url += ((path.indexOf('?') == -1) ? "?" : "&") + encoded;
        }

        try {
            final URL resturl = new URL(url);

            final HttpURLConnection con = (HttpURLConnection) resturl.openConnection();
            final String encodeuserpass = new String(Base64.encodeBase64((this.accountSid + ":" + this.authToken).getBytes()));

            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);
            con.setRequestProperty("Authorization", "Basic " + encodeuserpass);
            con.setDoOutput(true);

            switch (method) {
                case GET: {
                    con.setRequestMethod("GET");
                    break;
                }
                case DELETE: {
                    con.setRequestMethod("DELETE");
                    break;
                }
                case POST: {
                    con.setRequestMethod("POST");
                    OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                    out.write(encoded);
                    out.close();
                }
                case PUT: {
                    con.setRequestMethod("PUT");
                    OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                    out.write(encoded);
                    out.close();
                    break;
                }
                default: {
                    throw new TwilioRestException("Unknown method " + method);
                }
            }

            BufferedReader in = null;
            try {
                if (con.getInputStream() != null) {
                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                }
            } catch (IOException e) {
                if (con.getErrorStream() != null) {
                    in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
            }

            if (in == null) {
                throw new TwilioRestException("Unable to read response from server");
            }

            final StringBuilder decodedString = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                decodedString.append(line);
            }
            in.close();

            return new TwilioRestResponse(url, decodedString.toString(), con.getResponseCode());
        } catch (MalformedURLException e) {
            throw new TwilioRestException(e);
        } catch (IOException e) {
            throw new TwilioRestException(e);
        }
    }

    private String encodeVars(Map<String, String> vars) {
        String encoded = "";
        if (vars != null && !vars.keySet().isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (String key : vars.keySet()) {
                try {
                    sb.append("&").append(key).append("=").append(URLEncoder.encode(vars.get(key), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new TwilioRestException(e);
                }
            }
            encoded = sb.toString().substring(1);
        }
        return encoded;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }
}
