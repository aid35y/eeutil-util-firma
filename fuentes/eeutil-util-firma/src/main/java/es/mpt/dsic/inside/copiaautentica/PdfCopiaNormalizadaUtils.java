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

package es.mpt.dsic.inside.copiaautentica;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import es.mpt.dsic.inside.copiaautentica.exception.CopiaAutenticaCantBeGeneratedException;
import es.mpt.dsic.inside.csv.CSVUtil;
import es.mpt.dsic.inside.pdf.PdfOptions;
import es.mpt.dsic.inside.pdf.PdfUtils;
import es.mpt.dsic.inside.utils.file.FileUtil;
import es.mpt.dsic.inside.ws.service.model.CopiaInfo;
import es.mpt.dsic.inside.ws.service.model.FirmaInfo;
import es.mpt.dsic.inside.ws.service.model.ListaFirmaInfo;
import es.mpt.dsic.inside.ws.service.model.OpcionesPagina;
//import com.lowagie.text.BaseColor;

@Service("pdfCopiaNormalizadaUtils")
public class PdfCopiaNormalizadaUtils {

	
	private static final int FILAS_SIMPLE=1;
	private static final int FILAS_FIRMA=6;
	private static final int FILAS_COMPLEJO=3;
	
	private static final float[] RELATIVE_WIDTHS_COMPLEJO = new float[]{0.25f, 0.4f, 0.35f};
	
	
	private static final String EMPTY = "EMPTY";
	
	private static final int NUM_FIRMANTES_ABAJO_MAX = 3;
	private static final int NUM_FIRMANTES_PAGINA_SIMPLE = 50;
	private static final int NUM_FIRMANTES_PAGINA_COMPLEJO = 25;
	private static final float TABLE_WIDTH = 297;
	
	
	public int numeroPaginasEnBlanco (ListaFirmaInfo firmantes, boolean simple) {
		int paginas = 0;
		int numMax = NUM_FIRMANTES_PAGINA_COMPLEJO;
		if (simple) {
			numMax = NUM_FIRMANTES_PAGINA_SIMPLE;
		}
		if (firmantes != null && firmantes.getInformacionFirmas().size() > NUM_FIRMANTES_ABAJO_MAX) {
			paginas = firmantes.getInformacionFirmas().size() / numMax;
			int modulo = firmantes.getInformacionFirmas().size() % numMax;
			if (modulo > 0) {
				paginas ++;
			}
		}
		return paginas;
	}
	
	public PdfOptions createPdfOptions (OpcionesPagina opcionesPagina) {
		PdfOptions pdfOptions = PdfOptions.createDefault();
		pdfOptions.setPrintPageNumbers(false);
		if (opcionesPagina != null) {
			if (opcionesPagina.getPorcentajeDocumento() != null) {
				pdfOptions.setPagePercent(opcionesPagina.getPorcentajeDocumento());
			}
			if (opcionesPagina.getSeparacionX() != null) {
				pdfOptions.setPagePositionX(opcionesPagina.getSeparacionX());
			}
			if (opcionesPagina.getSeparacionY() != null) {
				pdfOptions.setPagePositionY(opcionesPagina.getSeparacionY());
			}
		}
		return pdfOptions;
	}
	
	public File insertarPaginasEnBlanco (File ficheroPDF, int numeroPaginas) throws CopiaAutenticaCantBeGeneratedException, DocumentException, IOException {
		PdfReader reader = null;
		FileOutputStream fos = null;
		PdfStamper stamper = null;
		FileInputStream fis = null;
		String resultPath = null;
		try {
			fis = new FileInputStream (ficheroPDF);
			reader = PdfUtils.unlockPdf(new PdfReader(fis));
			
			resultPath = FileUtil.createFilePath("pagBlanco");
			fos = new FileOutputStream (resultPath);
	
			stamper = new PdfStamper(reader, fos);
			stamper.setFullCompression();
			for (int i=1; i<=numeroPaginas; i++) {			
				stamper.insertPage(reader.getNumberOfPages() + i, PageSize.A4);
			}
			
		} catch (Throwable t) {
			throw new CopiaAutenticaCantBeGeneratedException ("No se pueden insertar páginas en blanco", t);
		} finally {
			PdfUtils.close(stamper);
			PdfUtils.close(reader);
			FileUtil.close(fos);
			FileUtil.close(fis);
		}
		return new File (resultPath);
	}
	
