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

package es.mpt.dsic.inside.csv;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CSVUtil {

  public static final String CSV_SEPARATOR = "-";

  protected final static Log logger = LogFactory.getLog(CSVUtil.class);

  public static byte[] generateDigest(byte[] toDigest, String alg) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance(alg);
    return md.digest(toDigest);
  }

  public static byte[] generateRandomNumber(int numBytes) {
    /*
     * logger.debug("new SecureRandom()"); SecureRandom randomSeed = new SecureRandom();
     * logger.debug("randomSeed.generateSeed(numBytes)"); // la siguiente instruccion tarda 15
     * segundos en desarrollo... byte[] seed = randomSeed.generateSeed(numBytes);
     * logger.debug("new SecureRandom (seed)"); SecureRandom random = new SecureRandom (seed);
     * logger.debug("new byte[numBytes]"); byte[] randomBytes = new byte[numBytes];
     * logger.debug("random.nextBytes(randomBytes);"); random.nextBytes(randomBytes);
     */

    logger.debug("new SecureRandom()");
    SecureRandom random = new SecureRandom();
    logger.debug("new byte[numBytes];");
    byte randomBytes[] = new byte[numBytes];
    logger.debug("random.nextBytes(randomBytes);");
    random.nextBytes(randomBytes);

    return randomBytes;
  }

  public static Vector<byte[]> generateDigest(Vector<byte[]> toDigestList, String alg)
      throws NoSuchAlgorithmException {
    Vector<byte[]> digestList = new Vector<byte[]>();
    for (byte[] toDigest : toDigestList) {
      digestList.add(generateDigest(toDigest, alg));
    }
    return digestList;
  }

  public static String toBinaryString(byte b) {
    return ("0b" + ("0000000" + Integer.toBinaryString(0xFF & b)).replaceAll(".*(.{8})$", "$1"));
  }

  public static String toBinaryString(byte[] bytes) {
    StringBuffer sb = new StringBuffer("");

    for (byte b : bytes) {
      sb.append(toBinaryString(b));
    }
    return sb.toString().replaceAll("0b", "");
  }

  public static String caracteresPares(String cadena) {
    StringBuffer sb = new StringBuffer("");

    for (int i = 0; i < cadena.length(); i++) {
      if (i % 2 == 0) {
        sb.append(cadena.charAt(i));
      }
    }
    return sb.toString();
  }

  public static String bigBinaryToHex(String s) {
    int maxLength = 4;
    StringBuffer sb = new StringBuffer("");
    if (s.length() < maxLength) {
      sb.append(binaryToHex(rellenarCeros(s, maxLength)));
    } else {

      int trozos = s.length() / maxLength;
      if (s.length() % maxLength > 0) {
        s = rellenarCeros(s, maxLength * (trozos + 1));
        trozos++;
      }

      for (int i = 0; i < trozos; i++) {
        int i_inicial = i * maxLength;
        int i_final = i_inicial + maxLength;
        sb.append(binaryToHex(s.substring(i_inicial, i_final)));
      }

    }
    return sb.toString();
  }

  private static String binaryToHex(String s) {
    String hex = Long.toHexString(Long.parseLong(s, 2));
    return hex;
  }

  public static String rellenarCeros(String s, int length) {
    while (s.length() < length) {
      s = "0" + s;
    }
    return s;
  }

  /**
   * Recibe un CSV que puede incluir el �mbito o no.
   * 
   * @param ambitoCsv
   * @return
   */
  public static String formatCSV(String ambitoCsv) {
    StringTokenizer stk = new StringTokenizer(ambitoCsv, "-");
    // Primero suponemos que no se incluye el �mbito en el CSV, pero..
    String ambito = "";
    String csv = ambitoCsv;
    // .. si el �mbito est� incluido en el CSV recibido, se recupera convenientemente
    if (stk.countTokens() == 2) {
      ambito = stk.nextToken();
      csv = stk.nextToken();
    }
    // Se incluyen los guiones en el csv
    csv = formatCSV(csv, 4, "-");

    // Si el �mbito viene separado, hay que forzar la concatenaci�n
    return StringUtils.isNotBlank(ambito) ? ambito + CSV_SEPARATOR + csv : csv;
  }

  public static boolean hasScope(String ambitoCSV) {
    StringTokenizer stk = new StringTokenizer(ambitoCSV, "-");
    return stk.countTokens() == 2;
  }

  /**
   * Si encuentra el �mbito lo concatena en el campo de CSV
   * 
   * @param copia
   */
  public static String unificarAmbitoCSV(String ambito, String csv) {
    String retorno = csv;
    if (StringUtils.isNotEmpty(ambito)) {
      StringTokenizer stk = new StringTokenizer(csv, "-");
      if (stk.countTokens() == 1) {
        retorno = ambito + CSV_SEPARATOR + csv;
      }
    }
    return retorno;
  }

  /**
   * Formatea el csv: Cada "num" elementos escribe el String "ss" Ejemplo: csv="123456789", num=4,
   * ss="-" devolvería 1234-6789
   * 
   * @param csv csv a formatear
   * @param num cada cuantos elementos se desea escribir el String "ss"
   * @param ss cadena que se desea introducir
   * @return el csv formateado.
   */
  private static String formatCSV(String csv, int num, String ss) {
    StringBuilder result = new StringBuilder("");
    if (csv.length() < num) {
      result.append(csv);
      return result.toString();
    } else {
      int i = 0;
      while (i + num <= csv.length()) {
        String aux = csv.substring(i, i + num);
        result.append(ss);
        result.append(aux);
        i += num;
      }
      if (i < csv.length()) {
        String aux = csv.substring(i, csv.length());
        result.append(ss);
        result.append(aux);
      }
      return result.toString().substring(1);
    }

  }

  public static void main(String[] args) {
    // String numerito = "0000 0001 0010 0011 0100 0101 0110 0111 1000 1001 1010 1011 1100 1101 1110
    // 1111";
    String numerito =
        "01 0010 0011 0000 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101 1101";
    String hex = bigBinaryToHex(numerito.replaceAll(" ", ""));
    System.out.println(hex);
  }


}

