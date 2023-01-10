
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per signRequestV2 complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="signRequestV2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="binaryinput" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="certID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dstName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="identity" type="{http://arubasignservice.arubapec.it/}auth"/&gt;
 *         &lt;element name="notify_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="notifymail" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="profile" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="requiredmark" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="session_id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signingTime" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="srcName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="stream" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="transport" type="{http://arubasignservice.arubapec.it/}typeTransport"/&gt;
 *         &lt;element name="tsa_identity" type="{http://arubasignservice.arubapec.it/}tsaAuth" minOccurs="0"/&gt;
 *         &lt;element name="signatureLevel" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signRequestV2", propOrder = {
    "binaryinput",
    "certID",
    "dstName",
    "identity",
    "notifyId",
    "notifymail",
    "profile",
    "requiredmark",
    "sessionId",
    "signingTime",
    "srcName",
    "stream",
    "transport",
    "tsaIdentity",
    "signatureLevel"
})
public class SignRequestV2 {

    protected byte[] binaryinput;
    @XmlElement(required = true)
    protected String certID;
    protected String dstName;
    @XmlElement(required = true)
    protected Auth identity;
    @XmlElement(name = "notify_id")
    protected String notifyId;
    protected String notifymail;
    protected String profile;
    protected boolean requiredmark;
    @XmlElement(name = "session_id")
    protected String sessionId;
    protected String signingTime;
    protected String srcName;
    @XmlMimeType("application/octet-stream")
    protected DataHandler stream;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected TypeTransport transport;
    @XmlElement(name = "tsa_identity")
    protected TsaAuth tsaIdentity;
    protected String signatureLevel;

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
     * Recupera il valore della proprietà notifyId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotifyId() {
        return notifyId;
    }

    /**
     * Imposta il valore della proprietà notifyId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotifyId(String value) {
        this.notifyId = value;
    }

    /**
     * Recupera il valore della proprietà notifymail.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotifymail() {
        return notifymail;
    }

    /**
     * Imposta il valore della proprietà notifymail.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotifymail(String value) {
        this.notifymail = value;
    }

    /**
     * Recupera il valore della proprietà profile.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Imposta il valore della proprietà profile.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProfile(String value) {
        this.profile = value;
    }

    /**
     * Recupera il valore della proprietà requiredmark.
     * 
     */
    public boolean isRequiredmark() {
        return requiredmark;
    }

    /**
     * Imposta il valore della proprietà requiredmark.
     * 
     */
    public void setRequiredmark(boolean value) {
        this.requiredmark = value;
    }

    /**
     * Recupera il valore della proprietà sessionId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Imposta il valore della proprietà sessionId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSessionId(String value) {
        this.sessionId = value;
    }

    /**
     * Recupera il valore della proprietà signingTime.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSigningTime() {
        return signingTime;
    }

    /**
     * Imposta il valore della proprietà signingTime.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSigningTime(String value) {
        this.signingTime = value;
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

}
