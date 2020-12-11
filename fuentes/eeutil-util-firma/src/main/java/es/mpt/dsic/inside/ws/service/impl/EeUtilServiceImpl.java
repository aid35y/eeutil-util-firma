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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;

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
import es.mpt.dsic.eeutil.misc.consumer.impl.ConsumerEeutilMiscImpl;
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
import es.mpt.dsic.inside.security.model.ApplicationLogin;
import es.mpt.dsic.inside.services.AfirmaService;
import es.mpt.dsic.inside.util.SignedData;
import es.mpt.dsic.inside.util.SignedDataExtractor;
import es.mpt.dsic.inside.util.pdfoptimizer.ItextPdfOptimizer;
import es.mpt.dsic.inside.util.pdfoptimizer.PdfOptimizerPdfTools;
import es.mpt.dsic.inside.utils.file.FdUtils;
import es.mpt.dsic.inside.utils.file.FileUtil;
import es.mpt.dsic.inside.utils.io.IOUtil;
import es.mpt.dsic.inside.utils.mime.MimeUtil;
import es.mpt.dsic.inside.ws.service.EeUtilService;
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

@Service("eeUtilService")
@WebService(endpointInterface = "es.mpt.dsic.inside.ws.service.EeUtilService")
@SOAPBinding(style = Style.RPC, parameterStyle = ParameterStyle.BARE, use = Use.LITERAL)
public class EeUtilServiceImpl implements EeUtilService {

	protected final static Log logger = LogFactory
			.getLog(EeUtilServiceImpl.class);

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
	
	@Autowired
	ConsumerEeutilMiscImpl consumerEeutilMiscImpl;
	
	@Autowired
	ItextPdfOptimizer itextPdfOptimizer;
	
	@Autowired
	PdfOptimizerPdfTools pdfOptimizerPdfTools;
	
	private long MAX_FILE_SIZE_BYTES = 4 * 1024 * 1024;

