
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per UpdateSignatureRequest complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="UpdateSignatureRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="transport" type="{http://arubasignservice.arubapec.it/}typeTransport"/&gt;
 *         &lt;element name="binaryinput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="originalDataBinaryinput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="stream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="originalDataStream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="srcName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="originalDataSrcName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="dstName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signatureType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signatureLevel" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="signaturePath" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="returnder" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="tsa_identity" type="{http://arubasignservice.arubapec.it/}tsaAuth" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UpdateSignatureRequest", propOrder = {
    "transport",
    "binaryinput",
    "originalDataBinaryinput",
    "stream",
    "originalDataStream",
    "srcName",
    "originalDataSrcName",
    "dstName",
    "signatureType",
    "signatureLevel",
    "signaturePath",
    "returnder",
    "tsaIdentity"
})
public class UpdateSignatureRequest {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected TypeTransport transport;
    protected byte[] binaryinput;
    protected byte[] originalDataBinaryinput;
    @XmlMimeType("application/octet-stream")
    protected DataHandler stream;
    @XmlMimeType("application/octet-stream")
    protected DataHandler originalDataStream;
    protected String srcName;
    protected String originalDataSrcName;
    protected String dstName;
    protected String signatureType;
    @XmlElement(required = true)
    protected String signatureLevel;
    @XmlElement(required = true)
    protected String signaturePath;
    @XmlElement(defaultValue = "false")
    protected boolean returnder;
    @XmlElement(name = "tsa_identity")
    protected TsaAuth tsaIdentity;

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
     * Recupera il valore della proprietà originalDataBinaryinput.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getOriginalDataBinaryinput() {
        return originalDataBinaryinput;
    }

    /**
     * Imposta il valore della proprietà originalDataBinaryinput.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setOriginalDataBinaryinput(byte[] value) {
        this.originalDataBinaryinput = value;
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
     * Recupera il valore della proprietà originalDataStream.
     * 
     * @return
     *     possible object is
     *     {@link DataHandler }
     *     
     */
    public DataHandler getOriginalDataStream() {
        return originalDataStream;
    }

    /**
     * Imposta il valore della proprietà originalDataStream.
     * 
     * @param value
     *     allowed object is
     *     {@link DataHandler }
     *     
     */
    public void setOriginalDataStream(DataHandler value) {
        this.originalDataStream = value;
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
     * Recupera il valore della proprietà originalDataSrcName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginalDataSrcName() {
        return originalDataSrcName;
    }

    /**
     * Imposta il valore della proprietà originalDataSrcName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginalDataSrcName(String value) {
        this.originalDataSrcName = value;
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
     * Recupera il valore della proprietà signatureType.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignatureType() {
        return signatureType;
    }

    /**
     * Imposta il valore della proprietà signatureType.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignatureType(String value) {
        this.signatureType = value;
    }

    /**
     * Recupera il valore della proprietà signatureLevel.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignatureLevel() {
        return signatureLevel;
    }

    /**
     * Imposta il valore della proprietà signatureLevel.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignatureLevel(String value) {
        this.signatureLevel = value;
    }

    /**
     * Recupera il valore della proprietà signaturePath.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignaturePath() {
        return signaturePath;
    }

    /**
     * Imposta il valore della proprietà signaturePath.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignaturePath(String value) {
        this.signaturePath = value;
    }

    /**
     * Recupera il valore della proprietà returnder.
     * 
     */
    public boolean isReturnder() {
        return returnder;
    }

    /**
     * Imposta il valore della proprietà returnder.
     * 
     */
    public void setReturnder(boolean value) {
        this.returnder = value;
    }

    /**
     * Recupera il valore della proprietà tsaIdentity.
     * 
     * @return
     *     possible object is
     *     {@link TsaAuth }
     *     
     */
    public TsaAuth getTsaIdentity() {
        return tsaIdentity;
    }

    /**
     * Imposta il valore della proprietà tsaIdentity.
     * 
     * @param value
     *     allowed object is
     *     {@link TsaAuth }
     *     
     */
    public void setTsaIdentity(TsaAuth value) {
        this.tsaIdentity = value;
    }

}
