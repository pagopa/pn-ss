
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java per credentialInfo complex type.
 *
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 *
 * <pre>
 * &lt;complexType name="credentialInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="certs" type="{http://arubasignservice.arubapec.it/}certificato" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="userid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "credentialInfo", propOrder = {
        "certs",
        "status",
        "userid"
})
public class CredentialInfo {

    @XmlElement(nillable = true)
    protected List<Certificato> certs;
    protected String status;
    protected String userid;

    /**
     * Gets the value of the certs property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the certs property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCerts().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Certificato }
     *
     *
     */
    public List<Certificato> getCerts() {
        if (certs == null) {
            certs = new ArrayList<Certificato>();
        }
        return this.certs;
    }

    /**
     * Recupera il valore della proprietà status.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getStatus() {
        return status;
    }

    /**
     * Imposta il valore della proprietà status.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Recupera il valore della proprietà userid.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUserid() {
        return userid;
    }

    /**
     * Imposta il valore della proprietà userid.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUserid(String value) {
        this.userid = value;
    }

}
