package io.mosip.registration.util.common;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

public class RectangleSelection {

    final DragContext dragContext = new DragContext();
    Rectangle rect = null;

    Group group;

    public Bounds getBounds() {
        return rect.getBoundsInParent();
    }

    public RectangleSelection(Group group) {

        this.group = group;

        rect = new ResizableRectangle(0, 0, 0, 0, group);
        rect.setStroke(Color.BLUE);
        rect.setStrokeWidth(1);
        rect.setStrokeLineCap(StrokeLineCap.ROUND);
        rect.setFill(Color.LIGHTBLUE.deriveColor(0, 1.2, 1, 0.6));

        //If this is not set x, y co-ordinates go wrong

        javafx.scene.image.ImageView imageView = (javafx.scene.image.ImageView) this.group.getChildren().get(0);
        imageView.fitWidthProperty().bind(imageView.getImage().widthProperty());
        imageView.fitHeightProperty().bind(imageView.getImage().heightProperty());
        imageView.setPreserveRatio(true);

        imageView.addEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
        imageView.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
        imageView.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
    }

    public void removeEventHandlers() {
        javafx.scene.image.ImageView imageView = (javafx.scene.image.ImageView) this.group.getChildren().get(0);
        imageView.removeEventHandler(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
        imageView.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
        imageView.removeEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
    }

    EventHandler<MouseEvent> onMousePressedEventHandler = e -> {
        if (e.isSecondaryButtonDown()) {
            return;
        }

        // remove old rect
        rect.setX(0);
        rect.setY(0);
        rect.setWidth(0);
        rect.setHeight(0);

        group.getChildren().remove(rect);

        // prepare new drag operation
        dragContext.mouseAnchorX = e.getX();
        dragContext.mouseAnchorY = e.getY();

        rect.setX(dragContext.mouseAnchorX);
        rect.setY(dragContext.mouseAnchorY);
        rect.setWidth(0);
        rect.setHeight(0);

        group.getChildren().add(rect);
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = e -> {
        if (e.isSecondaryButtonDown()) {
            return;
        }

        double offsetX = e.getX() - dragContext.mouseAnchorX;
        double offsetY = e.getY() - dragContext.mouseAnchorY;

        if (offsetX > 0) {
            rect.setWidth(offsetX);
        } else {
            rect.setX(e.getX());
            rect.setWidth(dragContext.mouseAnchorX - rect.getX());
        }

        if (offsetY > 0) {
            rect.setHeight(offsetY);
        } else {
            rect.setY(e.getY());
            rect.setHeight(dragContext.mouseAnchorY - rect.getY());
        }
    };

    EventHandler<MouseEvent> onMouseReleasedEventHandler = e -> {

        if (e.isSecondaryButtonDown()) {
        }
    };

    private static final class DragContext {

        public double mouseAnchorX;
        public double mouseAnchorY;

    }

}
