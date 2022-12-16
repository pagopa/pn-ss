
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per updateSignature complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="updateSignature"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="UpdateSignatureRequest" type="{http://arubasignservice.arubapec.it/}UpdateSignatureRequest"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "updateSignature", propOrder = {
    "updateSignatureRequest"
})
public class UpdateSignature {

    @XmlElement(name = "UpdateSignatureRequest", required = true)
    protected UpdateSignatureRequest updateSignatureRequest;

    /**
     * Recupera il valore della proprietà updateSignatureRequest.
     * 
     * @return
     *     possible object is
     *     {@link UpdateSignatureRequest }
     *     
     */
    public UpdateSignatureRequest getUpdateSignatureRequest() {
        return updateSignatureRequest;
    }

    /**
     * Imposta il valore della proprietà updateSignatureRequest.
     * 
     * @param value
     *     allowed object is
     *     {@link UpdateSignatureRequest }
     *     
     */
    public void setUpdateSignatureRequest(UpdateSignatureRequest value) {
        this.updateSignatureRequest = value;
    }

}
