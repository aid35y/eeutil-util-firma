/* Copyright (C) 2012-13 MINHAP, Gobierno de Espa√±a
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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.pdftools.NativeLibrary;
import com.pdftools.pola.PdfOptimize;

import es.mpt.dsic.inside.pdf.exception.PdfConversionException;
import es.mpt.dsic.inside.utils.file.FileUtil;

@Component
public class PdfOptimizerPdfTools {
	
	protected final static Log logger = LogFactory.getLog(PdfOptimizerPdfTools.class);
	
	public File optimizePdf(String inputPathFile, int optimizationLevel) throws PdfConversionException {
		PdfOptimize doc = null;
		try {
        	if (!PdfOptimize.getLicenseIsValid()) {
    			throw new PdfConversionException("Licencia no v·lida optimizador pdf");
    		}

            doc = new PdfOptimize();
            
            // Open input file
            if (!doc.open(inputPathFile, "")) {
    			throw new PdfConversionException("No se ha podido optimizar la salida");
            }

            // Choose the optimization profile for the web
            doc.setProfile(optimizationLevel);

            // Disable linearizetion
            doc.setLinearize(false);

            String outputPathFile = FileUtil.createFilePath("pdftools-Optimize") + ".pdf";
            // Save output file
            if (!doc.saveAs(outputPathFile, "", "", NativeLibrary.PERMISSION.ePermNoEncryption)) {
    			throw new PdfConversionException("No se ha podido optimizar la salida");
            }
            return new File(outputPathFile);
        } finally {
            // Release the optimizer
        	if (doc != null) {
        		doc.close();
        		doc.destroyObject();
        	}
        }
	}
	
}
