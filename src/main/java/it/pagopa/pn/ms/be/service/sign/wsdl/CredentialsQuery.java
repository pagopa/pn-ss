
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per credentials_query complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="credentials_query"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="credential_query" type="{http://arubasignservice.arubapec.it/}credentialListQuery" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "credentials_query", propOrder = {
    "credentialQuery"
})
public class CredentialsQuery {

    @XmlElement(name = "credential_query")
    protected CredentialListQuery credentialQuery;

    /**
     * Recupera il valore della proprietà credentialQuery.
     * 
     * @return
     *     possible object is
     *     {@link CredentialListQuery }
     *     
     */
    public CredentialListQuery getCredentialQuery() {
        return credentialQuery;
    }

    /**
     * Imposta il valore della proprietà credentialQuery.
     * 
     * @param value
     *     allowed object is
     *     {@link CredentialListQuery }
     *     
     */
    public void setCredentialQuery(CredentialListQuery value) {
        this.credentialQuery = value;
    }

}