	/*public InputStream copiaAutenticaSimpleFirma (InputStream ficheroPDF, 
														 CopiaInfo copia, 
														 String rutaLogo,
														 boolean simple, 
														 ListaFirmaInfo listaFirma,
														 int pagsBlanco) throws Exception {*/
	public File copiaAutenticaNormalizada (File ficheroPDF, 
			 CopiaInfo copia, 
			 String rutaLogo,
			 boolean simple, 
			 ListaFirmaInfo listaFirma,
			 int pagsBlanco) throws CopiaAutenticaCantBeGeneratedException, DocumentException, IOException {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		PdfStamper stamp = null;
		PdfReader reader = null;
		
		String resultPath = FileUtil.createFilePath("copiaNormalizada");
		
		copia.setCsv(CSVUtil.unificarAmbitoCSV(copia.getIdAplicacion(), copia.getCsv()));
		
		try {
			fis = new FileInputStream (ficheroPDF);
			reader = PdfUtils.unlockPdf(new PdfReader(fis));
			int numPags = reader.getNumberOfPages();
			int primeraPaginaBlanco = numPags - pagsBlanco + 1;
			
			int pagsDocumentoOriginal = numPags - pagsBlanco;
			
			simple = simple || (pagsDocumentoOriginal == 1 && listaFirma != null);
			
			//ByteArrayOutputStream out = new ByteArrayOutputStream();
			fos = new FileOutputStream (resultPath);
			stamp = new PdfStamper(reader, fos);
			stamp.setFullCompression();
	
			stamp.setFormFlattening(false);
			int i = 1;
			
			boolean dibujarQR = copia.getUrlQR() != null && !copia.getUrlQR().contentEquals("");
			float offsetTableY; // M�ximo 18 + 9 + 9 + 9

			while (i <= numPags) {
				
				offsetTableY = 18;
				PdfContentByte over = stamp.getOverContent(i);
				PdfPTable footer = null;
	
				
				// SIMPLE
				if (simple) {
					
					float tableWidth = TABLE_WIDTH;
					footer = createWideTable(FILAS_SIMPLE, tableWidth);
					// Se añade informacion a la tabla footer
					tratarTablaInfo(copia, true, footer);
					
					// SIMPLE - Hay firmantes
					if (listaFirma != null) {
						
						// SIMPLE - Hay Firmantes - Pocos firmantes
						if (listaFirma.getInformacionFirmas().size() <= NUM_FIRMANTES_ABAJO_MAX) {
							offsetTableY += 9 * listaFirma.getInformacionFirmas().size();
							// Se añade info de los firmantes a la tabla footer
							tratarTablaFirmantes(listaFirma.getInformacionFirmas(), 0, listaFirma.getInformacionFirmas().size(), copia, true, footer);
						
						// SIMPLE - Hay Firmantes - Muchos firmantes - No se meten salvo que sean las páginas en blanco.
						} else {
							//SIMPLE - Hay Firmantes - Muchos firmantes - es hoja en blanco
							if (i >= primeraPaginaBlanco) {
								int numPaginaBlanco = i - primeraPaginaBlanco;
								int iInicial = numPaginaBlanco * NUM_FIRMANTES_PAGINA_SIMPLE;
								int iFinal = iInicial + NUM_FIRMANTES_PAGINA_SIMPLE;
								
								if (iFinal > listaFirma.getInformacionFirmas().size()) {
									iFinal = listaFirma.getInformacionFirmas().size();
								}
								
								PdfPTable tablaFirmantes = createWideTable(FILAS_SIMPLE, reader.getPageSize(i).getWidth());
								tratarTablaFirmantes (listaFirma.getInformacionFirmas(), iInicial, iFinal, copia, true, tablaFirmantes);
								
								// escribir tabla Firmantes
								pintarTablaFirmantesEnHojaBlanca (tablaFirmantes, over, reader.getPageSize(i).getWidth(), reader.getPageSize(i).getHeight());
							}
							
						}
						
					}
					
				// COMPLEJO
				} else {
					 // Complejo - hay firmantes
					if (listaFirma != null) {
						
						// Complejo - hay firmantes- Pocos firmantes
						if (listaFirma.getInformacionFirmas().size() <= NUM_FIRMANTES_ABAJO_MAX) {
							offsetTableY += 9 * listaFirma.getInformacionFirmas().size();
							// Complejo - hay firmantes- Pocos firmantes- No es ultima pagina
							if (i != numPags ) {
								
								//footer = createWideTable(FILAS_COMPLEJO, reader.getPageSize(i).getWidth());
								footer = createWideTableRelativeWidths(RELATIVE_WIDTHS_COMPLEJO, reader.getPageSize(i).getWidth());
								tratarTablaInfo(copia, false, footer);
								
								// CREAR E INSERTAR CODIGO DE BARRAS AL FOOTER
								if (!("").equalsIgnoreCase(copia.getCsv())) {
									PdfPCell cell2 = createCodeBarCell(createImageCodeBar(copia.getCsv(), over));
									footer.addCell(cell2);
								}
	
								
							// Complejo - hay firmantes- Pocos firmantes- Ultima pagina
							} else {
								
								int columnas = FILAS_FIRMA;
								if (!algunNif (listaFirma.getInformacionFirmas())){					    	    			
									columnas = FILAS_FIRMA -1;
								}
	
								footer = createWideTable(columnas, reader.getPageSize(i).getWidth());
								tratarTablaFirmantes(listaFirma.getInformacionFirmas(), 0, listaFirma.getInformacionFirmas().size(), copia, false, footer);
							}
							
						// Complejo - hay firmantes - Muchos firmantes
						} else {
							
							//footer = createWideTable(FILAS_COMPLEJO, reader.getPageSize(i).getWidth());
							footer = createWideTableRelativeWidths(RELATIVE_WIDTHS_COMPLEJO, reader.getPageSize(i).getWidth());
							tratarTablaInfo(copia, false, footer);
							
							// CREAR E INSERTAR CODIGO DE BARRAS AL FOOTER
							if (!("").equalsIgnoreCase(copia.getCsv())) {
								PdfPCell cell2 = createCodeBarCell(createImageCodeBar(copia.getCsv(), over));
								footer.addCell(cell2);
							}
							
							// Complejo - hay firmantes - Muchos firmantes - Hoja blanca
							if (i >= primeraPaginaBlanco)	{
								
								int numPaginaBlanco = i - primeraPaginaBlanco;
								int iInicial = numPaginaBlanco * NUM_FIRMANTES_PAGINA_COMPLEJO;
								int iFinal = iInicial + NUM_FIRMANTES_PAGINA_COMPLEJO;
								
								if (iFinal > listaFirma.getInformacionFirmas().size()) {
									iFinal = listaFirma.getInformacionFirmas().size();
								}
								
								int columnas = FILAS_FIRMA;
								if (!algunNif (listaFirma.getInformacionFirmas())){					    	    			
									columnas = FILAS_FIRMA -1;
								}
								
								PdfPTable tablaFirmantes = createWideTable(columnas, reader.getPageSize(i).getWidth());
								tratarTablaFirmantes (listaFirma.getInformacionFirmas(), iInicial, iFinal, copia, false, tablaFirmantes);
								
								// escribir tabla Firmantes
								pintarTablaFirmantesEnHojaBlanca (tablaFirmantes, over,  reader.getPageSize(i).getWidth(), reader.getPageSize(i).getHeight());
								
							}
							
						}
						
					// Complejo - no hay firmantes
					} else {
						
						//footer = createWideTable(FILAS_COMPLEJO, reader.getPageSize(i).getWidth());
						footer = createWideTableRelativeWidths(RELATIVE_WIDTHS_COMPLEJO, reader.getPageSize(i).getWidth());					
						tratarTablaInfo(copia, false, footer);
						
						// CREAR E INSERTAR CODIGO DE BARRAS AL FOOTER
						if (!("").equalsIgnoreCase(copia.getCsv())) {
							PdfPCell cell2 = createCodeBarCell(createImageCodeBar(copia.getCsv(), over));
							footer.addCell(cell2);
						}
						
					}
				}
				
				if (copia.getEstamparLogo()) {
					pintarLogo (rutaLogo, over);				
				}
				
				pintarRecuadro (over);
				pintarPie (footer, over, offsetTableY);
				
				if (dibujarQR) {
					pintarQR (copia.getUrlQR(), over);
				}
				
				i++;
			}
	
		} catch (Throwable t) {
			throw new CopiaAutenticaCantBeGeneratedException ("No se puede generar la copia auténtica", t);
		} finally {
			PdfUtils.close(stamp);
			PdfUtils.close(reader);
			FileUtil.close(fos);
			FileUtil.close(fis);
		}
		return new File (resultPath);
	}
	
	
	private void pintarRecuadro (PdfContentByte over) {
		// PINTAR LINEA HORIZONTAL
		Document document = over.getPdfDocument();
		float x = document.left() + 100;
		float y = document.bottom() + 45;
		over.moveTo(x, y);
		
		over.lineTo(x + 338, y);
		over.lineTo(x + 338, 3);
		over.lineTo(x, 3);
		over.lineTo(x, y);
		over.lineTo(x + 338, y);
		
		over.stroke();
	}
	
	
	private void pintarPie (PdfPTable footer, PdfContentByte over, float offsetTableY) {
		Document document = over.getPdfDocument();
		float xPosition = document.left() + 141;
		float yPosition = document.left() + offsetTableY;
		footer.writeSelectedRows(0, -1, xPosition, yPosition, over);
//		(document.right() - document.left() - 285) / 2, document.bottom() + 45, over);
	}
	
