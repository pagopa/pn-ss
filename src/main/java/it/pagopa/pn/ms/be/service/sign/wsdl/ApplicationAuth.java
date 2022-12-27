
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per applicationAuth complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="applicationAuth"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="applicationpassword" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="applicationuser" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "applicationAuth", propOrder = {
    "applicationpassword",
    "applicationuser"
})
public class ApplicationAuth {

    protected String applicationpassword;
    protected String applicationuser;

    /**
     * Recupera il valore della proprietà applicationpassword.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationpassword() {
        return applicationpassword;
    }

    /**
     * Imposta il valore della proprietà applicationpassword.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setApplicationpassword(String value) {
        this.applicationpassword = value;
    }

    /**
     * Recupera il valore della proprietà applicationuser.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationuser() {
        return applicationuser;
    }

    /**
     * Imposta il valore della proprietà applicationuser.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setApplicationuser(String value) {
        this.applicationuser = value;
    }

}
