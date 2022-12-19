
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.*;


/**
 * <p>Classe Java per pdfsignatureV2 complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pdfsignatureV2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2" minOccurs="0"/&gt;
 *         &lt;element name="Apparence" type="{http://arubasignservice.arubapec.it/}pdfSignApparence" minOccurs="0"/&gt;
 *         &lt;element name="fieldName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="pdfprofile" type="{http://arubasignservice.arubapec.it/}pdfProfile" minOccurs="0"/&gt;
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="dict_signed_attributes" type="{http://arubasignservice.arubapec.it/}dictionarySignedAttributes" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pdfsignatureV2", propOrder = {
    "signRequestV2",
    "apparence",
    "fieldName",
    "pdfprofile",
    "password",
    "dictSignedAttributes"
})
public class PdfsignatureV2 {

    @XmlElement(name = "SignRequestV2")
    protected SignRequestV2 signRequestV2;
    @XmlElement(name = "Apparence")
    protected PdfSignApparence apparence;
    protected String fieldName;
    @XmlSchemaType(name = "string")
    protected PdfProfile pdfprofile;
    protected String password;
    @XmlElement(name = "dict_signed_attributes")
    protected DictionarySignedAttributes dictSignedAttributes;

    /**
     * Recupera il valore della proprietà signRequestV2.
     * 
     * @return
     *     possible object is
     *     {@link SignRequestV2 }
     *     
     */
    public SignRequestV2 getSignRequestV2() {
        return signRequestV2;
    }

    /**
     * Imposta il valore della proprietà signRequestV2.
     * 
     * @param value
     *     allowed object is
     *     {@link SignRequestV2 }
     *     
     */
    public void setSignRequestV2(SignRequestV2 value) {
        this.signRequestV2 = value;
    }

    /**
     * Recupera il valore della proprietà apparence.
     * 
     * @return
     *     possible object is
     *     {@link PdfSignApparence }
     *     
     */
    public PdfSignApparence getApparence() {
        return apparence;
    }

    /**
     * Imposta il valore della proprietà apparence.
     * 
     * @param value
     *     allowed object is
     *     {@link PdfSignApparence }
     *     
     */
    public void setApparence(PdfSignApparence value) {
        this.apparence = value;
    }

    /**
     * Recupera il valore della proprietà fieldName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Imposta il valore della proprietà fieldName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFieldName(String value) {
        this.fieldName = value;
    }

    /**
     * Recupera il valore della proprietà pdfprofile.
     * 
     * @return
     *     possible object is
     *     {@link PdfProfile }
     *     
     */
    public PdfProfile getPdfprofile() {
        return pdfprofile;
    }

    /**
     * Imposta il valore della proprietà pdfprofile.
     * 
     * @param value
     *     allowed object is
     *     {@link PdfProfile }
     *     
     */
    public void setPdfprofile(PdfProfile value) {
        this.pdfprofile = value;
    }

    /**
     * Recupera il valore della proprietà password.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Imposta il valore della proprietà password.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Recupera il valore della proprietà dictSignedAttributes.
     * 
     * @return
     *     possible object is
     *     {@link DictionarySignedAttributes }
     *     
     */
    public DictionarySignedAttributes getDictSignedAttributes() {
        return dictSignedAttributes;
    }

    /**
     * Imposta il valore della proprietà dictSignedAttributes.
     * 
     * @param value
     *     allowed object is
     *     {@link DictionarySignedAttributes }
     *     
     */
    public void setDictSignedAttributes(DictionarySignedAttributes value) {
        this.dictSignedAttributes = value;
    }

}
