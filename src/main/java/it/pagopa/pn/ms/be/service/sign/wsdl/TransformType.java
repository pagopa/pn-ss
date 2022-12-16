
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per transformType.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="transformType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="CANONICAL_WITH_COMMENT"/&gt;
 *     &lt;enumeration value="CANONICAL_OMIT_COMMENT"/&gt;
 *     &lt;enumeration value="BASE64"/&gt;
 *     &lt;enumeration value="XPATH2_INTERSECT"/&gt;
 *     &lt;enumeration value="XPATH2_SUBTRACT"/&gt;
 *     &lt;enumeration value="XPATH2_UNION"/&gt;
 *     &lt;enumeration value="XSLT"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "transformType")
@XmlEnum
public enum TransformType {

    CANONICAL_WITH_COMMENT("CANONICAL_WITH_COMMENT"),
    CANONICAL_OMIT_COMMENT("CANONICAL_OMIT_COMMENT"),
    @XmlEnumValue("BASE64")
    BASE_64("BASE64"),
    @XmlEnumValue("XPATH2_INTERSECT")
    XPATH_2_INTERSECT("XPATH2_INTERSECT"),
    @XmlEnumValue("XPATH2_SUBTRACT")
    XPATH_2_SUBTRACT("XPATH2_SUBTRACT"),
    @XmlEnumValue("XPATH2_UNION")
    XPATH_2_UNION("XPATH2_UNION"),
    XSLT("XSLT");
    private final String value;

    TransformType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TransformType fromValue(String v) {
        for (TransformType c: TransformType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
