package folioxml.core;

import folioxml.folio.FolioToken;
import folioxml.slx.SlxToken;

import java.io.PrintWriter;
import java.io.StringWriter;


public class InvalidMarkupException extends Exception {
    public InvalidMarkupException(String message) {
        super(message);
    }

    public InvalidMarkupException(String message, Object token) {
        super(message);
        setToken(token);
    }

    @Override
    public String getMessage() {
        String s = super.getMessage();

        if (token != null && token instanceof SlxToken) {
            try {
                s += "\n" + ((SlxToken) token);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                s += "\n[SlxToken.getText() failed: " + e.getMessage() + "\n" + sw.toString() + "]";

            }
        }
        if (tokenInfo != null) {
            if (tokenInfo.text != null) s += "\n" + tokenInfo.text;
            s += "\n" + tokenInfo.toString();
        }
        return s;
    }

    protected Object token = null;
    public TokenInfo tokenInfo = null;

    public Object getToken() {
        return token;
    }


    public void setToken(Object token) {
        this.token = token;
        if (token != null) {

            if (token instanceof TokenInfo) tokenInfo = (TokenInfo) token;
            if (token instanceof FolioToken) tokenInfo = ((FolioToken) token).info;
            if (token instanceof SlxToken && ((SlxToken) token).sourceToken != null)
                tokenInfo = ((SlxToken) token).sourceToken.info;
        }
    }

    public InvalidMarkupException() {

    }
}