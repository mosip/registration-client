package io.mosip.registration.api;

import io.mosip.registration.api.docscanner.DocScannerUtil;
import javafx.scene.image.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DocScannerUtilTest {

    @Test
    public void conversionTest() throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream("/images/stubdoc.png")) {
            byte[] stubImageBytes = in.readAllBytes();
            BufferedImage bufferedImage = DocScannerUtil.getBufferedImageFromBytes(stubImageBytes);
            Assert.assertNotNull(bufferedImage);

            byte[] imageBytes = DocScannerUtil.getImageBytesFromBufferedImage(bufferedImage);
            Assert.assertNotNull(imageBytes);

            Image image = DocScannerUtil.getImage(bufferedImage);
            Assert.assertNotNull(image);

            List<BufferedImage> bufferedImageList = new ArrayList<>();
            bufferedImageList.add(bufferedImage);
            bufferedImageList.add(bufferedImage);
            byte[] concatenatedImageBytes = DocScannerUtil.asImage(bufferedImageList);
            Assert.assertNotNull(concatenatedImageBytes);

            List<BufferedImage> bufferedImageList2 = new ArrayList<>();
            bufferedImageList2.add(bufferedImage);
            byte[] concatenatedImageBytes2 = DocScannerUtil.asImage(bufferedImageList2);
            Assert.assertNotNull(concatenatedImageBytes2);
        }
    }

    @Test
    public void pdfConversionTest() throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream("/images/stubdoc.png")) {
            BufferedImage bufferedImage = DocScannerUtil.getBufferedImageFromBytes(in.readAllBytes());

            List<BufferedImage> bufferedImageList = new ArrayList<>();
            bufferedImageList.add(bufferedImage);
            bufferedImageList.add(bufferedImage);

            byte[] pdfBytes = DocScannerUtil.asPDF(bufferedImageList, null);
            Assert.assertNotNull(pdfBytes);

            PDDocument pdDocument = PDDocument.load(pdfBytes);
            Assert.assertEquals(2, pdDocument.getNumberOfPages());

            List<BufferedImage> images = DocScannerUtil.pdfToImages(pdfBytes);
            Assert.assertEquals(2, images.size());
        }
    }
}
