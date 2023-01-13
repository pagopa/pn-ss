
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per jwsSignatureParameter complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="jwsSignatureParameter"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="jwtId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="audience" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="jwsSerializationType" type="{http://arubasignservice.arubapec.it/}jwsSerializationType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "jwsSignatureParameter", propOrder = {
    "jwtId",
    "audience",
    "jwsSerializationType"
})
public class JwsSignatureParameter {

    protected String jwtId;
    protected String audience;
    @XmlSchemaType(name = "string")
    protected JwsSerializationType jwsSerializationType;

    /**
     * Recupera il valore della proprietà jwtId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJwtId() {
        return jwtId;
    }

    /**
     * Imposta il valore della proprietà jwtId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJwtId(String value) {
        this.jwtId = value;
    }

    /**
     * Recupera il valore della proprietà audience.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudience() {
        return audience;
    }

    /**
     * Imposta il valore della proprietà audience.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudience(String value) {
        this.audience = value;
    }

    /**
     * Recupera il valore della proprietà jwsSerializationType.
     * 
     * @return
     *     possible object is
     *     {@link JwsSerializationType }
     *     
     */
    public JwsSerializationType getJwsSerializationType() {
        return jwsSerializationType;
    }

    /**
     * Imposta il valore della proprietà jwsSerializationType.
     * 
     * @param value
     *     allowed object is
     *     {@link JwsSerializationType }
     *     
     */
    public void setJwsSerializationType(JwsSerializationType value) {
        this.jwsSerializationType = value;
    }

}
