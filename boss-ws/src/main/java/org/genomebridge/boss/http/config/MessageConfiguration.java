package org.genomebridge.boss.http.config;

import org.apache.commons.lang.StringUtils;

import javax.validation.Valid;
import java.util.HashMap;

/**
 * Created by vvicario on 3/20/2015.
 */
public class MessageConfiguration {


    private HashMap<String,String> messages;

    public String get(String key) {
        String message = messages.get(key);
        if(StringUtils.isEmpty(message)){
            message = "CODE ERROR: message text for key not found" +key;
        }
        return message;
    }

    public void setMessages(HashMap<String, String> messages) {
        this.messages = messages;
    }
}
