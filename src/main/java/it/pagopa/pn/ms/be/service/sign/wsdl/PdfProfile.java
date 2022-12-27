
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pdfProfile.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="pdfProfile"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="BASIC"/&gt;
 *     &lt;enumeration value="PADESBES"/&gt;
 *     &lt;enumeration value="PADESLTV"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "pdfProfile")
@XmlEnum
public enum PdfProfile {

    BASIC,
    PADESBES,
    PADESLTV;

    public String value() {
        return name();
    }

    public static PdfProfile fromValue(String v) {
        return valueOf(v);
    }

}
