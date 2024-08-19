/**
 *  Copyright 2000-2023, signotec GmbH, Ratingen, Germany, All Rights Reserved
 *  signotec GmbH
 *  Am Gierath 20b
 *  40885 Ratingen
 *  Tel: +49 (2102) 5 35 75-10
 *  Fax: +49 (2102) 5 35 75-39
 *  E-Mail: <info@signotec.de>
 * 
 * -----------------------------------------------------------------------------
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 * 
 *    * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the signotec GmbH nor the names of its contributors
 *      may be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 * 
 *  THIS SOFTWARE ONLY DEMONSTRATES HOW TO IMPLEMENT SIGNOTEC SOFTWARE COMPONENTS
 *  AND IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 *  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 *  OF THE POSSIBILITY OF SUCH DAMAGE.
 * -----------------------------------------------------------------------------
 */
package io.mosip.registration.ref.signotec.constant;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for resources of the signoPAD-API sample applications.
 */
@Component
public class SampleResources {

    public static final String RES_IMG_BTN_CANCEL = "btn_cancel.png";
    public static final String RES_IMG_BTN_CANCEL_BW = "btn_cancel_sigma.png";
    public static final String RES_IMG_BTN_CANCEL_PENDISPLAY = "btn_cancel_pendisplay.png";

    public static final String RES_IMG_BTN_OK = "btn_ok.png";
    public static final String RES_IMG_BTN_OK_BW = "btn_ok_sigma.png";
    public static final String RES_IMG_BTN_OK_PENDISPLAY = "btn_ok_pendisplay.png";

    public static final String RES_IMG_BTN_RETRY = "btn_retry.png";
    public static final String RES_IMG_BTN_RETRY_PENDISPLAY = "btn_retry_pendisplay.png";

    public static final String RES_IMG_BTN_SCROLL = "btn_scroll.png";

    public static final String RES_IMG_DEVICE_SIGMA = "device_sigma.png";
    public static final String RES_IMG_DEVICE_ZETA = "device_zeta.png";
    public static final String RES_IMG_DEVICE_OMEGA = "device_omega.png";
    public static final String RES_IMG_DEVICE_GAMMA = "device_gamma.png";
    public static final String RES_IMG_DEVICE_DELTA = "device_delta.png";
    public static final String RES_IMG_DEVICE_ALPHA = "device_alpha.png";

    public static final String RES_IMG_LED1_GREEN = "led1_green.png";
    public static final String RES_IMG_LED2_GREEN = "led2_green.png";
    public static final String RES_IMG_LED3_GREEN = "led3_green.png";

    public static final String RES_IMG_LED1_YELLOW = "led1_yellow.png";
    public static final String RES_IMG_LED2_YELLOW = "led2_yellow.png";
    public static final String RES_IMG_LED3_YELLOW = "led3_yellow.png";

    public static final String RES_IMG_LOGO_SIGMA = "logo_sigma.png";
    public static final String RES_IMG_LOGO_ZETA = "logo_zeta.png";
    public static final String RES_IMG_LOGO_OMEGA = "logo_omega.png";
    public static final String RES_IMG_LOGO_GAMMA = "logo_gamma.png";
    public static final String RES_IMG_LOGO_DELTA = "logo_delta.png";
    public static final String RES_IMG_LOGO_ALPHA = "logo_alpha.png";
    public static final String RES_IMG_LOGO_PENDISPLAY = "logo_pendisplay.png";

    public static final String RES_IMG_SIGNATURE_SIGMA = "signature_sigma.png";
    public static final String RES_IMG_SIGNATURE_ZETA = "signature_zeta.png";
    public static final String RES_IMG_SIGNATURE_OMEGA = "signature_omega.png";
    public static final String RES_IMG_SIGNATURE_GAMMA = "signature_gamma.png";
    public static final String RES_IMG_SIGNATURE_DELTA = "signature_delta.png";
    public static final String RES_IMG_SIGNATURE_PENDISPLAY = "signature_pendisplay.png";

    public static final String RES_IMG_WELCOME = "welcome.png";
    public static final String WINDOW_TITLE = "signotec signoPAD-API Sample Swing Application";
    public static final int WINDOW_WIDTH = 560;
    public static final int WINDOW_HEIGHT = 730; // height without the window title bar