	@Secured("ROLE_TRAMITAR")
	public CopiaInfo comprobarAplicacion(ApplicationLogin info, CopiaInfo copia)
			throws InSideException {
		logger.warn("Descriptores abiertos inicio comprobarAplicacion: " + FdUtils.getFdOpen());
		CopiaInfo retorno = generarCopia(info, copia, false);
		logger.warn("Descriptores abiertos fin comprobarAplicacion: " + FdUtils.getFdOpen());
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
	public CopiaInfo generarCopia(ApplicationLogin info, CopiaInfo copia,
			boolean simple) throws InSideException {
		logger.warn("Descriptores abiertos inicio generarCopia: " + FdUtils.getFdOpen());
		CopiaInfo retorno = generarCopiaFirmaComun(info, copia, null, simple);
		logger.warn("Descriptores abiertos fin generarCopia: " + FdUtils.getFdOpen());
		return retorno;
	}

	@Override
	public String generarCSV(ApplicationLogin info, CSVInfo copia)
			throws InSideException {
		logger.debug("Generar CSV: " + info.getIdApplicacion());
		logger.warn("Descriptores abiertos inicio generarCSV: " + FdUtils.getFdOpen());
		byte[] contenido = copia.getContenido();
		byte[] documentoFirmado = copia.getContenidoFirmado();
		String cvsText;

		try {
			cvsText = obtenerCSV(contenido, documentoFirmado, info.getIdApplicacion());

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
		logger.warn("Descriptores abiertos fin generarCSV: " + FdUtils.getFdOpen());
		return cvsText;
	}

	@Override
	public String generarCSVAmbito(ApplicationLogin info, CSVInfoAmbito copia)
			throws InSideException {
		logger.warn("Descriptores abiertos inicio generarCSVAmbito: " + FdUtils.getFdOpen());
		final byte[] contenido = copia.getContenido();
		final byte[] documentoFirmado = copia.getContenidoFirmado();
		final String cvsAmbito = copia.getAmbito();
		final String cvsText;

		try {
			cvsText = obtenerCSV(contenido, documentoFirmado, info.getIdApplicacion());

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
		logger.warn("Descriptores abiertos fin generarCSVAmbito: " + FdUtils.getFdOpen());
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
	public CopiaInfo generarCopiaFirma(ApplicationLogin login, CopiaInfo copia,
			ListaFirmaInfo firmas, boolean simple) throws InSideException {
		logger.debug("generarCopiaFirma: " + login.getIdApplicacion());
		
		logger.warn("Descriptores abiertos inicio generarCopiaFirma: " + FdUtils.getFdOpen());

		CopiaInfo retorno = generarCopiaFirmaComun(login, copia, firmas, simple);
		
		logger.warn("Descriptores abiertos fin generarCopiaFirma: " + FdUtils.getFdOpen());
		
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
	public CopiaInfo generarCopiaFirmaNormalizada(ApplicationLogin info,
			CopiaInfo copia, ListaFirmaInfo firmas, boolean simple)
			throws InSideException {
		File entrada = null;
		File pdf = null;
		File pdfCopy = null;
		File pdfResult = null;
		
		File pdfReduced = null;
		
		File itextSmaller = null;
		//String smallerPathFile = null;
		File itextSmaller2 = null; 
		File conPaginasEnBlanco =null;
				
		try {
			logger.debug("generarCopiaFirma: " + info.getIdApplicacion());
			
			logger.warn("Descriptores abiertos inicio generarCopiaFirmaNormalizada: " + FdUtils.getFdOpen());

			if (StringUtils.isEmpty(copia.getIdAplicacion())
					&& !CSVUtil.hasScope(copia.getIdAplicacion())) {
				throw new InSideException("No se ha especificado el �mbito",
						null);
			}

			String rutaLogo = aplicacionContext.getAplicacionInfo().getPropiedades().get("rutaLogo");

			String inputPathFile = FileUtil.createFilePath("copiaFirmaNormalizada", copia.getContenido().getContenido());

			entrada = new File(inputPathFile);

			// Se convierte a PDF Es importante para los documentos pdf con campos editables para que al minimizarles 
			// el tama�o no se pierda esos valores
			pdf = convertir(info, entrada, copia.getContenido().getTipoMIME());

			byte[] filecopy = newPdfConversion.copyPdf(pdf);
			
			String inputPathFileCopy = FileUtil.createFilePath("filecopy", filecopy, ".pdf");
			pdfCopy = new File(inputPathFileCopy);
			//********************************************************************************************************//
			
	
			/////reduce el tama�o para insertar datos de csv ///		
						
			// Se convierte a un PDF con las páginas del anterior pdf
			// estampadas a tamaño reducido
			pdfReduced = newPdfConversion.copiaPDFReducidoStamper(
					pdfCopy, pdfCopiaAutenticaUtils
							.createPdfOptionsWithOutPercent(copia.getOpcionesPagina()));
			
			//TODO Elimninar este codigo para usar el optimizador de jpdfoptimizer o pdfoptimizer de pdf tools
			if (pdfReduced.length() > MAX_FILE_SIZE_BYTES) {
				if (pdfReduced != null  && pdfReduced.exists()) {
					FileUtils.forceDelete(pdfReduced);
				}
				pdfReduced = newPdfConversion.shrinkPdf(pdfCopy, pdfCopiaAutenticaUtils
					.createPdfOptions(copia.getOpcionesPagina()));
			}
			
			//jPdfOptimizer.auditFile(pdfReduced);
			//fileOptimized = jPdfOptimizer.optimize(pdfReduced);
			//fileOptimized = pdfOptimizerPdfTools.optimizePdf(pdfReduced.getAbsolutePath(), PdfOptimize.OPTIMIZATIONPROFILE.eOptimizationProfileWeb);
			//jPdfOptimizer.auditFile(fileOptimized);
			
			
			//ITEXT
			//itextSmaller = itextPdfOptimizer.makeSmaller(fileOptimized);
			
			itextSmaller = itextPdfOptimizer.makeSmaller(pdfReduced);
			
			
			
			
			// Se calcula el numero de paginas en blanco a meter al final, si
			// hay muchos firmantes.
			int pagsBlancas = pdfCopiaNormalizadaUtils.numeroPaginasEnBlanco(
								firmas, simple);
			
			
			if (pagsBlancas > 0) {
				// Se meten las páginas en blanco
				conPaginasEnBlanco = pdfCopiaAutenticaUtils
						.insertarPaginasEnBlanco(itextSmaller, pagsBlancas);
				
				logger.debug("Descriptores abiertos despues insertarPaginasEnBlanco: " + FdUtils.getFdOpen());
				
				// Se estampa la información
				pdfResult = pdfCopiaNormalizadaUtils.copiaAutenticaNormalizada(conPaginasEnBlanco, copia, rutaLogo , simple, firmas, pagsBlancas);
				
				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: " + FdUtils.getFdOpen());
			} else {
				// Se estampa la información
				pdfResult = pdfCopiaNormalizadaUtils.copiaAutenticaNormalizada(itextSmaller, copia, rutaLogo, simple, firmas, pagsBlancas);
				
				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: " + FdUtils.getFdOpen());
			}
			
			
			//ITEXT
			itextSmaller2 = itextPdfOptimizer.makeSmaller(pdfResult);

			copia.getContenido().setContenido(IOUtil.getBytesFromObject(itextSmaller2));
			copia.getContenido().setTipoMIME("application/pdf");
			
	
			/////*******  reduce el tama�o para insertar datos de csv ***********************///
			

			logger.warn("Descriptores abiertos fin generarCopiaFirmaNormalizada: " + FdUtils.getFdOpen());

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
				if (pdf != null && pdf.exists()) {
					FileUtils.forceDelete(pdf);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdf.getAbsolutePath());
			}
			try {
				if (entrada != null && entrada.exists()) {
					FileUtils.forceDelete(entrada);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + entrada.getAbsolutePath());
			}
			try {
				if (pdfReduced != null && pdfReduced.exists()) {
					FileUtils.forceDelete(pdfReduced);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfReduced.getAbsolutePath());
			}
			try {
				if (pdfResult != null && pdfResult.exists()) {
					FileUtils.forceDelete(pdfResult);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfResult.getAbsolutePath());
			}
			try {
				if (itextSmaller != null && itextSmaller.exists()) {
					FileUtils.forceDelete(itextSmaller);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + itextSmaller.getAbsolutePath());
			}
			try {
				if (itextSmaller2 != null && itextSmaller2.exists()) {
					FileUtils.forceDelete(itextSmaller2);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + itextSmaller2.getAbsolutePath());
			}
			/*
			try {
				if (fileOptimized != null) {
					FileUtils.forceDelete(fileOptimized);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + fileOptimized.getAbsolutePath());
			}
			*/
		
			try {
				if (pdf != null && pdf.exists()) {
					FileUtils.forceDelete(pdf);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdf.getAbsolutePath());
			}
			try {
				if (pdfResult != null && pdfResult.exists()) {
					FileUtils.forceDelete(pdfResult);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfResult.getAbsolutePath());
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
	public CopiaInfoFirmaSalida generarJustificanteFirma(ApplicationLogin info,
			CopiaInfoFirma copiaInfoFirma, boolean simple)
			throws InSideException {

		byte[] bytesDocumento = null;
		String mimeDocumento = null;
		CopiaInfoFirmaSalida salida = new CopiaInfoFirmaSalida();

		try {
			logger.warn("Descriptores abiertos inicio generarJustificanteFirma: " + FdUtils.getFdOpen());

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
						copiaInfoFirma.getDocumento(), info.getIdApplicacion());
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
				firmantes = obtenerFirmantes(info.getIdApplicacion(),
						copiaInfoFirma.getFirma());
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

			copiaInfo = this.generarCopiaFirmaComun(info, copiaInfo, firmantes,
					simple);

			salida.setContenido(copiaInfo.getContenido().getContenido());
			salida.setTipoMime(copiaInfo.getContenido().getTipoMIME());
			
			logger.warn("Descriptores abiertos fin generarJustificanteFirma: " + FdUtils.getFdOpen());
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
	public CopiaInfo generarInforme(ApplicationLogin login,
			CopiaInfoExtended copia) throws InSideException {
		try {
			logger.warn("Descriptores abiertos inicio generarInforme: " + FdUtils.getFdOpen());
			
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
				csv.append(obtenerCSV(copia.getFirma(), copia.getContenido().getContenido(), copia.getIdAplicacion()));
				copia.setCsv(csv.toString());
			}
			
			ListaFirmaInfo firmantes = obtenerFirmantes(
					login.getIdApplicacion(), copia.getFirma());
			ponerNifsVacios(firmantes);
			CopiaInfo retorno = generarCopiaFirmaComun(login, copia, firmantes, copia.isSimple());
			logger.warn("Descriptores abiertos fin generarInforme: " + FdUtils.getFdOpen());
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
	private String obtenerCSV(byte[] firma, byte[] documentoFirmado, String idAplicacion)
			throws AOUnsupportedSignFormatException, AOException, Exception {
		try {
			processor.processSign(firma, documentoFirmado, idAplicacion);

			CVSSignature signature = new CVSSignature(processor);
			// String cvsText = signature.getCVSText();
			String cvsText = signature.getCVSTextOM();
			return cvsText;
		} catch (AOUnsupportedSignFormatException e) {
			//en este punto retornamos un csv a partir del hash de documento, el documento no lleva ninguna firma
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
	private ListaFirmaInfo obtenerFirmantes(String aplicacion, byte[] firma)
			throws InSideException {
		InformacionFirmaAfirma info;
		try {
			info = afirmaService.obtenerInformacionFirma(aplicacion, firma,
					true, false, false, null);
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
	private File convertir(ApplicationLogin info, File contenido,
			String mimeType) throws Exception {

		String ipOO = aplicacionContext.getAplicacionInfo().getPropiedades()
				.get("ip.openoffice");
		String portOO = aplicacionContext.getAplicacionInfo()
				.getPropiedades().get("port.openoffice");

		if (StringUtils.isNotEmpty(mimeType)
			&& (mimeType.contentEquals("text-tcn/html")
			|| mimeType.contentEquals("text/tcn"))) {
			return consumerEeutilMiscImpl.convertirTCNAPdf(info, contenido, mimeType);
		} else {
			return pdfConverter.convertir(ipOO, portOO, contenido, mimeType);
		}
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
	
	private CopiaInfo generarCopiaFirmaComun(ApplicationLogin login, CopiaInfo copia,
			ListaFirmaInfo firmas, boolean simple) throws InSideException {
		File pdf = null;
		File entrada = null;
		File pdfReduced = null;
		File pdfResult = null;
		File itextSmaller = null;
		//String smallerPathFile = null;
		File itextSmaller2 = null; 
		//File fileOptimized = null;
		
		File pdfCopy = null;
		try {
			logger.debug("generarCopiaFirmaComun: " + login.getIdApplicacion());

			// Si llega el CSV, se obliga a que tenga �mbito
			if (!StringUtils.isEmpty(copia.getCsv())) {
				if (StringUtils.isEmpty(copia.getIdAplicacion())
						&& !CSVUtil.hasScope(copia.getIdAplicacion())) {
					throw new InSideException(
							"No se ha especificado el �mbito", null);
				}
			}

			ImageProperties logoProperties = getLogoProperties();

			String inputPathFile = FileUtil.createFilePath("copiaFirma", copia.getContenido().getContenido());

			entrada = new File(inputPathFile);

			// Se convierte a PDF
			pdf = convertir(login, entrada, copia.getContenido().getTipoMIME());
			
			
			
			
			/// ***** necesario para pdf con campos editables hay que trabajar sobre una copia de el para no perder el valor de esos campos **///
									
			byte[] filecopy = newPdfConversion.copyPdf(pdf);
			
			String inputPathFileCopy = FileUtil.createFilePath("filecopy", filecopy, ".pdf");
			pdfCopy = new File(inputPathFileCopy);
			
			/// ****************************************************************************************  ///
			
				
			
			logger.debug("Descriptores abiertos despues convertir: " + FdUtils.getFdOpen());

			// Se convierte a un PDF con las páginas del anterior pdf
			// estampadas a tamaño reducido
			pdfReduced = newPdfConversion.copiaPDFReducidoStamper(pdfCopy, pdfCopiaAutenticaUtils.createPdfOptionsWithOutPercent(copia.getOpcionesPagina()));
			
			//TODO Elimninar este codigo para usar el optimizador de jpdfoptimizer o pdfoptimizer de pdf tools
			if (pdfReduced.length() > MAX_FILE_SIZE_BYTES) {
				if (pdfReduced != null && pdfReduced.exists()) {
					FileUtils.forceDelete(pdfReduced);
				}
				pdfReduced = newPdfConversion.shrinkPdf(pdfCopy, pdfCopiaAutenticaUtils.createPdfOptions(copia.getOpcionesPagina()));
			}
			
			//jPdfOptimizer.auditFile(pdfReduced);
			//fileOptimized = jPdfOptimizer.optimize(pdfReduced);
			//fileOptimized = pdfOptimizerPdfTools.optimizePdf(pdfReduced.getAbsolutePath(), PdfOptimize.OPTIMIZATIONPROFILE.eOptimizationProfileWeb);
			//jPdfOptimizer.auditFile(fileOptimized);
			
			
			//ITEXT
			//itextSmaller = itextPdfOptimizer.makeSmaller(fileOptimized);
			itextSmaller = itextPdfOptimizer.makeSmaller(pdfReduced);
			
			logger.debug("Descriptores abiertos despues copiaPDFReducidoStamper: " + FdUtils.getFdOpen());

			// Se calcula el número de páginas en blanco a meter al final, si
			// hay muchos firmantes.
			int pagsBlancas = pdfCopiaAutenticaUtils.numeroPaginasEnBlanco(firmas, simple);
			
			logger.debug("Descriptores abiertos despues numeroPaginasEnBlanco: " + FdUtils.getFdOpen());

			if (pagsBlancas > 0) {
				// Se meten las páginas en blanco
				File conPaginasEnBlanco = pdfCopiaAutenticaUtils
						.insertarPaginasEnBlanco(itextSmaller, pagsBlancas);
				
				logger.debug("Descriptores abiertos despues insertarPaginasEnBlanco: " + FdUtils.getFdOpen());
				
				// Se estampa la información
				pdfResult = pdfCopiaAutenticaUtils.copiaAutenticaSimpleFirma(conPaginasEnBlanco, copia, simple, firmas, pagsBlancas,
						logoProperties);
				
				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: " + FdUtils.getFdOpen());
			} else {
				// Se estampa la información
				pdfResult = pdfCopiaAutenticaUtils.copiaAutenticaSimpleFirma(itextSmaller, copia, simple, firmas, pagsBlancas,
						logoProperties);
				
				logger.debug("Descriptores abiertos despues copiaAutenticaSimpleFirma: " + FdUtils.getFdOpen());
			}
			
			//ITEXT
			itextSmaller2 = itextPdfOptimizer.makeSmaller(pdfResult);

			copia.getContenido().setContenido(IOUtil.getBytesFromObject(itextSmaller2));
			copia.getContenido().setTipoMIME("application/pdf");

		} catch (InSideException e) {
			throw e;
		} catch (Throwable t) {
			logger.error("Error generando copia autentica", t);
			EstadoInfo estadoInfo = new EstadoInfo();
			throw new InSideException(t.getMessage(), estadoInfo, t.getCause());
		} finally {
			try {
				if (pdf != null && pdf.exists()) {
					FileUtils.forceDelete(pdf);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdf.getAbsolutePath());
			}
			try {
				if (entrada != null && entrada.exists()) {
					FileUtils.forceDelete(entrada);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + entrada.getAbsolutePath());
			}
			try {
				if (pdfReduced != null && pdfReduced.exists()) {
					FileUtils.forceDelete(pdfReduced);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfReduced.getAbsolutePath());
			}
			try {
				if (pdfResult != null && pdfResult.exists()) {
					FileUtils.forceDelete(pdfResult);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfResult.getAbsolutePath());
			}
			try {
				if (itextSmaller != null && itextSmaller.exists()) {
					FileUtils.forceDelete(itextSmaller);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + itextSmaller.getAbsolutePath());
			}
			try {
				if (itextSmaller2 != null && itextSmaller2.exists()) {
					FileUtils.forceDelete(itextSmaller2);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + itextSmaller2.getAbsolutePath());
			}
			try {
				if (pdfCopy != null && pdfCopy.exists()) {
					FileUtils.forceDelete(pdfCopy);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + pdfCopy.getAbsolutePath());
			}
			/*
			try {
				if (fileOptimized != null) {
					FileUtils.forceDelete(fileOptimized);
				}
			} catch (IOException e) {
				logger.error("Error al eliminar fichero:" + fileOptimized.getAbsolutePath());
			}
			*/
		}

		return copia;
	}
}
