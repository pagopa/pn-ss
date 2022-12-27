
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per xmlSignatureType.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="xmlSignatureType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="XMLENVELOPED"/&gt;
 *     &lt;enumeration value="XMLENVELOPING"/&gt;
 *     &lt;enumeration value="XMLDETACHED_INTERNAL"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "xmlSignatureType")
@XmlEnum
public enum XmlSignatureType {

    XMLENVELOPED,
    XMLENVELOPING,
    XMLDETACHED_INTERNAL;

    public String value() {
        return name();
    }

    public static XmlSignatureType fromValue(String v) {
        return valueOf(v);
    }

}
