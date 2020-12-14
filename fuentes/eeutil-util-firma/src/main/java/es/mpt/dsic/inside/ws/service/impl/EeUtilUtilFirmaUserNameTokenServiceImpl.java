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

package es.mpt.dsic.inside.ws.service.impl;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.WebServiceContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.mpt.dsic.inside.copiaautentica.ImageProperties;
import es.mpt.dsic.inside.copiaautentica.PdfCopiaAutenticaUtils;
import es.mpt.dsic.inside.copiaautentica.PdfCopiaNormalizadaUtils;
import es.mpt.dsic.inside.csv.CSVUtil;
import es.mpt.dsic.inside.csv.ext.CVSSignature;
import es.mpt.dsic.inside.csv.ext.SignatureProcessor;
import es.mpt.dsic.inside.exception.AfirmaException;
import es.mpt.dsic.inside.model.FirmaInfoAfirma;
import es.mpt.dsic.inside.model.InformacionFirmaAfirma;
import es.mpt.dsic.inside.pdf.NewPdfConversion;
import es.mpt.dsic.inside.pdf.converter.PdfConverter;
import es.mpt.dsic.inside.security.context.AplicacionContext;
import es.mpt.dsic.inside.security.wss4j.CredentialUtil;
import es.mpt.dsic.inside.services.AfirmaService;
import es.mpt.dsic.inside.util.SignedData;
import es.mpt.dsic.inside.util.SignedDataExtractor;
import es.mpt.dsic.inside.utils.file.FdUtils;
import es.mpt.dsic.inside.utils.file.FileUtil;
import es.mpt.dsic.inside.utils.io.IOUtil;
import es.mpt.dsic.inside.utils.mime.MimeUtil;
import es.mpt.dsic.inside.ws.service.EeUtilUtilFirmaUserNameTokenService;
import es.mpt.dsic.inside.ws.service.exception.InSideException;
import es.mpt.dsic.inside.ws.service.model.CSVInfo;
import es.mpt.dsic.inside.ws.service.model.CSVInfoAmbito;
import es.mpt.dsic.inside.ws.service.model.ContenidoInfo;
import es.mpt.dsic.inside.ws.service.model.CopiaInfo;
import es.mpt.dsic.inside.ws.service.model.CopiaInfoExtended;
import es.mpt.dsic.inside.ws.service.model.CopiaInfoFirma;
import es.mpt.dsic.inside.ws.service.model.CopiaInfoFirmaSalida;
import es.mpt.dsic.inside.ws.service.model.EstadoInfo;
import es.mpt.dsic.inside.ws.service.model.FirmaInfo;
import es.mpt.dsic.inside.ws.service.model.ListaFirmaInfo;

