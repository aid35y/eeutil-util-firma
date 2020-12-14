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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="ObjectCopiaFirmaCSV", propOrder={"ambito" ,"generarCSV", "normalizado","simple","dir3"})
public class ObjectCopiaFirmaCSV implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@XmlElement(required = true, name = "ambito")
	private String ambito;
	
	@XmlElement(required = true, name = "generarCSV")
	private Boolean generarCSV;
	
	@XmlElement(required = true, name = "normalizado")
	private Boolean normalizado;
	
	@XmlElement(required = true, name = "simple")
	private Boolean simple;
	
	@XmlElement(required = true, name = "dir3")
	private String dir3;
	

	public String getAmbito() {
		return ambito;
	}

	public void setAmbito(String ambito) {
		this.ambito = ambito;
	}

	public Boolean getGenerarCSV() {
		return generarCSV;
	}

	public void setGenerarCSV(Boolean generarCSV) {
		this.generarCSV = generarCSV;
	}

	public Boolean getNormalizado() {
		return normalizado;
	}

	public void setNormalizado(Boolean normalizado) {
		this.normalizado = normalizado;
	}

	public Boolean getSimple() {
		return simple;
	}

	public void setSimple(Boolean simple) {
		this.simple = simple;
	}

	public String getDir3() {
		return dir3;
	}

	public void setDir3(String dir3) {
		this.dir3 = dir3;
	}


}
