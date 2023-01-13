
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per retriveCredentialReturn complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="retriveCredentialReturn"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="blob" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="return_code" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="textvalue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retriveCredentialReturn", propOrder = {
    "blob",
    "description",
    "returnCode",
    "status",
    "textvalue"
})
public class RetriveCredentialReturn {

    protected byte[] blob;
    protected String description;
    @XmlElement(name = "return_code")
    protected String returnCode;
    protected String status;
    protected String textvalue;

    /**
     * Recupera il valore della proprietà blob.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getBlob() {
        return blob;
    }

    /**
     * Imposta il valore della proprietà blob.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setBlob(byte[] value) {
        this.blob = value;
    }

    /**
     * Recupera il valore della proprietà description.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Imposta il valore della proprietà description.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Recupera il valore della proprietà returnCode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReturnCode() {
        return returnCode;
    }

    /**
     * Imposta il valore della proprietà returnCode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReturnCode(String value) {
        this.returnCode = value;
    }

    /**
     * Recupera il valore della proprietà status.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * Imposta il valore della proprietà status.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Recupera il valore della proprietà textvalue.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextvalue() {
        return textvalue;
    }

    /**
     * Imposta il valore della proprietà textvalue.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextvalue(String value) {
        this.textvalue = value;
    }

}
