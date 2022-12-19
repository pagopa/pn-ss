
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.*;


/**
 * <p>Classe Java per auth complex type.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 *
 * <pre>
 * &lt;complexType name="auth"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="delegated_domain" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="delegated_password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="delegated_user" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ext_auth_blobvalue" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="ext_auth_value" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ext_authtype" type="{http://arubasignservice.arubapec.it/}credentialsType" minOccurs="0"/&gt;
 *         &lt;element name="otpPwd" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="typeHSM" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="typeOtpAuth" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="user" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="userPWD" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auth", propOrder = {
        "delegatedDomain",
        "delegatedPassword",
        "delegatedUser",
        "extAuthBlobvalue",
        "extAuthValue",
        "extAuthtype",
        "otpPwd",
        "typeHSM",
        "typeOtpAuth",
        "user",
        "userPWD"
})
public class Auth {

    @XmlElement(name = "delegated_domain")
    protected String delegatedDomain;
    @XmlElement(name = "delegated_password")
    protected String delegatedPassword;
    @XmlElement(name = "delegated_user")
    protected String delegatedUser;
    @XmlElement(name = "ext_auth_blobvalue")
    protected byte[] extAuthBlobvalue;
    @XmlElement(name = "ext_auth_value")
    protected String extAuthValue;
    @XmlElement(name = "ext_authtype")
    @XmlSchemaType(name = "string")
    protected CredentialsType extAuthtype;
    protected String otpPwd;
    protected String typeHSM;
    protected String typeOtpAuth;
    protected String user;
    protected String userPWD;

    /**
     * Recupera il valore della proprietà delegatedDomain.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDelegatedDomain() {
        return delegatedDomain;
    }

    /**
     * Imposta il valore della proprietà delegatedDomain.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDelegatedDomain(String value) {
        this.delegatedDomain = value;
    }

    /**
     * Recupera il valore della proprietà delegatedPassword.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDelegatedPassword() {
        return delegatedPassword;
    }

    /**
     * Imposta il valore della proprietà delegatedPassword.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDelegatedPassword(String value) {
        this.delegatedPassword = value;
    }

    /**
     * Recupera il valore della proprietà delegatedUser.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDelegatedUser() {
        return delegatedUser;
    }

    /**
     * Imposta il valore della proprietà delegatedUser.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDelegatedUser(String value) {
        this.delegatedUser = value;
    }

    /**
     * Recupera il valore della proprietà extAuthBlobvalue.
     *
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getExtAuthBlobvalue() {
        return extAuthBlobvalue;
    }

    /**
     * Imposta il valore della proprietà extAuthBlobvalue.
     *
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setExtAuthBlobvalue(byte[] value) {
        this.extAuthBlobvalue = value;
    }

    /**
     * Recupera il valore della proprietà extAuthValue.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getExtAuthValue() {
        return extAuthValue;
    }

    /**
     * Imposta il valore della proprietà extAuthValue.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setExtAuthValue(String value) {
        this.extAuthValue = value;
    }

    /**
     * Recupera il valore della proprietà extAuthtype.
     *
     * @return
     *     possible object is
     *     {@link CredentialsType }
     *
     */
    public CredentialsType getExtAuthtype() {
        return extAuthtype;
    }

    /**
     * Imposta il valore della proprietà extAuthtype.
     *
     * @param value
     *     allowed object is
     *     {@link CredentialsType }
     *
     */
    public void setExtAuthtype(CredentialsType value) {
        this.extAuthtype = value;
    }

    /**
     * Recupera il valore della proprietà otpPwd.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getOtpPwd() {
        return otpPwd;
    }

    /**
     * Imposta il valore della proprietà otpPwd.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setOtpPwd(String value) {
        this.otpPwd = value;
    }

    /**
     * Recupera il valore della proprietà typeHSM.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTypeHSM() {
        return typeHSM;
    }

    /**
     * Imposta il valore della proprietà typeHSM.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTypeHSM(String value) {
        this.typeHSM = value;
    }

    /**
     * Recupera il valore della proprietà typeOtpAuth.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTypeOtpAuth() {
        return typeOtpAuth;
    }

    /**
     * Imposta il valore della proprietà typeOtpAuth.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTypeOtpAuth(String value) {
        this.typeOtpAuth = value;
    }

    /**
     * Recupera il valore della proprietà user.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUser() {
        return user;
    }

    /**
     * Imposta il valore della proprietà user.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUser(String value) {
        this.user = value;
    }

    /**
     * Recupera il valore della proprietà userPWD.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUserPWD() {
        return userPWD;
    }

    /**
     * Imposta il valore della proprietà userPWD.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUserPWD(String value) {
        this.userPWD = value;
    }

}
