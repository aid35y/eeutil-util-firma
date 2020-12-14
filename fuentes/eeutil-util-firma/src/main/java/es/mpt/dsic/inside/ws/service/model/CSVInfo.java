/*
 * Copyright (C) 2012-13 MINHAP, Gobierno de España This program is licensed and may be used,
 * modified and redistributed under the terms of the European Public License (EUPL), either version
 * 1.1 or (at your option) any later version as soon as they are approved by the European
 * Commission. Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * more details. You should have received a copy of the EUPL1.1 license along with this program; if
 * not, you may find it at http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 */

package es.mpt.dsic.inside.ws.service.model;

import java.io.Serializable;
import java.util.Arrays;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CSVInfo", propOrder = {"contenido", "mime", "contenidoFirmado"})

public class CSVInfo implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @XmlElement(required = true, name = "contenido")
  private byte[] contenido;
  @XmlElement(required = true, name = "mime")
  private String mime;
  @XmlElement(required = false, name = "contenidoFirmado")
  private byte[] contenidoFirmado;


  public byte[] getContenido() {
    return contenido;
  }

  public void setContenido(byte[] contenido) {
    this.contenido = contenido;
  }

  public String getMime() {
    return mime;
  }

  public void setMime(String mime) {
    this.mime = mime;
  }

  public byte[] getContenidoFirmado() {
    return contenidoFirmado;
  }

  public void setContenidoFirmado(byte[] contenidoFirmado) {
    this.contenidoFirmado = contenidoFirmado;
  }

  @Override
  public String toString() {
    return "CSVInfo [contenido=" + Arrays.toString(contenido) + ", mime=" + mime + "]";
  }



}
