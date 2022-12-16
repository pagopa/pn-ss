
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per xmlsignature complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="xmlsignature"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2" minOccurs="0"/&gt;
 *         &lt;element name="parameter" type="{http://arubasignservice.arubapec.it/}xmlSignatureParameter" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "xmlsignature", propOrder = {
    "signRequestV2",
    "parameter"
})
public class Xmlsignature {

    @XmlElement(name = "SignRequestV2")
    protected SignRequestV2 signRequestV2;
    protected XmlSignatureParameter parameter;

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
     * Recupera il valore della proprietà parameter.
     * 
     * @return
     *     possible object is
     *     {@link XmlSignatureParameter }
     *     
     */
    public XmlSignatureParameter getParameter() {
        return parameter;
    }

    /**
     * Imposta il valore della proprietà parameter.
     * 
     * @param value
     *     allowed object is
     *     {@link XmlSignatureParameter }
     *     
     */
    public void setParameter(XmlSignatureParameter value) {
        this.parameter = value;
    }

}
