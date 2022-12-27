
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per verifyRequest complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="verifyRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="binaryinput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="dstName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="notity_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="notitymail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="srcName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="stream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="transport" type="{http://arubasignservice.arubapec.it/}typeTransport" minOccurs="0"/&gt;
 *         &lt;element name="type" type="{http://arubasignservice.arubapec.it/}documentType" minOccurs="0"/&gt;
 *         &lt;element name="verdate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "verifyRequest", propOrder = {
    "binaryinput",
    "dstName",
    "notityId",
    "notitymail",
    "srcName",
    "stream",
    "transport",
    "type",
    "verdate"
})
public class VerifyRequest {

    protected byte[] binaryinput;
    protected String dstName;
    @XmlElement(name = "notity_id")
    protected String notityId;
    protected String notitymail;
    protected String srcName;
    @XmlMimeType("application/octet-stream")
    protected DataHandler stream;
    @XmlSchemaType(name = "string")
    protected TypeTransport transport;
    @XmlSchemaType(name = "string")
    protected DocumentType type;
    protected String verdate;

    /**
     * Recupera il valore della proprietà binaryinput.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getBinaryinput() {
        return binaryinput;
    }

    /**
     * Imposta il valore della proprietà binaryinput.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setBinaryinput(byte[] value) {
        this.binaryinput = value;
    }

    /**
     * Recupera il valore della proprietà dstName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDstName() {
        return dstName;
    }

    /**
     * Imposta il valore della proprietà dstName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDstName(String value) {
        this.dstName = value;
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
     * Recupera il valore della proprietà stream.
     * 
     * @return
     *     possible object is
     *     {@link DataHandler }
     *     
     */
    public DataHandler getStream() {
        return stream;
    }

    /**
     * Imposta il valore della proprietà stream.
     * 
     * @param value
     *     allowed object is
     *     {@link DataHandler }
     *     
     */
    public void setStream(DataHandler value) {
        this.stream = value;
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

    /**
     * Recupera il valore della proprietà type.
     * 
     * @return
     *     possible object is
     *     {@link DocumentType }
     *     
     */
    public DocumentType getType() {
        return type;
    }

    /**
     * Imposta il valore della proprietà type.
     * 
     * @param value
     *     allowed object is
     *     {@link DocumentType }
     *     
     */
    public void setType(DocumentType value) {
        this.type = value;
    }

    /**
     * Recupera il valore della proprietà verdate.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVerdate() {
        return verdate;
    }

    /**
     * Imposta il valore della proprietà verdate.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVerdate(String value) {
        this.verdate = value;
    }

}
