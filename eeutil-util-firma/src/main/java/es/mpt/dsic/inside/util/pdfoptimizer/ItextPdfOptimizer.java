/* Copyright (C) 2012-13 MINHAP, Gobierno de España
   This program is licensed and may be used, modified and redistributed under the terms
   of the European Public License (EUPL), either version 1.1 or (at your
   option) any later version as soon as they are approved by the European Commission.
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
   or implied. See the License for the specific language governing permissions and
   more details.
   You should have received a copy of the EUPL1.1 license
   along with this program; if not, you may find it at
   http://joinup.ec.europa.eu/software/page/eupl/licence-eupl */

package es.mpt.dsic.inside.util.pdfoptimizer;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfImageObject;

import es.mpt.dsic.inside.pdf.PdfUtils;
import es.mpt.dsic.inside.utils.file.FileUtil;

@Component
public class ItextPdfOptimizer {
	
	protected final static Log logger = LogFactory.getLog(ItextPdfOptimizer.class);

	
	public File makeSmaller(byte[] input) throws DocumentException, IOException {
		String inputFile = FileUtil.createFilePath("fileInput", input);
		File retorno = makeSmaller(new File(inputFile));	
		
		FileUtils.forceDelete(new File(inputFile));
		return retorno;
	}
	
	public File makeSmaller(File input) throws IOException, DocumentException {
		FileInputStream fin = null;
		//necesario para tener un aux para hacer el recorrido readerAux.getPageContent(i + 1)
		FileInputStream finAux = null;
		FileOutputStream fos = null;
		PdfStamper stamper = null;
		
		PdfReader reader = null;
		PdfReader readerAux = null;
		
		String outputFile = FileUtil.createFilePath("pdfSmaller") + ".pdf";
		try {
			fin = new FileInputStream(input);
			finAux = new FileInputStream(input);
			reader = new PdfReader(fin);
			readerAux = new PdfReader(finAux);

			fos = new FileOutputStream(outputFile);
			stamper = new PdfStamper(reader, fos);
			stamper.setFullCompression();

			//reader.removeFields();
			//reader.removeUnusedObjects();

			int total = readerAux.getNumberOfPages();
			for (int i = 0; i < total; i++) {
				reader.setPageContent(i + 1, readerAux.getPageContent(i + 1));
			}
		} finally {
			if (stamper != null) {
				stamper.close();
			}
			if (fin != null) {
				fin.close();
			}
			if (finAux != null) {
				finAux.close();
			}
			if (fos != null) {
				fos.close();
			}
			if (reader != null) {
				PdfUtils.close(reader);
			}
			if (readerAux != null) {
				PdfUtils.close(readerAux);
			}
		}
		return new File(outputFile);
	}
	
	public void manipulatePdf(String src, String dest, float factor) throws DocumentException, IOException {
		FileInputStream fin = null;
		PdfReader readerMetadata = null;
		PdfReader reader = null;
		
		// Save altered PDF
		PdfStamper stamper = null;
		try {
			fin = new FileInputStream(src);
			readerMetadata = new PdfReader(fin);
			readerMetadata.getCatalog().remove(PdfName.METADATA);
			readerMetadata.getCatalog().remove(PdfName.PROPERTIES);
			readerMetadata.removeUnusedObjects();
			
			reader = new PdfReader(src);
			int n = reader.getXrefSize();
			PdfObject object;
			PRStream stream;
			// Look for image and manipulate image stream
			for (int i = 0; i < n; i++) {
			    object = reader.getPdfObject(i);
			    if (object == null || !object.isStream())
			        continue;
			    stream = (PRStream)object;
			    logger.debug("#################### stream ####################");
			    logger.debug(stream.getAsName(PdfName.TYPE));
			    logger.debug(stream.getAsName(PdfName.SUBTYPE));
			    logger.debug(stream.getAsName(PdfName.FILTER));
			    logger.debug(stream.getLength());
			    logger.debug(stream.getRawLength());
			    logger.debug(stream.getBytes());
			    if (!PdfName.FORM.equals(stream.getAsName(PdfName.SUBTYPE))
			    	&& !PdfName.IMAGE.equals(stream.getAsName(PdfName.SUBTYPE))) {
			    	//FileUtil.createFilePath("longFile", PdfReader.getStreamBytes(stream));
			    	logger.debug("################ steam before ##############");
			    	byte[] data = PdfReader.getStreamBytes(stream);
			    	logger.debug(data.length);
			    	
			    	
			    	stream.flateCompress(PRStream.BEST_COMPRESSION);
			    	data = PdfReader.getStreamBytes(stream);
			    	
			    	logger.debug("################ steam after ##############");
			    	logger.debug(data.length);
			    	
			    	
			    	stream.clear();
			    	stream.setData(data, true, PRStream.BEST_COMPRESSION);
			    }
			    
			    if (!PdfName.IMAGE.equals(stream.getAsName(PdfName.SUBTYPE)))
			        continue;
			    if (!PdfName.DCTDECODE.equals(stream.getAsName(PdfName.FILTER))
			    	&& !PdfName.FLATEDECODE.equals(stream.getAsName(PdfName.FILTER)))
			        continue;
			    PdfImageObject image = new PdfImageObject(stream);
			    BufferedImage bi = image.getBufferedImage();
			    if (bi == null)
			        continue;
			    int width = (int)(bi.getWidth() * factor);
			    int height = (int)(bi.getHeight() * factor);
			    if (width <= 0 || height <= 0)
			        continue;
			    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			    AffineTransform at = AffineTransform.getScaleInstance(factor, factor);
			    Graphics2D g = img.createGraphics();
			    g.drawRenderedImage(bi, at);
			    ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
			    ImageIO.write(img, "JPG", imgBytes);
			    stream.clear();
			    stream.setData(imgBytes.toByteArray(), false, PRStream.NO_COMPRESSION);
			    stream.put(PdfName.TYPE, PdfName.XOBJECT);
			    stream.put(PdfName.SUBTYPE, PdfName.IMAGE);
			    stream.put(PdfName.FILTER, PdfName.DCTDECODE);
			    stream.put(PdfName.WIDTH, new PdfNumber(width));
			    stream.put(PdfName.HEIGHT, new PdfNumber(height));
			    stream.put(PdfName.BITSPERCOMPONENT, new PdfNumber(8));
			    stream.put(PdfName.COLORSPACE, PdfName.DEVICERGB);
			}
			reader.removeUnusedObjects();
			
			stamper = new PdfStamper(reader, new FileOutputStream(dest));
			stamper.setFullCompression();
		} finally {
			fin.close();
			readerMetadata.close();
			stamper.close();
	        reader.close();
		}
    }
	
	
}
