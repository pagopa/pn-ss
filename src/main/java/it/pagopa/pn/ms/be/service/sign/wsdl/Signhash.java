
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per signhash complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="signhash"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignHashRequest" type="{http://arubasignservice.arubapec.it/}signHashRequest" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signhash", propOrder = {
    "signHashRequest"
})
public class Signhash {

    @XmlElement(name = "SignHashRequest")
    protected SignHashRequest signHashRequest;

    /**
     * Recupera il valore della proprietà signHashRequest.
     * 
     * @return
     *     possible object is
     *     {@link SignHashRequest }
     *     
     */
    public SignHashRequest getSignHashRequest() {
        return signHashRequest;
    }

    /**
     * Imposta il valore della proprietà signHashRequest.
     * 
     * @param value
     *     allowed object is
     *     {@link SignHashRequest }
     *     
     */
    public void setSignHashRequest(SignHashRequest value) {
        this.signHashRequest = value;
    }

}
