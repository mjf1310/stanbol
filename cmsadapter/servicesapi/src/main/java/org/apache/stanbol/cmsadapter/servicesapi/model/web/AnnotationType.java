//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.3-hudson-jaxb-ri-2.2.3-3- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.17 at 10:53:39 AM EEST 
//


package org.apache.stanbol.cmsadapter.servicesapi.model.web;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for annotationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="annotationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="subsumption"/>
 *     &lt;enumeration value="transitive"/>
 *     &lt;enumeration value="inverseFunctional"/>
 *     &lt;enumeration value="instanceOf"/>
 *     &lt;enumeration value="equivalentClass"/>
 *     &lt;enumeration value="disjointWith"/>
 *     &lt;enumeration value="functional"/>
 *     &lt;enumeration value="symmetric"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "annotationType")
@XmlEnum
public enum AnnotationType {

    @XmlEnumValue("subsumption")
    SUBSUMPTION("subsumption"),
    @XmlEnumValue("transitive")
    TRANSITIVE("transitive"),
    @XmlEnumValue("inverseFunctional")
    INVERSE_FUNCTIONAL("inverseFunctional"),
    @XmlEnumValue("instanceOf")
    INSTANCE_OF("instanceOf"),
    @XmlEnumValue("equivalentClass")
    EQUIVALENT_CLASS("equivalentClass"),
    @XmlEnumValue("disjointWith")
    DISJOINT_WITH("disjointWith"),
    @XmlEnumValue("functional")
    FUNCTIONAL("functional"),
    @XmlEnumValue("symmetric")
    SYMMETRIC("symmetric");
    private final String value;

    AnnotationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AnnotationType fromValue(String v) {
        for (AnnotationType c: AnnotationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
