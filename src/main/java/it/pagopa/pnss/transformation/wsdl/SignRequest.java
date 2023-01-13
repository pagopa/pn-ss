
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per signRequest complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="signRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="bynaryinput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="certID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dstNmae" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="identity" type="{http://arubasignservice.arubapec.it/}auth"/&gt;
 *         &lt;element name="notity_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="notitymail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="srcName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="transport" type="{http://arubasignservice.arubapec.it/}typeTransport"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signRequest", propOrder = {
    "bynaryinput",
    "certID",
    "dstNmae",
    "identity",
    "notityId",
    "notitymail",
    "srcName",
    "transport"
})
public class SignRequest {

    protected byte[] bynaryinput;
    @XmlElement(required = true)
    protected String certID;
    protected String dstNmae;
    @XmlElement(required = true)
    protected Auth identity;
    @XmlElement(name = "notity_id")
    protected String notityId;
    protected String notitymail;
    protected String srcName;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected TypeTransport transport;

    /**
     * Recupera il valore della proprietà bynaryinput.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getBynaryinput() {
        return bynaryinput;
    }

    /**
     * Imposta il valore della proprietà bynaryinput.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setBynaryinput(byte[] value) {
        this.bynaryinput = value;
    }

    /**
     * Recupera il valore della proprietà certID.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCertID() {
        return certID;
    }

    /**
     * Imposta il valore della proprietà certID.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCertID(String value) {
        this.certID = value;
    }

    /**
     * Recupera il valore della proprietà dstNmae.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDstNmae() {
        return dstNmae;
    }

    /**
     * Imposta il valore della proprietà dstNmae.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDstNmae(String value) {
        this.dstNmae = value;
    }

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
     * Recupera il valore della proprietà notityId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotityId() {
        return notityId;
    }

    /**
     * Imposta il valore della proprietà notityId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotityId(String value) {
        this.notityId = value;
    }

    /**
     * Recupera il valore della proprietà notitymail.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotitymail() {
        return notitymail;
    }

    /**
     * Imposta il valore della proprietà notitymail.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotitymail(String value) {
        this.notitymail = value;
    }

    /**
     * Recupera il valore della proprietà srcName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSrcName() {
        return srcName;
    }

    /**
     * Imposta il valore della proprietà srcName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSrcName(String value) {
        this.srcName = value;
    }

    /**
     * Recupera il valore della proprietà transport.
     * 
     * @return
     *     possible object is
     *     {@link TypeTransport }
     *     
     */
    public TypeTransport getTransport() {
        return transport;
    }

    /**
     * Imposta il valore della proprietà transport.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeTransport }
     *     
     */
    public void setTransport(TypeTransport value) {
        this.transport = value;
    }

}
