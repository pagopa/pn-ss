
package it.pagopa.pn.ms.be.service.sign.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per xmlSignatureParameter complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="xmlSignatureParameter"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="canonicalizedType" type="{http://arubasignservice.arubapec.it/}canonicalizedType" minOccurs="0"/&gt;
 *         &lt;element name="transforms" type="{http://arubasignservice.arubapec.it/}transform" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="type" type="{http://arubasignservice.arubapec.it/}xmlSignatureType" minOccurs="0"/&gt;
 *         &lt;element name="signatureProfile" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "xmlSignatureParameter", propOrder = {
    "canonicalizedType",
    "transforms",
    "type",
    "signatureProfile"
})
public class XmlSignatureParameter {

    @XmlSchemaType(name = "string")
    protected CanonicalizedType canonicalizedType;
    @XmlElement(nillable = true)
    protected List<Transform> transforms;
    @XmlSchemaType(name = "string")
    protected XmlSignatureType type;
    protected String signatureProfile;

    /**
     * Recupera il valore della proprietà canonicalizedType.
     * 
     * @return
     *     possible object is
     *     {@link CanonicalizedType }
     *     
     */
    public CanonicalizedType getCanonicalizedType() {
        return canonicalizedType;
    }

    /**
     * Imposta il valore della proprietà canonicalizedType.
     * 
     * @param value
     *     allowed object is
     *     {@link CanonicalizedType }
     *     
     */
    public void setCanonicalizedType(CanonicalizedType value) {
        this.canonicalizedType = value;
    }

    /**
     * Gets the value of the transforms property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the transforms property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTransforms().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Transform }
     * 
     * 
     */
    public List<Transform> getTransforms() {
        if (transforms == null) {
            transforms = new ArrayList<Transform>();
        }
        return this.transforms;
    }

    /**
     * Recupera il valore della proprietà type.
     * 
     * @return
     *     possible object is
     *     {@link XmlSignatureType }
     *     
     */
    public XmlSignatureType getType() {
        return type;
    }

    /**
     * Imposta il valore della proprietà type.
     * 
     * @param value
     *     allowed object is
     *     {@link XmlSignatureType }
     *     
     */
    public void setType(XmlSignatureType value) {
        this.type = value;
    }

    /**
     * Recupera il valore della proprietà signatureProfile.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignatureProfile() {
        return signatureProfile;
    }

    /**
     * Imposta il valore della proprietà signatureProfile.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignatureProfile(String value) {
        this.signatureProfile = value;
    }

}
