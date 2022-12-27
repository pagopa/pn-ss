
package it.pagopa.pn.ms.be.service.sign.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per credentialListReturn complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="credentialListReturn"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://arubasignservice.arubapec.it/}gwReturn"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="credentials" type="{http://arubasignservice.arubapec.it/}credentialInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "credentialListReturn", propOrder = {
    "credentials"
})
public class CredentialListReturn
    extends GwReturn
{

    @XmlElement(nillable = true)
    protected List<CredentialInfo> credentials;

    /**
     * Gets the value of the credentials property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the credentials property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCredentials().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CredentialInfo }
     * 
     * 
     */
    public List<CredentialInfo> getCredentials() {
        if (credentials == null) {
            credentials = new ArrayList<CredentialInfo>();
        }
        return this.credentials;
    }

}
