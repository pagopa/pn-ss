
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per sendCredential complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="sendCredential"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Identity" type="{http://arubasignservice.arubapec.it/}auth" minOccurs="0"/&gt;
 *         &lt;element name="type" type="{http://arubasignservice.arubapec.it/}credentialsType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sendCredential", propOrder = {
    "identity",
    "type"
})
public class SendCredential {

    @XmlElement(name = "Identity")
    protected Auth identity;
    @XmlSchemaType(name = "string")
    protected CredentialsType type;

    /**
     * Recupera il valore della proprietà identity.
     * 
     * @return
     *     possible object is
     *     {@link Auth }
     *     
     */
    public Auth getIdentity() {
        return identity;
    }

    /**
     * Imposta il valore della proprietà identity.
     * 
     * @param value
     *     allowed object is
     *     {@link Auth }
     *     
     */
    public void setIdentity(Auth value) {
        this.identity = value;
    }

    /**
     * Recupera il valore della proprietà type.
     * 
     * @return
     *     possible object is
     *     {@link CredentialsType }
     *     
     */
    public CredentialsType getType() {
        return type;
    }

    /**
     * Imposta il valore della proprietà type.
     * 
     * @param value
     *     allowed object is
     *     {@link CredentialsType }
     *     
     */
    public void setType(CredentialsType value) {
        this.type = value;
    }

}
