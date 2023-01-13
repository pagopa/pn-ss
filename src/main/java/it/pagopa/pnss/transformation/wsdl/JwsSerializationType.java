
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per jwsSerializationType.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="jwsSerializationType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="COMPACT"/&gt;
 *     &lt;enumeration value="JSON_FLATTENED"/&gt;
 *     &lt;enumeration value="JSON"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "jwsSerializationType")
@XmlEnum
public enum JwsSerializationType {

    COMPACT,
    JSON_FLATTENED,
    JSON;

    public String value() {
        return name();
    }

    public static JwsSerializationType fromValue(String v) {
        return valueOf(v);
    }

}
