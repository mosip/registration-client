package io.mosip.registration.util.common;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

@Component
public class DisableActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "disable";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        boolean disable = true;
        Scene scene = parentPane.getScene();
        
        Node copyEnabledFlagNode = scene.lookup(HASH.concat(source));
        if(isValidNode(copyEnabledFlagNode)) {
            disable = ((CheckBox)copyEnabledFlagNode).isSelected() ? true : false;
        }

        for(String arg : args) {
            Node node = scene.lookup(HASH.concat(arg));
            if(isValidNode(node)) {
                node.setDisable(disable);
            }
        }
    }
}