@Service("eeUtilUtilFirmaUserNameTokenService")
@WebService(endpointInterface = "es.mpt.dsic.inside.ws.service.EeUtilUtilFirmaUserNameTokenService")
@SOAPBinding(style = Style.RPC, parameterStyle = ParameterStyle.BARE, use = Use.LITERAL)
public class EeUtilUtilFirmaUserNameTokenServiceImpl implements
		EeUtilUtilFirmaUserNameTokenService {

	protected final static Log logger = LogFactory
			.getLog(EeUtilUtilFirmaUserNameTokenServiceImpl.class);

	@Autowired(required = false)
	private AplicacionContext aplicacionContext;

	@Autowired
	private AfirmaService afirmaService;

	@Autowired
	private NewPdfConversion newPdfConversion;

	@Autowired
	private PdfCopiaAutenticaUtils pdfCopiaAutenticaUtils;

	@Autowired
	private PdfCopiaNormalizadaUtils pdfCopiaNormalizadaUtils;

	@Autowired
	SignatureProcessor processor;

	@Autowired
	PdfConverter pdfConverter;

	@Resource
	private WebServiceContext wsContext;

	@Autowired
	CredentialUtil credentialUtil;
	
	
	
    
    /*
     * constantes para calcular el hash
     */
    private String ALGORITMO_SHA256 = "SHA256";
    private String ALGORITMO_SHA384 = "SHA384";
    private String ALGORITMO_SHA512 = "SHA512";

    private int LONGITUD_HASHHEX_SHA256 = 64;
    private int LONGITUD_HASHHEX_SHA384 = 96;
    private int LONGITUD_HASHHEX_SHA512 = 128;
    
    

	@Secured("ROLE_TRAMITAR")
	public CopiaInfo comprobarAplicacion(CopiaInfo copia)
			throws InSideException {
		logger.warn("Descriptores abiertos inicio comprobarAplicacion: "
				+ FdUtils.getFdOpen());
		CopiaInfo retorno = generarCopia(copia, false);
		logger.warn("Descriptores abiertos fin comprobarAplicacion: "
				+ FdUtils.getFdOpen());
		return retorno;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.mpt.dsic.inside.ws.service.EeUtilService#generarCopia(es.mpt.dsic.
	 * inside.security.model.ApplicationLogin,
	 * es.mpt.dsic.inside.ws.service.model.CopiaInfo, boolean)
	 * 
	 * Genera el justificante de firma a partir de la firma, CSV pero sin la
	 * lista de firmantes. Los firmantes no se calculan.
	 */
	@Override
	@Secured("ROLE_TRAMITAR")
	public CopiaInfo generarCopia(CopiaInfo copia, boolean simple)
			throws InSideException {
		logger.warn("Descriptores abiertos inicio generarCopia: "
				+ FdUtils.getFdOpen());
		CopiaInfo retorno = generarCopiaFirmaComun(copia, null, simple);
		logger.warn("Descriptores abiertos fin generarCopia: "
				+ FdUtils.getFdOpen());
		return retorno;
	}

	@Override
	public String generarCSV(CSVInfo copia) throws InSideException {
		logger.debug("Generar CSV");
		logger.warn("Descriptores abiertos inicio generarCSV: "
				+ FdUtils.getFdOpen());
		byte[] contenido = copia.getContenido();
		byte[] documentoFirmado = copia.getContenidoFirmado();
		String cvsText;

		try {
			cvsText = obtenerCSV(contenido, documentoFirmado);

			if (cvsText == null) {
				EstadoInfo estadoInfo = new EstadoInfo();
				throw new InSideException("EL CSV OBTENIDO ES NULO", estadoInfo);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			logger.error("Error obteniendo csv: ", t);
			EstadoInfo estadoInfo = new EstadoInfo();
			throw new InSideException("No se puede obtener el csv de la firma "
					+ t.getMessage(), estadoInfo, t.getCause());
		}
		logger.warn("Descriptores abiertos fin generarCSV: "
				+ FdUtils.getFdOpen());
		return cvsText;
	}

	@Override
	public String generarCSVAmbito(CSVInfoAmbito copia) throws InSideException {
		logger.warn("Descriptores abiertos inicio generarCSVAmbito: "
				+ FdUtils.getFdOpen());
		final byte[] contenido = copia.getContenido();
		final byte[] documentoFirmado = copia.getContenidoFirmado();
		final String cvsAmbito = copia.getAmbito();
		final String cvsText;

		try {
			cvsText = obtenerCSV(contenido, documentoFirmado);

			if (cvsText == null) {
				EstadoInfo estadoInfo = new EstadoInfo();
				throw new InSideException("EL CSV OBTENIDO ES NULO", estadoInfo);
			}
		} catch (Throwable t) {
			logger.error("Error obteniendo csv: ", t);
			EstadoInfo estadoInfo = new EstadoInfo();
			throw new InSideException("No se puede obtener el csv de la firma "
					+ t.getMessage(), estadoInfo, t.getCause());
		}
		logger.warn("Descriptores abiertos fin generarCSVAmbito: "
				+ FdUtils.getFdOpen());
		return StringUtils.isNotBlank(cvsAmbito) ? cvsAmbito
				+ CSVUtil.CSV_SEPARATOR + cvsText : cvsText;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.mpt.dsic.inside.ws.service.EeUtilService#generarCopiaFirma(es.mpt.
	 * dsic.inside.security.model.ApplicationLogin,
	 * es.mpt.dsic.inside.ws.service.model.CopiaInfo,
	 * es.mpt.dsic.inside.ws.service.model.ListaFirmaInfo, boolean)
	 * 
	 * Genera el justificante de firma a partir de la firma, CSV y lista de
	 * firmantes
	 */
	@Override
	public CopiaInfo generarCopiaFirma(CopiaInfo copia, ListaFirmaInfo firmas,
			boolean simple) throws InSideException {
		logger.debug("generarCopiaFirma");

		logger.warn("Descriptores abiertos inicio generarCopiaFirma: "
				+ FdUtils.getFdOpen());

		CopiaInfo retorno = generarCopiaFirmaComun(copia, firmas, simple);

		logger.warn("Descriptores abiertos fin generarCopiaFirma: "
				+ FdUtils.getFdOpen());

		return retorno;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.mpt.dsic.inside.ws.service.EeUtilService#generarCopiaFirmaNormalizada
	 * (es.mpt.dsic.inside.security.model.ApplicationLogin,
	 * es.mpt.dsic.inside.ws.service.model.CopiaInfo,
	 * es.mpt.dsic.inside.ws.service.model.ListaFirmaInfo, boolean)
	 * 
	 * Genera el justificante de firma en formato normalizado a partir de la
	 * firma, CSV y lista de firmantes
	 */
	@Override
	public CopiaInfo generarCopiaFirmaNormalizada(CopiaInfo copia,
			ListaFirmaInfo firmas, boolean simple) throws InSideException {
		File entrada = null;
		File pdf = null;
		File pdfResult = null;
		try {
			logger.debug("generarCopiaFirma");

			logger.warn("Descriptores abiertos inicio generarCopiaFirmaNormalizada: "
					+ FdUtils.getFdOpen());

			if (StringUtils.isEmpty(copia.getIdAplicacion())
					&& !CSVUtil.hasScope(copia.getIdAplicacion())) {
				throw new InSideException("No se ha especificado el �mbito",
						null);
			}

			String rutaLogo = aplicacionContext.getAplicacionInfo()
					.getPropiedades().get("rutaLogo");

			String inputPathFile = FileUtil.createFilePath(
					"copiaFirmaNormalizada", copia.getContenido()
							.getContenido());

			entrada = new File(inputPathFile);

			// Se convierte a PDF
			pdf = convertir(entrada, copia.getContenido().getTipoMIME());

			// Se calcula el número de páginas en blanco a meter al final, si
			// hay muchos firmantes.
			int pagsBlancas = pdfCopiaNormalizadaUtils.numeroPaginasEnBlanco(
					firmas, simple);

			if (pagsBlancas > 0) {
				// Se meten las páginas en blanco
				pdf = pdfCopiaNormalizadaUtils.insertarPaginasEnBlanco(pdf,
						pagsBlancas);
			}
			// Se estampa la información
			pdfResult = pdfCopiaNormalizadaUtils.copiaAutenticaNormalizada(pdf,
					copia, rutaLogo, simple, firmas, pagsBlancas);

			copia.getContenido().setContenido(
					IOUtil.getBytesFromObject(pdfResult));
			copia.getContenido().setTipoMIME("application/pdf");

			logger.warn("Descriptores abiertos fin generarCopiaFirmaNormalizada: "
					+ FdUtils.getFdOpen());

		} catch (IOException e) {
			EstadoInfo estadoInfo = new EstadoInfo();
			logger.error(e.getMessage(), e);
			throw new InSideException(e.getMessage(), estadoInfo, e.getCause());
		} catch (Throwable t) {
			logger.error("Error generando copia autentica", t);
			EstadoInfo estadoInfo = new EstadoInfo();
			throw new InSideException(t.getMessage(), estadoInfo, t.getCause());
		} finally {
			try {
				if (entrada != null) {
					FileUtils.forceDelete(entrada);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ entrada.getAbsolutePath());
			}
			try {
				if (pdf != null) {
					FileUtils.forceDelete(pdf);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ pdf.getAbsolutePath());
			}
			try {
				if (pdfResult != null) {
					FileUtils.forceDelete(pdfResult);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ pdfResult.getAbsolutePath());
			}
		}

		return copia;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.mpt.dsic.inside.ws.service.EeUtilService#generarJustificanteFirma(
	 * es.mpt.dsic.inside.security.model.ApplicationLogin,
	 * es.mpt.dsic.inside.ws.service.model.CopiaInfoFirma, boolean)
	 * 
	 * Genera el justificante de firma a partir del documento original y la
	 * firma. El CSV y la lista de firmantes se calcula dinamicamente.
	 */
	@Override
	public CopiaInfoFirmaSalida generarJustificanteFirma(
			CopiaInfoFirma copiaInfoFirma, boolean simple)
			throws InSideException {

		byte[] bytesDocumento = null;
		String mimeDocumento = null;
		CopiaInfoFirmaSalida salida = new CopiaInfoFirmaSalida();

		try {
			logger.warn("Descriptores abiertos inicio generarJustificanteFirma: "
					+ FdUtils.getFdOpen());

			// Obtención del documento firmado y del mime de éste.
			if (copiaInfoFirma.isDocumentoEnFirma()) {
				logger.debug("El documento SI está en la firma");
				SignedData signedData = SignedDataExtractor
						.getDataFromSign(copiaInfoFirma.getFirma());
				bytesDocumento = signedData.getContenido();
				mimeDocumento = signedData.getTipoMime();
			} else {
				logger.debug("El documento NO está en la firma");

				bytesDocumento = copiaInfoFirma.getDocumento();
				mimeDocumento = MimeUtil.getMimeNotNull(bytesDocumento);

			}

			String csv = "";
			String tituloCSV = "EMPTY";
			if (copiaInfoFirma.isIncluirCSV()) {
				csv = obtenerCSV(copiaInfoFirma.getFirma(),
						copiaInfoFirma.getDocumento());
				tituloCSV = copiaInfoFirma.getTituloCSV();
				if (csv == null) {
					EstadoInfo estadoInfo = new EstadoInfo();
					throw new InSideException(
							"No se puede generar el justificante de la firma porque no se ha podido obtener el CSV",
							estadoInfo);
				}
			}

			if (copiaInfoFirma.isIncluirCSVEnRespuesta() != null
					&& copiaInfoFirma.isIncluirCSVEnRespuesta()) {
				salida.setCsv(csv);
			}

			ListaFirmaInfo firmantes = null;
			if (copiaInfoFirma.isIncluirFirmantes()) {
				firmantes = obtenerFirmantes(copiaInfoFirma.getFirma());
				if (!copiaInfoFirma.isIncluirNifFirmantes()) {
					ponerNifsVacios(firmantes);
				}
			}

			CopiaInfo copiaInfo = new CopiaInfo();
			copiaInfo.setIdAplicacion(copiaInfoFirma.getIdAplicacion());
			copiaInfo.setLateral(copiaInfoFirma.getLateral());
			copiaInfo.setEstamparLogo(copiaInfoFirma.isEstamparLogo());
			copiaInfo.setCsv(csv);
			copiaInfo.setExpediente(copiaInfoFirma.getExpediente());
			copiaInfo.setFecha(copiaInfoFirma.getFecha());
			copiaInfo.setNif(copiaInfoFirma.getNif());
			if (copiaInfoFirma.getUrlSede() != null) {
				copiaInfo.setUrlSede(copiaInfoFirma.getUrlSede());
			} else {
				copiaInfo
						.setUrlSede("https://sede.administracionespublicas.gob.es/valida");
			}
			ContenidoInfo cInfo = new ContenidoInfo();
			cInfo.setContenido(bytesDocumento);
			cInfo.setTipoMIME(mimeDocumento);
			copiaInfo.setContenido(cInfo);

			copiaInfo.setTituloAplicacion(copiaInfoFirma.getTituloAplicacion());
			copiaInfo.setTituloCSV(tituloCSV);
			copiaInfo.setTituloFecha(copiaInfoFirma.getTituloFecha());
			copiaInfo.setTituloExpediente(copiaInfoFirma.getTituloExpediente());
			copiaInfo.setTituloNif(copiaInfoFirma.getTituloNif());
			copiaInfo.setTituloURL(copiaInfoFirma.getTituloURL());

			copiaInfo.setOpcionesPagina(copiaInfoFirma.getOpcionesPagina());

			copiaInfo = this.generarCopiaFirmaComun(copiaInfo, firmantes,
					simple);

			salida.setContenido(copiaInfo.getContenido().getContenido());
			salida.setTipoMime(copiaInfo.getContenido().getTipoMIME());

			logger.warn("Descriptores abiertos fin generarJustificanteFirma: "
					+ FdUtils.getFdOpen());
		} catch (InSideException e) {
			throw e;
		} catch (Throwable t) {
			EstadoInfo estadoInfo = new EstadoInfo();
			logger.error("Error generando justificante de firma", t);
			throw new InSideException(
					"No se puede obtener el justificante de la firma "
							+ t.getMessage(), estadoInfo);
		}

		return salida;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * es.mpt.dsic.inside.ws.service.EeUtilService#generarInforme(es.mpt.dsic
	 * .inside.security.model.ApplicationLogin,
	 * es.mpt.dsic.inside.ws.service.model.CopiaInfoExtended)
	 * 
	 * Genera el justificante de firma a partir del documento original, la firma
	 * y el CSV. La lista de firmantes se calcula dinamicamente a partir de la
	 * firma.
	 */
	public CopiaInfo generarInforme(CopiaInfoExtended copia)
			throws InSideException {
		try {
			logger.warn("Descriptores abiertos inicio generarInforme: "
					+ FdUtils.getFdOpen());
			// Si no llega �mbito, se comprueba si viene incluido en el CSV:
			// AMB-CSVCSVCSVCSVCSVCSVCSVCSVCSVCSVCS
			if (StringUtils.isBlank(copia.getIdAplicacion())) {
				StringTokenizer stk = new StringTokenizer(
						copia.getIdAplicacion(), "-");
				if (stk.countTokens() != 2) {
					throw new InSideException(
							"No se ha recibido el �mbito del CSV.",
							new EstadoInfo());
				}
			}
			
			if (StringUtils.isEmpty(copia.getCsv())) {
				StringTokenizer stk = new StringTokenizer(
						copia.getIdAplicacion(), "-");
				StringBuilder csv = new StringBuilder(stk.nextToken());
				csv.append(CSVUtil.CSV_SEPARATOR);
				csv.append(obtenerCSV(copia.getFirma(), copia.getContenido().getContenido()));
				copia.setCsv(csv.toString());
			}
			
			ListaFirmaInfo firmantes = obtenerFirmantes(copia.getFirma());
			ponerNifsVacios(firmantes);
			CopiaInfo retorno = generarCopiaFirmaComun(copia, firmantes,
					copia.isSimple());
			logger.warn("Descriptores abiertos fin generarInforme: "
					+ FdUtils.getFdOpen());
			return retorno;
		} catch (InSideException e) {
			throw e;
		} catch (Throwable t) {
			logger.error("Error generando informe de la firma", t);
			throw new InSideException(
					"No se puede obtener el informe de la firma "
							+ t.getMessage(), new EstadoInfo());
		}
	}

	private void ponerNifsVacios(ListaFirmaInfo firmantes) {
		List<FirmaInfo> lista = firmantes.getInformacionFirmas();

		for (FirmaInfo firmante : lista) {
			firmante.setNifcif("");
		}
	}

	/**
	 * Obtiene el CSV de una firma.
	 * 
	 * @param firma
	 * @return
	 * @throws AOUnsupportedSignFormatException
	 * @throws AOException
	 */
	private String obtenerCSV(byte[] firma, byte[] documentoFirmado)
			throws AOUnsupportedSignFormatException, AOException, Exception {
		try {
			processor
					.processSign(firma, documentoFirmado, credentialUtil
							.getCredentialEeutilUserToken(wsContext)
							.getIdApplicacion());

			CVSSignature signature = new CVSSignature(processor);
			// String cvsText = signature.getCVSText();
			String cvsText = signature.getCVSTextOM();
			return cvsText;
		} catch (AOUnsupportedSignFormatException e) {
			// en este punto retornamos un csv a partir del hash de documento,
			// el documento no lleva ninguna firma
			return DigestUtils.md5Hex(firma);
		}
	}

	/**
	 * Obtiene los firmantes de una firma
	 * 
	 * @param firma
	 * @return
	 * @throws InSideException
	 */
	private ListaFirmaInfo obtenerFirmantes(byte[] firma)
			throws InSideException {
		InformacionFirmaAfirma info;
		try {
			info = afirmaService.obtenerInformacionFirma(
					credentialUtil.getCredentialEeutilUserToken(wsContext)
							.getIdApplicacion(), firma, true, false, false,
					null);
		} catch (AfirmaException e) {
			logger.error("Error obteniendo firmantes", e);
			EstadoInfo estadoInfo = new EstadoInfo("ERROR", e.getCode(),
					e.getMessage());
			throw new InSideException(estadoInfo.getDescripcion(), estadoInfo);
		}
		return listaFirmaInfoAfirmaToListaFirmaInfo(info.getFirmantes());
	}

	/**
	 * M�todo que comprueba si una firma es en formato PADES
	 */
	/*
	 * private boolean esPADES(byte[] firma) { boolean esPADES = false; AOSigner
	 * signer = AOSignerFactory.getSigner(firma); if (signer instanceof
	 * AOPDFSigner) { esPADES = true; } return esPADES; }
	 */

	/**
	 * Convierte a PDF
	 */
	private File convertir(File contenido, String mimeType) throws Exception {

		String ipOO = aplicacionContext.getAplicacionInfo().getPropiedades()
				.get("ip.openoffice");
		String portOO = aplicacionContext.getAplicacionInfo().getPropiedades()
				.get("port.openoffice");

		return pdfConverter.convertir(ipOO, portOO, contenido, mimeType);

	}

	private ListaFirmaInfo listaFirmaInfoAfirmaToListaFirmaInfo(
			List<FirmaInfoAfirma> listaAfirma) {
		if (listaAfirma == null) {
			return null;
		}

		ListaFirmaInfo lista = new ListaFirmaInfo();
		lista.setInformacionFirmas(new ArrayList<FirmaInfo>());

		for (FirmaInfoAfirma firmaInfoAfirma : listaAfirma) {
			lista.getInformacionFirmas().add(
					firmaInfoAfirmaToFirmaInfo(firmaInfoAfirma));
		}
		return lista;

	}

	private FirmaInfo firmaInfoAfirmaToFirmaInfo(FirmaInfoAfirma firmaInfoAfirma) {
		if (firmaInfoAfirma == null) {
			return null;
		}

		FirmaInfo firmaInfo = new FirmaInfo();
		firmaInfo.setNombre(firmaInfoAfirma.getNombre());
		firmaInfo.setApellido1(firmaInfoAfirma.getApellido1());
		firmaInfo.setApellido2(firmaInfoAfirma.getApellido2());
		firmaInfo.setNifcif(firmaInfoAfirma.getNifcif());
		firmaInfo.setFecha(firmaInfoAfirma.getFecha());
		firmaInfo.setExtras(firmaInfoAfirma.getExtras());

		return firmaInfo;
	}

	private ImageProperties getLogoProperties() {
		String rutaLogo = aplicacionContext.getAplicacionInfo()
				.getPropiedades().get("rutaLogo");
		float escalaLogoX = 15;
		float escalaLogoY = 15;
		if (StringUtils.isNotBlank(aplicacionContext.getAplicacionInfo()
				.getPropiedades().get("escalaLogoX"))) {
			escalaLogoX = Float.valueOf(aplicacionContext.getAplicacionInfo()
					.getPropiedades().get("escalaLogoX"));
		}
		if (StringUtils.isNotBlank(aplicacionContext.getAplicacionInfo()
				.getPropiedades().get("escalaLogoY"))) {
			escalaLogoY = Float.valueOf(aplicacionContext.getAplicacionInfo()
					.getPropiedades().get("escalaLogoY"));
		}
		return new ImageProperties(rutaLogo, escalaLogoX, escalaLogoY);
	}

	private CopiaInfo generarCopiaFirmaComun(CopiaInfo copia,
			ListaFirmaInfo firmas, boolean simple) throws InSideException {
		File pdf = null;
		File entrada = null;
		File pdfReduced = null;
		File pdfResult = null;
		try {

			// Si llega el CSV, se obliga a que tenga �mbito
			if (!StringUtils.isEmpty(copia.getCsv())) {
				if (StringUtils.isEmpty(copia.getIdAplicacion())
						&& !CSVUtil.hasScope(copia.getIdAplicacion())) {
					throw new InSideException(
							"No se ha especificado el �mbito", null);
				}
			}

			ImageProperties logoProperties = getLogoProperties();

			String inputPathFile = FileUtil.createFilePath("copiaFirma", copia
					.getContenido().getContenido());

			entrada = new File(inputPathFile);

			// Se convierte a PDF
			pdf = convertir(entrada, copia.getContenido().getTipoMIME());

			logger.debug("Descriptores abiertos despues convertir: "
					+ FdUtils.getFdOpen());

			// Se convierte a un PDF con las páginas del anterior pdf
			// estampadas a tamaño reducido
			pdfReduced = newPdfConversion.copiaPDFReducidoStamper(pdf,
					pdfCopiaAutenticaUtils.createPdfOptions(copia
							.getOpcionesPagina()));

			logger.debug("Descriptores abiertos despues copiaPDFReducidoStamper: "
					+ FdUtils.getFdOpen());

			// Se calcula el número de páginas en blanco a meter al final, si
			// hay muchos firmantes.
			int pagsBlancas = pdfCopiaAutenticaUtils.numeroPaginasEnBlanco(
					firmas, simple);

			logger.debug("Descriptores abiertos despues numeroPaginasEnBlanco: "
					+ FdUtils.getFdOpen());

			if (pagsBlancas > 0) {
				// Se meten las páginas en blanco
				File conPaginasEnBlanco = pdfCopiaAutenticaUtils
						.insertarPaginasEnBlanco(pdfReduced, pagsBlancas);

				logger.debug("Descriptores abiertos despues insertarPaginasEnBlanco: "
						+ FdUtils.getFdOpen());

				// Se estampa la información
				pdfResult = pdfCopiaAutenticaUtils.copiaAutenticaSimpleFirma(
						conPaginasEnBlanco, copia, simple, firmas, pagsBlancas,
						logoProperties);

				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: "
						+ FdUtils.getFdOpen());
			} else {
				// Se estampa la información
				pdfResult = pdfCopiaAutenticaUtils.copiaAutenticaSimpleFirma(
						pdfReduced, copia, simple, firmas, pagsBlancas,
						logoProperties);

				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: "
						+ FdUtils.getFdOpen());
			}

			copia.getContenido().setContenido(
					IOUtil.getBytesFromObject(pdfResult));
			copia.getContenido().setTipoMIME("application/pdf");

		} catch (InSideException e) {
			throw e;
		} catch (Throwable t) {
			logger.error("Error generando copia autentica", t);
			EstadoInfo estadoInfo = new EstadoInfo();
			throw new InSideException(t.getMessage(), estadoInfo, t.getCause());
		} finally {
			try {
				if (pdf != null) {
					FileUtils.forceDelete(pdf);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ pdf.getAbsolutePath());
			}
			try {
				if (entrada != null) {
					FileUtils.forceDelete(entrada);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ entrada.getAbsolutePath());
			}
			try {
				if (pdfReduced != null) {
					FileUtils.forceDelete(pdfReduced);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ pdfReduced.getAbsolutePath());
			}
			try {
				if (pdfResult != null) {
					FileUtils.forceDelete(pdfResult);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:"
						+ pdfResult.getAbsolutePath());
			}
		}

		return copia;
	}
	
	
//  obligado el parametro algoritmo a ser asi: SHA256 , SHA384 , SHA512
//	SHA256("") e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855 longitud=64 caracteres hexadecimales
//	SHA384("") 38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b longitud=96 caracteres hexadecimales
//	SHA512("") cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e longitud=128 caracteres hexadecimales

    @Override
    public String generarHash(@XmlMimeType("application/octet-stream") DataHandler data, String algoritmo) throws InSideException 
    {
    	String hashHEX = null;
           
        	EstadoInfo error = new EstadoInfo();
        	byte[] byteArrayFichero;
        	
        	
			try 
			{
				final InputStream in = data.getInputStream();
				byteArrayFichero = org.apache.commons.io.IOUtils.toByteArray(in);
			} 
			catch (IOException e) 
			{
				throw new InSideException(e.getMessage(),null);
			}
			
            
        	if(algoritmo == null || algoritmo.trim().equals(""))
        	{
        		
    	    	error.setCodigo("ERROR");
    	    	error.setEstado("ERROR en el algoritmo");
    	    	error.setDescripcion("El algoritmo debe ser o SHA256 o SHA384 o SHA512.");
    	    	throw new InSideException(error);
        	}
        	else if(algoritmo.trim().toUpperCase().equals(ALGORITMO_SHA256) || algoritmo.trim().contains(String.valueOf(LONGITUD_HASHHEX_SHA256*4)))
        	{
        		hashHEX = org.apache.commons.codec.digest.DigestUtils.sha256Hex(byteArrayFichero).toUpperCase(); 
        	}
        	else if(algoritmo.trim().toUpperCase().equals(ALGORITMO_SHA384) || algoritmo.trim().contains(String.valueOf(LONGITUD_HASHHEX_SHA384*4)))
        	{
        		hashHEX = org.apache.commons.codec.digest.DigestUtils.sha384Hex(byteArrayFichero).toUpperCase();
        	}
        	else if(algoritmo.trim().toUpperCase().equals(ALGORITMO_SHA512) || algoritmo.trim().contains(String.valueOf(LONGITUD_HASHHEX_SHA512*4)))
        	{
        		hashHEX = org.apache.commons.codec.digest.DigestUtils.sha512Hex(byteArrayFichero).toUpperCase();
        	}
        	else
        	{
        		error.setCodigo("ERROR");
    	    	error.setEstado("ERROR en el algoritmo");
    	    	error.setDescripcion("El algoritmo debe ser o SHA256 o SHA384 o SHA512.");
    	    	throw new InSideException(error);
        	}
            
        
        
        return hashHEX.toUpperCase();
		
    }

    
	@Override
	public boolean validarHash(@XmlMimeType("application/octet-stream") DataHandler fichero, String hashHEX) throws InSideException 
	{
		String hashCalculado = null;
		
          
    	    if((hashHEX == null || "".equals(hashHEX.trim())) 
    	    		|| (hashHEX.trim().length()!= LONGITUD_HASHHEX_SHA256 && hashHEX.trim().length()!= LONGITUD_HASHHEX_SHA384 && hashHEX.trim().length()!= LONGITUD_HASHHEX_SHA512) )
    	    {
    	    	EstadoInfo error = new EstadoInfo();
    	    	error.setCodigo("ERROR");
    	    	error.setEstado("ERROR en el hash");
    	    	error.setDescripcion("El hash informado no est� generado con algoritmo ni SHA256 ni SHA384 ni SHA512.");
    	    	throw new InSideException(error);
    	    }
    	    else if(hashHEX.trim().length() == LONGITUD_HASHHEX_SHA256 )
    	    {
    	    	hashCalculado = this.generarHash(fichero, ALGORITMO_SHA256); 
    	    }
    	    else if(hashHEX.trim().length() == LONGITUD_HASHHEX_SHA384 )
    	    {
    	    	hashCalculado = this.generarHash(fichero, ALGORITMO_SHA384);
    	    }
    	    else if(hashHEX.trim().length() == LONGITUD_HASHHEX_SHA512 )
    	    {
    	    	hashCalculado = this.generarHash(fichero, ALGORITMO_SHA512);
    	    }
    	          	     	
            return hashCalculado.equalsIgnoreCase(hashHEX);       
            
        
	}
}
