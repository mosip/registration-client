package io.mosip.registration.util.common;

import io.mosip.registration.constants.RegistrationConstants;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;

@Component
public class NonEditableCopyActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "copy&disable";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        Scene scene = parentPane.getScene();
        Node copyEnabledFlagNode = scene.lookup(HASH.concat(source));
        boolean enabled = ((CheckBox)copyEnabledFlagNode).isSelected() ? true : false;

        for(String arg : args) {
            String[] parts = arg.split("=");
            if(parts.length == 2) {
                Node fromNode = scene.lookup(HASH.concat(parts[0]));
                Node toNode = scene.lookup(HASH.concat(parts[1]));

                if(!isValidNode(fromNode) || !isValidNode(toNode))
                    continue;

                if(fromNode instanceof TextField) {
                    copy(scene, (TextField) fromNode, (TextField) toNode, enabled);
                }
                else if(fromNode instanceof ComboBox) {
                    copy((ComboBox) fromNode, (ComboBox) toNode, enabled);
                }
            }
        }
    }

    private void copy(Scene scene, TextField fromNode, TextField toNode, boolean isEnabled) {
        if(isEnabled) {
            toNode.setText(fromNode.getText());
            toNode.setDisable(true);
            Node localLangNode = scene.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
            if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
                ((TextField)localLangNode).setText(fromNode.getText());
                localLangNode.setDisable(true);
            }
        }
        else {
            toNode.setDisable(false);
            Node localLangNode = scene.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
            if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
                localLangNode.setDisable(false);
            }
        }
    }

    private void copy(ComboBox fromNode, ComboBox toNode, boolean isEnabled) {
        if(isEnabled) {
            toNode.getSelectionModel().select(fromNode.getSelectionModel().getSelectedItem());
            toNode.setDisable(true);
        }
        else {
            toNode.setDisable(false);
        }
    }
}
