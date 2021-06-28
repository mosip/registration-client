package io.mosip.registration.device.scanner.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_DOC_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.*;
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
				Dimension scaledDimension = getScaledDimension(new Dimension(pdImageXObject.getWidth(), pdImageXObject.getHeight()),
						new Dimension((int)pdPage.getMediaBox().getWidth(), (int)pdPage.getMediaBox().getHeight()));
				//PDImageXObject pdImageXObject = LosslessFactory.createFromImage(pdDocument, bufferedImage);
				try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage)) {
					float startx = (pdPage.getMediaBox().getWidth() - scaledDimension.width)/2;
					float starty = (pdPage.getMediaBox().getHeight() - scaledDimension.height)/2;
					contentStream.drawImage(pdImageXObject, startx, starty, scaledDimension.width, scaledDimension.height);
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

	private Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
		int original_width = imgSize.width;
		int original_height = imgSize.height;
		int bound_width = boundary.width;
		int bound_height = boundary.height;
		int new_width = original_width;
		int new_height = original_height;

		// first check if we need to scale width
		if (original_width > bound_width) {
			//scale width to fit
			new_width = bound_width;
			//scale height to maintain aspect ratio
			new_height = (new_width * original_height) / original_width;
		}

		// then check if we need to scale even with the new height
		if (new_height > bound_height) {
			//scale height to fit instead
			new_height = bound_height;
			//scale width to maintain aspect ratio
			new_width = (new_height * original_width) / original_height;
		}

		return new Dimension(new_width, new_height);
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
