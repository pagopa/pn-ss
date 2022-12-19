
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per credentials_queryResponse complex type.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 *
 * <pre>
 * &lt;complexType name="credentials_queryResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="return" type="{http://arubasignservice.arubapec.it/}credentialListReturn" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "credentials_queryResponse", propOrder = {
        "_return"
})
public class CredentialsQueryResponse {

    @XmlElement(name = "return")
    protected CredentialListReturn _return;

    /**
     * Recupera il valore della proprietà return.
     *
     * @return
     *     possible object is
     *     {@link CredentialListReturn }
     *
     */
    public CredentialListReturn getReturn() {
        return _return;
    }

    /**
     * Imposta il valore della proprietà return.
     *
     * @param value
     *     allowed object is
     *     {@link CredentialListReturn }
     *
     */
    public void setReturn(CredentialListReturn value) {
        this._return = value;
    }

}
