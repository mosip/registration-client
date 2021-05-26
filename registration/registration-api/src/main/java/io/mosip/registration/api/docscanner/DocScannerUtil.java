package io.mosip.registration.api.docscanner;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DocScannerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocScannerUtil.class);
    public static final String SCANNER_IMG_TYPE = "jpg";

    public static byte[] getImageBytesFromBufferedImage(@NonNull final BufferedImage bufferedImage)
            throws IOException {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, SCANNER_IMG_TYPE, byteArrayOutputStream);
            byteArrayOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    /*public static byte[] asPDF(@NonNull final List<BufferedImage> bufferedImages) {
        Document document = new Document();
        PdfWriter writer = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            writer = PdfWriter.getInstance(document, byteArrayOutputStream);
            document.open();

            PdfContentByte pdfPage = new PdfContentByte(writer);
            for (BufferedImage bufferedImage : bufferedImages) {
                Image image = Image.getInstance(pdfPage, bufferedImage, 1);
                image.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                float x = (PageSize.A4.getWidth() - image.getScaledWidth()) / 2;
                float y = (PageSize.A4.getHeight() - image.getScaledHeight()) / 2;
                image.setAbsolutePosition(x, y);
                document.add(image);
                document.newPage();
            }

            return byteArrayOutputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            LOGGER.error("Failed to create PDF", exception);
        } finally {
            if(document != null) { document.close(); }
            if(writer != null) { writer.close(); }
        }
        return new byte[0];
    }*/

    public static byte[] asPDF(@NonNull final List<BufferedImage> bufferedImages) throws IOException {
        try (PDDocument pdDocument = new PDDocument();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            for(BufferedImage bufferedImage : bufferedImages) {
                PDPage pdPage = new PDPage();
                PDImageXObject pdImageXObject = LosslessFactory.createFromImage(pdDocument, bufferedImage);
                try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage)) {
                    contentStream.drawImage(pdImageXObject, 20, 20);
                }
                pdDocument.addPage(pdPage);
            }
            pdDocument.save(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static byte[] asImage(@NonNull final List<BufferedImage> bufferedImages) throws IOException {
        byte[] newSingleImage = null;
        if (!bufferedImages.isEmpty()) {

            if (bufferedImages.size() == 1) {
                return getImageBytesFromBufferedImage(bufferedImages.get(0));
            }
            int offset = 2;
            int width = offset;
            for (BufferedImage bufferedImage : bufferedImages) {
                width += bufferedImage.getWidth();
            }
            int height = Math.max(bufferedImages.get(0).getHeight(), bufferedImages.get(1).getHeight()) + offset;
            BufferedImage singleBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = singleBufferedImage.createGraphics();
            Color oldColor = g2.getColor();
            g2.setPaint(Color.BLACK);
            g2.fillRect(0, 0, width, height);
            g2.setColor(oldColor);
            for (int i = 0; i < bufferedImages.size(); i++) {
                g2.drawImage(bufferedImages.get(i), null, (i * bufferedImages.get(i).getWidth()) + offset, 0);
            }

            g2.dispose();
            newSingleImage = getImageBytesFromBufferedImage(singleBufferedImage);
        }
        return newSingleImage;
    }

    public static List<BufferedImage> pdfToImages(byte[] pdfBytes) throws IOException {
        List<BufferedImage> bufferedImages = new ArrayList<>();
        try(PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));) {
            for(int i=0; i<document.getNumberOfPages(); i++) {
                Iterable<COSName> objectNames =  document.getPage(i).getResources().getXObjectNames();
                Iterator<COSName> itr = objectNames.iterator();
                while (itr.hasNext()) {
                    PDXObject pdxObject = document.getPage(i).getResources().getXObject(itr.next());
                    if(pdxObject instanceof  PDImageXObject) {
                        bufferedImages.add(((PDImageXObject)pdxObject).getImage());
                    }
                }
            }
        }
        return bufferedImages;
    }


    public static Image getImage(BufferedImage bufferedImage) {
        WritableImage wr = null;
        if (bufferedImage != null) {
            wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                }
            }
        }
        return wr;
    }
}
