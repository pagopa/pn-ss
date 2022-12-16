
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per documentType.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="documentType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="PKCS7"/&gt;
 *     &lt;enumeration value="PDF"/&gt;
 *     &lt;enumeration value="XML"/&gt;
 *     &lt;enumeration value="TSD"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "documentType")
@XmlEnum
public enum DocumentType {

    @XmlEnumValue("PKCS7")
    PKCS_7("PKCS7"),
    PDF("PDF"),
    XML("XML"),
    TSD("TSD");
    private final String value;

    DocumentType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DocumentType fromValue(String v) {
        for (DocumentType c: DocumentType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
