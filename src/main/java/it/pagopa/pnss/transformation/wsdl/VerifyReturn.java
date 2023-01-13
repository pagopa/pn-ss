
package it.pagopa.pnss.transformation.wsdl;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per verifyReturn complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="verifyReturn"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="binaryoutput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="dstPath" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="result" type="{http://arubasignservice.arubapec.it/}verifySignatures" minOccurs="0"/&gt;
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="stream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "verifyReturn", propOrder = {
    "binaryoutput",
    "description",
    "dstPath",
    "result",
    "status",
    "stream"
})
public class VerifyReturn {

    protected byte[] binaryoutput;
    protected String description;
    protected String dstPath;
    protected VerifySignatures result;
    protected String status;
    @XmlMimeType("application/octet-stream")
    protected DataHandler stream;

    /**
     * Recupera il valore della proprietà binaryoutput.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getBinaryoutput() {
        return binaryoutput;
    }

    /**
     * Imposta il valore della proprietà binaryoutput.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setBinaryoutput(byte[] value) {
        this.binaryoutput = value;
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
     * Recupera il valore della proprietà dstPath.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDstPath() {
        return dstPath;
    }

    /**
     * Imposta il valore della proprietà dstPath.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDstPath(String value) {
        this.dstPath = value;
    }

    /**
     * Recupera il valore della proprietà result.
     * 
     * @return
     *     possible object is
     *     {@link VerifySignatures }
     *     
     */
    public VerifySignatures getResult() {
        return result;
    }

    /**
     * Imposta il valore della proprietà result.
     * 
     * @param value
     *     allowed object is
     *     {@link VerifySignatures }
     *     
     */
    public void setResult(VerifySignatures value) {
        this.result = value;
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

}