    public static final int COMBO_INDEX_FACADE_STPAD_NATIVE = 0;
    public static final int COMBO_INDEX_FACADE_STPAD_PURE = 1;
    public static final int COMBO_INDEX_FACADE_PD_JPEN = 2;
    public static final int COMBO_INDEX_FACADE_PD_JWINPOINTER = 3;
    public static final int COMBO_INDEX_FACADE_DEFAULT = COMBO_INDEX_FACADE_STPAD_NATIVE;

    /** Capture the signature with the mouse and the pen if available. */
    public static final int COMBO_INDEX_PEN_TYPE_ALL = 0;
    /** Capture the signature with the pen if available. */
    public static final int COMBO_INDEX_PEN_TYPE_PEN = 1;
    /** Capture the signature with the mouse if available. */
    public static final int COMBO_INDEX_PEN_TYPE_MOUSE = 2;

    /** Font used to render the text on the pad display. */
    public static final String FONT_NAME_PAD = "Arial";

    /** The width of the pen shown on the Sigma pad. */
    public static final int PEN_WIDTH_SIGMA_PAD = 2;
    /** The width of the pen shown in the graphics control element for the Sigma pad. */
    public static final int PEN_WIDTH_SIGMA_CONTROL = 2;
    /** The width of the pen shown on the Sigma pad. */
    public static final int PEN_WIDTH_ZETA_PAD = 2;
    /** The width of the pen shown in the graphics control element for the Zeta pad. */
    public static final int PEN_WIDTH_ZETA_CONTROL = 2;
    /** The width of the pen shown on the Omega pad. */
    public static final int PEN_WIDTH_OMEGA_PAD = 3;
    /** The width of the pen shown in the graphics control element for the Omega pad. */
    public static final int PEN_WIDTH_OMEGA_CONTROL = 3;
    /** The width of the pen shown on the Gamma pad. */
    public static final int PEN_WIDTH_GAMMA_PAD = 3;
    /** The width of the pen shown in the graphics control element for the Gamma pad. */
    public static final int PEN_WIDTH_GAMMA_CONTROL = 3;
    /** The width of the pen shown on the Delta pad. */
    public static final int PEN_WIDTH_DELTA_PAD = 3;
    /** The width of the pen shown in the graphics control element for the Delta pad. */
    public static final int PEN_WIDTH_DELTA_CONTROL = 3;
    /** The width of the pen shown on the Alpha pad. */
    public static final int PEN_WIDTH_ALPHA_PAD = 1;
    /** The width of the pen shown in the graphics control element for the Alpha pad. */
    public static final int PEN_WIDTH_ALPHA_CONTROL = 1;
    /** The width of the pen shown in the graphics control element for the Pen Display device. */
    public static final int PEN_WIDTH_PEN_DISPLAY_CONTROL_MAX = 3;
    public static final int PEN_WIDTH_PEN_DISPLAY_CONTROL_MIN = 1;

    /** The scroll speed of the disclaimer text in pixel per second for the Omega pad. */
    public static final int SCROLL_SPEED_OMEGA = 300;
    /** The scroll speed of the disclaimer text in pixel per second for the Gamma pad. */
    public static final int SCROLL_SPEED_GAMMA = 300;
    /** The scroll speed of the disclaimer text in pixel per second for the Delta pad. */
    public static final int SCROLL_SPEED_DELTA = 500;


    /**
     * Load image file with the given name.
     * 
     * @param name
     *            The image resource name.
     * 
     * @return Returns the image or <code>null</code> if an error occurred.
     */
    public static BufferedImage loadImage(String name) {
        try (InputStream is = SampleResources.class.getResourceAsStream("/signaturepad/" + name)) {
            if (is != null) {
                return ImageIO.read(is);
            }

            return ImageIO.read(new File(name));
        } catch (IOException e) {
            Logger.getLogger(SampleResources.class.getName()).log(Level.SEVERE,
                    "error loading image: " + name, e);
        }

        return null;
    }

    /**
     * Load resource file with the given name.
     * 
     * @param name
     *            The resource name.
     * 
     * @return Returns the resource content or <code>null</code> if an error occurred.
     */
    public static InputStream loadResource(String name) {
        return SampleResources.class.getResourceAsStream(name);
    }

    private SampleResources() {
        // utility class
    }
}
