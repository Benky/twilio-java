package com.twilio.sdk;

public class TwilioRestException extends RuntimeException {

    public TwilioRestException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TwilioRestException(Throwable throwable) {
        super(throwable);
    }

    public TwilioRestException(String arg0) {
        super(arg0);
    }

}
