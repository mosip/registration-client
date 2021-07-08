package registrationtest.pojo.schema;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings{
    public String name;
    public HashMap<String, String> description;
    public HashMap<String, String> label;
    public String fxml;
    public String icon;
    public String order;
 
     @JsonProperty("shortcut-icon") 
     public String shortcutIcon;
     @JsonProperty("access-control") 
     public List<String> accessControl;

}

