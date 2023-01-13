
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per encryptionAlgorithm.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="encryptionAlgorithm"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="DES_EDE3_CBC"/&gt;
 *     &lt;enumeration value="RC2_CBC"/&gt;
 *     &lt;enumeration value="IDEA_CBC"/&gt;
 *     &lt;enumeration value="CAST5_CBC"/&gt;
 *     &lt;enumeration value="AES128_CBC"/&gt;
 *     &lt;enumeration value="AES192_CBC"/&gt;
 *     &lt;enumeration value="AES256_CBC"/&gt;
 *     &lt;enumeration value="CAMELLIA128_CBC"/&gt;
 *     &lt;enumeration value="CAMELLIA192_CBC"/&gt;
 *     &lt;enumeration value="CAMELLIA256_CBC"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "encryptionAlgorithm")
@XmlEnum
public enum EncryptionAlgorithm {

    @XmlEnumValue("DES_EDE3_CBC")
    DES_EDE_3_CBC("DES_EDE3_CBC"),
    @XmlEnumValue("RC2_CBC")
    RC_2_CBC("RC2_CBC"),
    IDEA_CBC("IDEA_CBC"),
    @XmlEnumValue("CAST5_CBC")
    CAST_5_CBC("CAST5_CBC"),
    @XmlEnumValue("AES128_CBC")
    AES_128_CBC("AES128_CBC"),
    @XmlEnumValue("AES192_CBC")
    AES_192_CBC("AES192_CBC"),
    @XmlEnumValue("AES256_CBC")
    AES_256_CBC("AES256_CBC"),
    @XmlEnumValue("CAMELLIA128_CBC")
    CAMELLIA_128_CBC("CAMELLIA128_CBC"),
    @XmlEnumValue("CAMELLIA192_CBC")
    CAMELLIA_192_CBC("CAMELLIA192_CBC"),
    @XmlEnumValue("CAMELLIA256_CBC")
    CAMELLIA_256_CBC("CAMELLIA256_CBC");
    private final String value;

    EncryptionAlgorithm(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EncryptionAlgorithm fromValue(String v) {
        for (EncryptionAlgorithm c: EncryptionAlgorithm.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
