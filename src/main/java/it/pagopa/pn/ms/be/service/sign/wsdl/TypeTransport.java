
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per typeTransport.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="typeTransport"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="BYNARYNET"/&gt;
 *     &lt;enumeration value="FILENAME"/&gt;
 *     &lt;enumeration value="DIRECTORYNAME"/&gt;
 *     &lt;enumeration value="STREAM"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "typeTransport")
@XmlEnum
public enum TypeTransport {

    BYNARYNET,
    FILENAME,
    DIRECTORYNAME,
    STREAM;

    public String value() {
        return name();
    }

    public static TypeTransport fromValue(String v) {
        return valueOf(v);
    }

}
