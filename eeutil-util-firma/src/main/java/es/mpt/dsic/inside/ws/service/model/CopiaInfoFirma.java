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

package es.mpt.dsic.inside.ws.service.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="copiaInfoFirma", propOrder={"firma", "tipoFirma", "incluirCSV", "incluirFirmantes",
		"incluirNifFirmantes", "documentoEnFirma", "documento", "incluirCSVEnRespuesta",
		"idAplicacion", "fecha", "expediente", "nif", "urlSede",
		"tituloAplicacion", "tituloCSV", "tituloFecha", "tituloExpediente", "tituloNif",
		"tituloURL", "estamparLogo", "lateral", "opcionesPagina"})
public class CopiaInfoFirma {

	@XmlElement(required=true,name="firma")
	private byte[] firma;
	@XmlElement(required=true,name="tipoFirma")
	private String tipoFirma;
	@XmlElement(required=false,name="incluirCSV")
	private boolean incluirCSV;
	@XmlElement(required=false,name="incluirFirmantes")
	private boolean incluirFirmantes;
	@XmlElement(required=false, name="incluirNifFirmantes")
	private boolean incluirNifFirmantes;
	@XmlElement(required=false,name="documentoEnFirma")
	private boolean documentoEnFirma;
	@XmlElement(required=false,name="documento")
	private byte[] documento;
	@XmlElement(required=false,name="incluirCSVEnRespuesta")
	private Boolean incluirCSVEnRespuesta;
	
	
	@XmlElement(required=false,name="idAplicacion")
	private String idAplicacion;	
	@XmlElement(required=false,name="fecha")
	private String fecha;
	@XmlElement(required=false,name="expediente")
	private String expediente;
	@XmlElement(required=false,name="nif")
	private String nif;
	@XmlElement(required=false,name="urlSede", defaultValue="https://sede.administracionespublicas.gob.es/valida")	
	private String urlSede;
	
	@XmlElement (required=false,name="tituloAplicacion")
	private String tituloAplicacion;
	@XmlElement (required=false,name="tituloCSV")
	private String tituloCSV;
	@XmlElement (required=false,name="tituloFecha")
	private String tituloFecha;
	@XmlElement (required=false,name="tituloExpediente")
	private String tituloExpediente;
	@XmlElement (required=false,name="tituloNif")
	private String tituloNif;
	@XmlElement (required=false,name="tituloURL")
	private String tituloURL;	
	@XmlElement(required=false,name = "estamparLogo")
	private boolean estamparLogo;
	@XmlElement(required=false,name = "lateral")
	private String lateral;
	
	@XmlElement(required=false, name="opcionesPagina")
	private OpcionesPagina opcionesPagina;
	
	
	
	
	public byte[] getFirma() {
		return firma;
	}
	public void getFirma(byte[] firma) {
		this.firma = firma;
	}
	public String getTipoFirma() {
		return tipoFirma;
	}
	public void setTipoFirma (String tipoFirma) {
		this.tipoFirma = tipoFirma;
	}
	public boolean isIncluirCSV() {
		return incluirCSV;
	}
	public void setIncluirCSV(boolean incluirCSV) {
		this.incluirCSV = incluirCSV;
	}
	public boolean isIncluirFirmantes() {
		return incluirFirmantes;
	}
	public void setIncluirFirmantes(boolean incluirFirmantes) {
		this.incluirFirmantes = incluirFirmantes;
	}
	public boolean isIncluirNifFirmantes() {
		return incluirNifFirmantes;
	}
	public void setIncluirNifFirmantes(boolean incluirNifFirmantes) {
		this.incluirNifFirmantes = incluirNifFirmantes;
	}
	
	public boolean isDocumentoEnFirma() {
		return documentoEnFirma;
	}
	public void setDocumentoEnFirma(boolean documentoEnFirma) {
		this.documentoEnFirma = documentoEnFirma;
	}
	public byte[] getDocumento() {
		return documento;
	}
	public void setDocumento(byte[] documento) {
		this.documento = documento;
	}	
	public Boolean isIncluirCSVEnRespuesta() {
		return incluirCSVEnRespuesta;
	}
	public void setIncluirCSVEnRespuesta(Boolean incluirCSVEnRespuesta) {
		this.incluirCSVEnRespuesta = incluirCSVEnRespuesta;
	}
	public void setFirma(byte[] firma) {
		this.firma = firma;
	}
	public String getIdAplicacion() {
		return idAplicacion;
	}
	public void setIdAplicacion(String idAplicacion) {
		this.idAplicacion = idAplicacion;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public String getExpediente() {
		return expediente;
	}
	public void setExpediente(String expediente) {
		this.expediente = expediente;
	}
	public String getNif() {
		return nif;
	}
	public void setNif(String nif) {
		this.nif = nif;
	}
	public String getUrlSede() {
		return urlSede;
	}
	public void setUrlSede(String urlSede) {
		this.urlSede = urlSede;
	}
	public String getTituloAplicacion() {
		return tituloAplicacion;
	}
	public void setTituloAplicacion(String tituloAplicacion) {
		this.tituloAplicacion = tituloAplicacion;
	}
	public String getTituloCSV() {
		return tituloCSV;
	}
	public void setTituloCSV(String tituloCSV) {
		this.tituloCSV = tituloCSV;
	}
	public String getTituloFecha() {
		return tituloFecha;
	}
	public void setTituloFecha(String tituloFecha) {
		this.tituloFecha = tituloFecha;
	}
	public String getTituloExpediente() {
		return tituloExpediente;
	}
	public void setTituloExpediente(String tituloExpediente) {
		this.tituloExpediente = tituloExpediente;
	}
	public String getTituloNif() {
		return tituloNif;
	}
	public void setTituloNif(String tituloNif) {
		this.tituloNif = tituloNif;
	}
	public String getTituloURL() {
		return tituloURL;
	}
	public void setTituloURL(String tituloURL) {
		this.tituloURL = tituloURL;
	}
	public boolean isEstamparLogo() {
		return estamparLogo;
	}
	public void setEstamparLogo(boolean estamparLogo) {
		this.estamparLogo = estamparLogo;
	}
	public String getLateral() {
		return lateral;
	}
	public void setLateral(String lateral) {
		this.lateral = lateral;
	}
	public OpcionesPagina getOpcionesPagina() {
		return opcionesPagina;
	}
	public void setOpcionesPagina(OpcionesPagina opcionesPagina) {
		this.opcionesPagina = opcionesPagina;
	}
	
	
		 
	
}
