
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per jwsSignature complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="jwsSignature"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequestV2" type="{http://arubasignservice.arubapec.it/}signRequestV2"/&gt;
 *         &lt;element name="parameter" type="{http://arubasignservice.arubapec.it/}jwsSignatureParameter" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "jwsSignature", propOrder = {
    "signRequestV2",
    "parameter"
})
public class JwsSignature {

    @XmlElement(name = "SignRequestV2", required = true)
    protected SignRequestV2 signRequestV2;
    protected JwsSignatureParameter parameter;

    /**
     * Recupera il valore della proprietà signRequestV2.
     * 
     * @return
     *     possible object is
     *     {@link SignRequestV2 }
     *     
     */
    public SignRequestV2 getSignRequestV2() {
        return signRequestV2;
    }

    /**
     * Imposta il valore della proprietà signRequestV2.
     * 
     * @param value
     *     allowed object is
     *     {@link SignRequestV2 }
     *     
     */
    public void setSignRequestV2(SignRequestV2 value) {
        this.signRequestV2 = value;
    }

    /**
     * Recupera il valore della proprietà parameter.
     * 
     * @return
     *     possible object is
     *     {@link JwsSignatureParameter }
     *     
     */
    public JwsSignatureParameter getParameter() {
        return parameter;
    }

    /**
     * Imposta il valore della proprietà parameter.
     * 
     * @param value
     *     allowed object is
     *     {@link JwsSignatureParameter }
     *     
     */
    public void setParameter(JwsSignatureParameter value) {
        this.parameter = value;
    }

}