	private void pintarTablaFirmantesEnHojaBlanca (PdfPTable tabla, PdfContentByte over, float width, float hight) {
		Document document = over.getPdfDocument();
		tabla.writeSelectedRows(0, -1,
				(document.right() - document.left() - 400) / 2, hight - 85, over);		
		
	}
	
	
	private Image getImagenLogo (String rutaLogo, float percent, float pos_x, float pos_y) throws BadElementException, MalformedURLException, IOException {
		Image img = null;
		if (rutaLogo!=null && !rutaLogo.equals("")){

			File f  = new File(rutaLogo);
			if ( f.exists()) {

				img=Image.getInstance(rutaLogo);
				img.scalePercent(percent);
				img.setAbsolutePosition(pos_x, pos_y);				

			}
		}
		
		return img;
	}
	
	private void pintarLogo (String rutaLogo, PdfContentByte pdfContentByte) throws MalformedURLException, IOException, DocumentException {

		float percent = 15;
		float pos_x = 15;
		
		//PdfDocument pdfDocument = stamp.getOverContent(numPag).getPdfDocument();
		PdfDocument pdfDocument = pdfContentByte.getPdfDocument();
		
		float pos_y = pdfDocument.top() - 10;
		
		Image imagenLogo = getImagenLogo (rutaLogo, percent, pos_x, pos_y);
		
		//stamp.getOverContent(numPag).addImage(imagenLogo);
		pdfContentByte.addImage(imagenLogo);
	}


