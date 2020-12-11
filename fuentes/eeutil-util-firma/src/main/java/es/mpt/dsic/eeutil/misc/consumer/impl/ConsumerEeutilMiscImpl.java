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

package es.mpt.dsic.eeutil.misc.consumer.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import es.mpt.dsic.eeutil.misc.consumer.model.ContenidoInfo;
import es.mpt.dsic.eeutil.misc.consumer.model.EeUtilService;
import es.mpt.dsic.eeutil.misc.consumer.model.EeUtilServiceImplService;
import es.mpt.dsic.eeutil.misc.consumer.model.InSideException;
import es.mpt.dsic.eeutil.misc.consumer.model.TCNInfo;
import es.mpt.dsic.inside.security.model.ApplicationLogin;
import es.mpt.dsic.inside.utils.file.FileUtil;

@Service
public class ConsumerEeutilMiscImpl {
	
	private static final String TCN_PDF_PREFIX = "tcnPdf";

	protected static final Log logger = LogFactory
			.getLog(ConsumerEeutilMiscImpl.class);

	private EeUtilService miscWs;

	private Properties properties;
	
	private String truststore;
	private String passTruststore;
	
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String getTruststore() {
		return truststore;
	}

	public void setTruststore(String truststore) {
		this.truststore = truststore;
	}

	public String getPassTruststore() {
		return passTruststore;
	}

	public void setPassTruststore(String passTruststore) {
		this.passTruststore = passTruststore;
	}

	public void configure() throws InSideException {
		if (miscWs == null) {
			URL urlMisc = null;
			String urlMiscString = null;
			try {
				urlMiscString = properties.getProperty("eeutil.misc.ws.url");
				logger.debug(String.format("El WS se encuentra en %s", urlMiscString));
				urlMisc = new URL(urlMiscString);
				
				System.setProperty("javax.net.ssl.trustStore", this.truststore);
				System.setProperty("javax.net.ssl.trustStorePassword", this.passTruststore);
				
				EeUtilServiceImplService ssMisc = new EeUtilServiceImplService(
						urlMisc);
				miscWs = ssMisc.getEeUtilServiceImplPort();
			} catch (MalformedURLException e) {
				logger.error(
						"No se puede crear la URL del servicio Eeutil Misc "
								+ urlMiscString, e);
				throw new InSideException(
						"No se puede crear la URL del servicio Eeutil Misc ",
						e);
			}
		}
	}

	public File convertirTCNAPdf(ApplicationLogin info, File tcnFile, String mime)
			throws InSideException, IOException {
		configure();
		es.mpt.dsic.eeutil.misc.consumer.model.ApplicationLogin credential = new es.mpt.dsic.eeutil.misc.consumer.model.ApplicationLogin();
		credential.setIdaplicacion(info.getIdApplicacion());
		credential.setPassword(info.getPassword());
		TCNInfo tcn = new TCNInfo();
		ContenidoInfo contenido = new ContenidoInfo();
		contenido.setContenido(FileUtils.readFileToByteArray(tcnFile));
		contenido.setTipoMIME(mime);
		tcn.setContenido(contenido);
		contenido =  miscWs.convertirTCNAPdf(credential, tcn);
		String filePath = FileUtil.createFilePath(TCN_PDF_PREFIX);
		FileUtils.writeByteArrayToFile(new File(filePath), contenido.getContenido());
		return new File(filePath);
	}

}
