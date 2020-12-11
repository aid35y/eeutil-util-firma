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

package es.mpt.dsic.inside.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.MimeHelper;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.AOSignerFactory;
import es.mpt.dsic.inside.utils.mime.MimeUtil;
import es.mpt.dsic.inside.utils.xml.XMLUtil;

public class SignedDataExtractor {

	
	/**
	 * Obtiene un objeto SignData a partir de una firma.
	 * @param sign firma.
	 * @return un objeto que contiene el documento que se ha firmado y el mime de ese documento.
	 * @throws Exception cuando no se puedan obtener los datod e la firma.
	 */
	public static SignedData getDataFromSign (byte[] sign) throws Exception{
		
		byte[] datosFirma = null;
		
		try {
			datosFirma = getDataSignedFromSignXML (sign);
			
			
		} catch (Exception e) {
			AOSigner signer = obtenerSigner(sign);			
			datosFirma = getDataSignedFromSignerGenerico(signer, sign);
			
		}
		
		//String mime = MimeUtil.getMimeNotNull(datosFirma);
				
		//forma alternativa de calcular mimetipe. otra alternativa seria desdoblar este metodo para cuando venimos por el proceso de generarcsv
		//porque no lo necesita para nada y evitamos el error de ava.util.ConcurrentModificationException
		MimeHelper helperMime = new MimeHelper(datosFirma);
		String mime = helperMime.getMimeType();		
		
		SignedData signData = new SignedData(datosFirma, mime);
		
		return signData;
		
	}
	
	
	/**
	 * Obtiene los datos firmados de una firma XML implícita.
	 * @param sign bytes de la firma
	 * @return bytes de los datos firmados de una firma XML
	 * @throws Exception si no se pueden obtener los datos firmados.
	 */
	private static byte[] getDataSignedFromSignXML(byte[] sign) throws Exception{
		byte[] datosFirma = null;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {			
			throw new Exception ("No se puede parsear la firma " + e.getMessage(), e);
		}
		Document dom = null;
		try {
			dom = db.parse(new ByteArrayInputStream(sign));
		} catch (SAXException e) {
			throw new Exception ("No se puede parsear la firma " + e.getMessage(), e);			
		} catch (IOException e) {
			throw new Exception ("No se puede parsear la firma " + e.getMessage(), e);			
		}
		
		datosFirma = getContent(dom);		
		return datosFirma;
	}
	
	/**
	 * Obtiene el contenido del nodo hijo del nodo padre del documento. Este nodo debe contener
	 * los atributos "id" y "encoding".
	 * @param dom Árbol XML del documento
	 * @return el contenido del nodo hijo del nodo padre del documento.
	 * @throws Exception si no se encuentra el nodo que cumpla estas condiciones.
	 */
	private static byte[] getContent (Document dom) throws Exception{
		Element elementRoot = dom.getDocumentElement();
		
		Node child = elementRoot.getFirstChild();
		boolean lastChild = false;
		boolean encontrado = false;
		while (!lastChild && !encontrado) {
			
			encontrado = XMLUtil.contieneIdEncoding(child);
			
			if (child == elementRoot.getLastChild()) {
				lastChild = true;
			} else if (!encontrado) {
				child = child.getNextSibling();
			}
		}
		
		String b64Content = null;
		byte[] document = null;
		
		if (encontrado) {
			b64Content = child.getFirstChild().getNodeValue();
			document = Base64.decodeBase64(b64Content);
		} else {
			throw new Exception ("No se encuentra el documento Implícito en la firma");
		}
		
		return document;
		
	}
	
	/**
	 * Se obtiene un objeto para manipular la firma.
	 * @param bytes de la firma
	 * @return Instancia de un objeto para manipular la firma.
	 * @throws IOException 
	 */
	private static AOSigner obtenerSigner (byte[] bytes) throws IOException {
		AOSigner signer = null;		
		signer = AOSignerFactory.getSigner(bytes);			
		return signer;
	}
	
	
	private static byte[] getDataSignedFromSignerGenerico (AOSigner signer, byte[] bytesFirma) throws Exception {
		byte[] bytesDatosFirmados = null;
		try  {
			if (signer == null) {
				throw new Exception ("No se puede obtener el documento Implícito en la firma");
			}
			
			bytesDatosFirmados = signer.getData(bytesFirma);
		} catch (AOException e) {
			throw new Exception ("No se puede obtener el documento Implícito en la firma");
		}
		
		return bytesDatosFirmados;
	}
	
}
