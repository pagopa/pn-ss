
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pkcs7signV2 complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pkcs7signV2"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2" minOccurs="0"/&gt;
 *         &lt;element name="detached" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="returnder" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pkcs7signV2", propOrder = {
    "signRequestV2",
    "detached",
    "returnder"
})
public class Pkcs7SignV2 {

    @XmlElement(name = "SignRequestV2")
    protected SignRequestV2 signRequestV2;
    protected boolean detached;
    protected boolean returnder;

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
     * Recupera il valore della proprietà detached.
     * 
     */
    public boolean isDetached() {
        return detached;
    }

    /**
     * Imposta il valore della proprietà detached.
     * 
     */
    public void setDetached(boolean value) {
        this.detached = value;
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

}
