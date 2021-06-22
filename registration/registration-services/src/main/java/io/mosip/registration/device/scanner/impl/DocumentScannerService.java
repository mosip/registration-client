package io.mosip.registration.device.scanner.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_DOC_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.device.scanner.IMosipDocumentScannerService;

/**
 * This class is used to handle all the requests related to scanner devices
 * through Sane Daemon service
 * 
 * @author balamurugan.ramamoorthy
 * @since 1.0.0
 */
@Service
public abstract class DocumentScannerService implements IMosipDocumentScannerService {

//	@Value("${DOCUMENT_SCANNER_DEPTH}")
//	protected int scannerDepth;
//
//	@Value("${DOCUMENT_SCANNER_HOST}")
//	protected String scannerhost;
//
//	@Value("${DOCUMENT_SCANNER_PORT}")
//	protected int scannerPort;
//
//	@Value("${DOCUMENT_SCANNER_TIMEOUT}")
//	protected long scannerTimeout;


	@Value("${mosip.registration.doc.jpg.compression:0.7f}")
	private float compressionQuality;

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScannerService.class);

	/**
	 * This method converts the BufferedImage to byte[]
	 * 
	 * @param bufferedImage
	 *            - holds the scanned image from the scanner
	 * @return byte[] - scanned document Content
	 * @throws IOException - holds the IOExcepion
	 */
	public byte[] getImageBytesFromBufferedImage(BufferedImage bufferedImage) throws IOException {
		byte[] imageInByte;

		ByteArrayOutputStream imagebyteArray = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, RegistrationConstants.SCANNER_IMG_TYPE, imagebyteArray);
		imagebyteArray.flush();
		imageInByte = imagebyteArray.toByteArray();
		imagebyteArray.close();

		return imageInByte;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.device.scanner.impl.DocumentScannerService#
	 * getSinglePDFInBytes(java.util.List)
	 */
	@Override
	public byte[] asPDF(List<BufferedImage> bufferedImages) {
		try (PDDocument pdDocument = new PDDocument();
			 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			for(BufferedImage bufferedImage : bufferedImages) {
				PDPage pdPage = new PDPage();
				byte[] image = getCompressedImage(bufferedImage);
				LOGGER.info("image size after compression : {}", image.length);
				PDImageXObject pdImageXObject = PDImageXObject.createFromByteArray(pdDocument, image, "");

				int w = pdImageXObject.getWidth();
				int h = pdImageXObject.getHeight();

				//PDImageXObject pdImageXObject = LosslessFactory.createFromImage(pdDocument, bufferedImage);
				try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage)) {
					float x_pos = pdPage.getCropBox().getWidth();
					float y_pos = pdPage.getCropBox().getHeight();

					float x_adjusted = ( x_pos - w ) / 2 + pdPage.getCropBox().getLowerLeftX();
					float y_adjusted = ( y_pos - h ) / 2 + pdPage.getCropBox().getLowerLeftY();
					contentStream.drawImage(pdImageXObject, x_adjusted, y_adjusted, w, h);
				}
				pdDocument.addPage(pdPage);
			}
			pdDocument.save(byteArrayOutputStream);
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			LOGGER.error(LOG_REG_DOC_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
		}
		return null;
	}

	private byte[] getCompressedImage(BufferedImage bufferedImage) throws IOException {
		ImageWriter imageWriter = null;
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			imageWriter = ImageIO.getImageWritersByFormatName("jpg").next();
			ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
			imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			imageWriteParam.setCompressionQuality(compressionQuality);
			imageWriter.setOutput(new MemoryCacheImageOutputStream(bos));
			imageWriter.write(bufferedImage);
			return bos.toByteArray();
		} finally {
			if(imageWriter != null)
				imageWriter.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.device.scanner.impl.DocumentScannerService#
	 * getSingleImageFromList(java.util.List)
	 */
	@Override
	public byte[] asImage(List<BufferedImage> bufferedImages) throws IOException {
		byte[] newSingleImage = null;
		if (isListNotEmpty(bufferedImages)) {

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

	@Override
	public List<BufferedImage> pdfToImages(byte[] pdfBytes) throws IOException {
		List<BufferedImage> bufferedImages = new ArrayList<>();
		try(PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));) {
			for(int i=0; i<document.getNumberOfPages(); i++) {
				Iterable<COSName> objectNames =  document.getPage(i).getResources().getXObjectNames();
				Iterator<COSName> itr = objectNames.iterator();
				while (itr.hasNext()) {
					PDXObject pdxObject = document.getPage(i).getResources().getXObject(itr.next());
					if(pdxObject instanceof PDImageXObject) {
						bufferedImages.add(((PDImageXObject)pdxObject).getImage());
					}
				}
			}
		}
		return bufferedImages;
	}

	/**
	 * converts bytes to BufferedImage
	 * 
	 * @param imageBytes
	 *            - scanned image file in bytes
	 * @return BufferedImage - image file in bufferedimage format
	 * @throws IOException
	 *             - holds the ioexception
	 */
	protected BufferedImage getBufferedImageFromBytes(byte[] imageBytes) throws IOException {

		return ImageIO.read(new ByteArrayInputStream(imageBytes));
	}

	protected boolean isListNotEmpty(List<?> values) {
		return values != null && !values.isEmpty();
	}
}
