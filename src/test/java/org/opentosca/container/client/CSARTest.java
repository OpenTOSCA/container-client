package org.opentosca.container.client;

import java.util.Map;

public class CSARTest {

    private String name;
    private Map<String, String> input;

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name=name;
    }

    public Map<String,String> getInput(){
        return input;
    }
    public void setInput(Map<String,String> input){
        this.input=input;
    }

}
