
package registrationtest.utility;

import java.util.Optional;

import io.mosip.registration.dto.mastersync.GenericDto;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;

public class ComboBoxUtil {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(ComboBoxUtil.class);
    static WaitsUtil waitsUtil = new WaitsUtil();

    public void user_selects_combo_item2(String comboBoxId, String val) {
        try {
            Platform.runLater(new Runnable() {
                public void run() {

                    ComboBox comboBox = waitsUtil.lookupById(comboBoxId);

                    // comboBox.getSelectionModel().select(dto);
                    Optional<GenericDto> op = comboBox.getItems().stream()
                            .filter(i -> ((GenericDto) i).getName().equalsIgnoreCase(val)).findFirst();
                    if (op.isEmpty())
                        comboBox.getSelectionModel().selectFirst();
                    else
                        comboBox.getSelectionModel().select(op.get());

                    try {
                        Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
                    } catch (InterruptedException e) {
                        logger.error("", e);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }
            });
        } catch (Exception e) {

            logger.error("", e);
        }

    }

    public static void user_selects_combo_item1(String comboBoxId, String val) {
        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        ComboBox comboBox = waitsUtil.lookupById(comboBoxId);

                        Optional<GenericDto> op = comboBox.getItems().stream()
                                .filter(i -> ((GenericDto) i).getName().equalsIgnoreCase(val)).findFirst();
                        if (op.isEmpty())
                            comboBox.getSelectionModel().selectFirst();
                        else
                            comboBox.getSelectionModel().select(op.get());
                    }
                });
            }
        });
        taskThread.start();
        try {
            taskThread.join();
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }

    }
}
