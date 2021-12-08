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
public class CopytoActionHandler extends ChangeActionHandler {

    @Override
    public String getActionClassName() {
        return "copyto";
    }

    @Override
    public void handle(Pane parentPane, String source, String[] args) {
        Scene scene = parentPane.getScene();

        for(int i=0; i<args.length; i++) {
            boolean copyEnabled = false;
            String[] parts = args[i].split("\\?|=");

            if(parts.length == 3) {
                Node flagNode = scene.lookup(HASH.concat(parts[0]));
                if(flagNode != null && flagNode instanceof CheckBox) {
                    copyEnabled = ((CheckBox) flagNode).isSelected();
                    if(!copyEnabled) { continue; }
                }
            }

            String sourceId = (parts.length == 3) ? parts[1] : ((parts.length == 2) ? parts[0] : null);
            String targetId = (parts.length == 3) ? parts[2] : ((parts.length == 2) ? parts[1] : null);

            if(sourceId != null && targetId != null) {

                Node fromNode = scene.lookup(HASH.concat(sourceId));
                Node toNode = scene.lookup(HASH.concat(targetId));

                if(!isValidNode(fromNode) || !isValidNode(toNode))
                    continue;

                if(fromNode instanceof TextField) {
                    copy(scene, (TextField) fromNode, (TextField) toNode);
                }
                else if(fromNode instanceof ComboBox) {
                    copy((ComboBox) fromNode, (ComboBox) toNode);
                }

                if(copyEnabled) { toNode.setDisable(true); }
            }
        }
    }

    private void copy(Scene scene, TextField fromNode, TextField toNode) {
        toNode.setText(fromNode.getText());
        Node localLangNode = scene.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
        if(isValidNode(localLangNode) && localLangNode instanceof TextField) {
            ((TextField)localLangNode).setText(fromNode.getText());
        }
    }

    private void copy(ComboBox fromNode, ComboBox toNode) {
        toNode.getSelectionModel().select(fromNode.getSelectionModel().getSelectedItem());
    }
}