	private void escribirLateral (String lateral, float height, PdfContentByte over) {
		Font fontLateral = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,7, Font.NORMAL);

		PdfPTable header = new PdfPTable(1);		
		
		header.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);		
		header.getDefaultCell().setRotation(90);
		header.getDefaultCell().setBorder(0);
		header.setTotalWidth(40);

		//Paragraph p = new Paragraph (lateral, fontLateral);
		//p.setAlignment(Element.ALIGN_CENTER);
		PdfPCell cell;
		cell = new PdfPCell(new Phrase(lateral,fontLateral));
		//cell = new PdfPCell(p);
		cell.setRotation(90);
		cell.setBorder(0);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		

		header.addCell(cell);
		//header.writeSelectedRows(0, -1,15,(reader.getPageSizeWithRotation(1).getHeight()/2)+300, over);
		
		header.writeSelectedRows(0, -1,15,(height/2)+ lateral.length() + 150, over);
		//header.writeSelectedRows(0, -1,15,(height/2)+370, over);

	}


	private void tratarTablaInfo(CopiaInfo copia,  boolean simple,  PdfPTable footer) throws DocumentException, IOException 
	{

		TextosProperties tprop = TextosProperties.getInstance();

		Font fontStyleHeaders = null;
		Font fontTexto = null;


		if (simple==true) {
		
			fontStyleHeaders = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.UNDERLINE);
			fontTexto = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,8, Font.BOLD);

			String all = "";
			String all2="";

			String cabecera = trataCabecera (copia.getTituloCSV(), tprop.getProperty("simple.cabecera.csv"));
			String concatenar = tratarParCabeceraValor (cabecera, CSVUtil.formatCSV(copia.getCsv()));
			all += concatenar;

			cabecera = trataCabecera (copia.getTituloFecha(), tprop.getProperty("simple.cabecera.fecha"));
			concatenar = tratarParCabeceraValor (cabecera, copia.getFecha());			 
			all += concatenar;

			if (all.endsWith(" | ")) {
				all = all.substring(0, all.length()-3);
			}

			cabecera = trataCabecera (copia.getTituloExpediente(), tprop.getProperty("simple.cabecera.expediente"));
			concatenar = tratarParCabeceraValor (cabecera, copia.getExpediente());			 
			all2 += concatenar;

			cabecera = trataCabecera (copia.getTituloURL(), tprop.getProperty("simple.cabecera.url"));
			concatenar = tratarParCabeceraValor (cabecera, tratarURL(copia.getUrlSede()));			 
			all2 += concatenar;

			cabecera = trataCabecera (copia.getTituloNif(), tprop.getProperty("simple.cabecera.nif"));
			concatenar = tratarParCabeceraValor (cabecera, copia.getNif());			 
			all2 += concatenar;

			if (all2.endsWith(" | ")) {
				all2 = all2.substring(0, all2.length()-3);
			}

			//footer.addCell(new Phrase("",fontTexto));
			footer.addCell(new Phrase(all,fontTexto));
			footer.addCell(new Phrase(all2,fontStyleHeaders));
			
		} else { 
			fontStyleHeaders = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.UNDERLINE);
			fontTexto = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.BOLD);

			footer.addCell(new Phrase(trataCabecera(copia.getTituloAplicacion(), tprop.getProperty("complejo.cabecera.aplicacion")),fontStyleHeaders));			
			footer.addCell(new Phrase(trataCabecera(copia.getTituloCSV(), tprop.getProperty("complejo.cabecera.csv")),fontStyleHeaders));
			footer.addCell(new Phrase(trataCabecera(copia.getTituloFecha(), tprop.getProperty("complejo.cabecera.fecha")),fontStyleHeaders));


			footer.addCell(new Phrase(copia.getIdAplicacion(),fontTexto));
			footer.addCell(new Phrase(CSVUtil.formatCSV(copia.getCsv()),fontTexto));
			footer.addCell(new Phrase(copia.getFecha(),fontTexto));

			footer.addCell(new Phrase(trataCabecera(copia.getTituloExpediente(), tprop.getProperty("complejo.cabecera.expediente")),fontStyleHeaders));
			footer.addCell(new Phrase(trataCabecera(copia.getTituloURL(), tprop.getProperty("complejo.cabecera.url")),fontStyleHeaders));
			footer.addCell(new Phrase(trataCabecera(copia.getTituloNif(), tprop.getProperty("complejo.cabecera.nif")),fontStyleHeaders));


			footer.addCell(new Phrase(copia.getExpediente(),fontTexto));
			footer.addCell(parrafoURL(tratarURL(copia.getUrlSede()), fontTexto));
			//footer.addCell(new Phrase(tratarURL(copia.getUrlSede()),fontTexto));
			
			/*String url = tratarURL(copia.getUrlSede());
			Paragraph p = new Paragraph();
		    Anchor anchor = new Anchor(url, fontTexto);	          
		    anchor.setReference(url);
	      
	      	p.add(anchor);
			footer.addCell(p);*/
			
			footer.addCell(new Phrase(copia.getNif(),fontTexto));
		}
	}



	private void tratarTablaFirmantes(List<FirmaInfo> listaFirmas,  int iInicial, int iFinal, CopiaInfo copia, boolean simple,  PdfPTable footer) throws DocumentException, IOException
	{
		TextosProperties tprop = TextosProperties.getInstance(); 

		Font fontStyleHeaders = null;
		Font fontTexto = null;


		if (simple==true) {
		
			fontStyleHeaders = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.UNDERLINE);
			fontTexto = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.BOLD);

			for (int i = iInicial; i < iFinal; i++) 
			{
				FirmaInfo f = listaFirmas.get(i);

				String app1 = f.getApellido1();
				String app2 = f.getApellido2();
				String nombre = f.getNombre();
				String fecha = f.getFecha();
				String nif = f.getNifcif();

				StringBuffer allBuff = new StringBuffer().append(tprop.getProperty("simple.firmantes.firmante") + "("+(i+1)+") : ");
				allBuff.append(nombre+" "+app1+ " "+app2 + " | ");
				if (nif != null && !nif.contentEquals("")) {
					allBuff.append(tprop.getProperty("simple.firmantes.nif") + ":" + nif + " | "); 
				}

				if (StringUtils.isNotEmpty(f.getExtras())) {
					allBuff.append(tprop.getProperty("simple.firmantes.fecha") + " : " + fecha + " | " + tprop.getProperty("simple.firmantes.notas") + " : " + f.getExtras());
				} else {
					allBuff.append(tprop.getProperty("simple.firmantes.fecha") + " : " + fecha);
				}

				footer.addCell(new Phrase(allBuff.toString(),fontTexto));


			}

		} else { 
		
			fontStyleHeaders = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.UNDERLINE);
			fontTexto = FontFactory.getFont(FontFactory.HELVETICA,"ISO-8859-1" ,6, Font.BOLD);

			boolean algunNif = algunNif(listaFirmas);

			footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.firmante"),fontStyleHeaders,1));
			footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.nombre"),fontStyleHeaders,1));
			footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.fecha"),fontStyleHeaders,1));
			if (algunNif) {
				footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.nif"),fontStyleHeaders,1));
			}
			footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.notas"),fontStyleHeaders,2));
			
			
			for (int i = iInicial; i < iFinal; i++) 
			{
				FirmaInfo f = listaFirmas.get(i);

				String app1 = f.getApellido1();
				String app2 = f.getApellido2();
				String nombre = f.getNombre();
				String fecha = f.getFecha();
				String nif = f.getNifcif();

				String extras = "";
				if (f.getExtras() != null) {
					extras = f.getExtras();
				}
				
				footer.addCell(createCellFirmantes(tprop.getProperty("complejo.firmantes.firmante")+ "["+(i+1)+"]",fontTexto,1));
				footer.addCell(createCellFirmantes(nombre + " " + app1 + " " + app2,fontTexto,1));
				footer.addCell(createCellFirmantes(fecha,fontTexto,1));
				if (algunNif) {
					footer.addCell(createCellFirmantes(nif,fontTexto,1));
				}
				footer.addCell(createCellFirmantes(extras,fontTexto,2));
		
			}

		}


	}

	/**
	 * Crea la celda de la tabla de los firmantes.
	 * @param texto
	 * @param font
	 * @param colspan
	 * @return
	 */
	private PdfPCell createCellFirmantes (String texto, Font font, int colspan) {
		PdfPCell cell = new PdfPCell ();
		cell.setColspan(colspan);
		cell.addElement(new Phrase (texto, font));
		cell.setBorder(PdfPCell.NO_BORDER);
		cell.setPaddingTop(-0.2f);
		return cell;
	}
	
	private Image createImageCodeBar (String codigo, PdfContentByte pdfContentByte) {
		
		Barcode128 code128 = new Barcode128();
		code128.setCode(codigo);
		code128.setStartStopText(true);
		code128.setGenerateChecksum(false);
		code128.setExtended(true);
		code128.setBarHeight(20);

		Image imgCodeBar=code128.createImageWithBarcode(pdfContentByte,BaseColor.BLACK,BaseColor.BLACK);
		imgCodeBar.scalePercent(100);
		return imgCodeBar;
	}

	private PdfPTable createWideTable (int columnas, float width) {
		PdfPTable footer = new PdfPTable(columnas);
		configureTable (footer, width);
		return footer;

	}
	
	private PdfPTable createWideTableRelativeWidths(float[] relativeWidths, float width) {
		PdfPTable footer = new PdfPTable(relativeWidths);
		configureTable (footer, width);
		return footer;
	}
		
	private void configureTable (PdfPTable table, float width) {
		table.setTotalWidth(width);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setBorderWidth(0);
	}
	
	private boolean algunNif (List<FirmaInfo> listFirmaInfo) {


		boolean nifEncontrado = false;

		if (listFirmaInfo != null) {
			int i=0;
			while (i < listFirmaInfo.size() && !nifEncontrado) {
				if (listFirmaInfo.get(i).getNifcif() != null &&
						!listFirmaInfo.get(i).getNifcif().contentEquals("")) {
					nifEncontrado = true;
				} else {
					i++;
				}
			}
		}

		return nifEncontrado;

	}
	
	
		
	
	private String trataCabecera (String recibido, String defecto) {
		String cabecera = null;
		if (recibido == null || ("").equals(recibido)) {
			cabecera = defecto;
		} else if (recibido.equalsIgnoreCase(EMPTY)) {
			cabecera = "";
		} else {
			cabecera = recibido;
		}
		return cabecera;
	}
	
	private String tratarParCabeceraValor (String cabecera, String valor) {
		String resultado = "";
		// Si alguno de los dos no es vacÃ­o, se escribe la cabecera-valor de el campo.
		if (!("").equalsIgnoreCase(cabecera) && (!("").equalsIgnoreCase(valor) && valor!=null)) {
			resultado = cabecera + " : " + valor + " | ";
		} else {
			if (!("").equalsIgnoreCase(cabecera)) {
				resultado = cabecera + " | ";
			}
			if (!("").equalsIgnoreCase(valor) && valor!=null) {
				resultado = valor + " | ";
			}
		}
		return resultado;
	}
	
	private Paragraph parrafoURL (String url, Font font) {
		Paragraph p = new Paragraph();
	    Anchor anchor = new Anchor(url, font);	          
	    anchor.setReference(url);      
      	p.add(anchor);
      	return p;
	}
	
	private String tratarURL (String valor) {
		String resultado = "";
		if (!valor.contentEquals("EMPTY")) {
			resultado = valor;
		}
		return resultado;
	}
	
	private PdfPCell createCodeBarCell (Image imgCodeBar) {
		PdfPCell cell2 = new PdfPCell(imgCodeBar);
    	cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
    	cell2.setColspan(3);
    	cell2.setBorderWidth(0);
    	return cell2;
    	
	}
	
	
	private void pintarQR (String url, PdfContentByte pdfContentByte) throws DocumentException, IOException {
		
		byte[] qr = createQR (url, 15, 15);
		float percent = 75;
		
		//PdfDocument pdfDocument = stamp.getOverContent(numPag).getPdfDocument();
		PdfDocument pdfDocument = pdfContentByte.getPdfDocument();
		
		// Si lo queremos mas a la derecha disminuimos el segundo n�mero.
		float pos_x = pdfDocument.left() + 101;
		// Si lo queremos mas abajo aumentamos el segundo n�mero.
		float pos_y = pdfDocument.bottom() - 14;
		
		
		
		Image imagenQr = getImagenQr (qr, percent, pos_x, pos_y);
		
		//stamp.getOverContent(numPag).addImage(imagenLogo);
		pdfContentByte.addImage(imagenQr);
		
	}
	
	private Image getImagenQr (byte[] qr, float percent, float pos_x, float pos_y) throws IOException, BadElementException {
		Image img=Image.getInstance(qr);		
		img.scalePercent(percent);		
		img.setAbsolutePosition(pos_x, pos_y);
		return img;
	}
	
	private byte[] createQR (String url, int width, int height) {	
		 //String sUrl = "https://portafirma.seap.minhap.es";
		 ByteArrayOutputStream out = QRCode.from(url).to(ImageType.JPG).withSize(width, height).stream();
		 return out.toByteArray();
	}
	
//	public static void main (String[] args) {
//		
//		try {
//			File fi = new File("D:/tmp/Lorem_ipsum.pdf");
//			
//			CopiaInfo ci = new CopiaInfo ();
//			
//			ci.setCsv("12345656666");
//			ci.setUrlSede("EMPTY");
//			
//			ListaFirmaInfo lista = new ListaFirmaInfo ();
//			List<FirmaInfo> informacionFirmas = new ArrayList<FirmaInfo> ();
//			
//			FirmaInfo finfo = new FirmaInfo ();
//			finfo.setNombre("Rus");
//			finfo.setApellido1("Poves");
//			finfo.setNifcif("47492294P");
//			
//			informacionFirmas.add(finfo);
//			lista.setInformacionFirmas(informacionFirmas);
//			
//			PdfCopiaNormalizadaUtils pdfCopiaNormalizadaUtils = new PdfCopiaNormalizadaUtils();
//			
//			File fo = pdfCopiaNormalizadaUtils.copiaAutenticaNormalizada(fi, ci, "", true, lista, 0);
//			
//			System.out.println("Fin de la prueba.");
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
}
