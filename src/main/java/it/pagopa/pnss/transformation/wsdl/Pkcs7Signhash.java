
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pkcs7signhash complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pkcs7signhash"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2" minOccurs="0"/&gt;
 *         &lt;element name="countersignature" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="excludeSigningTime" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pkcs7signhash", propOrder = {
    "signRequestV2",
    "countersignature",
    "excludeSigningTime"
})
public class Pkcs7Signhash {

    @XmlElement(name = "SignRequestV2")
    protected SignRequestV2 signRequestV2;
    protected boolean countersignature;
    protected boolean excludeSigningTime;

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
     * Recupera il valore della proprietà countersignature.
     * 
     */
    public boolean isCountersignature() {
        return countersignature;
    }

    /**
     * Imposta il valore della proprietà countersignature.
     * 
     */
    public void setCountersignature(boolean value) {
        this.countersignature = value;
    }

    /**
     * Recupera il valore della proprietà excludeSigningTime.
     * 
     */
    public boolean isExcludeSigningTime() {
        return excludeSigningTime;
    }

    /**
     * Imposta il valore della proprietà excludeSigningTime.
     * 
     */
    public void setExcludeSigningTime(boolean value) {
        this.excludeSigningTime = value;
    }

}
