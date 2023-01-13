
package it.pagopa.pnss.transformation.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pkcs7signhash_multiple complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pkcs7signhash_multiple"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="identity" type="{http://arubasignservice.arubapec.it/}auth" minOccurs="0"/&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="countersignature" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pkcs7signhash_multiple", propOrder = {
    "identity",
    "signRequestV2",
    "countersignature"
})
public class Pkcs7SignhashMultiple {

    protected Auth identity;
    @XmlElement(name = "SignRequestV2", nillable = true)
    protected List<SignRequestV2> signRequestV2;
    protected boolean countersignature;

    /**
     * Recupera il valore della proprietà identity.
     * 
     * @return
     *     possible object is
     *     {@link Auth }
     *     
     */
    public Auth getIdentity() {
        return identity;
    }

    /**
     * Imposta il valore della proprietà identity.
     * 
     * @param value
     *     allowed object is
     *     {@link Auth }
     *     
     */
    public void setIdentity(Auth value) {
        this.identity = value;
    }

    /**
     * Gets the value of the signRequestV2 property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the signRequestV2 property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSignRequestV2().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SignRequestV2 }
     * 
     * 
     */
    public List<SignRequestV2> getSignRequestV2() {
        if (signRequestV2 == null) {
            signRequestV2 = new ArrayList<SignRequestV2>();
        }
        return this.signRequestV2;
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

}
