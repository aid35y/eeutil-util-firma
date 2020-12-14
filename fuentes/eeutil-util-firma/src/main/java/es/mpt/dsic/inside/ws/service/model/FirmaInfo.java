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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FirmaInfo",
    propOrder = {"nifcif", "nombre", "apellido1", "apellido2", "fecha", "extras"})

public class FirmaInfo implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @XmlElement(required = true, name = "nifcif")
  private String nifcif;
  @XmlElement(required = true, name = "nombre")
  private String nombre;
  @XmlElement(required = true, name = "apellido1")
  private String apellido1;
  @XmlElement(required = true, name = "apellido2")
  private String apellido2;
  @XmlElement(required = true, name = "fecha")
  private String fecha;
  @XmlElement(required = false, name = "extras")
  private String extras;


  public String getNifcif() {
    return nifcif;
  }

  public void setNifcif(String nifcif) {
    this.nifcif = nifcif;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellido1() {
    return apellido1;
  }

  public void setApellido1(String apellido1) {
    this.apellido1 = apellido1;
  }

  public String getApellido2() {
    return apellido2;
  }

  public void setApellido2(String apellido2) {
    this.apellido2 = apellido2;
  }

  public String getFecha() {
    return fecha;
  }

  public void setFecha(String fecha) {
    this.fecha = fecha;
  }

  public String getExtras() {
    return extras;
  }

  public void setExtras(String extras) {
    this.extras = extras;
  }

  @Override
  public String toString() {
    return "FimaInfo [nifcif=" + nifcif + ", nombre=" + nombre + ", apellido1=" + apellido1
        + ", apellido2=" + apellido2 + ", fecha=" + fecha + ", extras=" + extras + "]";
  }



}
