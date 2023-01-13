
package it.pagopa.pnss.transformation.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per signer complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="signer"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="countersigners" type="{http://arubasignservice.arubapec.it/}signer" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="marks" type="{http://arubasignservice.arubapec.it/}mark" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="result" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="serialnumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signername" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "signer", propOrder = {
    "countersigners",
    "description",
    "marks",
    "result",
    "serialnumber",
    "signername"
})
public class Signer {

    @XmlElement(nillable = true)
    protected List<Signer> countersigners;
    protected String description;
    @XmlElement(nillable = true)
    protected List<Mark> marks;
    protected String result;
    protected String serialnumber;
    protected String signername;

    /**
     * Gets the value of the countersigners property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the countersigners property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCountersigners().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Signer }
     * 
     * 
     */
    public List<Signer> getCountersigners() {
        if (countersigners == null) {
            countersigners = new ArrayList<Signer>();
        }
        return this.countersigners;
    }

    /**
     * Recupera il valore della proprietà description.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Imposta il valore della proprietà description.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the marks property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the marks property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMarks().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Mark }
     * 
     * 
     */
    public List<Mark> getMarks() {
        if (marks == null) {
            marks = new ArrayList<Mark>();
        }
        return this.marks;
    }

    /**
     * Recupera il valore della proprietà result.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResult() {
        return result;
    }

    /**
     * Imposta il valore della proprietà result.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResult(String value) {
        this.result = value;
    }

    /**
     * Recupera il valore della proprietà serialnumber.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSerialnumber() {
        return serialnumber;
    }

    /**
     * Imposta il valore della proprietà serialnumber.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSerialnumber(String value) {
        this.serialnumber = value;
    }

    /**
     * Recupera il valore della proprietà signername.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignername() {
        return signername;
    }

    /**
     * Imposta il valore della proprietà signername.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignername(String value) {
        this.signername = value;
    }

}
